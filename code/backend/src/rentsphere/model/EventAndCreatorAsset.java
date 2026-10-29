package rentsphere.model;

/**
 * Tier 4A: Event and Creator Asset
 * Cameras, audio systems, projectors, and stage lighting with wattage/battery specs
 * and accessory kit tracking.
 * MCA OOT Lab - Assignment 2 (5-Tier Inheritance Hierarchy)
 */
public abstract class EventAndCreatorAsset extends CategorizedRentalAsset {
    protected String indoorOutdoorRating;
    protected String wattageOrBatterySpec;
    protected int accessoryKitCount;

    public EventAndCreatorAsset(String id, String title, String ownerId, double baseDailyRate, 
                                double securityCollateral, String lockerCoordinate, 
                                String category, double lateFeeMultiplier, String protectionTier,
                                String indoorOutdoorRating, String wattageOrBatterySpec, int accessoryKitCount) {
        super(id, title, ownerId, baseDailyRate, securityCollateral, lockerCoordinate, 
              category, lateFeeMultiplier, protectionTier);
        this.indoorOutdoorRating = indoorOutdoorRating;
        this.wattageOrBatterySpec = wattageOrBatterySpec;
        this.accessoryKitCount = accessoryKitCount;
    }

    public String getIndoorOutdoorRating() {
        return indoorOutdoorRating;
    }

    public void setIndoorOutdoorRating(String indoorOutdoorRating) {
        this.indoorOutdoorRating = indoorOutdoorRating;
        touch();
    }

    public String getWattageOrBatterySpec() {
        return wattageOrBatterySpec;
    }

    public void setWattageOrBatterySpec(String wattageOrBatterySpec) {
        this.wattageOrBatterySpec = wattageOrBatterySpec;
        touch();
    }

    public int getAccessoryKitCount() {
        return accessoryKitCount;
    }

    public void setAccessoryKitCount(int accessoryKitCount) {
        this.accessoryKitCount = accessoryKitCount;
        touch();
    }
}
