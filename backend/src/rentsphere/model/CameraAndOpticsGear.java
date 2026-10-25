package rentsphere.model;

/**
 * Tier 5 Leaf Class: Camera and Optics Gear
 * Mirrorless cameras, 4K/8K bodies, cinema lenses, gimbals, and event projectors.
 * MCA OOT Lab - Assignment 2 (5-Tier Inheritance Hierarchy)
 */
public class CameraAndOpticsGear extends EventAndCreatorAsset {
    private double sensorResolutionMp;
    private boolean is4kOr8kVideo;
    private String mountType;

    public CameraAndOpticsGear(String id, String title, String ownerId, double baseDailyRate, 
                               double securityCollateral, String lockerCoordinate, 
                               double lateFeeMultiplier, String protectionTier,
                               String indoorOutdoorRating, String wattageOrBatterySpec, int accessoryKitCount,
                               double sensorResolutionMp, boolean is4kOr8kVideo, String mountType) {
        super(id, title, ownerId, baseDailyRate, securityCollateral, lockerCoordinate, 
              "CAMERAS", lateFeeMultiplier, protectionTier, indoorOutdoorRating, wattageOrBatterySpec, accessoryKitCount);
        this.sensorResolutionMp = sensorResolutionMp;
        this.is4kOr8kVideo = is4kOr8kVideo;
        this.mountType = mountType;
    }

    public CameraAndOpticsGear(String id, String title, String ownerId, double baseDailyRate, 
                               double securityCollateral, String lockerCoordinate, 
                               double lateFeeMultiplier, String protectionTier,
                               double sensorResolutionMp, boolean is4kOr8kVideo) {
        this(id, title, ownerId, baseDailyRate, securityCollateral, lockerCoordinate,
             lateFeeMultiplier, protectionTier, "Studio/Outdoor", "Dual NP-FZ100 Batteries", 4,
             sensorResolutionMp, is4kOr8kVideo, "Sony E-Mount");
    }

    public double getSensorResolutionMp() {
        return sensorResolutionMp;
    }

    public void setSensorResolutionMp(double sensorResolutionMp) {
        this.sensorResolutionMp = sensorResolutionMp;
        touch();
    }

    public boolean isIs4kOr8kVideo() {
        return is4kOr8kVideo;
    }

    public void setIs4kOr8kVideo(boolean is4kOr8kVideo) {
        this.is4kOr8kVideo = is4kOr8kVideo;
        touch();
    }

    public String getMountType() {
        return mountType;
    }

    public void setMountType(String mountType) {
        this.mountType = mountType;
        touch();
    }

    @Override
    public double calculateDailyRentalTariff(int rentalDays) {
        double resolutionFactor = is4kOr8kVideo ? 1.15 : 1.0;
        double raw = baseDailyRate * resolutionFactor * rentalDays;
        return Math.round(raw * 100.0) / 100.0;
    }

    @Override
    public double calculateRequiredCollateral() {
        double sensorSurcharge = (sensorResolutionMp >= 30.0) ? 1.20 : 1.0;
        return Math.round(securityCollateral * sensorSurcharge * 100.0) / 100.0;
    }

    @Override
    public String serializeToPipe() {
        String logs = String.join(",", serviceLogs);
        String extra = String.format("sensorMp=%.1f;is4k=%b;mount=%s;protection=%s;kitCount=%d",
                sensorResolutionMp, is4kOr8kVideo, mountType, protectionTier, accessoryKitCount);
        return String.join("|",
                id, title, category, "CameraAndOpticsGear", ownerId, renterId,
                currentState.getStateName(), String.format("%.2f", baseDailyRate),
                String.format("%.2f", securityCollateral), lockerCoordinate, extra, logs);
    }
}
