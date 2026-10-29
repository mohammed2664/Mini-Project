package rentsphere.auth;

import java.time.Instant;

/**
 * User Account Domain Entity with RBAC, Cryptographic Credentials, and KYC state.
 * Roles: RENTER, HOST, ADMIN, VISITOR.
 */
public class UserAccount {
    private final String userId;
    private String email;
    private String fullName;
    private String passwordHash;
    private String salt;
    private String role; // "RENTER", "HOST", "ADMIN", "VISITOR"
    private String phone;
    private boolean kycVerified;
    private String accountStatus; // "ACTIVE", "SUSPENDED", "PENDING_VERIFICATION"
    private final long createdAt;
    private long lastLoginAt;

    public UserAccount(String userId, String email, String fullName, String passwordHash, 
                       String salt, String role, String phone, boolean kycVerified, 
                       String accountStatus, long createdAt) {
        this.userId = userId;
        this.email = email;
        this.fullName = fullName;
        this.passwordHash = passwordHash;
        this.salt = salt;
        this.role = (role == null || role.isBlank()) ? "RENTER" : role.toUpperCase();
        this.phone = (phone == null) ? "" : phone;
        this.kycVerified = kycVerified;
        this.accountStatus = (accountStatus == null) ? "ACTIVE" : accountStatus.toUpperCase();
        this.createdAt = (createdAt > 0) ? createdAt : Instant.now().toEpochMilli();
        this.lastLoginAt = this.createdAt;
    }

    public String getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPassword(String rawPassword) {
        this.salt = PasswordHasher.generateSalt();
        this.passwordHash = PasswordHasher.hashPassword(rawPassword, this.salt);
    }

    public String getSalt() {
        return salt;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role.toUpperCase();
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public boolean isKycVerified() {
        return kycVerified;
    }

    public void setKycVerified(boolean kycVerified) {
        this.kycVerified = kycVerified;
    }

    public String getAccountStatus() {
        return accountStatus;
    }

    public void setAccountStatus(String accountStatus) {
        this.accountStatus = accountStatus;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(long lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }

    public boolean checkPassword(String inputPassword) {
        if ("SUSPENDED".equalsIgnoreCase(this.accountStatus)) {
            return false;
        }
        return PasswordHasher.verifyPassword(inputPassword, this.salt, this.passwordHash);
    }

    public boolean isHost() {
        return "HOST".equals(role) || "ADMIN".equals(role);
    }

    public boolean isAdmin() {
        return "ADMIN".equals(role) || "INSPECTOR".equals(role);
    }

    public String serializeToPipe() {
        return String.format("%s|%s|%s|%s|%s|%s|%s|%b|%s|%d",
                userId, email, fullName, passwordHash, salt, role, phone, kycVerified, accountStatus, createdAt);
    }
}
