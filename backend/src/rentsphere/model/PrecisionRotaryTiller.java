package rentsphere.model;

/**
 * Tier 5 Leaf Class: Precision Rotary Tiller
 * Heavy-duty soil cultivation implement. Heavy machinery flag is locked to true.
 * Must be stored in Row 7 (Row H) of the 8x8 Smart Locker Matrix.
 * MCA OOT Lab - Assignment 2 & Assignment 5
 */
public class PrecisionRotaryTiller extends AgricultureAsset {
    private int bladeRpm;
    private double workingWidthCm;

    public PrecisionRotaryTiller(String id, String title, String ownerId, double baseDailyRate, 
                                 double securityCollateral, String lockerCoordinate, 
                                 double seasonalDemandMultiplier, String insuranceTier,
                                 String compatibleSoilType, double powerKwRating,
                                 int bladeRpm, double workingWidthCm) {
        super(id, title, ownerId, baseDailyRate, securityCollateral, lockerCoordinate, 
              seasonalDemandMultiplier, insuranceTier, compatibleSoilType, powerKwRating, true);
        this.bladeRpm = bladeRpm;
        this.workingWidthCm = workingWidthCm;
    }

    public int getBladeRpm() {
        return bladeRpm;
    }

    public void setBladeRpm(int bladeRpm) {
        this.bladeRpm = bladeRpm;
        touch();
    }

    public double getWorkingWidthCm() {
        return workingWidthCm;
    }

    public void setWorkingWidthCm(double workingWidthCm) {
        this.workingWidthCm = workingWidthCm;
        touch();
    }

    @Override
    public double calculateDailyRentalTariff(int rentalDays) {
        double powerMultiplier = 1.0 + (powerKwRating / 100.0);
        double rawTariff = baseDailyRate * seasonalDemandMultiplier * powerMultiplier * rentalDays;
        return Math.round(rawTariff * 100.0) / 100.0;
    }

    @Override
    public double calculateRequiredCollateral() {
        // Heavy machinery carries heavy deposit factor (1.15)
        return Math.round(securityCollateral * 1.15 * 100.0) / 100.0;
    }

    @Override
    public String serializeToPipe() {
        String logs = String.join(",", maintenanceLogs);
        String extra = String.format("rpm=%d;width=%.1f;soil=%s;kw=%.1f;heavy=%b;multiplier=%.2f",
                bladeRpm, workingWidthCm, compatibleSoilType, powerKwRating, heavyMachineryFlag, seasonalDemandMultiplier);
        return String.join("|", 
                id, title, sector, "PrecisionRotaryTiller", ownerId, renterId,
                currentState.getStateName(), String.format("%.2f", baseDailyRate),
                String.format("%.2f", securityCollateral), lockerCoordinate, extra, logs);
    }
}
