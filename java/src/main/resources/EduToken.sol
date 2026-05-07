// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

/**
 * EduLink Token (EDU) — Sepolia Testnet
 *
 * ERC-20 compatible token with:
 *   - buyTokens()       : payable, mint tokens at a fixed ETH rate
 *   - mint()            : admin-only, free mint for rewards/testing
 *   - distributeBounty(): admin-only, transfer from student → tutor
 *   - withdraw()        : admin-only, drain accumulated ETH
 *
 * Deploy on Sepolia, then paste the contract address in Web3Config.java
 */
contract EduToken {

    string  public name     = "EduLink Token";
    string  public symbol   = "EDU";
    uint8   public decimals = 18;
    uint256 public totalSupply;

    address public admin;
    uint256 public constant TOKENS_PER_ETH = 1000; // 1 ETH = 1000 EDU

    mapping(address => uint256) public balanceOf;
    mapping(address => mapping(address => uint256)) public allowance;

    // ── Events ────────────────────────────────────────────
    event Transfer(address indexed from, address indexed to, uint256 value);
    event Approval(address indexed owner, address indexed spender, uint256 value);
    event TokensPurchased(address indexed buyer, uint256 ethAmount, uint256 tokensIssued);
    event BountyDistributed(address indexed student, address indexed tutor, uint256 amount);
    event Minted(address indexed to, uint256 amount);

    modifier onlyAdmin() {
        require(msg.sender == admin, "EduToken: caller is not admin");
        _;
    }

    constructor() {
        admin = msg.sender;
        // Pre-mint 1,000,000 EDU to admin for initial distribution & testing
        _mint(admin, 1_000_000 * 10 ** decimals);
    }

    // ── ERC-20 Standard ───────────────────────────────────
    function transfer(address to, uint256 amount) external returns (bool) {
        _transfer(msg.sender, to, amount);
        return true;
    }

    function approve(address spender, uint256 amount) external returns (bool) {
        allowance[msg.sender][spender] = amount;
        emit Approval(msg.sender, spender, amount);
        return true;
    }

    function transferFrom(address from, address to, uint256 amount) external returns (bool) {
        require(allowance[from][msg.sender] >= amount, "EduToken: allowance exceeded");
        allowance[from][msg.sender] -= amount;
        _transfer(from, to, amount);
        return true;
    }

    // ── Buy Tokens ────────────────────────────────────────
    /**
     * Send ETH to get EDU tokens at the TOKENS_PER_ETH rate.
     * Tokens are minted fresh (inflationary buy-in model).
     */
    function buyTokens() external payable {
        require(msg.value > 0, "EduToken: send ETH to buy tokens");
        uint256 tokens = (msg.value * TOKENS_PER_ETH * 10 ** decimals) / 1 ether;
        _mint(msg.sender, tokens);
        emit TokensPurchased(msg.sender, msg.value, tokens);
    }

    // ── Admin: Mint ───────────────────────────────────────
    /**
     * Admin mints tokens to any address (for rewards, corrections, etc.)
     */
    function mint(address to, uint256 amount) external onlyAdmin {
        _mint(to, amount);
        emit Minted(to, amount);
    }

    // ── Admin: Distribute Bounty ──────────────────────────
    /**
     * Transfers bounty tokens from student's balance to the tutor.
     * Called server-side by HelpSessionService after anti-farming checks pass.
     * The admin must be pre-approved OR this uses admin's own balance as relay.
     *
     * NOTE: In production, students call approve(adminAddress, bountyAmount)
     * when escrowing. Admin then calls this to finalize.
     */
    function distributeBounty(
        address student,
        address tutor,
        uint256 amount
    ) external onlyAdmin {
        require(balanceOf[student] >= amount, "EduToken: student has insufficient tokens");
        _transfer(student, tutor, amount);
        emit BountyDistributed(student, tutor, amount);
    }

    // ── Admin: Withdraw ETH ───────────────────────────────
    function withdraw() external onlyAdmin {
        payable(admin).transfer(address(this).balance);
    }

    // ── Internal ──────────────────────────────────────────
    function _transfer(address from, address to, uint256 amount) internal {
        require(from != address(0), "EduToken: from zero address");
        require(to   != address(0), "EduToken: to zero address");
        require(balanceOf[from] >= amount, "EduToken: insufficient balance");
        balanceOf[from] -= amount;
        balanceOf[to]   += amount;
        emit Transfer(from, to, amount);
    }

    function _mint(address to, uint256 amount) internal {
        require(to != address(0), "EduToken: mint to zero address");
        totalSupply      += amount;
        balanceOf[to]    += amount;
        emit Transfer(address(0), to, amount);
    }
}
