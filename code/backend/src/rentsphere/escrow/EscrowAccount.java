package rentsphere.escrow;

/**
 * Double-Entry Escrow Account Entity.
 * MCA OOT Lab - Assignment 2 (Escrow Engine)
 */
public class EscrowAccount {
    private final String userId;
    private final String userName;
    private final String role; // "HOST", "RENTER", "INSPECTOR"
    private double walletBalance;
    private double lockedEscrow;
    private double earnedRevenue;
    private boolean kycVerified;

    public EscrowAccount(String userId, String userName, String role, 
                         double walletBalance, double lockedEscrow, double earnedRevenue, boolean kycVerified) {
        this.userId = userId;
        this.userName = userName;
        this.role = role;
        this.walletBalance = walletBalance;
        this.lockedEscrow = lockedEscrow;
        this.earnedRevenue = earnedRevenue;
        this.kycVerified = kycVerified;
    }

    public String getUserId() {
        return userId;
    }

    public String getUserName() {
        return userName;
    }

    public String getRole() {
        return role;
    }

    public synchronized double getWalletBalance() {
        return walletBalance;
    }

    public synchronized void setWalletBalance(double walletBalance) {
        this.walletBalance = Math.round(walletBalance * 100.0) / 100.0;
    }

    public synchronized double getLockedEscrow() {
        return lockedEscrow;
    }

    public synchronized void setLockedEscrow(double lockedEscrow) {
        this.lockedEscrow = Math.round(lockedEscrow * 100.0) / 100.0;
    }

    public synchronized double getEarnedRevenue() {
        return earnedRevenue;
    }

    public synchronized void setEarnedRevenue(double earnedRevenue) {
        this.earnedRevenue = Math.round(earnedRevenue * 100.0) / 100.0;
    }

    public boolean isKycVerified() {
        return kycVerified;
    }

    public void setKycVerified(boolean kycVerified) {
        this.kycVerified = kycVerified;
    }

    public synchronized void creditWallet(double amount) {
        this.walletBalance = Math.round((this.walletBalance + amount) * 100.0) / 100.0;
    }

    public synchronized void debitWallet(double amount) {
        this.walletBalance = Math.round((this.walletBalance - amount) * 100.0) / 100.0;
    }

    public synchronized void creditLocked(double amount) {
        this.lockedEscrow = Math.round((this.lockedEscrow + amount) * 100.0) / 100.0;
    }

    public synchronized void debitLocked(double amount) {
        this.lockedEscrow = Math.round((this.lockedEscrow - amount) * 100.0) / 100.0;
    }

    public synchronized void creditEarned(double amount) {
        this.earnedRevenue = Math.round((this.earnedRevenue + amount) * 100.0) / 100.0;
    }

    public String serializeToPipe() {
        return String.format("%s|%s|%s|%.2f|%.2f|%.2f|%b",
                userId, userName, role, walletBalance, lockedEscrow, earnedRevenue, kycVerified);
    }
}
