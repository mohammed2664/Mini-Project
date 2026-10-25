package rentsphere.model;

/**
 * Tier 3: Sectoral Asset
 * Differentiates between Tourism and Agriculture sectors.
 * MCA OOT Lab - Assignment 2 (5-Tier Inheritance Hierarchy)
 */
public abstract class SectoralAsset extends RentableAsset {
    protected String sector; // "TOURISM" or "AGRICULTURE"
    protected double seasonalDemandMultiplier;
    protected String insuranceTier;

    public SectoralAsset(String id, String title, String ownerId, double baseDailyRate, 
                          double securityCollateral, String lockerCoordinate, 
                          String sector, double seasonalDemandMultiplier, String insuranceTier) {
        super(id, title, ownerId, baseDailyRate, securityCollateral, lockerCoordinate);
        this.sector = sector;
        this.seasonalDemandMultiplier = seasonalDemandMultiplier;
        this.insuranceTier = insuranceTier;
    }

    public String getSector() {
        return sector;
    }

    public void setSector(String sector) {
        this.sector = sector;
        touch();
    }

    public double getSeasonalDemandMultiplier() {
        return seasonalDemandMultiplier;
    }

    public void setSeasonalDemandMultiplier(double seasonalDemandMultiplier) {
        this.seasonalDemandMultiplier = seasonalDemandMultiplier;
        touch();
    }

    public String getInsuranceTier() {
        return insuranceTier;
    }

    public void setInsuranceTier(String insuranceTier) {
        this.insuranceTier = insuranceTier;
        touch();
    }

    @Override
    public double calculateDailyRentalTariff(int rentalDays) {
        return Math.round(baseDailyRate * seasonalDemandMultiplier * rentalDays * 100.0) / 100.0;
    }
}
