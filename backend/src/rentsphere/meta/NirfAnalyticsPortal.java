package rentsphere.meta;

/**
 * Ministry of Education NIRF Institutional Analytics Portal.
 * Computes official composite scores using standard weights:
 * Score = (TLR * 0.30) + (RP * 0.30) + (GO * 0.20) + (OI * 0.10) + (PR * 0.10)
 * Uses deep cloning for What-If scenario modeling without mutating baseline records.
 * MCA OOT Lab - Assignment 4
 */
public class NirfAnalyticsPortal {

    public static class InstitutionMetrics implements Cloneable {
        private String institutionName;
        private double tlr; // Teaching, Learning & Resources (30%)
        private double rp;  // Research & Professional Practice (30%)
        private double go;  // Graduation Outcomes (20%)
        private double oi;  // Outreach & Inclusivity (10%)
        private double pr;  // Peer Perception (10%)

        public InstitutionMetrics(String institutionName, double tlr, double rp, double go, double oi, double pr) {
            this.institutionName = institutionName;
            this.tlr = Math.max(0.0, Math.min(100.0, tlr));
            this.rp = Math.max(0.0, Math.min(100.0, rp));
            this.go = Math.max(0.0, Math.min(100.0, go));
            this.oi = Math.max(0.0, Math.min(100.0, oi));
            this.pr = Math.max(0.0, Math.min(100.0, pr));
        }

        public String getInstitutionName() {
            return institutionName;
        }

        public void setInstitutionName(String institutionName) {
            this.institutionName = institutionName;
        }

        public double getTlr() {
            return tlr;
        }

        public void setTlr(double tlr) {
            this.tlr = Math.max(0.0, Math.min(100.0, tlr));
        }

        public double getRp() {
            return rp;
        }

        public void setRp(double rp) {
            this.rp = Math.max(0.0, Math.min(100.0, rp));
        }

        public double getGo() {
            return go;
        }

        public void setGo(double go) {
            this.go = Math.max(0.0, Math.min(100.0, go));
        }

        public double getOi() {
            return oi;
        }

        public void setOi(double oi) {
            this.oi = Math.max(0.0, Math.min(100.0, oi));
        }

        public double getPr() {
            return pr;
        }

        public void setPr(double pr) {
            this.pr = Math.max(0.0, Math.min(100.0, pr));
        }

        public double computeCompositeScore() {
            double composite = (tlr * 0.30) + (rp * 0.30) + (go * 0.20) + (oi * 0.10) + (pr * 0.10);
            return Math.round(composite * 100.0) / 100.0;
        }

        public String computeTierBadge() {
            double score = computeCompositeScore();
            if (score >= 75.0) return "Top 10 National Excellence";
            if (score >= 60.0) return "Tier 1 High Performance";
            if (score >= 45.0) return "Tier 2 Promising Institution";
            return "Tier 3 Regional Institution";
        }

        public InstitutionMetrics deepClone() {
            try {
                return (InstitutionMetrics) super.clone();
            } catch (CloneNotSupportedException e) {
                return new InstitutionMetrics(institutionName, tlr, rp, go, oi, pr);
            }
        }
    }

    public static class SimulationResult {
        public InstitutionMetrics baseline;
        public InstitutionMetrics simulated;
        public double baselineScore;
        public double simulatedScore;
        public double scoreDelta;
        public String baselineTier;
        public String simulatedTier;
        public String policyRecommendation;
    }

    /**
     * Executes a deep-cloned What-If policy simulation.
     */
    public static SimulationResult simulateWhatIf(InstitutionMetrics baseline, 
                                                  double deltaTlr, double deltaRp, 
                                                  double deltaGo, double deltaOi, double deltaPr) {
        // Deep clone guarantees that baseline is NEVER modified
        InstitutionMetrics sim = baseline.deepClone();
        sim.setInstitutionName(baseline.getInstitutionName() + " (What-If Simulation)");
        sim.setTlr(sim.getTlr() + deltaTlr);
        sim.setRp(sim.getRp() + deltaRp);
        sim.setGo(sim.getGo() + deltaGo);
        sim.setOi(sim.getOi() + deltaOi);
        sim.setPr(sim.getPr() + deltaPr);

        SimulationResult res = new SimulationResult();
        res.baseline = baseline;
        res.simulated = sim;
        res.baselineScore = baseline.computeCompositeScore();
        res.simulatedScore = sim.computeCompositeScore();
        res.scoreDelta = Math.round((res.simulatedScore - res.baselineScore) * 100.0) / 100.0;
        res.baselineTier = baseline.computeTierBadge();
        res.simulatedTier = sim.computeTierBadge();

        if (res.scoreDelta > 0.0) {
            res.policyRecommendation = String.format("Simulation demonstrates positive rank shift (+%.2f pts). Projected Tier: %s",
                    res.scoreDelta, res.simulatedTier);
        } else {
            res.policyRecommendation = "Simulation indicates stable or decreased score. Recommend focusing on Research (RP) and Teaching (TLR).";
        }

        return res;
    }
}
