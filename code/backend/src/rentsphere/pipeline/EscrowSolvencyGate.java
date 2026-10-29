package rentsphere.pipeline;

/**
 * Gate 3: Escrow Solvency Gate
 * Verifies that the renter's wallet holds sufficient liquidity to cover the
 * polymorphic rental tariff plus the security collateral deposit.
 * MCA OOT Lab - Assignment 1 & Assignment 2
 */
public class EscrowSolvencyGate implements VerificationGate {

    @Override
    public int getGateIndex() {
        return 3;
    }

    @Override
    public String getGateName() {
        return "EscrowSolvencyGate";
    }

    @Override
    public GateResult execute(PipelineContext ctx) {
        // Escrow solvency is strictly required during the RESERVE action
        if (!"RESERVE".equalsIgnoreCase(ctx.getRequestedAction())) {
            return GateResult.success(3, getGateName());
        }

        if (ctx.getAsset() == null) {
            return GateResult.failure(3, getGateName(), "Asset is null.");
        }

        double tariff = ctx.getAsset().calculateDailyRentalTariff(ctx.getRentalDays());
        double collateral = ctx.getAsset().calculateRequiredCollateral();
        double requiredTotal = Math.round((tariff + collateral) * 100.0) / 100.0;
        double balance = ctx.getUserWalletBalance();

        if (balance < requiredTotal) {
            String msg = String.format("Insolvent Wallet: Required $%.2f (Tariff: $%.2f [%d days] + Collateral: $%.2f), but wallet balance is only $%.2f.",
                    requiredTotal, tariff, ctx.getRentalDays(), collateral, balance);
            return GateResult.failure(3, getGateName(), msg);
        }

        return GateResult.success(3, getGateName());
    }
}
