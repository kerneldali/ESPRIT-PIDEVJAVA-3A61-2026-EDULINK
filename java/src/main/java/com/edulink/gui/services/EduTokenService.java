package com.edulink.gui.services;

import com.edulink.gui.util.Web3Config;
import com.edulink.gui.util.MyConnection;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Keys;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.*;
import org.web3j.protocol.http.HttpService;
import org.web3j.utils.Convert;
import org.web3j.utils.Numeric;
import okhttp3.OkHttpClient;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import com.edulink.gui.util.SessionManager;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint256;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.security.Security;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

/**
 * Handles all Ethereum / Sepolia interactions for the EduToken economy.
 * Uses Web3j to call the deployed EduToken.sol contract.
 *
 * Before use:
 *   1. Deploy EduToken.sol on Sepolia
 *   2. Fill in Web3Config.CONTRACT_ADDRESS and Web3Config.ADMIN_PRIVATE_KEY
 */
public class EduTokenService {

    static {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    // ERC-20 function selectors (keccak256 of signature, first 4 bytes)
    private static final String SIG_BALANCE_OF         = "0x70a08231"; // balanceOf(address)
    private static final String SIG_BUY_TOKENS         = "0xec8ac4d8"; // buyTokens() — no args
    private static final String SIG_DISTRIBUTE_BOUNTY  = "0x5a1b9aa4"; // distributeBounty(address,address,uint256)
    private static final String SIG_MINT               = "0x40c10f19"; // mint(address,uint256)
    private static final String SIG_TRANSFER           = "0xa9059cbb"; // transfer(address,uint256)
    private static final String SIG_TOTAL_SUPPLY       = "0x18160ddd"; // totalSupply()

    private final Web3j web3;
    private Connection dbCnx;

    public EduTokenService() {
        // Increase timeout to 30 seconds for slower public RPCs
        OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .build();
        this.web3   = Web3j.build(new HttpService(Web3Config.RPC_URL, client));
        this.dbCnx  = MyConnection.getInstance().getCnx();
        System.out.println("[EduTokenService] Initialized for contract: " + Web3Config.CONTRACT_ADDRESS);
        ensureColumnsExist();
    }

    private void ensureColumnsExist() {
        if (dbCnx == null) {
            this.dbCnx = MyConnection.getInstance().getCnx();
            if (dbCnx == null) return;
        }
        try (Statement st = dbCnx.createStatement()) {
            // Check if columns exist by selecting them
            try {
                st.executeQuery("SELECT eth_wallet_address, eth_private_key FROM user LIMIT 1").close();
            } catch (SQLException e) {
                // Columns likely missing, try adding them
                try { st.execute("ALTER TABLE user ADD COLUMN eth_wallet_address VARCHAR(255)"); } catch (Exception ignored) {}
                try { st.execute("ALTER TABLE user ADD COLUMN eth_private_key VARCHAR(255)"); } catch (Exception ignored) {}
                System.out.println("[EduTokenService] Web3 columns added to user table.");
            }
            // Ensure any admin user has "unlimited" credits and the CORRECT fixed private key from Web3Config
            String adminAddr = org.web3j.crypto.Credentials.create(Web3Config.ADMIN_PRIVATE_KEY).getAddress();
            String updateAdmin = "UPDATE user SET wallet_balance = 999999, eth_private_key = ?, eth_wallet_address = ? " +
                                "WHERE roles LIKE '%ROLE_ADMIN%'";
            try (PreparedStatement pups = dbCnx.prepareStatement(updateAdmin)) {
                pups.setString(1, Web3Config.ADMIN_PRIVATE_KEY);
                pups.setString(2, adminAddr);
                pups.executeUpdate();
                System.out.println("[EduTokenService] Admin wallet synced with Web3Config: " + adminAddr);
            }
        } catch (Exception e) {
            System.err.println("[EduTokenService] Column/Admin check failed: " + e.getMessage());
        }
    }

    /** Generates a new Ethereum wallet and saves it to the user record in DB */
    public String[] generateAndSaveWallet(int userId) {
        try {
            ECKeyPair keyPair = Keys.createEcKeyPair();
            String privateKey = Numeric.toHexStringNoPrefix(keyPair.getPrivateKey());
            String address    = "0x" + Keys.getAddress(keyPair);

            if (dbCnx != null) {
                String sql = "UPDATE user SET eth_wallet_address=?, eth_private_key=? WHERE id=?";
                try (PreparedStatement ps = dbCnx.prepareStatement(sql)) {
                    ps.setString(1, address);
                    ps.setString(2, privateKey);
                    ps.setInt(3, userId);
                    ps.executeUpdate();
                }

                // ✨ NEW: Welcome bonus — Mint 1000 EDU tokens for the new user!
                // This happens on-chain so it's "real" testnet value
                new Thread(() -> {
                    try {
                        System.out.println("[EduTokenService] Minting 1000 EDU bonus for user " + userId);
                        adminMint(address, 1000);
                        // Also update DB balance so it's immediately visible
                        try (PreparedStatement ups = dbCnx.prepareStatement("UPDATE user SET wallet_balance = wallet_balance + 1000 WHERE id = ?")) {
                            ups.setInt(1, userId);
                            ups.executeUpdate();
                        }
                    } catch (Exception e) {
                        System.err.println("[EduTokenService] Welcome bonus failed: " + e.getMessage());
                    }
                }).start();
            }
            return new String[]{address, privateKey};
        } catch (Exception e) {
            System.err.println("[EduTokenService] Wallet generation failed!");
            e.printStackTrace();
            return null;
        }
    }

    /** Returns true if the RPC endpoint is reachable */
    public boolean isConnected() {
        try {
            web3.ethBlockNumber().send();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // ─────────────────────────────────────────────────────
    // READ — on-chain balance (in whole EDU tokens, 18 decimals stripped)
    // ─────────────────────────────────────────────────────
    public BigDecimal getBalance(String ethAddress) {
        try {
            String data = SIG_BALANCE_OF + padAddress(ethAddress);
            EthCall call = web3.ethCall(
                Transaction.createEthCallTransaction(null, Web3Config.CONTRACT_ADDRESS, data),
                DefaultBlockParameterName.LATEST
            ).send();
            
            if (call.hasError()) {
                System.err.println("[EduTokenService] RPC Error: " + call.getError().getMessage());
                return BigDecimal.ZERO;
            }
            String rawVal = call.getValue();
            if (rawVal == null || rawVal.equals("0x")) {
                System.err.println("[EduTokenService] Empty response for balance at " + ethAddress);
                return BigDecimal.ZERO;
            }
            
            List<TypeReference<?>> outputParameters = new ArrayList<>();
            outputParameters.add(new TypeReference<Uint256>() {});
            List<Type> results = FunctionReturnDecoder.decode(rawVal, (List) outputParameters);
            
            if (results.isEmpty()) {
                System.err.println("[EduTokenService] Decoding failed for " + rawVal);
                return BigDecimal.ZERO;
            }
            BigInteger raw = (BigInteger) results.get(0).getValue();
            BigDecimal balance = new BigDecimal(raw).movePointLeft(18);
            System.out.println("[EduTokenService] Address " + ethAddress + " balance: " + balance + " EDU");
            
            // Sync with DB
            updateDbBalanceByAddress(ethAddress, balance.doubleValue());
            
            return balance.setScale(4, java.math.RoundingMode.DOWN);
        } catch (Exception e) {
            System.err.println("[EduTokenService] getBalance error: " + e.getMessage());
            return BigDecimal.ZERO;
        }
    }

    private void updateDbBalanceByAddress(String address, double amount) {
        if (dbCnx == null) return;
        String sql = "UPDATE user SET wallet_balance = ? WHERE eth_wallet_address = ?";
        try (PreparedStatement ps = dbCnx.prepareStatement(sql)) {
            ps.setDouble(1, amount);
            ps.setString(2, address);
            ps.executeUpdate();

            // Sync with current session if this is the logged-in user
            com.edulink.gui.models.User currentUser = SessionManager.getCurrentUser();
            if (currentUser != null && address.equalsIgnoreCase(currentUser.getEthWalletAddress())) {
                currentUser.setWalletBalance(amount);
                System.out.println("[EduTokenService] Session balance synchronized for " + currentUser.getEmail());
            }
        } catch (Exception e) {
            System.err.println("[EduTokenService] DB Sync failed: " + e.getMessage());
        }
    }

    public BigDecimal getTotalSupply() {
        try {
            EthCall call = web3.ethCall(
                Transaction.createEthCallTransaction(
                    null, Web3Config.CONTRACT_ADDRESS, SIG_TOTAL_SUPPLY),
                DefaultBlockParameterName.LATEST
            ).send();
            
            if (call.hasError() || call.getValue() == null || call.getValue().equals("0x")) return BigDecimal.ZERO;
            
            List<TypeReference<?>> outputParameters = new ArrayList<>();
            outputParameters.add(new TypeReference<Uint256>() {});
            List<Type> results = FunctionReturnDecoder.decode(call.getValue(), (List) outputParameters);
                
            if (results.isEmpty()) return BigDecimal.ZERO;
            BigInteger raw = (BigInteger) results.get(0).getValue();
            return new BigDecimal(raw).movePointLeft(18).setScale(2, java.math.RoundingMode.DOWN);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    // ─────────────────────────────────────────────────────
    // WRITE — Buy tokens (user sends ETH amount)
    // ─────────────────────────────────────────────────────
    /**
     * Calls buyTokens() on the contract.
     * @param ethAmountInEther  how much ETH to send (e.g. "0.01")
     * @return tx hash or null on failure
     */
    public String buyTokens(String buyerAddress, String buyerPrivateKey, String ethAmountInEther) {
        try {
            Credentials creds = Credentials.create(buyerPrivateKey);
            BigInteger nonce  = getNonce(buyerAddress);
            BigInteger gasPrice = getGasPrice();
            BigInteger gasLimit = BigInteger.valueOf(100_000);
            BigInteger value    = Convert.toWei(ethAmountInEther, Convert.Unit.ETHER).toBigInteger();

            // buyTokens() has no arguments
            RawTransaction tx = RawTransaction.createTransaction(
                nonce, gasPrice, gasLimit,
                Web3Config.CONTRACT_ADDRESS,
                value,
                SIG_BUY_TOKENS
            );
            byte[] signed = TransactionEncoder.signMessage(tx, Web3Config.CHAIN_ID, creds);
            EthSendTransaction sent = web3.ethSendRawTransaction(Numeric.toHexString(signed)).send();

            if (sent.hasError()) {
                System.err.println("[EduTokenService] buyTokens error: " + sent.getError().getMessage());
                return null;
            }
            String txHash = sent.getTransactionHash();
            logTransaction(null, buyerAddress, value.longValue(), "BUY", txHash);
            return txHash;
        } catch (Exception e) {
            System.err.println("[EduTokenService] buyTokens exception: " + e.getMessage());
            return null;
        }
    }

    // ─────────────────────────────────────────────────────
    // WRITE — Transfer tokens (user to user)
    // ─────────────────────────────────────────────────────
    public String transferTokens(String fromAddress, String fromPrivateKey,
                                  String toAddress, long amount) {
        try {
            Credentials creds = Credentials.create(fromPrivateKey);
            BigInteger amountWei = BigInteger.valueOf(amount).multiply(BigInteger.TEN.pow(18));
            String data = SIG_TRANSFER + padAddress(toAddress) + padUint256(amountWei);

            String txHash = sendTransaction(creds, data, BigInteger.ZERO, 100_000);
            if (txHash != null) logTransaction(fromAddress, toAddress, amount, "TRANSFER", txHash);
            return txHash;
        } catch (Exception e) {
            System.err.println("[EduTokenService] transferTokens exception: " + e.getMessage());
            return null;
        }
    }

    // ─────────────────────────────────────────────────────
    // WRITE — Distribute bounty (admin signs, student → tutor)
    // ─────────────────────────────────────────────────────
    public String distributeBounty(String studentAddress, String tutorAddress,
                                    long amount, int sessionId) {
        try {
            Credentials adminCreds = Credentials.create(Web3Config.ADMIN_PRIVATE_KEY);
            BigInteger amountWei = BigInteger.valueOf(amount).multiply(BigInteger.TEN.pow(18));
            String data = SIG_DISTRIBUTE_BOUNTY
                + padAddress(studentAddress)
                + padAddress(tutorAddress)
                + padUint256(amountWei);

            String txHash = sendTransaction(adminCreds, data, BigInteger.ZERO, 150_000);
            if (txHash != null) {
                logTransaction(studentAddress, tutorAddress, amount, "BOUNTY", txHash);
                System.out.println("[EduTokenService] Bounty distributed on-chain: " + txHash);
            }
            return txHash;
        } catch (Exception e) {
            System.err.println("[EduTokenService] distributeBounty exception: " + e.getMessage());
            return null;
        }
    }

    // ─────────────────────────────────────────────────────
    // WRITE — Admin mint
    // ─────────────────────────────────────────────────────
    public String adminMint(String toAddress, long amount) {
        try {
            Credentials adminCreds = Credentials.create(Web3Config.ADMIN_PRIVATE_KEY);
            BigInteger amountWei = BigInteger.valueOf(amount).multiply(BigInteger.TEN.pow(18));
            String data = SIG_MINT + padAddress(toAddress) + padUint256(amountWei);

            String txHash = sendTransaction(adminCreds, data, BigInteger.ZERO, 100_000);
            if (txHash != null) logTransaction(null, toAddress, amount, "MINT", txHash);
            return txHash;
        } catch (Exception e) {
            System.err.println("[EduTokenService] adminMint exception: " + e.getMessage());
            return null;
        }
    }

    // ─────────────────────────────────────────────────────
    // DB — Transaction log
    // ─────────────────────────────────────────────────────
    private void logTransaction(String from, String to, long amount, String type, String txHash) {
        if (dbCnx == null) return;
        try {
            PreparedStatement ps = dbCnx.prepareStatement(
                "INSERT INTO token_transaction (from_user_id, to_user_id, amount, tx_hash, tx_type) VALUES (?,?,?,?,?)");
            ps.setObject(1, resolveUserId(from));
            ps.setObject(2, resolveUserId(to));
            ps.setLong(3, amount);
            ps.setString(4, txHash);
            ps.setString(5, type);
            ps.executeUpdate();
        } catch (Exception e) {
            System.err.println("[EduTokenService] logTransaction error: " + e.getMessage());
        }
    }

    public List<TokenTransaction> getTransactionHistory(int userId) {
        List<TokenTransaction> list = new ArrayList<>();
        if (dbCnx == null) return list;
        String sql = "SELECT * FROM token_transaction WHERE from_user_id=? OR to_user_id=? ORDER BY created_at DESC LIMIT 50";
        try (PreparedStatement ps = dbCnx.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new TokenTransaction(
                    rs.getInt("id"),
                    rs.getObject("from_user_id") != null ? rs.getInt("from_user_id") : -1,
                    rs.getObject("to_user_id")   != null ? rs.getInt("to_user_id")   : -1,
                    rs.getLong("amount"),
                    rs.getString("tx_hash"),
                    rs.getString("tx_type"),
                    rs.getTimestamp("created_at")
                ));
            }
        } catch (Exception e) {
            System.err.println("[EduTokenService] getHistory error: " + e.getMessage());
        }
        return list;
    }

    // ─────────────────────────────────────────────────────
    // Internal helpers
    // ─────────────────────────────────────────────────────
    private String sendTransaction(Credentials creds, String data,
                                    BigInteger value, long gasLimitLong) throws Exception {
        String from   = creds.getAddress();
        // Use PENDING to avoid nonce collisions when multiple transactions are sent in row
        EthGetTransactionCount cnt = web3.ethGetTransactionCount(
            from, DefaultBlockParameterName.PENDING).send();
        BigInteger nonce = cnt.getTransactionCount();
        
        BigInteger gasPrice = web3.ethGasPrice().send().getGasPrice();
        BigInteger gasLimit = BigInteger.valueOf(gasLimitLong);

        RawTransaction tx = RawTransaction.createTransaction(
            nonce, gasPrice, gasLimit, Web3Config.CONTRACT_ADDRESS, value, data);
        byte[] signed = TransactionEncoder.signMessage(tx, Web3Config.CHAIN_ID, creds);
        EthSendTransaction sent = web3.ethSendRawTransaction(Numeric.toHexString(signed)).send();
        
        if (sent.hasError()) {
            System.err.println("[EduTokenService] sendTx error: " + sent.getError().getMessage() + " (Nonce was: " + nonce + ")");
            return null;
        }
        return sent.getTransactionHash();
    }

    private BigInteger getNonce(String address) throws Exception {
        EthGetTransactionCount cnt = web3.ethGetTransactionCount(
            address, DefaultBlockParameterName.LATEST).send();
        return cnt.getTransactionCount();
    }

    private BigInteger getGasPrice() throws Exception {
        return web3.ethGasPrice().send().getGasPrice();
    }

    /** ABI-encode a 20-byte address to 32 bytes */
    private String padAddress(String addr) {
        String clean = addr.startsWith("0x") ? addr.substring(2) : addr;
        return "000000000000000000000000" + clean.toLowerCase();
    }

    /** ABI-encode a BigInteger as 32-byte (64-char hex) */
    private String padUint256(BigInteger val) {
        String hex = val.toString(16);
        return String.format("%64s", hex).replace(' ', '0');
    }

    private Integer resolveUserId(String ethAddress) {
        if (ethAddress == null || dbCnx == null) return null;
        try (PreparedStatement ps = dbCnx.prepareStatement(
                "SELECT id FROM user WHERE eth_wallet_address=?")) {
            ps.setString(1, ethAddress);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt("id");
        } catch (Exception ignored) {}
        return null;
    }

    // ─────────────────────────────────────────────────────
    // DTO
    // ─────────────────────────────────────────────────────
    public static class TokenTransaction {
        public final int id, fromUserId, toUserId;
        public final long amount;
        public final String txHash, txType;
        public final java.sql.Timestamp createdAt;

        public TokenTransaction(int id, int fromUserId, int toUserId, long amount,
                                 String txHash, String txType, java.sql.Timestamp createdAt) {
            this.id         = id;
            this.fromUserId = fromUserId;
            this.toUserId   = toUserId;
            this.amount     = amount;
            this.txHash     = txHash;
            this.txType     = txType;
            this.createdAt  = createdAt;
        }

        public String getEtherscanUrl() {
            if (txHash == null || txHash.isBlank()) return null;
            return "https://sepolia.etherscan.io/tx/" + txHash;
        }
    }
}
