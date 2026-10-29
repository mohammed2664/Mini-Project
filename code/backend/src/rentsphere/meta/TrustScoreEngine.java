package rentsphere.meta;

/**
 * RentSphere Trust & Asset Reliability Engine.
 * Computes official community trust scores out of 100 using weighted composite parameters:
 *  - Condition History (30%)
 *  - On-Time Return Rate (30%)
 *  - Escrow Completion History (20%)
 *  - Peer / Community Rating (10%)
 *  - KYC & Verification Tier (10%)
 * 
 * Employs Prototype Pattern Deep Cloning for What-If scenario simulations
 * ensuring baseline member metrics remain unmutated during experimentation.
 * MCA OOT Lab - Assignment 4 (Meta-OOP & Composite Metric Modeling)
 */
public class TrustScoreEngine {

    public static class TrustMetrics implements Cloneable {
        private String memberId;
        private String memberName;
        private double conditionHistory;    // 0-100 (Gear return condition integrity, 30%)
        private double punctualityRate;     // 0-100 (On-time pickup and return, 30%)
        private double escrowCompletion;    // 0-100 (Clean transactions without dispute, 20%)
        private double peerRating;          // 0-100 (Community feedback and reviews, 10%)
        private double kycTier;             // 0-100 (Identity & physical verification level, 10%)

        public TrustMetrics(String memberId, String memberName, double conditionHistory, 
                            double punctualityRate, double escrowCompletion, double peerRating, double kycTier) {
            this.memberId = memberId;
            this.memberName = memberName;
            this.conditionHistory = Math.max(0.0, Math.min(100.0, conditionHistory));
            this.punctualityRate = Math.max(0.0, Math.min(100.0, punctualityRate));
            this.escrowCompletion = Math.max(0.0, Math.min(100.0, escrowCompletion));
            this.peerRating = Math.max(0.0, Math.min(100.0, peerRating));
            this.kycTier = Math.max(0.0, Math.min(100.0, kycTier));
        }

        public String getMemberId() {
            return memberId;
        }

        public String getMemberName() {
            return memberName;
        }

        public double getConditionHistory() {
            return conditionHistory;
        }

        public void setConditionHistory(double conditionHistory) {
            this.conditionHistory = Math.max(0.0, Math.min(100.0, conditionHistory));
        }

        public double getPunctualityRate() {
            return punctualityRate;
        }

        public void setPunctualityRate(double punctualityRate) {
            this.punctualityRate = Math.max(0.0, Math.min(100.0, punctualityRate));
        }

        public double getEscrowCompletion() {
            return escrowCompletion;
        }

        public void setEscrowCompletion(double escrowCompletion) {
            this.escrowCompletion = Math.max(0.0, Math.min(100.0, escrowCompletion));
        }

        public double getPeerRating() {
            return peerRating;
        }

        public void setPeerRating(double peerRating) {
            this.peerRating = Math.max(0.0, Math.min(100.0, peerRating));
        }

        public double getKycTier() {
            return kycTier;
        }

        public void setKycTier(double kycTier) {
            this.kycTier = Math.max(0.0, Math.min(100.0, kycTier));
        }

        public double computeTrustScore() {
            double composite = (conditionHistory * 0.30) + 
                               (punctualityRate * 0.30) + 
                               (escrowCompletion * 0.20) + 
                               (peerRating * 0.10) + 
                               (kycTier * 0.10);
            return Math.round(composite * 100.0) / 100.0;
        }

        public String computeTierBadge() {
            double score = computeTrustScore();
            if (score >= 85.0) return "Elite Verified Host / Renter";
            if (score >= 70.0) return "Trusted Community Member";
            if (score >= 50.0) return "Standard Active Member";
            return "Probationary Review Required";
        }

        public TrustMetrics deepClone() {
            try {
                return (TrustMetrics) super.clone();
            } catch (CloneNotSupportedException e) {
                return new TrustMetrics(memberId, memberName, conditionHistory, punctualityRate, escrowCompletion, peerRating, kycTier);
            }
        }
    }

    public static class SimulationResult {
        public TrustMetrics baseline;
        public TrustMetrics simulated;
        public double baselineScore;
        public double simulatedScore;
        public double scoreDelta;
        public String baselineTier;
        public String simulatedTier;
        public String recommendation;
    }

    /**
     * Executes a deep-cloned What-If policy simulation.
     */
    public static SimulationResult simulateWhatIf(TrustMetrics baseline, 
                                                  double deltaCondition, double deltaPunctuality, 
                                                  double deltaEscrow, double deltaPeer, double deltaKyc) {
        // Deep clone guarantees baseline metrics are never corrupted
        TrustMetrics sim = baseline.deepClone();
        sim.setConditionHistory(sim.getConditionHistory() + deltaCondition);
        sim.setPunctualityRate(sim.getPunctualityRate() + deltaPunctuality);
        sim.setEscrowCompletion(sim.getEscrowCompletion() + deltaEscrow);
        sim.setPeerRating(sim.getPeerRating() + deltaPeer);
        sim.setKycTier(sim.getKycTier() + deltaKyc);

        SimulationResult res = new SimulationResult();
        res.baseline = baseline;
        res.simulated = sim;
        res.baselineScore = baseline.computeTrustScore();
        res.simulatedScore = sim.computeTrustScore();
        res.scoreDelta = Math.round((res.simulatedScore - res.baselineScore) * 100.0) / 100.0;
        res.baselineTier = baseline.computeTierBadge();
        res.simulatedTier = sim.computeTierBadge();

        if (res.scoreDelta > 0.0) {
            res.recommendation = String.format("Simulation demonstrates positive Trust Tier shift (+%.2f pts). Projected Tier: %s",
                    res.scoreDelta, res.simulatedTier);
        } else {
            res.recommendation = "Simulation indicates stable or decreased trust score. Recommend maintaining on-time returns and condition integrity.";
        }

        return res;
    }
}
