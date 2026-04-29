package com.edulink.gui.models;

public class User {
    private int id;
    private String email;
    private String fullName;
    private String password;
    private String roles; // Store as raw JSON string for simplicity, or we could parse
    private double walletBalance;
    private String ethWalletAddress;
    private String faceDescriptor;
    private String resetOtp;
    private String resetOtpExpiresAt; // simplified datetime
    private int xp;
    private boolean isVerified;

    public User() {}

    public User(int id, String email, String fullName, String roles, int xp, boolean isVerified) {
        this.id = id;
        this.email = email;
        this.fullName = fullName;
        this.roles = roles;
        this.xp = xp;
        this.isVerified = isVerified;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getRoles() { return roles; }
    public void setRoles(String roles) { this.roles = roles; }

    public double getWalletBalance() { return walletBalance; }
    public void setWalletBalance(double walletBalance) { this.walletBalance = walletBalance; }

    public String getEthWalletAddress() { return ethWalletAddress; }
    public void setEthWalletAddress(String ethWalletAddress) { this.ethWalletAddress = ethWalletAddress; }

    public String getFaceDescriptor() { return faceDescriptor; }
    public void setFaceDescriptor(String faceDescriptor) { this.faceDescriptor = faceDescriptor; }

    public String getResetOtp() { return resetOtp; }
    public void setResetOtp(String resetOtp) { this.resetOtp = resetOtp; }

    public String getResetOtpExpiresAt() { return resetOtpExpiresAt; }
    public void setResetOtpExpiresAt(String resetOtpExpiresAt) { this.resetOtpExpiresAt = resetOtpExpiresAt; }

    public int getXp() { return xp; }
    public void setXp(int xp) { this.xp = xp; }

    public boolean isVerified() { return isVerified; }
    public void setVerified(boolean verified) { isVerified = verified; }

    // Helper to check roles safely
    public boolean hasRole(String roleName) {
        if (roles == null) return false;
        return roles.contains(roleName);
    }
}
