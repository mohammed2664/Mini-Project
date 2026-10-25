package rentsphere.model;

/**
 * Tier 4B: Utility and Mobility Asset
 * Power tools, cleaning machines, electric scooters, and self-drive vehicles.
 * Houses the requiresGroundBay flag which enforces Row H Ground Parking Slot allocation.
 * MCA OOT Lab - Assignment 2 & Assignment 5 (Spatial Invariants)
 */
public abstract class UtilityAndMobilityAsset extends CategorizedRentalAsset {
    protected String powerOrEngineSpec;
    protected String safetyCertification;
    protected boolean requiresGroundBay;

    public UtilityAndMobilityAsset(String id, String title, String ownerId, double baseDailyRate, 
                                   double securityCollateral, String lockerCoordinate, 
                                   String category, double lateFeeMultiplier, String protectionTier,
                                   String powerOrEngineSpec, String safetyCertification, boolean requiresGroundBay) {
        super(id, title, ownerId, baseDailyRate, securityCollateral, lockerCoordinate, 
              category, lateFeeMultiplier, protectionTier);
        this.powerOrEngineSpec = powerOrEngineSpec;
        this.safetyCertification = safetyCertification;
        this.requiresGroundBay = requiresGroundBay;
    }

    public String getPowerOrEngineSpec() {
        return powerOrEngineSpec;
    }

    public void setPowerOrEngineSpec(String powerOrEngineSpec) {
        this.powerOrEngineSpec = powerOrEngineSpec;
        touch();
    }

    public String getSafetyCertification() {
        return safetyCertification;
    }

    public void setSafetyCertification(String safetyCertification) {
        this.safetyCertification = safetyCertification;
        touch();
    }

    public boolean isRequiresGroundBay() {
        return requiresGroundBay;
    }

    public void setRequiresGroundBay(boolean requiresGroundBay) {
        this.requiresGroundBay = requiresGroundBay;
        touch();
    }
}
