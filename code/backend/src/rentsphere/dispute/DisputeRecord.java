package rentsphere.dispute;

/**
 * Dispute Record Entity tracking contested damage claims and deposit settlements.
 */
public class DisputeRecord {
    private final String disputeId;
    private final String assetId;
    private final String renterId;
    private final String ownerId;
    private final double heldDeposit;
    private final double claimedDeduction;
    private final String renterReason;
    private final String renterEvidenceNotes;
    private String status; // "OPEN", "UNDER_REVIEW", "RESOLVED_REFUND", "RESOLVED_SLASH"
    private String adminNotes;
    private final long createdAt;
    private long resolvedAt;

    public DisputeRecord(String disputeId, String assetId, String renterId, String ownerId, 
                         double heldDeposit, double claimedDeduction, String renterReason, 
                         String renterEvidenceNotes) {
        this.disputeId = disputeId;
        this.assetId = assetId;
        this.renterId = renterId;
        this.ownerId = ownerId;
        this.heldDeposit = heldDeposit;
        this.claimedDeduction = claimedDeduction;
        this.renterReason = renterReason;
        this.renterEvidenceNotes = (renterEvidenceNotes == null) ? "" : renterEvidenceNotes;
        this.status = "OPEN";
        this.adminNotes = "";
        this.createdAt = System.currentTimeMillis();
    }

    public String getDisputeId() { return disputeId; }
    public String getAssetId() { return assetId; }
    public String getRenterId() { return renterId; }
    public String getOwnerId() { return ownerId; }
    public double getHeldDeposit() { return heldDeposit; }
    public double getClaimedDeduction() { return claimedDeduction; }
    public String getRenterReason() { return renterReason; }
    public String getRenterEvidenceNotes() { return renterEvidenceNotes; }
    public synchronized String getStatus() { return status; }
    public synchronized void setStatus(String status) { this.status = status; }
    public synchronized String getAdminNotes() { return adminNotes; }
    public synchronized void setAdminNotes(String adminNotes) { this.adminNotes = adminNotes; }
    public long getCreatedAt() { return createdAt; }
    public synchronized long getResolvedAt() { return resolvedAt; }
    public synchronized void setResolvedAt(long resolvedAt) { this.resolvedAt = resolvedAt; }

    public String serializeToPipe() {
        return String.format("%s|%s|%s|%s|%.2f|%.2f|%s|%s|%s|%s|%d|%d",
                disputeId, assetId, renterId, ownerId, heldDeposit, claimedDeduction,
                renterReason.replace("|", "/"), renterEvidenceNotes.replace("|", "/"),
                status, adminNotes.replace("|", "/"), createdAt, resolvedAt);
    }
}
