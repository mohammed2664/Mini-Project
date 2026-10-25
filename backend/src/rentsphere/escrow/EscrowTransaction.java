package rentsphere.escrow;

import java.time.Instant;
import java.util.UUID;

/**
 * Immutable transaction record representing an escrow operation.
 * MCA OOT Lab - Assignment 2
 */
public class EscrowTransaction {
    private final String txId;
    private final String timestamp;
    private final String payerId;
    private final String payeeId;
    private final String assetId;
    private final double amount;
    private final String type; // "LOCK", "PAYOUT_CLEAN", "PAYOUT_SLASHED", "REFUND"
    private final String details;

    public EscrowTransaction(String payerId, String payeeId, String assetId, 
                             double amount, String type, String details) {
        this.txId = "TX-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        this.timestamp = Instant.now().toString();
        this.payerId = payerId;
        this.payeeId = payeeId;
        this.assetId = assetId;
        this.amount = Math.round(amount * 100.0) / 100.0;
        this.type = type;
        this.details = details;
    }

    public String getTxId() {
        return txId;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getPayerId() {
        return payerId;
    }

    public String getPayeeId() {
        return payeeId;
    }

    public String getAssetId() {
        return assetId;
    }

    public double getAmount() {
        return amount;
    }

    public String getType() {
        return type;
    }

    public String getDetails() {
        return details;
    }
}
