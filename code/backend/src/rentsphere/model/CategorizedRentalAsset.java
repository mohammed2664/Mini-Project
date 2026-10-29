package rentsphere.model;

/**
 * Tier 3: Categorized Rental Asset
 * Adds category classification, late-fee multipliers, and protection plan tiers.
 * MCA OOT Lab - Assignment 2 (5-Tier Inheritance Hierarchy)
 */
public abstract class CategorizedRentalAsset extends RentableAsset {
    protected String category; // "CAMERAS", "AUDIO_LIGHTING", "POWER_TOOLS", "MOBILITY_VEHICLES"
    protected double lateFeeMultiplier;
    protected String protectionTier;

    public CategorizedRentalAsset(String id, String title, String ownerId, double baseDailyRate, 
                                  double securityCollateral, String lockerCoordinate, 
                                  String category, double lateFeeMultiplier, String protectionTier) {
        super(id, title, ownerId, baseDailyRate, securityCollateral, lockerCoordinate);
        this.category = category;
        this.lateFeeMultiplier = lateFeeMultiplier;
        this.protectionTier = protectionTier;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
        touch();
    }

    public double getLateFeeMultiplier() {
        return lateFeeMultiplier;
    }

    public void setLateFeeMultiplier(double lateFeeMultiplier) {
        this.lateFeeMultiplier = lateFeeMultiplier;
        touch();
    }

    public String getProtectionTier() {
        return protectionTier;
    }

    public void setProtectionTier(String protectionTier) {
        this.protectionTier = protectionTier;
        touch();
    }

    @Override
    public double calculateDailyRentalTariff(int rentalDays) {
        return Math.round(baseDailyRate * rentalDays * 100.0) / 100.0;
    }
}
