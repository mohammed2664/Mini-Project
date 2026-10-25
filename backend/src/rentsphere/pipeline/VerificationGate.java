package rentsphere.pipeline;

/**
 * Interface contract for short-circuiting verification gates.
 * MCA OOT Lab - Assignment 1
 */
public interface VerificationGate {
    int getGateIndex();
    String getGateName();
    GateResult execute(PipelineContext ctx);
}
