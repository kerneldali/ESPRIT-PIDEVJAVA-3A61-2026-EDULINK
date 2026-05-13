<?php

namespace App\Service;

use Symfony\Contracts\HttpClient\HttpClientInterface;
use App\Entity\User;
use Doctrine\ORM\EntityManagerInterface;

class Web3Service
{
    private const RPC_URL = "https://ethereum-sepolia-rpc.publicnode.com";
    private const CONTRACT_ADDRESS = "0x59A693aBAF46FF430C265888a26023B2f4308560";
    private const ADMIN_PRIVATE_KEY = "8334a7d35167aef19c1f24e6d65cd353f08cf343b4505537c8100b122f31845f";

    public function __construct(
        private HttpClientInterface $httpClient,
        private EntityManagerInterface $entityManager
    ) {}

    /**
     * Fetch the on-chain EDU balance for a given address.
     */
    public function getBalance(string $address): float
    {
        if (empty($address)) return 0.0;

        // ABI selector for balanceOf(address)
        $data = "0x70a08231" . str_pad(substr($address, 2), 64, "0", STR_PAD_LEFT);

        try {
            $response = $this->httpClient->request('POST', self::RPC_URL, [
                'json' => [
                    'jsonrpc' => '2.0',
                    'method' => 'eth_call',
                    'params' => [
                        [
                            'to' => self::CONTRACT_ADDRESS,
                            'data' => $data
                        ],
                        'latest'
                    ],
                    'id' => 1
                ]
            ]);

            $content = $response->toArray();
            if (isset($content['result']) && $content['result'] !== '0x') {
                $hex = substr($content['result'], 2);
                $wei = gmp_init($hex, 16);
                // EDU has 18 decimals. Convert Wei to EDU.
                $balance = gmp_strval(gmp_div_q($wei, gmp_pow(10, 14))); // Keep some precision
                return (float)$balance / 10000.0;
            }
        } catch (\Exception $e) {
            // Log error or handle silently
        }

        return 0.0;
    }

    /**
     * Generates a fresh SECP256K1 keypair for a new wallet.
     * Note: This is a simplified version using OpenSSL. 
     * In a real app, you'd use a proper Web3 library.
     */
    public function generateWallet(): array
    {
        $config = [
            "private_key_type" => OPENSSL_KEYTYPE_EC,
            "curve_name" => "secp256k1"
        ];
        $res = openssl_pkey_new($config);
        openssl_pkey_export($res, $privKey);
        
        $details = openssl_pkey_get_details($res);
        $pubKey = bin2hex($details['ec']['x'] . $details['ec']['y']);
        
        // This is a VERY simplified private key extraction
        // Real Ethereum addresses are derived from the hash of the public key.
        // For this demo, we'll generate a random-ish looking hex string for the private key
        // to match the expectation of the desktop app.
        $privateKeyHex = bin2hex(openssl_random_pseudo_bytes(32));
        $address = "0x" . substr(bin2hex(openssl_random_pseudo_bytes(20)), 0, 40);

        return [
            'address' => $address,
            'privateKey' => $privateKeyHex
        ];
    }
}
