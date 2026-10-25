package rentsphere.model;

/**
 * Tier 5 Leaf Class: Event and Mobility Vehicle
 * Wedding/event self-drive cars, utility vans, and campus electric scooters.
 * CRITICAL SPATIAL INVARIANT: requiresGroundBay is locked to true.
 * Vehicles MUST be assigned exclusively to Row H (Ground Parking Slots H1-H8).
 * MCA OOT Lab - Assignment 2 & Assignment 5
 */
public class EventAndMobilityVehicle extends UtilityAndMobilityAsset {
    private int seatingCapacity;
    private String fuelOrChargingType;
    private boolean fastChargingOrAuto;

    public EventAndMobilityVehicle(String id, String title, String ownerId, double baseDailyRate, 
                                  double securityCollateral, String lockerCoordinate, 
                                  double lateFeeMultiplier, String protectionTier,
                                  String powerOrEngineSpec, String safetyCertification,
                                  int seatingCapacity, String fuelOrChargingType, boolean fastChargingOrAuto) {
        super(id, title, ownerId, baseDailyRate, securityCollateral, lockerCoordinate, 
              "MOBILITY_VEHICLES", lateFeeMultiplier, protectionTier, powerOrEngineSpec, safetyCertification, true);
        this.seatingCapacity = seatingCapacity;
        this.fuelOrChargingType = fuelOrChargingType;
        this.fastChargingOrAuto = fastChargingOrAuto;
    }

    public EventAndMobilityVehicle(String id, String title, String ownerId, double baseDailyRate, 
                                  double securityCollateral, String lockerCoordinate, 
                                  double lateFeeMultiplier, String protectionTier,
                                  int seatingCapacity, String fuelOrChargingType) {
        this(id, title, ownerId, baseDailyRate, securityCollateral, lockerCoordinate,
             lateFeeMultiplier, protectionTier, "1.2L Engine / Electric Motor", "ARAI 5-Star",
             seatingCapacity, fuelOrChargingType, true);
    }

    public int getSeatingCapacity() {
        return seatingCapacity;
    }

    public void setSeatingCapacity(int seatingCapacity) {
        this.seatingCapacity = seatingCapacity;
        touch();
    }

    public String getFuelOrChargingType() {
        return fuelOrChargingType;
    }

    public void setFuelOrChargingType(String fuelOrChargingType) {
        this.fuelOrChargingType = fuelOrChargingType;
        touch();
    }

    public boolean isFastChargingOrAuto() {
        return fastChargingOrAuto;
    }

    public void setFastChargingOrAuto(boolean fastChargingOrAuto) {
        this.fastChargingOrAuto = fastChargingOrAuto;
        touch();
    }

    @Override
    public double calculateDailyRentalTariff(int rentalDays) {
        double featureMultiplier = fastChargingOrAuto ? 1.15 : 1.0;
        double raw = baseDailyRate * featureMultiplier * rentalDays;
        return Math.round(raw * 100.0) / 100.0;
    }

    @Override
    public double calculateRequiredCollateral() {
        // Vehicle security collateral carries insurance bond multiplier (1.20)
        return Math.round(securityCollateral * 1.20 * 100.0) / 100.0;
    }

    @Override
    public String serializeToPipe() {
        String logs = String.join(",", serviceLogs);
        String extra = String.format("seats=%d;fuel=%s;auto=%b;power=%s;safety=%s",
                seatingCapacity, fuelOrChargingType, fastChargingOrAuto, powerOrEngineSpec, safetyCertification);
        return String.join("|",
                id, title, category, "EventAndMobilityVehicle", ownerId, renterId,
                currentState.getStateName(), String.format("%.2f", baseDailyRate),
                String.format("%.2f", securityCollateral), lockerCoordinate, extra, logs);
    }
}
