package rentsphere.model;

import rentsphere.state.AvailableState;
import rentsphere.state.LeaseState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Tier 2: Rentable Asset
 * Holds GoF State context, rates, locker coordinate, and cloning hooks.
 * MCA OOT Lab - Assignment 2 & Assignment 4 (Cloning)
 */
public abstract class RentableAsset extends AbstractDomainEntity implements Cloneable {
    protected String title;
    protected String ownerId;
    protected String renterId;
    protected double baseDailyRate;
    protected double securityCollateral;
    protected String lockerCoordinate;
    protected LeaseState currentState;
    protected List<String> maintenanceLogs;

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
        this.maintenanceLogs = new ArrayList<>();
        this.maintenanceLogs.add("Commissioning Log: Asset certified at Goa Central Depot");
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

    public List<String> getMaintenanceLogs() {
        return maintenanceLogs;
    }

    public void addMaintenanceLog(String entry) {
        this.maintenanceLogs.add(entry);
        touch();
    }

    // ========================================================================
    // POLYMORPHIC RATE & COLLATERAL FORMULAS (Assignment 2)
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
    // SHALLOW VS DEEP CLONING (Assignment 4)
    // ========================================================================
    /**
     * Shallow Clone: Uses standard Object.clone().
     * The cloned instance shares the maintenanceLogs reference with the parent!
     */
    public RentableAsset shallowClone() {
        try {
            return (RentableAsset) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException("Shallow clone failed", e);
        }
    }

    /**
     * Deep Clone: Deeply duplicates internal mutable objects (maintenanceLogs).
     * Guarantees total memory isolation.
     */
    public RentableAsset deepClone() {
        try {
            RentableAsset cloned = (RentableAsset) super.clone();
            cloned.maintenanceLogs = new ArrayList<>(this.maintenanceLogs);
            return cloned;
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException("Deep clone failed", e);
        }
    }
}
