package rentsphere.model;

/**
 * Tier 4A: Tourism Asset
 * Marine & coastal tourism equipment with waterproof ratings and corrosion factors.
 * MCA OOT Lab - Assignment 2 (5-Tier Inheritance Hierarchy)
 */
public abstract class TourismAsset extends SectoralAsset {
    protected String waterproofIpRating;
    protected double saltwaterCorrosionFactor;

    public TourismAsset(String id, String title, String ownerId, double baseDailyRate, 
                        double securityCollateral, String lockerCoordinate, 
                        double seasonalDemandMultiplier, String insuranceTier,
                        String waterproofIpRating, double saltwaterCorrosionFactor) {
        super(id, title, ownerId, baseDailyRate, securityCollateral, lockerCoordinate, 
              "TOURISM", seasonalDemandMultiplier, insuranceTier);
        this.waterproofIpRating = waterproofIpRating;
        this.saltwaterCorrosionFactor = saltwaterCorrosionFactor;
    }

    public String getWaterproofIpRating() {
        return waterproofIpRating;
    }

    public void setWaterproofIpRating(String waterproofIpRating) {
        this.waterproofIpRating = waterproofIpRating;
        touch();
    }

    public double getSaltwaterCorrosionFactor() {
        return saltwaterCorrosionFactor;
    }

    public void setSaltwaterCorrosionFactor(double saltwaterCorrosionFactor) {
        this.saltwaterCorrosionFactor = saltwaterCorrosionFactor;
        touch();
    }
}
