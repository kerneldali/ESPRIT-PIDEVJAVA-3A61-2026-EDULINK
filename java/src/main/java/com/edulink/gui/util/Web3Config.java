package com.edulink.gui.util;

/**
 * Stores configuration for the Ethereum Sepolia testnet integration.
 *
 * SETUP:
 * 1. Deploy EduToken.sol on Sepolia
 * 2. Paste your deployed contract address in CONTRACT_ADDRESS
 * 3. Paste your admin Sepolia wallet private key in ADMIN_PRIVATE_KEY
 * 4. Optionally swap the RPC URL for your own Alchemy/Infura endpoint
 */
public class Web3Config {

    // Sepolia public RPC — updated to a more stable public endpoint
    public static final String RPC_URL = "https://ethereum-sepolia-rpc.publicnode.com";

    // ⚠ Replace after deploying EduToken.sol on Sepolia
    public static final String CONTRACT_ADDRESS = "0x59A693aBAF46FF430C265888a26023B2f4308560";

    // ⚠ Admin Sepolia-only private key (no real ETH here!)
    // Used only for distributeBounty() calls signed server-side
    public static final String ADMIN_PRIVATE_KEY = "8334a7d35167aef19c1f24e6d65cd353f08cf343b4505537c8100b122f31845f";

    // Token exchange rate: how many EDU tokens per 1 ETH
    public static final long TOKENS_PER_ETH = 1_000L;

    // Sepolia chain ID
    public static final long CHAIN_ID = 11155111L;

    private Web3Config() {
    }
}
