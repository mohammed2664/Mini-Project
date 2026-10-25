package rentsphere.pipeline;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Sequential Fail-Fast Verification Pipeline Orchestrator.
 * Executes Gate 1 -> Gate 2 -> Gate 3 -> Gate 4.
 * Immediately short-circuits on the first failure.
 * MCA OOT Lab - Assignment 1
 */
public class FailFastPipeline {
    private final List<VerificationGate> gates;

    public FailFastPipeline() {
        this.gates = new ArrayList<>();
        this.gates.add(new KycIdentityGate());     // Gate 1
        this.gates.add(new StateLegalityGate());    // Gate 2
        this.gates.add(new EscrowSolvencyGate());   // Gate 3
        this.gates.add(new LockerSpatialGate());    // Gate 4
    }

    public List<VerificationGate> getRegisteredGates() {
        return Collections.unmodifiableList(gates);
    }

    /**
     * Executes all registered gates in sequential order.
     * Short-circuits immediately upon the first gate failure.
     */
    public GateResult verify(PipelineContext ctx) {
        for (VerificationGate gate : gates) {
            GateResult result = gate.execute(ctx);
            if (!result.isPassed()) {
                // Short-Circuit Abort
                return result;
            }
        }
        return GateResult.success(0, "AllGatesPassed");
    }
}
