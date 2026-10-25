package rentsphere.payment;

/**
 * Payment Order Model representing an authorized or pending transaction in ₹ INR.
 */
public class PaymentOrder {
    private final String orderId;
    private final String bookingId;
    private final String assetId;
    private final String renterId;
    private final String ownerId;
    private final double rentalTariff;       // ₹ INR Rental fee
    private final double securityDeposit;    // ₹ INR Refundable deposit
    private final double totalAmount;        // ₹ INR Total
    private final String currency;           // "INR"
    private String status;                   // "CREATED", "AUTHORIZED", "CAPTURED", "REFUNDED", "FAILED"
    private String paymentId;
    private final long createdAt;
    private long completedAt;

    public PaymentOrder(String orderId, String bookingId, String assetId, String renterId, 
                        String ownerId, double rentalTariff, double securityDeposit) {
        this.orderId = orderId;
        this.bookingId = bookingId;
        this.assetId = assetId;
        this.renterId = renterId;
        this.ownerId = ownerId;
        this.rentalTariff = Math.round(rentalTariff * 100.0) / 100.0;
        this.securityDeposit = Math.round(securityDeposit * 100.0) / 100.0;
        this.totalAmount = Math.round((rentalTariff + securityDeposit) * 100.0) / 100.0;
        this.currency = "INR";
        this.status = "CREATED";
        this.createdAt = System.currentTimeMillis();
    }

    public String getOrderId() { return orderId; }
    public String getBookingId() { return bookingId; }
    public String getAssetId() { return assetId; }
    public String getRenterId() { return renterId; }
    public String getOwnerId() { return ownerId; }
    public double getRentalTariff() { return rentalTariff; }
    public double getSecurityDeposit() { return securityDeposit; }
    public double getTotalAmount() { return totalAmount; }
    public String getCurrency() { return currency; }
    public synchronized String getStatus() { return status; }
    public synchronized void setStatus(String status) { this.status = status; }
    public synchronized String getPaymentId() { return paymentId; }
    public synchronized void setPaymentId(String paymentId) { this.paymentId = paymentId; }
    public long getCreatedAt() { return createdAt; }
    public synchronized long getCompletedAt() { return completedAt; }
    public synchronized void setCompletedAt(long completedAt) { this.completedAt = completedAt; }

    public String serializeToPipe() {
        return String.format("%s|%s|%s|%s|%s|%.2f|%.2f|%.2f|%s|%s|%s|%d|%d",
                orderId, bookingId, assetId, renterId, ownerId, rentalTariff, securityDeposit, 
                totalAmount, currency, status, paymentId != null ? paymentId : "", createdAt, completedAt);
    }
}
