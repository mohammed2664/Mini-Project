package rentsphere.pipeline;

/**
 * Gate execution result indicating pass/fail, gate number, and diagnostic message.
 * MCA OOT Lab - Assignment 1 (Fail-Fast Verification Pipeline)
 */
public class GateResult {
    private final boolean passed;
    private final int gateIndex;
    private final String gateName;
    private final String diagnostic;

    private GateResult(boolean passed, int gateIndex, String gateName, String diagnostic) {
        this.passed = passed;
        this.gateIndex = gateIndex;
        this.gateName = gateName;
        this.diagnostic = diagnostic;
    }

    public static GateResult success(int gateIndex, String gateName) {
        return new GateResult(true, gateIndex, gateName, "Gate passed successfully");
    }

    public static GateResult failure(int gateIndex, String gateName, String diagnostic) {
        return new GateResult(false, gateIndex, gateName, diagnostic);
    }

    public boolean isPassed() {
        return passed;
    }

    public int getGateIndex() {
        return gateIndex;
    }

    public String getGateName() {
        return gateName;
    }

    public String getDiagnostic() {
        return diagnostic;
    }

    @Override
    public String toString() {
        return String.format("GateResult[Gate %d: %s | %s | %s]", 
                gateIndex, gateName, passed ? "PASS" : "FAIL", diagnostic);
    }
}
