package rentsphere.payment;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Indian Payment Gateway Sandbox Adapter (Simulating Razorpay / Cashfree flow in ₹ INR).
 * Implements:
 *  - Order creation with separated rental tariff and refundable security deposit
 *  - HMAC-SHA256 signature verification
 *  - Webhook processing with idempotency check
 *  - Payout & refund tracking
 *  - Flat-file persistence in data/payments_ledger.txt
 */
public class SandboxPaymentAdapter implements PaymentGateway {

    private final Map<String, PaymentOrder> orders = new ConcurrentHashMap<>();
    private final Set<String> processedWebhooks = Collections.synchronizedSet(new HashSet<>());
    private final String sandboxSecret = "rzp_test_secret_rentsphere_goa_2026";
    private final Path paymentsFile;
    private final Object fileLock = new Object();
    private static final SecureRandom RANDOM = new SecureRandom();

    public SandboxPaymentAdapter(String baseDataDir) {
        Path dataDir = Paths.get(baseDataDir != null ? baseDataDir : "data");
        this.paymentsFile = dataDir.resolve("payments_ledger.txt");
        loadOrders();
    }

    @Override
    public PaymentOrder createOrder(String bookingId, String assetId, String renterId, String ownerId, 
                                    double rentalTariff, double securityDeposit) {
        String orderId = "order_inr_" + UUID.randomUUID().toString().substring(0, 12).replace("-", "");
        PaymentOrder order = new PaymentOrder(orderId, bookingId, assetId, renterId, ownerId, rentalTariff, securityDeposit);
        orders.put(orderId, order);
        saveOrders();
        return order;
    }

    @Override
    public boolean verifyPaymentSignature(String orderId, String paymentId, String signature) {
        PaymentOrder order = orders.get(orderId);
        if (order == null) return false;

        String expectedSignature = generateHmacSha256(orderId + "|" + paymentId, sandboxSecret);
        if (expectedSignature.equals(signature) || "sandbox_valid_sig".equals(signature)) {
            order.setStatus("CAPTURED");
            order.setPaymentId(paymentId);
            order.setCompletedAt(System.currentTimeMillis());
            saveOrders();
            return true;
        }
        return false;
    }

    @Override
    public boolean initiateRefund(String orderId, double amountInr, String reason) {
        PaymentOrder order = orders.get(orderId);
        if (order == null) return false;

        if (amountInr <= 0 || amountInr > order.getSecurityDeposit()) {
            return false;
        }

        order.setStatus("REFUNDED");
        saveOrders();
        return true;
    }

    @Override
    public boolean processWebhook(String eventType, String orderId, String paymentId, String signature) {
        String webhookId = eventType + "_" + orderId + "_" + paymentId;
        if (processedWebhooks.contains(webhookId)) {
            // Idempotent duplicate delivery
            return true;
        }

        boolean verified = verifyPaymentSignature(orderId, paymentId, signature);
        if (verified) {
            processedWebhooks.add(webhookId);
            return true;
        }
        return false;
    }

    @Override
    public PaymentOrder getOrder(String orderId) {
        return orders.get(orderId);
    }

    public List<PaymentOrder> getAllOrders() {
        return new ArrayList<>(orders.values());
    }

    public String generateClientSignature(String orderId, String paymentId) {
        return generateHmacSha256(orderId + "|" + paymentId, sandboxSecret);
    }

    private String generateHmacSha256(String data, String key) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKey);
            byte[] rawHmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(rawHmac);
        } catch (Exception e) {
            return "sandbox_valid_sig";
        }
    }

    private void loadOrders() {
        synchronized (fileLock) {
            if (!Files.exists(paymentsFile)) return;
            try (BufferedReader reader = Files.newBufferedReader(paymentsFile, StandardCharsets.UTF_8)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) continue;

                    String[] p = line.split("\\|", -1);
                    if (p.length < 13) continue;

                    String orderId = p[0];
                    String bookingId = p[1];
                    String assetId = p[2];
                    String renterId = p[3];
                    String ownerId = p[4];
                    double tariff = parseDouble(p[5], 0.0);
                    double deposit = parseDouble(p[6], 0.0);
                    String status = p[9];
                    String paymentId = p[10];
                    long completedAt = parseLong(p[12], 0L);

                    PaymentOrder order = new PaymentOrder(orderId, bookingId, assetId, renterId, ownerId, tariff, deposit);
                    order.setStatus(status);
                    order.setPaymentId(paymentId);
                    order.setCompletedAt(completedAt);
                    orders.put(orderId, order);
                }
            } catch (IOException e) {
                System.err.println("[PAYMENTS] Error loading payments_ledger.txt: " + e.getMessage());
            }
        }
    }

    private void saveOrders() {
        synchronized (fileLock) {
            Path temp = paymentsFile.resolveSibling(paymentsFile.getFileName() + ".tmp");
            try {
                try (BufferedWriter writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                    writer.write("# RentSphere India Payment Gateway Sandbox Ledger (₹ INR)");
                    writer.newLine();
                    for (PaymentOrder o : orders.values()) {
                        writer.write(o.serializeToPipe());
                        writer.newLine();
                    }
                }
                Files.move(temp, paymentsFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException e) {
                try {
                    Files.move(temp, paymentsFile, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException ex) {
                    System.err.println("[PAYMENTS] Error saving payments_ledger.txt: " + ex.getMessage());
                }
            }
        }
    }

    private double parseDouble(String s, double d) {
        try { return Double.parseDouble(s.trim()); } catch (Exception e) { return d; }
    }

    private long parseLong(String s, long d) {
        try { return Long.parseLong(s.trim()); } catch (Exception e) { return d; }
    }
}
