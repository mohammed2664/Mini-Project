package rentsphere.pipeline;

import rentsphere.state.LeaseState;

/**
 * Gate 2: State Legality Gate
 * Validates whether the requested action is legal for the asset's current GoF LeaseState.
 * MCA OOT Lab - Assignment 1 & Assignment 2
 */
public class StateLegalityGate implements VerificationGate {

    @Override
    public int getGateIndex() {
        return 2;
    }

    @Override
    public String getGateName() {
        return "StateLegalityGate";
    }

    @Override
    public GateResult execute(PipelineContext ctx) {
        if (ctx.getAsset() == null) {
            return GateResult.failure(2, getGateName(), "Target asset reference is null.");
        }

        LeaseState state = ctx.getAsset().getCurrentState();
        String action = ctx.getRequestedAction().toUpperCase();

        switch (action) {
            case "RESERVE":
                if (!state.canReserve()) {
                    return GateResult.failure(2, getGateName(), 
                            "Illegal State Transition: Cannot RESERVE asset currently in '" + state.getStateName() + "' state (Must be AVAILABLE).");
                }
                break;
            case "PICKUP":
                if (!state.canPickup()) {
                    return GateResult.failure(2, getGateName(), 
                            "Illegal State Transition: Cannot PICKUP asset in '" + state.getStateName() + "' state (Must be RESERVED_ESCROW).");
                }
                break;
            case "RETURN":
                if (!state.canReturn()) {
                    return GateResult.failure(2, getGateName(), 
                            "Illegal State Transition: Cannot RETURN asset in '" + state.getStateName() + "' state (Must be ACTIVE_LEASE).");
                }
                break;
            case "INSPECT_APPROVE":
                if (!state.canApproveInspection()) {
                    return GateResult.failure(2, getGateName(), 
                            "Illegal State Transition: Cannot APPROVE asset in '" + state.getStateName() + "' state (Must be INSPECTION_QUARANTINE).");
                }
                break;
            case "INSPECT_SLASH":
                if (!state.canSlashInspection()) {
                    return GateResult.failure(2, getGateName(), 
                            "Illegal State Transition: Cannot SLASH asset in '" + state.getStateName() + "' state (Must be INSPECTION_QUARANTINE).");
                }
                break;
            default:
                return GateResult.failure(2, getGateName(), "Unknown requested lifecycle action: " + action);
        }

        return GateResult.success(2, getGateName());
    }
}
