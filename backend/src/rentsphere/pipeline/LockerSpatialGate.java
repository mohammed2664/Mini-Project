package rentsphere.pipeline;

import rentsphere.model.UtilityAndMobilityAsset;

/**
 * Gate 4: Locker & Hub Bay Spatial Gate
 * Validates compartment coordinate validity and enforces the Row H Ground Parking Slot Invariant.
 * Vehicles and heavy equipment requiring ground access cannot be placed in upper lockers (Rows A-G).
 * MCA OOT Lab - Assignment 1 & Assignment 5
 */
public class LockerSpatialGate implements VerificationGate {

    @Override
    public int getGateIndex() {
        return 4;
    }

    @Override
    public String getGateName() {
        return "LockerSpatialGate";
    }

    @Override
    public GateResult execute(PipelineContext ctx) {
        String bay = ctx.getTargetLockerBay();
        if (bay == null || bay.trim().length() < 2) {
            bay = (ctx.getAsset() != null) ? ctx.getAsset().getLockerCoordinate() : "";
        }

        if (bay == null || bay.trim().length() < 2) {
            return GateResult.failure(4, getGateName(), "Hub bay coordinate is empty or unassigned.");
        }

        bay = bay.trim().toUpperCase();
        char rowChar = bay.charAt(0);
        char colChar = bay.charAt(1);

        int rowIndex = rowChar - 'A';
        int colIndex = colChar - '1';

        if (rowIndex < 0 || rowIndex > 7 || colIndex < 0 || colIndex > 7) {
            return GateResult.failure(4, getGateName(), 
                    "Invalid 8x8 Smart Hub Coordinate '" + bay + "'. Row must be A-H and Column 1-8.");
        }

        // ENFORCE ROW H GROUND PARKING / HEAVY ACCESS SPATIAL INVARIANT
        if (ctx.getAsset() instanceof UtilityAndMobilityAsset) {
            UtilityAndMobilityAsset utilAsset = (UtilityAndMobilityAsset) ctx.getAsset();
            if (utilAsset.isRequiresGroundBay()) {
                if (rowIndex != 7) { // Row 7 corresponds to 'H'
                    return GateResult.failure(4, getGateName(), 
                            "Ground Bay Spatial Invariant: Vehicle/Heavy Event Item '" + utilAsset.getTitle() + 
                            "' is physically barred from Upper Locker Bay " + rowChar + 
                            ". Mobility gear and vehicles must be parked exclusively in Row H Ground Slots (H1–H8).");
                }
            }
        }

        return GateResult.success(4, getGateName());
    }
}
