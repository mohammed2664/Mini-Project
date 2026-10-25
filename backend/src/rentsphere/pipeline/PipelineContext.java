package rentsphere.pipeline;

import rentsphere.model.RentableAsset;

/**
 * Execution context carrying state payload across the 4-Gate Fail-Fast Pipeline.
 * MCA OOT Lab - Assignment 1
 */
public class PipelineContext {
    private final RentableAsset asset;
    private final String requesterUserId;
    private final String requestedAction; // RESERVE, PICKUP, RETURN, INSPECT_APPROVE, INSPECT_SLASH
    private final double userWalletBalance;
    private final boolean userKycVerified;
    private final String targetLockerBay;
    private final int rentalDays;
    private final double damagePercentage;

    public PipelineContext(RentableAsset asset, String requesterUserId, String requestedAction,
                           double userWalletBalance, boolean userKycVerified,
                           String targetLockerBay, int rentalDays, double damagePercentage) {
        this.asset = asset;
        this.requesterUserId = requesterUserId;
        this.requestedAction = requestedAction;
        this.userWalletBalance = userWalletBalance;
        this.userKycVerified = userKycVerified;
        this.targetLockerBay = targetLockerBay;
        this.rentalDays = Math.max(1, rentalDays);
        this.damagePercentage = damagePercentage;
    }

    public RentableAsset getAsset() {
        return asset;
    }

    public String getRequesterUserId() {
        return requesterUserId;
    }

    public String getRequestedAction() {
        return requestedAction;
    }

    public double getUserWalletBalance() {
        return userWalletBalance;
    }

    public boolean isUserKycVerified() {
        return userKycVerified;
    }

    public String getTargetLockerBay() {
        return targetLockerBay;
    }

    public int getRentalDays() {
        return rentalDays;
    }

    public double getDamagePercentage() {
        return damagePercentage;
    }
}
