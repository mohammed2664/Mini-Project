package rentsphere.model;

/**
 * Tier 5 Leaf Class: Submersible Action Camera Rig
 * Deep-water 4K/8K stabilization camera housing for tourism diving.
 * MCA OOT Lab - Assignment 2 (5-Tier Inheritance Hierarchy)
 */
public class SubmersibleActionRig extends TourismAsset {
    private String lensStabilizationTier;
    private boolean recording8kCapable;
    private double maxDepthMeters;

    public SubmersibleActionRig(String id, String title, String ownerId, double baseDailyRate, 
                                double securityCollateral, String lockerCoordinate, 
                                double seasonalDemandMultiplier, String insuranceTier,
                                String waterproofIpRating, double saltwaterCorrosionFactor,
                                String lensStabilizationTier, boolean recording8kCapable, double maxDepthMeters) {
        super(id, title, ownerId, baseDailyRate, securityCollateral, lockerCoordinate, 
              seasonalDemandMultiplier, insuranceTier, waterproofIpRating, saltwaterCorrosionFactor);
        this.lensStabilizationTier = lensStabilizationTier;
        this.recording8kCapable = recording8kCapable;
        this.maxDepthMeters = maxDepthMeters;
    }

    public String getLensStabilizationTier() {
        return lensStabilizationTier;
    }

    public void setLensStabilizationTier(String lensStabilizationTier) {
        this.lensStabilizationTier = lensStabilizationTier;
        touch();
    }

    public boolean isRecording8kCapable() {
        return recording8kCapable;
    }

    public void setRecording8kCapable(boolean recording8kCapable) {
        this.recording8kCapable = recording8kCapable;
        touch();
    }

    public double getMaxDepthMeters() {
        return maxDepthMeters;
    }

    public void setMaxDepthMeters(double maxDepthMeters) {
        this.maxDepthMeters = maxDepthMeters;
        touch();
    }

    @Override
    public double calculateDailyRentalTariff(int rentalDays) {
        double resolutionFactor = recording8kCapable ? 1.30 : 1.05;
        double rawTariff = baseDailyRate * seasonalDemandMultiplier * resolutionFactor * rentalDays;
        return Math.round(rawTariff * 100.0) / 100.0;
    }

    @Override
    public String serializeToPipe() {
        String logs = String.join(",", maintenanceLogs);
        String extra = String.format("stabilization=%s;depth=%.1f;is8k=%b;multiplier=%.2f;ip=%s;corrosion=%.2f",
                lensStabilizationTier, maxDepthMeters, recording8kCapable, seasonalDemandMultiplier, waterproofIpRating, saltwaterCorrosionFactor);
        return String.join("|", 
                id, title, sector, "SubmersibleActionRig", ownerId, renterId,
                currentState.getStateName(), String.format("%.2f", baseDailyRate),
                String.format("%.2f", securityCollateral), lockerCoordinate, extra, logs);
    }
}
