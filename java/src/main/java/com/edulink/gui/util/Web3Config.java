package com.edulink.gui.util;

/**
 * Stores configuration for the Ethereum Sepolia testnet integration.
 *
 * SETUP:
 *  1. Deploy EduToken.sol on Sepolia
 *  2. Paste your deployed contract address in CONTRACT_ADDRESS
 *  3. Paste your admin Sepolia wallet private key in ADMIN_PRIVATE_KEY
 *  4. Optionally swap the RPC URL for your own Alchemy/Infura endpoint
 */
public class Web3Config {

    // Sepolia public RPC — swap for your own Alchemy/Infura URL for reliability
    public static final String RPC_URL =
        "https://rpc.sepolia.org";

    // ⚠ Replace after deploying EduToken.sol on Sepolia
    public static final String CONTRACT_ADDRESS =
        "0x0000000000000000000000000000000000000000";

    // ⚠ Admin Sepolia-only private key (no real ETH here!)
    // Used only for distributeBounty() calls signed server-side
    public static final String ADMIN_PRIVATE_KEY =
        "0xYOUR_SEPOLIA_ADMIN_PRIVATE_KEY_HERE";

    // Token exchange rate: how many EDU tokens per 1 ETH
    public static final long TOKENS_PER_ETH = 1_000L;

    // Sepolia chain ID
    public static final long CHAIN_ID = 11155111L;

    private Web3Config() {}
}
