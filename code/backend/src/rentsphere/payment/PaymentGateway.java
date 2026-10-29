package rentsphere.payment;

/**
 * Standard Payment Gateway Provider Abstraction for India (Razorpay / Cashfree API specification).
 */
public interface PaymentGateway {

    /**
     * Creates an authorized payment order with separated rental tariff and refundable security deposit in ₹ INR.
     */
    PaymentOrder createOrder(String bookingId, String assetId, String renterId, String ownerId, 
                             double rentalTariff, double securityDeposit);

    /**
     * Verifies cryptographic signature for a client payment response.
     */
    boolean verifyPaymentSignature(String orderId, String paymentId, String signature);

    /**
     * Initiates a refund of security deposit or canceled booking fee.
     */
    boolean initiateRefund(String orderId, double amountInr, String reason);

    /**
     * Handles signed webhook events idempotently.
     */
    boolean processWebhook(String eventType, String orderId, String paymentId, String signature);

    /**
     * Retrieves existing order by ID.
     */
    PaymentOrder getOrder(String orderId);
}
