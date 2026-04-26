package com.edulink.gui.services;

import com.edulink.gui.util.Web3Config;
import com.edulink.gui.util.MyConnection;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.*;
import org.web3j.protocol.http.HttpService;
import org.web3j.utils.Convert;
import org.web3j.utils.Numeric;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles all Ethereum / Sepolia interactions for the EduToken economy.
 * Uses Web3j to call the deployed EduToken.sol contract.
 *
 * Before use:
 *   1. Deploy EduToken.sol on Sepolia
 *   2. Fill in Web3Config.CONTRACT_ADDRESS and Web3Config.ADMIN_PRIVATE_KEY
 */
public class EduTokenService {

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
        this.web3   = Web3j.build(new HttpService(Web3Config.RPC_URL));
        this.dbCnx  = MyConnection.getInstance().getCnx();
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
                Transaction.createEthCallTransaction(ethAddress, Web3Config.CONTRACT_ADDRESS, data),
                DefaultBlockParameterName.LATEST
            ).send();
            if (call.hasError()) return BigDecimal.ZERO;
            BigInteger raw = Numeric.decodeQuantity(call.getValue());
            return new BigDecimal(raw).movePointLeft(18).setScale(4, BigDecimal.ROUND_DOWN);
        } catch (Exception e) {
            System.err.println("[EduTokenService] getBalance error: " + e.getMessage());
            return BigDecimal.ZERO;
        }
    }

    public BigDecimal getTotalSupply() {
        try {
            EthCall call = web3.ethCall(
                Transaction.createEthCallTransaction(
                    Web3Config.CONTRACT_ADDRESS, Web3Config.CONTRACT_ADDRESS, SIG_TOTAL_SUPPLY),
                DefaultBlockParameterName.LATEST
            ).send();
            BigInteger raw = Numeric.decodeQuantity(call.getValue());
            return new BigDecimal(raw).movePointLeft(18).setScale(2, BigDecimal.ROUND_DOWN);
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
        BigInteger nonce    = getNonce(from);
        BigInteger gasPrice = getGasPrice();
        BigInteger gasLimit = BigInteger.valueOf(gasLimitLong);

        RawTransaction tx = RawTransaction.createTransaction(
            nonce, gasPrice, gasLimit, Web3Config.CONTRACT_ADDRESS, value, data);
        byte[] signed = TransactionEncoder.signMessage(tx, Web3Config.CHAIN_ID, creds);
        EthSendTransaction sent = web3.ethSendRawTransaction(Numeric.toHexString(signed)).send();
        if (sent.hasError()) {
            System.err.println("[EduTokenService] sendTx error: " + sent.getError().getMessage());
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
        return String.format("%064s", hex).replace(' ', '0');
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
