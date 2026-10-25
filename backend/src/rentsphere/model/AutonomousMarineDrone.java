package rentsphere.model;

/**
 * Tier 5 Leaf Class: Autonomous Marine Drone
 * Coastal and underwater bathymetric drone for tourism and survey missions.
 * MCA OOT Lab - Assignment 2 (5-Tier Inheritance Hierarchy)
 */
public class AutonomousMarineDrone extends TourismAsset {
    private double maxDepthMeters;
    private boolean sonarEquipped;

    public AutonomousMarineDrone(String id, String title, String ownerId, double baseDailyRate, 
                                 double securityCollateral, String lockerCoordinate, 
                                 double seasonalDemandMultiplier, String insuranceTier,
                                 String waterproofIpRating, double saltwaterCorrosionFactor,
                                 double maxDepthMeters, boolean sonarEquipped) {
        super(id, title, ownerId, baseDailyRate, securityCollateral, lockerCoordinate, 
              seasonalDemandMultiplier, insuranceTier, waterproofIpRating, saltwaterCorrosionFactor);
        this.maxDepthMeters = maxDepthMeters;
        this.sonarEquipped = sonarEquipped;
    }

    public double getMaxDepthMeters() {
        return maxDepthMeters;
    }

    public void setMaxDepthMeters(double maxDepthMeters) {
        this.maxDepthMeters = maxDepthMeters;
        touch();
    }

    public boolean isSonarEquipped() {
        return sonarEquipped;
    }

    public void setSonarEquipped(boolean sonarEquipped) {
        this.sonarEquipped = sonarEquipped;
        touch();
    }

    @Override
    public double calculateDailyRentalTariff(int rentalDays) {
        double depthMultiplier = 1.0 + (maxDepthMeters / 500.0);
        double rawTariff = baseDailyRate * seasonalDemandMultiplier * depthMultiplier * rentalDays;
        return Math.round(rawTariff * 100.0) / 100.0;
    }

    @Override
    public double calculateRequiredCollateral() {
        double sonarSurcharge = sonarEquipped ? 1.25 : 1.0;
        return Math.round(securityCollateral * sonarSurcharge * 100.0) / 100.0;
    }

    @Override
    public String serializeToPipe() {
        String logs = String.join(",", maintenanceLogs);
        String extra = String.format("depth=%.1f;sonar=%b;multiplier=%.2f;ip=%s;corrosion=%.2f",
                maxDepthMeters, sonarEquipped, seasonalDemandMultiplier, waterproofIpRating, saltwaterCorrosionFactor);
        return String.join("|", 
                id, title, sector, "AutonomousMarineDrone", ownerId, renterId,
                currentState.getStateName(), String.format("%.2f", baseDailyRate),
                String.format("%.2f", securityCollateral), lockerCoordinate, extra, logs);
    }
}
