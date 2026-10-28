package rentsphere.model;

/**
 * Tier 5 Leaf Class: Power Tool & Utility Equipment
 * Bosch impact drills, Karcher pressure washers, angle grinders, and electronic toolkits.
 * Fits in standard hub locker compartments (Rows A-E).
 * MCA OOT Lab - Assignment 2 (5-Tier Inheritance Hierarchy)
 */
public class PowerToolEquipment extends UtilityAndMobilityAsset {
    private boolean cordlessBatteryPowered;
    private int accessoryBitsIncluded;

    public PowerToolEquipment(String id, String title, String ownerId, double baseDailyRate, 
                              double securityCollateral, String lockerCoordinate, 
                              double lateFeeMultiplier, String protectionTier,
                              String powerOrEngineSpec, String safetyCertification,
                              boolean cordlessBatteryPowered, int accessoryBitsIncluded) {
        super(id, title, ownerId, baseDailyRate, securityCollateral, lockerCoordinate, 
              "POWER_TOOLS", lateFeeMultiplier, protectionTier, powerOrEngineSpec, safetyCertification, false);
        this.cordlessBatteryPowered = cordlessBatteryPowered;
        this.accessoryBitsIncluded = accessoryBitsIncluded;
    }

    public boolean isCordlessBatteryPowered() {
        return cordlessBatteryPowered;
    }

    public void setCordlessBatteryPowered(boolean cordlessBatteryPowered) {
        this.cordlessBatteryPowered = cordlessBatteryPowered;
        touch();
    }

    public int getAccessoryBitsIncluded() {
        return accessoryBitsIncluded;
    }

    public void setAccessoryBitsIncluded(int accessoryBitsIncluded) {
        this.accessoryBitsIncluded = accessoryBitsIncluded;
        touch();
    }

    @Override
    public double calculateDailyRentalTariff(int rentalDays) {
        double cordlessMultiplier = cordlessBatteryPowered ? 1.10 : 1.0;
        double raw = baseDailyRate * cordlessMultiplier * rentalDays;
        return Math.round(raw * 100.0) / 100.0;
    }

    @Override
    public double calculateRequiredCollateral() {
        return securityCollateral;
    }

    @Override
    public String serializeToPipe() {
        String logs = String.join(",", serviceLogs);
        String extra = String.format("power=%s;safety=%s;cordless=%b;bits=%d;days=%d",
                powerOrEngineSpec, safetyCertification, cordlessBatteryPowered, accessoryBitsIncluded, rentalDays);
        if (imageUrl != null && !imageUrl.isBlank()) {
            extra += ";img=" + imageUrl.replace(";", "_").replace("|", "_");
        }
        return String.join("|",
                id, title, category, "PowerToolEquipment", ownerId, renterId,
                currentState.getStateName(), String.format("%.2f", baseDailyRate),
                String.format("%.2f", securityCollateral), lockerCoordinate, extra, logs);
    }
}
