package rentsphere.model;

/**
 * Tier 5 Leaf Class: Event Audio and Lighting Equipment
 * JBL party speakers, PA systems, Pioneer DJ consoles, wireless mic kits, and stage RGB lighting.
 * MCA OOT Lab - Assignment 2 (5-Tier Inheritance Hierarchy)
 */
public class EventAudioAndLighting extends EventAndCreatorAsset {
    private int maxSoundDbRating;
    private int wirelessMicsCount;
    private boolean hasDmxOrRgb;

    public EventAudioAndLighting(String id, String title, String ownerId, double baseDailyRate, 
                                 double securityCollateral, String lockerCoordinate, 
                                 double lateFeeMultiplier, String protectionTier,
                                 String indoorOutdoorRating, String wattageOrBatterySpec, int accessoryKitCount,
                                 int maxSoundDbRating, int wirelessMicsCount, boolean hasDmxOrRgb) {
        super(id, title, ownerId, baseDailyRate, securityCollateral, lockerCoordinate, 
              "AUDIO_LIGHTING", lateFeeMultiplier, protectionTier, indoorOutdoorRating, wattageOrBatterySpec, accessoryKitCount);
        this.maxSoundDbRating = maxSoundDbRating;
        this.wirelessMicsCount = wirelessMicsCount;
        this.hasDmxOrRgb = hasDmxOrRgb;
    }

    public int getMaxSoundDbRating() {
        return maxSoundDbRating;
    }

    public void setMaxSoundDbRating(int maxSoundDbRating) {
        this.maxSoundDbRating = maxSoundDbRating;
        touch();
    }

    public int getWirelessMicsCount() {
        return wirelessMicsCount;
    }

    public void setWirelessMicsCount(int wirelessMicsCount) {
        this.wirelessMicsCount = wirelessMicsCount;
        touch();
    }

    public boolean isHasDmxOrRgb() {
        return hasDmxOrRgb;
    }

    public void setHasDmxOrRgb(boolean hasDmxOrRgb) {
        this.hasDmxOrRgb = hasDmxOrRgb;
        touch();
    }

    @Override
    public double calculateDailyRentalTariff(int rentalDays) {
        double micFactor = 1.0 + (wirelessMicsCount * 0.05);
        double raw = baseDailyRate * micFactor * rentalDays;
        return Math.round(raw * 100.0) / 100.0;
    }

    @Override
    public double calculateRequiredCollateral() {
        double lightingSurcharge = hasDmxOrRgb ? 1.10 : 1.0;
        return Math.round(securityCollateral * lightingSurcharge * 100.0) / 100.0;
    }

    @Override
    public String serializeToPipe() {
        String logs = String.join(",", serviceLogs);
        String extra = String.format("soundDb=%d;mics=%d;rgb=%b;protection=%s;kitCount=%d",
                maxSoundDbRating, wirelessMicsCount, hasDmxOrRgb, protectionTier, accessoryKitCount);
        return String.join("|",
                id, title, category, "EventAudioAndLighting", ownerId, renterId,
                currentState.getStateName(), String.format("%.2f", baseDailyRate),
                String.format("%.2f", securityCollateral), lockerCoordinate, extra, logs);
    }
}
