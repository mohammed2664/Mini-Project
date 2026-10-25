package rentsphere.pipeline;

/**
 * Gate 1: KYC Identity & Anti-Self-Rental Verification Gate
 * Enforces verified user identities and prevents equipment hosts from renting their own assets.
 * MCA OOT Lab - Assignment 1
 */
public class KycIdentityGate implements VerificationGate {

    @Override
    public int getGateIndex() {
        return 1;
    }

    @Override
    public String getGateName() {
        return "KycIdentityGate";
    }

    @Override
    public GateResult execute(PipelineContext ctx) {
        if (ctx.getRequesterUserId() == null || ctx.getRequesterUserId().isBlank()) {
            return GateResult.failure(1, getGateName(), "Requester identity is empty or unauthenticated.");
        }

        if (!ctx.isUserKycVerified()) {
            return GateResult.failure(1, getGateName(), 
                    "KYC Verification Required: User '" + ctx.getRequesterUserId() + "' has not completed KYC compliance.");
        }

        if (ctx.getAsset() != null && ctx.getRequesterUserId().equalsIgnoreCase(ctx.getAsset().getOwnerId())) {
            return GateResult.failure(1, getGateName(), 
                    "Anti-Self-Rental Invariant: Equipment host '" + ctx.getRequesterUserId() + "' cannot rent their own registered gear.");
        }

        return GateResult.success(1, getGateName());
    }
}
