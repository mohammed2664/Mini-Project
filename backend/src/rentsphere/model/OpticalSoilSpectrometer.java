package rentsphere.model;

/**
 * Tier 5 Leaf Class: Optical Soil Spectrometer
 * Precision optical sensor for non-destructive soil fertility and nitrogen analysis.
 * MCA OOT Lab - Assignment 2 (5-Tier Inheritance Hierarchy)
 */
public class OpticalSoilSpectrometer extends AgricultureAsset {
    private int spectralBandsCount;
    private double nitrogenDetectionAccuracy;

    public OpticalSoilSpectrometer(String id, String title, String ownerId, double baseDailyRate, 
                                   double securityCollateral, String lockerCoordinate, 
                                   double seasonalDemandMultiplier, String insuranceTier,
                                   String compatibleSoilType, double powerKwRating,
                                   int spectralBandsCount, double nitrogenDetectionAccuracy) {
        super(id, title, ownerId, baseDailyRate, securityCollateral, lockerCoordinate, 
              seasonalDemandMultiplier, insuranceTier, compatibleSoilType, powerKwRating, false);
        this.spectralBandsCount = spectralBandsCount;
        this.nitrogenDetectionAccuracy = nitrogenDetectionAccuracy;
    }

    public int getSpectralBandsCount() {
        return spectralBandsCount;
    }

    public void setSpectralBandsCount(int spectralBandsCount) {
        this.spectralBandsCount = spectralBandsCount;
        touch();
    }

    public double getNitrogenDetectionAccuracy() {
        return nitrogenDetectionAccuracy;
    }

    public void setNitrogenDetectionAccuracy(double nitrogenDetectionAccuracy) {
        this.nitrogenDetectionAccuracy = nitrogenDetectionAccuracy;
        touch();
    }

    @Override
    public double calculateDailyRentalTariff(int rentalDays) {
        double precisionMultiplier = 1.0 + (spectralBandsCount / 100.0);
        double rawTariff = baseDailyRate * seasonalDemandMultiplier * precisionMultiplier * rentalDays;
        return Math.round(rawTariff * 100.0) / 100.0;
    }

    @Override
    public String serializeToPipe() {
        String logs = String.join(",", maintenanceLogs);
        String extra = String.format("bands=%d;accuracy=%.1f;soil=%s;kw=%.1f;heavy=%b;multiplier=%.2f",
                spectralBandsCount, nitrogenDetectionAccuracy, compatibleSoilType, powerKwRating, heavyMachineryFlag, seasonalDemandMultiplier);
        return String.join("|", 
                id, title, sector, "OpticalSoilSpectrometer", ownerId, renterId,
                currentState.getStateName(), String.format("%.2f", baseDailyRate),
                String.format("%.2f", securityCollateral), lockerCoordinate, extra, logs);
    }
}
