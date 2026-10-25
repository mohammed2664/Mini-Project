package rentsphere.model;

/**
 * Tier 4B: Agriculture Asset
 * Soil preparation, monitoring, and harvesting equipment.
 * Houses the heavyMachineryFlag determining Row 7 locker bay assignment.
 * MCA OOT Lab - Assignment 2 & Assignment 5 (Spatial Invariants)
 */
public abstract class AgricultureAsset extends SectoralAsset {
    protected String compatibleSoilType;
    protected double powerKwRating;
    protected boolean heavyMachineryFlag;

    public AgricultureAsset(String id, String title, String ownerId, double baseDailyRate, 
                            double securityCollateral, String lockerCoordinate, 
                            double seasonalDemandMultiplier, String insuranceTier,
                            String compatibleSoilType, double powerKwRating, boolean heavyMachineryFlag) {
        super(id, title, ownerId, baseDailyRate, securityCollateral, lockerCoordinate, 
              "AGRICULTURE", seasonalDemandMultiplier, insuranceTier);
        this.compatibleSoilType = compatibleSoilType;
        this.powerKwRating = powerKwRating;
        this.heavyMachineryFlag = heavyMachineryFlag;
    }

    public String getCompatibleSoilType() {
        return compatibleSoilType;
    }

    public void setCompatibleSoilType(String compatibleSoilType) {
        this.compatibleSoilType = compatibleSoilType;
        touch();
    }

    public double getPowerKwRating() {
        return powerKwRating;
    }

    public void setPowerKwRating(double powerKwRating) {
        this.powerKwRating = powerKwRating;
        touch();
    }

    public boolean isHeavyMachineryFlag() {
        return heavyMachineryFlag;
    }

    public void setHeavyMachineryFlag(boolean heavyMachineryFlag) {
        this.heavyMachineryFlag = heavyMachineryFlag;
        touch();
    }
}
