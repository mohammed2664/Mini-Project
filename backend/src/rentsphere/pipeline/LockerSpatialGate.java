package rentsphere.pipeline;

import rentsphere.model.AgricultureAsset;

/**
 * Gate 4: Locker Spatial Gate
 * Validates compartment coordinate validity and enforces the Row 7 Heavy-Agri Spatial Invariant.
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
            return GateResult.failure(4, getGateName(), "Locker bay coordinate is empty or unassigned.");
        }

        bay = bay.trim().toUpperCase();
        char rowChar = bay.charAt(0);
        char colChar = bay.charAt(1);

        int rowIndex = rowChar - 'A';
        int colIndex = colChar - '1';

        if (rowIndex < 0 || rowIndex > 7 || colIndex < 0 || colIndex > 7) {
            return GateResult.failure(4, getGateName(), 
                    "Invalid 8x8 Smart Locker Coordinate '" + bay + "'. Row must be A-H and Column 1-8.");
        }

        // ENFORCE ROW 7 HEAVY-AGRI SPATIAL INVARIANT
        if (ctx.getAsset() instanceof AgricultureAsset) {
            AgricultureAsset agri = (AgricultureAsset) ctx.getAsset();
            if (agri.isHeavyMachineryFlag()) {
                if (rowIndex != 7) { // Row 7 corresponds to 'H'
                    return GateResult.failure(4, getGateName(), 
                            "Heavy Agricultural Machinery Spatial Invariant: Heavy implement '" + agri.getTitle() + 
                            "' is physically barred from Row " + rowChar + 
                            ". Heavy gear must be assigned exclusively to XL bays in Row H (Row 7).");
                }
            }
        }

        return GateResult.success(4, getGateName());
    }
}
