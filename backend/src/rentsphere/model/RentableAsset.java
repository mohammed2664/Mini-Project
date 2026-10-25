package rentsphere.model;

import rentsphere.state.AvailableState;
import rentsphere.state.LeaseState;

import java.util.ArrayList;
import java.util.List;

/**
 * Tier 2: Rentable Asset
 * Core rental context holding GoF LeaseState, INR rates, assigned Hub Bay,
 * and Prototype Pattern cloning hooks.
 * MCA OOT Lab - Assignment 2 (5-Tier Inheritance Hierarchy & State Pattern)
 */
public abstract class RentableAsset extends AbstractDomainEntity implements Cloneable {
    protected String title;
    protected String ownerId;
    protected String renterId;
    protected double baseDailyRate;        // In Indian Rupees (₹ INR)
    protected double securityCollateral;    // Refundable Security Deposit in ₹ INR
    protected String lockerCoordinate;     // Hub Compartment / Ground Slot (e.g., A1-H8)
    protected LeaseState currentState;     // GoF State Context
    protected List<String> serviceLogs;     // Mutable maintenance & dispatch history
    protected int rentalDays = 1;          // Active reservation duration in days

    public RentableAsset(String id, String title, String ownerId, double baseDailyRate, 
                         double securityCollateral, String lockerCoordinate) {
        super(id);
        this.title = title;
        this.ownerId = ownerId;
        this.renterId = "";
        this.baseDailyRate = baseDailyRate;
        this.securityCollateral = securityCollateral;
        this.lockerCoordinate = lockerCoordinate;
        this.currentState = new AvailableState();
        this.serviceLogs = new ArrayList<>();
        this.serviceLogs.add("Commissioning Log: QA verified at Goa University Rental Hub");
        this.rentalDays = 1;
    }

    public int getRentalDays() {
        return rentalDays > 0 ? rentalDays : 1;
    }

    public void setRentalDays(int rentalDays) {
        this.rentalDays = Math.max(1, rentalDays);
        touch();
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
        touch();
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
        touch();
    }

    public String getRenterId() {
        return renterId;
    }

    public void setRenterId(String renterId) {
        this.renterId = renterId;
        touch();
    }

    public double getBaseDailyRate() {
        return baseDailyRate;
    }

    public void setBaseDailyRate(double baseDailyRate) {
        this.baseDailyRate = baseDailyRate;
        touch();
    }

    public double getSecurityCollateral() {
        return securityCollateral;
    }

    public void setSecurityCollateral(double securityCollateral) {
        this.securityCollateral = securityCollateral;
        touch();
    }

    public String getLockerCoordinate() {
        return lockerCoordinate;
    }

    public void setLockerCoordinate(String lockerCoordinate) {
        this.lockerCoordinate = lockerCoordinate;
        touch();
    }

    public LeaseState getCurrentState() {
        return currentState;
    }

    public void setState(LeaseState newState) {
        this.currentState = newState;
        touch();
    }

    public List<String> getServiceLogs() {
        return serviceLogs;
    }

    // Backwards-compatible alias for reflection / meta tooling
    public List<String> getMaintenanceLogs() {
        return serviceLogs;
    }

    public void addServiceLog(String entry) {
        this.serviceLogs.add(entry);
        touch();
    }

    public void addMaintenanceLog(String entry) {
        addServiceLog(entry);
    }

    // ========================================================================
    // POLYMORPHIC RATE & COLLATERAL FORMULAS (INR ₹)
    // ========================================================================
    public abstract double calculateDailyRentalTariff(int rentalDays);

    public double calculateRequiredCollateral() {
        return securityCollateral;
    }

    public double calculateDamageSlashingPenalty(double damageSeverityPercentage) {
        double clamped = Math.max(0.0, Math.min(100.0, damageSeverityPercentage));
        return Math.round(securityCollateral * (clamped / 100.0) * 100.0) / 100.0;
    }

    // ========================================================================
    // PROTOTYPE PATTERN: SHALLOW VS DEEP CLONING (Assignment 4)
    // ========================================================================
    /**
     * Shallow Clone: Uses standard Object.clone().
     * The cloned instance shares the serviceLogs list reference with the parent!
     */
    public RentableAsset shallowClone() {
        try {
            return (RentableAsset) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException("Shallow clone failed", e);
        }
    }

    /**
     * Deep Clone: Deeply duplicates internal mutable objects (serviceLogs).
     * Guarantees total memory isolation for multi-unit inventory duplicate listings.
     */
    public RentableAsset deepClone() {
        try {
            RentableAsset cloned = (RentableAsset) super.clone();
            cloned.serviceLogs = new ArrayList<>(this.serviceLogs);
            return cloned;
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException("Deep clone failed", e);
        }
    }
}
