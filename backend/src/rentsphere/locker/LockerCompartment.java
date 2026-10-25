package rentsphere.locker;

import java.security.SecureRandom;

/**
 * Physical Locker Bay Compartment Entity in the 8x8 Grid.
 * MCA OOT Lab - Assignment 5
 */
public class LockerCompartment {
    private final String bayId;
    private final int row;
    private final int col;
    private final CompartmentSize sizeTier;
    private boolean occupied;
    private String currentAssetId;
    private String status; // "AVAILABLE", "ONLINE_READY", "RESERVED_LOCKED", "DISPATCHED_ACTIVE", "QUARANTINE_LOCKED"
    private String otpPin;
    private double tempC;
    private double humidityPct;
    private double weightKg;

    private static final SecureRandom RANDOM = new SecureRandom();

    public LockerCompartment(String bayId, int row, int col, CompartmentSize sizeTier, 
                             boolean occupied, String currentAssetId, String status, 
                             String otpPin, double tempC, double humidityPct, double weightKg) {
        this.bayId = bayId;
        this.row = row;
        this.col = col;
        this.sizeTier = sizeTier;
        this.occupied = occupied;
        this.currentAssetId = (currentAssetId == null) ? "" : currentAssetId;
        this.status = (status == null || status.isBlank()) ? "AVAILABLE" : status;
        this.otpPin = (otpPin == null || otpPin.isBlank()) ? "000000" : otpPin;
        this.tempC = tempC;
        this.humidityPct = humidityPct;
        this.weightKg = weightKg;
    }

    public String getBayId() {
        return bayId;
    }

    public int getRow() {
        return row;
    }

    public int getCol() {
        return col;
    }

    public CompartmentSize getSizeTier() {
        return sizeTier;
    }

    public synchronized boolean isOccupied() {
        return occupied;
    }

    public synchronized void setOccupied(boolean occupied) {
        this.occupied = occupied;
    }

    public synchronized String getCurrentAssetId() {
        return currentAssetId;
    }

    public synchronized void setCurrentAssetId(String currentAssetId) {
        this.currentAssetId = (currentAssetId == null) ? "" : currentAssetId;
    }

    public synchronized String getStatus() {
        return status;
    }

    public synchronized void setStatus(String status) {
        this.status = status;
    }

    public synchronized String getOtpPin() {
        return otpPin;
    }

    public synchronized void setOtpPin(String otpPin) {
        this.otpPin = otpPin;
    }

    public synchronized double getTempC() {
        return tempC;
    }

    public synchronized void setTempC(double tempC) {
        this.tempC = Math.round(tempC * 10.0) / 10.0;
    }

    public synchronized double getHumidityPct() {
        return humidityPct;
    }

    public synchronized void setHumidityPct(double humidityPct) {
        this.humidityPct = Math.round(humidityPct * 10.0) / 10.0;
    }

    public synchronized double getWeightKg() {
        return weightKg;
    }

    public synchronized void setWeightKg(double weightKg) {
        this.weightKg = Math.round(weightKg * 10.0) / 10.0;
    }

    public synchronized String generateNewOtp() {
        int pin = 100000 + RANDOM.nextInt(900000);
        this.otpPin = String.valueOf(pin);
        return this.otpPin;
    }

    public synchronized boolean verifyOtp(String inputPin) {
        if (inputPin == null || inputPin.isBlank()) return false;
        return this.otpPin.equals(inputPin.trim());
    }

    public String serializeToPipe() {
        return String.format("%s|%d|%d|%s|%b|%s|%s|%s|%.1f|%.1f|%.1f",
                bayId, row, col, sizeTier.name(), occupied, currentAssetId,
                status, otpPin, tempC, humidityPct, weightKg);
    }
}
