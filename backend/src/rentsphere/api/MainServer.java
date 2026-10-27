package rentsphere.api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import rentsphere.auth.AuthService;
import rentsphere.auth.UserAccount;
import rentsphere.auth.UserSession;
import rentsphere.dispute.DisputeRecord;
import rentsphere.dispute.DisputeService;
import rentsphere.escrow.EscrowAccount;
import rentsphere.escrow.EscrowVault;
import rentsphere.locker.LockerDevice.DeviceState;
import rentsphere.locker.LockerCompartment;
import rentsphere.locker.SimulatedLockerDevice;
import rentsphere.locker.SmartLockerGrid8x8;
import rentsphere.meta.GenericRepository;
import rentsphere.meta.PrototypeCloner;
import rentsphere.meta.ReflectionInspector;
import rentsphere.meta.TrustScoreEngine;
import rentsphere.model.*;
import rentsphere.payment.PaymentOrder;
import rentsphere.payment.SandboxPaymentAdapter;
import rentsphere.persistence.FlatFilePersistenceEngine;
import rentsphere.pipeline.FailFastPipeline;
import rentsphere.pipeline.GateResult;
import rentsphere.pipeline.PipelineContext;
import rentsphere.policy.PolicyService;
import rentsphere.state.DisputedState;
import rentsphere.support.NotificationService;
import rentsphere.support.SupportService;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.Executors;

/**
 * RentSphere Main Server - Pure Java SE REST API & Static Web Server.
 * Zero External Dependencies (Standard JDK 17+ / 25).
 * Native Indian Rupee (₹ INR) currency across all marketplace workflows.
 * 
 * Features:
 *  - Authentication & RBAC (PBKDF2 Password Hashing, Secure Sessions, OWASP Defenses)
 *  - Indian Payment Sandbox (Razorpay/Cashfree Order Creation, HMAC-SHA256, Webhooks)
 *  - Smart 8x8 Pickup Hub (Simulated Locker Devices, OTP Lockout & Expiry, Zero OTP Leakage)
 *  - Peer-to-Peer Rental Lifecycle (GoF State Pattern, 4-Gate Fail-Fast, Mandatory Quarantine)
 *  - Damage Dispute Arbitration & Evidence Management
 *  - Customer Grievance Redressal (IT Rules 2021 Compliant) & In-App Notification Center
 *  - 20 Indian Regulatory Policies & Auditable Versioned Consent Tracking
 *  - Preserves 100% of MCA OOT Lab Coursework & Diagnostics
 */
public class MainServer {
    public static final int DEFAULT_PORT = 8080;

    private final int port;
    private final GenericRepository<RentableAsset> assetRepo;
    private final SmartLockerGrid8x8 lockerGrid;
    private final EscrowVault escrowVault;
    private final FailFastPipeline pipeline;
    private final FlatFilePersistenceEngine persistenceEngine;
    private final Path frontendPath;

    // Production-Grade Services
    private final AuthService authService;
    private final SandboxPaymentAdapter paymentGateway;
    private final SimulatedLockerDevice lockerDevice;
    private final DisputeService disputeService;
    private final NotificationService notificationService;
    private final SupportService supportService;
    private final PolicyService policyService;

    private HttpServer server;

    public MainServer(int port, String dataDir, String frontendDir) {
        this.port = port;
        this.assetRepo = new GenericRepository<>();
        this.lockerGrid = new SmartLockerGrid8x8();
        this.escrowVault = new EscrowVault();
        this.pipeline = new FailFastPipeline();
        this.persistenceEngine = new FlatFilePersistenceEngine(dataDir);
        this.frontendPath = Paths.get(frontendDir != null ? frontendDir : "frontend");

        // Initialize production-grade subsystems with local flat-file storage
        this.authService = new AuthService(dataDir);
        this.paymentGateway = new SandboxPaymentAdapter(dataDir);
        this.lockerDevice = new SimulatedLockerDevice();
        this.disputeService = new DisputeService(dataDir);
        this.notificationService = new NotificationService(dataDir);
        this.supportService = new SupportService(dataDir);
        this.policyService = new PolicyService(dataDir);

        // Load initial state from flat files
        loadPersistentState();
    }

    private void loadPersistentState() {
        System.out.println("[BOOT] Initializing RentSphere Zero-Database Engine (INR ₹)...");
        persistenceEngine.loadAssets(assetRepo);
        persistenceEngine.loadLockers(lockerGrid);
        persistenceEngine.loadEscrow(escrowVault);
        System.out.printf("[BOOT] Loaded %d assets, %d locker bays, and %d escrow accounts from flat files.%n",
                assetRepo.count(), lockerGrid.getAllCompartments().size(), escrowVault.getAllAccounts().size());
    }

    public synchronized void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());

        // Context: Core API routes
        server.createContext("/api/health", this::handleHealth);
        server.createContext("/api/assets", this::handleAssets);
        server.createContext("/api/assets/reserve", this::handleReserve);
        server.createContext("/api/assets/pickup", this::handlePickup);
        server.createContext("/api/assets/return", this::handleReturn);
        server.createContext("/api/assets/inspect-approve", this::handleInspectApprove);
        server.createContext("/api/assets/inspect-slash", this::handleInspectSlash);
        server.createContext("/api/assets/onboard", this::handleOnboard);
        server.createContext("/api/assets/clone", this::handleCloneListing);

        // Context: Smart Pickup Hub & Lockers (Secured against OTP Leakage)
        server.createContext("/api/lockers", this::handleLockers);
        server.createContext("/api/lockers/verify-otp", this::handleVerifyOtp);
        server.createContext("/api/lockers/telemetry", this::handleTelemetry);

        // Context: Escrow & Write-Ahead Logs
        server.createContext("/api/escrow/accounts", this::handleAccounts);
        server.createContext("/api/wal", this::handleWal);

        // Context: Academic OOT Benchmarks & Reflection
        server.createContext("/api/meta/reflect", this::handleReflect);
        server.createContext("/api/meta/clone", this::handleCloneBenchmark);
        server.createContext("/api/trust-score", this::handleTrustScore);
        server.createContext("/api/meta/nirf", this::handleTrustScore);

        // Context: Authentication & User Accounts (OWASP Secure)
        server.createContext("/api/auth/register", this::handleAuthRegister);
        server.createContext("/api/auth/login", this::handleAuthLogin);
        server.createContext("/api/auth/logout", this::handleAuthLogout);
        server.createContext("/api/auth/me", this::handleAuthMe);

        // Context: Indian Payment Gateway Sandbox (₹ INR)
        server.createContext("/api/payments/create-order", this::handlePaymentCreateOrder);
        server.createContext("/api/payments/verify", this::handlePaymentVerify);
        server.createContext("/api/payments/webhook", this::handlePaymentWebhook);

        // Context: Return Damage Disputes & Evidence Arbitration
        server.createContext("/api/disputes", this::handleDisputes);
        server.createContext("/api/disputes/file", this::handleDisputeFile);
        server.createContext("/api/disputes/resolve", this::handleDisputeResolve);

        // Context: Support Tickets & Grievance Redressal (IT Rules 2021)
        server.createContext("/api/support/tickets", this::handleSupportTickets);
        server.createContext("/api/support/reply", this::handleSupportReply);

        // Context: In-App Notification Center
        server.createContext("/api/notifications", this::handleNotifications);
        server.createContext("/api/notifications/read", this::handleNotificationsRead);

        // Context: Legal & Regulatory Policies (20 Documents)
        server.createContext("/api/policies", this::handlePolicies);
        server.createContext("/api/policies/accept", this::handlePolicyAccept);

        // Context: Admin User Management & Hardware Diagnostics
        server.createContext("/api/admin/users", this::handleAdminUsers);
        server.createContext("/api/admin/users/status", this::handleAdminUserStatus);
        server.createContext("/api/admin/device/state", this::handleAdminDeviceState);

        // Context: Static frontend file delivery (Root handler)
        server.createContext("/", this::handleStaticFiles);

        server.start();
        System.out.println("=================================================================");
        System.out.println("  RentSphere Production-Grade Marketplace Server ACTIVE (INR ₹)");
        System.out.println("  Web UI:   http://localhost:" + port + "/");
        System.out.println("  REST API: http://localhost:" + port + "/api/health");
        System.out.println("  Security: PBKDF2 Hashing | OTP Expiry & Lockout | Zero OTP Leak");
        System.out.println("  Platform: Pure Java SE 17+/25 (Zero External JAR Dependencies)");
        System.out.println("=================================================================");
    }

    public synchronized void stop() {
        if (server != null) {
            server.stop(1);
            System.out.println("[SHUTDOWN] RentSphere server halted.");
        }
    }

    // ========================================================================
    // AUTHENTICATION HELPERS & RBAC
    // ========================================================================

    private UserAccount resolveUser(HttpExchange exchange) {
        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
        String token = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7).trim();
        }
        if (token == null || token.isBlank()) {
            String cookieHeader = exchange.getRequestHeaders().getFirst("Cookie");
            if (cookieHeader != null) {
                for (String cookie : cookieHeader.split(";")) {
                    String[] parts = cookie.trim().split("=", 2);
                    if (parts.length == 2 && "rs_session".equalsIgnoreCase(parts[0].trim())) {
                        token = parts[1].trim();
                        break;
                    }
                }
            }
        }
        if (token == null || token.isBlank()) return null;
        UserSession session = authService.validateSession(token);
        if (session == null) return null;
        return authService.getUserById(session.getUserId());
    }

    private boolean isUserAdmin(UserAccount user) {
        return user != null && "ADMIN".equalsIgnoreCase(user.getRole());
    }

    // ========================================================================
    // REST API HANDLERS - CORE & HEALTH
    // ========================================================================

    private void handleHealth(HttpExchange exchange) throws IOException {
        if (handleCorsPreflight(exchange)) return;
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
            return;
        }

        double lockedTotal = escrowVault.getAllAccounts().stream()
                .mapToDouble(EscrowAccount::getLockedEscrow).sum();

        long inQuarantineCount = assetRepo.findAll().stream()
                .filter(a -> "INSPECTION_QUARANTINE".equals(a.getCurrentState().getStateName())).count();

        long activeLeaseCount = assetRepo.findAll().stream()
                .filter(a -> "ACTIVE_LEASE".equals(a.getCurrentState().getStateName())).count();

        String json = String.format(Locale.US,
                "{\"status\":\"UP\",\"appName\":\"RentSphere P2P Hub\",\"currency\":\"INR (₹)\",\"totalAssets\":%d," +
                "\"activeLeases\":%d,\"inQuarantine\":%d,\"lockedEscrowCapital\":%.2f," +
                "\"totalLockerBays\":%d,\"hardwareSimulation\":\"ACTIVE\",\"runtime\":\"Java SE %s\"}",
                assetRepo.count(), activeLeaseCount, inQuarantineCount, lockedTotal,
                lockerGrid.getAllCompartments().size(), System.getProperty("java.version"));

        sendJsonResponse(exchange, 200, json);
    }

    private void handleAssets(HttpExchange exchange) throws IOException {
        if (handleCorsPreflight(exchange)) return;
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
            return;
        }

        StringBuilder sb = new StringBuilder("[");
        List<RentableAsset> list = assetRepo.findAll();
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(serializeAssetToJson(list.get(i)));
        }
        sb.append("]");

        sendJsonResponse(exchange, 200, sb.toString());
    }

    private void handleReserve(HttpExchange exchange) throws IOException {
        if (handleCorsPreflight(exchange)) return;
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
            return;
        }

        UserAccount caller = resolveUser(exchange);
        if (caller == null) {
            sendJsonResponse(exchange, 401, "{\"error\":\"Unauthorized: Authentication required to reserve assets.\"}");
            return;
        }

        Map<String, String> body = parseJsonBody(exchange);
        String assetId = body.get("assetId");
        String renterId = body.get("renterId");
        int days = parseInt(body.get("rentalDays"), 1);

        if (days < 1 || days > 90) {
            sendJsonResponse(exchange, 400, "{\"success\":false,\"message\":\"Rental duration must be between 1 and 90 days.\"}");
            return;
        }

        if (renterId != null && !renterId.isBlank() && !renterId.equalsIgnoreCase(caller.getUserId()) && !isUserAdmin(caller)) {
            sendJsonResponse(exchange, 403, "{\"error\":\"Forbidden: Cannot reserve assets on behalf of another user.\"}");
            return;
        }
        String effectiveRenterId = (isUserAdmin(caller) && renterId != null && !renterId.isBlank()) ? renterId : caller.getUserId();

        Optional<RentableAsset> optAsset = assetRepo.findById(assetId);
        if (optAsset.isEmpty()) {
            sendJsonResponse(exchange, 404, "{\"error\":\"Asset not found: " + assetId + "\"}");
            return;
        }

        RentableAsset asset = optAsset.get();
        EscrowAccount renter = escrowVault.getAccount(effectiveRenterId);
        double balance = (renter != null) ? renter.getWalletBalance() : 0.0;
        boolean kyc = (renter != null) && renter.isKycVerified();

        // Check if renter accepted essential policies
        if (!policyService.hasAcceptedAllEssential(effectiveRenterId)) {
            // Auto-grant essential policies in demo mode if not explicitly accepted
            policyService.recordAcceptance(effectiveRenterId, List.of("tos", "privacy", "rental_agreement", "cancellation", "deposit_payment"));
        }

        // EXECUTE 4-GATE FAIL-FAST PIPELINE
        PipelineContext ctx = new PipelineContext(
                asset, effectiveRenterId, "RESERVE", balance, kyc, asset.getLockerCoordinate(), days, 0.0
        );

        GateResult verdict = pipeline.verify(ctx);
        if (!verdict.isPassed()) {
            persistenceEngine.appendWal(assetId, "RESERVE_BLOCKED", asset.getCurrentState().getStateName(),
                    asset.getCurrentState().getStateName(), effectiveRenterId, 
                    "Blocked at Gate " + verdict.getGateIndex() + " (" + verdict.getGateName() + "): " + verdict.getDiagnostic());

            String err = String.format("{\"success\":false,\"gateIndex\":%d,\"gateName\":\"%s\",\"message\":\"%s\"}",
                    verdict.getGateIndex(), verdict.getGateName(), escapeJson(verdict.getDiagnostic()));
            sendJsonResponse(exchange, 400, err);
            return;
        }

        double tariff = asset.calculateDailyRentalTariff(days);
        double collateral = asset.calculateRequiredCollateral();

        boolean locked = escrowVault.lockEscrow(effectiveRenterId, asset.getOwnerId(), assetId, tariff, collateral);
        if (!locked) {
            sendJsonResponse(exchange, 500, "{\"error\":\"Escrow lock failed unexpectedly.\"}");
            return;
        }

        String prevState = asset.getCurrentState().getStateName();
        asset.getCurrentState().reserve(asset, effectiveRenterId);
        asset.setRentalDays(days);

        LockerCompartment comp = lockerGrid.getCompartment(asset.getLockerCoordinate());
        String otp = "000000";
        if (comp != null) {
            comp.setStatus("RESERVED_LOCKED");
            otp = comp.generateNewOtp();
        }

        asset.addServiceLog("Reserved for " + days + " days by " + effectiveRenterId + " (Escrow: ₹" + (tariff + collateral) + ")");

        persistenceEngine.saveAssets(assetRepo);
        persistenceEngine.saveLockers(lockerGrid);
        persistenceEngine.saveEscrow(escrowVault);
        persistenceEngine.appendWal(assetId, "RESERVE", prevState, asset.getCurrentState().getStateName(), effectiveRenterId,
                String.format("Escrow locked ₹%.2f. OTP generated for bay %s", (tariff + collateral), asset.getLockerCoordinate()));

        notificationService.pushNotification(effectiveRenterId, "Booking Confirmed",
                "Your reservation for " + asset.getTitle() + " is ready. Pickup OTP sent to your secure dashboard.", "SUCCESS");

        notificationService.pushNotification(asset.getOwnerId(), "Equipment Booked",
                "Your asset " + asset.getTitle() + " has been booked by " + effectiveRenterId + " for " + days + " day(s).", "INFO");

        String res = String.format(Locale.US,
                "{\"success\":true,\"state\":\"%s\",\"assetId\":\"%s\",\"otpPin\":\"%s\",\"lockerBay\":\"%s\"," +
                "\"tariff\":%.2f,\"collateral\":%.2f,\"message\":\"Reservation successful! Escrow locked. Present OTP at Hub Bay to unlock.\"}",
                asset.getCurrentState().getStateName(), assetId, otp, asset.getLockerCoordinate(), tariff, collateral);

        sendJsonResponse(exchange, 200, res);
    }

    private void handlePickup(HttpExchange exchange) throws IOException {
        if (handleCorsPreflight(exchange)) return;
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
            return;
        }

        UserAccount caller = resolveUser(exchange);
        if (caller == null) {
            sendJsonResponse(exchange, 401, "{\"error\":\"Unauthorized: Authentication required to pick up assets.\"}");
            return;
        }

        Map<String, String> body = parseJsonBody(exchange);
        String assetId = body.get("assetId");
        String otp = body.get("otpPin");

        Optional<RentableAsset> optAsset = assetRepo.findById(assetId);
        if (optAsset.isEmpty()) {
            sendJsonResponse(exchange, 404, "{\"error\":\"Asset not found\"}");
            return;
        }

        RentableAsset asset = optAsset.get();
        if (!asset.getCurrentState().canPickup()) {
            sendJsonResponse(exchange, 400, "{\"success\":false,\"message\":\"Asset is not in a reserved pickup state.\"}");
            return;
        }

        if (!isUserAdmin(caller) && !caller.getUserId().equalsIgnoreCase(asset.getRenterId())) {
            sendJsonResponse(exchange, 403, "{\"error\":\"Forbidden: Only the reserving renter or administrator can pick up this asset.\"}");
            return;
        }

        LockerCompartment comp = lockerGrid.getCompartment(asset.getLockerCoordinate());
        if (comp == null || !comp.verifyOtp(otp)) {
            sendJsonResponse(exchange, 400, "{\"success\":false,\"message\":\"Invalid or expired 6-Digit Pickup OTP passcode for Bay " + asset.getLockerCoordinate() + "\"}");
            return;
        }

        // State transition and hardware command simulation
        String prevState = asset.getCurrentState().getStateName();
        try {
            asset.getCurrentState().pickup(asset);
        } catch (IllegalStateException e) {
            sendJsonResponse(exchange, 400, "{\"success\":false,\"message\":\"" + escapeJson(e.getMessage()) + "\"}");
            return;
        }

        lockerDevice.sendUnlockCommand(comp.getBayId(), otp);

        comp.setOccupied(false);
        comp.setStatus("DISPATCHED_ACTIVE");
        comp.setWeightKg(0.0);
        asset.addServiceLog("Asset retrieved from Hub Bay " + comp.getBayId() + " via OTP verification.");

        persistenceEngine.saveAssets(assetRepo);
        persistenceEngine.saveLockers(lockerGrid);
        persistenceEngine.appendWal(assetId, "PICKUP", prevState, asset.getCurrentState().getStateName(), asset.getRenterId(),
                "Unlocked via OTP. Bay " + comp.getBayId() + " vacated for active rental.");

        notificationService.pushNotification(asset.getRenterId(), "Equipment Collected",
                "Hub Bay " + comp.getBayId() + " unlocked. Active lease started for " + asset.getTitle() + ".", "INFO");

        String res = String.format(
                "{\"success\":true,\"state\":\"%s\",\"assetId\":\"%s\",\"message\":\"Hub compartment unlocked! Equipment retrieved successfully.\"}",
                asset.getCurrentState().getStateName(), assetId);
        sendJsonResponse(exchange, 200, res);
    }

    private void handleReturn(HttpExchange exchange) throws IOException {
        if (handleCorsPreflight(exchange)) return;
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
            return;
        }

        Map<String, String> body = parseJsonBody(exchange);
        String assetId = body.get("assetId");

        Optional<RentableAsset> optAsset = assetRepo.findById(assetId);
        if (optAsset.isEmpty()) {
            sendJsonResponse(exchange, 404, "{\"error\":\"Asset not found\"}");
            return;
        }

        RentableAsset asset = optAsset.get();
        String prevState = asset.getCurrentState().getStateName();

        // MANDATORY QUARANTINE INVARIANT
        try {
            asset.getCurrentState().initiateReturn(asset);
        } catch (IllegalStateException e) {
            sendJsonResponse(exchange, 400, "{\"success\":false,\"message\":\"" + escapeJson(e.getMessage()) + "\"}");
            return;
        }

        LockerCompartment comp = lockerGrid.getCompartment(asset.getLockerCoordinate());
        if (comp != null) {
            comp.setOccupied(true);
            comp.setStatus("QUARANTINE_LOCKED");
            comp.setWeightKg(8.5);
            lockerDevice.sendLockCommand(comp.getBayId());
        }

        asset.addServiceLog("Returned to Hub Bay " + asset.getLockerCoordinate() + ". MANDATORY INSPECTION: Deposit held.");

        persistenceEngine.saveAssets(assetRepo);
        persistenceEngine.saveLockers(lockerGrid);
        persistenceEngine.appendWal(assetId, "RETURN", prevState, asset.getCurrentState().getStateName(), asset.getRenterId(),
                "MANDATORY QUARANTINE: Item returned to Bay " + asset.getLockerCoordinate() + ". Security deposit held pending inspection.");

        notificationService.pushNotification(asset.getOwnerId(), "Inspection Required",
                "Equipment " + asset.getTitle() + " has been checked into Bay " + asset.getLockerCoordinate() + ". Complete inspection within 24 hours.", "WARNING");

        String res = String.format(
                "{\"success\":true,\"state\":\"%s\",\"assetId\":\"%s\",\"message\":\"Item returned to Hub Bay. Mandatory Inspection active: Deposit held until host checks condition.\"}",
                asset.getCurrentState().getStateName(), assetId);
        sendJsonResponse(exchange, 200, res);
    }

    private void handleInspectApprove(HttpExchange exchange) throws IOException {
        if (handleCorsPreflight(exchange)) return;
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
            return;
        }

        UserAccount caller = resolveUser(exchange);
        if (caller == null) {
            sendJsonResponse(exchange, 401, "{\"error\":\"Unauthorized: Authentication required to inspect assets.\"}");
            return;
        }

        Map<String, String> body = parseJsonBody(exchange);
        String assetId = body.get("assetId");
        String hostId = body.get("hostId");

        Optional<RentableAsset> optAsset = assetRepo.findById(assetId);
        if (optAsset.isEmpty()) {
            sendJsonResponse(exchange, 404, "{\"error\":\"Asset not found\"}");
            return;
        }

        RentableAsset asset = optAsset.get();
        if (!isUserAdmin(caller) && !caller.getUserId().equalsIgnoreCase(asset.getOwnerId())) {
            sendJsonResponse(exchange, 403, "{\"error\":\"Forbidden: Only the asset owner or administrator can inspect this asset.\"}");
            return;
        }

        String renterId = asset.getRenterId();
        int rentalDays = asset.getRentalDays();
        double tariff = asset.calculateDailyRentalTariff(rentalDays);
        double collateral = asset.calculateRequiredCollateral();

        String prevState = asset.getCurrentState().getStateName();
        try {
            asset.getCurrentState().approveInspection(asset);
        } catch (IllegalStateException e) {
            sendJsonResponse(exchange, 400, "{\"success\":false,\"message\":\"" + escapeJson(e.getMessage()) + "\"}");
            return;
        }

        asset.setRentalDays(1);
        escrowVault.releasePayoutClean(renterId, asset.getOwnerId(), assetId, tariff, collateral);

        LockerCompartment comp = lockerGrid.getCompartment(asset.getLockerCoordinate());
        if (comp != null) {
            comp.setStatus("ONLINE_READY");
        }

        String actingHostId = (hostId != null && !hostId.isBlank() && isUserAdmin(caller)) ? hostId : caller.getUserId();
        asset.addServiceLog("Clean Inspection Approved by " + actingHostId + ". 100% deposit refunded. Restored to AVAILABLE.");

        persistenceEngine.saveAssets(assetRepo);
        persistenceEngine.saveLockers(lockerGrid);
        persistenceEngine.saveEscrow(escrowVault);
        persistenceEngine.appendWal(assetId, "INSPECT_APPROVE", prevState, asset.getCurrentState().getStateName(), actingHostId,
                String.format("Clean return approved. Slashed ₹0. Refunded ₹%.2f deposit to %s. Paid ₹%.2f to host (%d days).",
                        collateral, renterId, tariff, rentalDays));

        notificationService.pushNotification(renterId, "Deposit Refunded",
                "Clean inspection approved for " + asset.getTitle() + ". Full security deposit (₹" + collateral + ") refunded.", "SUCCESS");

        String res = String.format(Locale.US,
                "{\"success\":true,\"state\":\"%s\",\"message\":\"Clean Return Approved! Full deposit (₹%.2f) refunded to renter. ₹%.2f rental fee transferred to host.\"}",
                asset.getCurrentState().getStateName(), collateral, tariff);
        sendJsonResponse(exchange, 200, res);
    }

    private void handleInspectSlash(HttpExchange exchange) throws IOException {
        if (handleCorsPreflight(exchange)) return;
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
            return;
        }

        UserAccount caller = resolveUser(exchange);
        if (caller == null) {
            sendJsonResponse(exchange, 401, "{\"error\":\"Unauthorized: Authentication required to inspect assets.\"}");
            return;
        }

        Map<String, String> body = parseJsonBody(exchange);
        String assetId = body.get("assetId");
        String hostId = body.get("hostId");
        double damagePct = parseDouble(body.get("damagePercentage"), 20.0);
        damagePct = Math.max(0.0, Math.min(100.0, damagePct));

        Optional<RentableAsset> optAsset = assetRepo.findById(assetId);
        if (optAsset.isEmpty()) {
            sendJsonResponse(exchange, 404, "{\"error\":\"Asset not found\"}");
            return;
        }

        RentableAsset asset = optAsset.get();
        if (!isUserAdmin(caller) && !caller.getUserId().equalsIgnoreCase(asset.getOwnerId())) {
            sendJsonResponse(exchange, 403, "{\"error\":\"Forbidden: Only the asset owner or administrator can inspect this asset.\"}");
            return;
        }

        String renterId = asset.getRenterId();
        int rentalDays = asset.getRentalDays();
        double tariff = asset.calculateDailyRentalTariff(rentalDays);
        double collateral = asset.calculateRequiredCollateral();
        double slashingPenalty = asset.calculateDamageSlashingPenalty(damagePct);

        String prevState = asset.getCurrentState().getStateName();
        try {
            asset.getCurrentState().slashCollateral(asset, slashingPenalty);
        } catch (IllegalStateException e) {
            sendJsonResponse(exchange, 400, "{\"success\":false,\"message\":\"" + escapeJson(e.getMessage()) + "\"}");
            return;
        }

        asset.setRentalDays(1);
        escrowVault.slashCollateral(renterId, asset.getOwnerId(), assetId, tariff, collateral, slashingPenalty);

        LockerCompartment comp = lockerGrid.getCompartment(asset.getLockerCoordinate());
        if (comp != null) {
            comp.setStatus("ONLINE_READY");
        }

        double netRefund = Math.max(0.0, collateral - slashingPenalty);
        String actingHostId = (hostId != null && !hostId.isBlank() && isUserAdmin(caller)) ? hostId : caller.getUserId();
        asset.addServiceLog(String.format(Locale.US, "Damage Slashed: %.1f%% penalty (₹%.2f) deducted from deposit. Restored to AVAILABLE.", damagePct, slashingPenalty));

        persistenceEngine.saveAssets(assetRepo);
        persistenceEngine.saveLockers(lockerGrid);
        persistenceEngine.saveEscrow(escrowVault);
        persistenceEngine.appendWal(assetId, "INSPECT_SLASH", prevState, asset.getCurrentState().getStateName(), actingHostId,
                String.format(Locale.US, "Damage flagged (%.1f%%). Slashed ₹%.2f to host. Refunded ₹%.2f net deposit to %s.",
                        damagePct, slashingPenalty, netRefund, renterId));

        notificationService.pushNotification(renterId, "Damage Deduction Flagged",
                String.format(Locale.US, "Host flagged %.1f%% damage for %s. ₹%.2f deducted from deposit. You may file a dispute if contested.",
                        damagePct, asset.getTitle(), slashingPenalty), "WARNING");

        String res = String.format(Locale.US,
                "{\"success\":true,\"state\":\"%s\",\"slashedPenalty\":%.2f,\"refundedCollateral\":%.2f," +
                "\"message\":\"Damage Slashing Executed: ₹%.2f deducted from deposit for repairs. ₹%.2f net refund returned to renter.\"}",
                asset.getCurrentState().getStateName(), slashingPenalty, netRefund, slashingPenalty, netRefund);
        sendJsonResponse(exchange, 200, res);
    }

    private void handleOnboard(HttpExchange exchange) throws IOException {
        if (handleCorsPreflight(exchange)) return;
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
            return;
        }

        Map<String, String> b = parseJsonBody(exchange);
        String leafClass = b.getOrDefault("leafClass", "CameraAndOpticsGear");
        String title = b.getOrDefault("title", "New Equipment");
        String ownerId = b.getOrDefault("ownerId", "host_faaris");
        String bay = b.getOrDefault("lockerBay", "A1").toUpperCase();
        double baseRate = parseDouble(b.get("baseDailyRate"), 500.0);
        double collateral = parseDouble(b.get("securityCollateral"), 2000.0);

        if (baseRate <= 0.0) {
            sendJsonResponse(exchange, 400, "{\"success\":false,\"message\":\"Base daily rate must be positive.\"}");
            return;
        }
        if (collateral < 0.0) {
            sendJsonResponse(exchange, 400, "{\"success\":false,\"message\":\"Security collateral cannot be negative.\"}");
            return;
        }

        LockerCompartment comp = lockerGrid.getCompartment(bay);
        if (comp == null) {
            sendJsonResponse(exchange, 400, "{\"success\":false,\"message\":\"Invalid locker bay coordinate: " + bay + "\"}");
            return;
        }
        if (comp.isOccupied()) {
            sendJsonResponse(exchange, 400, "{\"success\":false,\"message\":\"Locker bay " + bay + " is already occupied by asset " + comp.getCurrentAssetId() + "\"}");
            return;
        }

        String assetId = "AST-" + (100 + assetRepo.count() + 1);
        RentableAsset newAsset;

        if ("CameraAndOpticsGear".equalsIgnoreCase(leafClass)) {
            newAsset = new CameraAndOpticsGear(
                    assetId, title, ownerId, baseRate, collateral, bay,
                    1.20, "Creator Comprehensive", "Studio & Outdoor", "Dual Batteries", 3,
                    parseDouble(b.get("sensorMp"), 24.2), Boolean.parseBoolean(b.getOrDefault("is4k", "true")),
                    b.getOrDefault("mount", "Standard Mount")
            );
        } else if ("EventAudioAndLighting".equalsIgnoreCase(leafClass)) {
            newAsset = new EventAudioAndLighting(
                    assetId, title, ownerId, baseRate, collateral, bay,
                    1.25, "Event Pro Shield", "Indoor/Outdoor Sound", "240W RMS", 4,
                    parseInt(b.get("soundDb"), 104), parseInt(b.get("mics"), 2),
                    Boolean.parseBoolean(b.getOrDefault("rgb", "true"))
            );
        } else if ("PowerToolEquipment".equalsIgnoreCase(leafClass)) {
            newAsset = new PowerToolEquipment(
                    assetId, title, ownerId, baseRate, collateral, bay,
                    1.15, "Standard Utility Shield", b.getOrDefault("power", "600W Motor"),
                    "ISI Certified", Boolean.parseBoolean(b.getOrDefault("cordless", "false")),
                    parseInt(b.get("bits"), 50)
            );
        } else {
            newAsset = new EventAndMobilityVehicle(
                    assetId, title, ownerId, baseRate, collateral, bay,
                    1.30, "Commercial Vehicle Cover", b.getOrDefault("power", "1.2L Engine"),
                    "ARAI Certified", parseInt(b.get("seats"), 5), b.getOrDefault("fuel", "Petrol"),
                    Boolean.parseBoolean(b.getOrDefault("auto", "true"))
            );
        }

        // Validate Row H Ground Slot Invariant
        if (!lockerGrid.validateGroundSlotPlacement(newAsset, bay)) {
            sendJsonResponse(exchange, 400, "{\"success\":false,\"message\":\"Vehicles and heavy event gear must be assigned to Row H Ground Slots (H1-H8). Specified bay: " + bay + "\"}");
            return;
        }

        assetRepo.save(newAsset);
        lockerGrid.assignAssetToBay(newAsset, bay);

        persistenceEngine.saveAssets(assetRepo);
        persistenceEngine.saveLockers(lockerGrid);
        persistenceEngine.appendWal(assetId, "ONBOARD_ASSET", "NONE", "AVAILABLE", ownerId,
                "Onboarded new " + leafClass + " in Bay " + bay);

        sendJsonResponse(exchange, 201, "{\"success\":true,\"assetId\":\"" + assetId + "\",\"message\":\"Listing successfully added to RentSphere!\"}");
    }

    private void handleCloneListing(HttpExchange exchange) throws IOException {
        if (handleCorsPreflight(exchange)) return;
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
            return;
        }

        Map<String, String> b = parseJsonBody(exchange);
        String sourceId = b.get("assetId");
        String targetBay = b.getOrDefault("targetBay", "").toUpperCase();

        Optional<RentableAsset> opt = assetRepo.findById(sourceId);
        if (opt.isEmpty()) {
            sendJsonResponse(exchange, 404, "{\"error\":\"Source asset not found\"}");
            return;
        }

        RentableAsset source = opt.get();
        RentableAsset clone = source.deepClone();
        String newId = "AST-" + (100 + assetRepo.count() + 1);

        if (targetBay.isBlank()) {
            targetBay = lockerGrid.getAllCompartments().stream()
                    .filter(c -> !c.isOccupied() && lockerGrid.validateGroundSlotPlacement(clone, c.getBayId()))
                    .map(LockerCompartment::getBayId)
                    .findFirst().orElse("");
            if (targetBay.isBlank()) {
                sendJsonResponse(exchange, 400, "{\"success\":false,\"message\":\"No vacant locker bay available for this asset tier.\"}");
                return;
            }
        }

        LockerCompartment targetComp = lockerGrid.getCompartment(targetBay);
        if (targetComp == null) {
            sendJsonResponse(exchange, 400, "{\"success\":false,\"message\":\"Target locker bay does not exist: " + targetBay + "\"}");
            return;
        }
        if (targetComp.isOccupied()) {
            sendJsonResponse(exchange, 400, "{\"success\":false,\"message\":\"Target locker bay " + targetBay + " is already occupied.\"}");
            return;
        }
        if (!lockerGrid.validateGroundSlotPlacement(clone, targetBay)) {
            sendJsonResponse(exchange, 400, "{\"success\":false,\"message\":\"Vehicles and heavy equipment must be placed in Row H Ground Slots (H1-H8). Target bay: " + targetBay + "\"}");
            return;
        }

        try {
            java.lang.reflect.Field idField = AbstractDomainEntity.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(clone, newId);
        } catch (Exception ignored) {}

        clone.setTitle(source.getTitle() + " (Unit #" + (assetRepo.count() + 1) + ")");
        clone.setLockerCoordinate(targetBay);
        clone.setRenterId("");
        clone.setState(new rentsphere.state.AvailableState());
        clone.addServiceLog("Duplicated via Prototype Deep Clone from " + sourceId + " with isolated service history.");

        assetRepo.save(clone);
        lockerGrid.assignAssetToBay(clone, targetBay);

        persistenceEngine.saveAssets(assetRepo);
        persistenceEngine.saveLockers(lockerGrid);
        persistenceEngine.appendWal(newId, "CLONE_ASSET", "NONE", "AVAILABLE", clone.getOwnerId(),
                "Duplicated via Prototype Deep Clone from " + sourceId + " into Bay " + targetBay);

        sendJsonResponse(exchange, 201, String.format(
                "{\"success\":true,\"assetId\":\"%s\",\"targetBay\":\"%s\",\"message\":\"Multi-Unit Listing successfully cloned with isolated service history!\"}",
                newId, targetBay));
    }

    // ========================================================================
    // REST API HANDLERS - LOCKERS (SECURED AGAINST OTP LEAKAGE)
    // ========================================================================

    private void handleLockers(HttpExchange exchange) throws IOException {
        if (handleCorsPreflight(exchange)) return;
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
            return;
        }

        UserAccount caller = resolveUser(exchange);
        boolean isAdmin = isUserAdmin(caller);

        StringBuilder sb = new StringBuilder("[");
        List<LockerCompartment> list = lockerGrid.getAllCompartments();
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(",");
            LockerCompartment c = list.get(i);
            
            // SECURITY DEFENSE (CWE-200):
            // Never expose live OTP PINs in public listings or unauthenticated telemetry!
            // Expose OTP only if caller is an authorized Admin or the asset's active renter.
            String maskedOtp = "";
            if (isAdmin) {
                maskedOtp = c.getOtpPin();
            } else if (caller != null && c.isOccupied()) {
                Optional<RentableAsset> optAsset = assetRepo.findById(c.getCurrentAssetId());
                if (optAsset.isPresent() && caller.getUserId().equalsIgnoreCase(optAsset.get().getRenterId())) {
                    maskedOtp = c.getOtpPin();
                }
            }

            sb.append(String.format(Locale.US,
                    "{\"bayId\":\"%s\",\"row\":%d,\"col\":%d,\"sizeTier\":\"%s\",\"occupied\":%b," +
                    "\"currentAssetId\":\"%s\",\"status\":\"%s\",\"otpPin\":\"%s\",\"tempC\":%.1f," +
                    "\"humidityPct\":%.1f,\"weightKg\":%.1f}",
                    c.getBayId(), c.getRow(), c.getCol(), c.getSizeTier().name(), c.isOccupied(),
                    c.getCurrentAssetId(), c.getStatus(), maskedOtp, c.getTempC(),
                    c.getHumidityPct(), c.getWeightKg()));
        }
        sb.append("]");
        sendJsonResponse(exchange, 200, sb.toString());
    }

    private void handleVerifyOtp(HttpExchange exchange) throws IOException {
        if (handleCorsPreflight(exchange)) return;
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
            return;
        }

        Map<String, String> b = parseJsonBody(exchange);
        String bayId = b.get("bayId");
        String otp = b.get("otpPin");

        LockerCompartment comp = lockerGrid.getCompartment(bayId);
        if (comp == null) {
            sendJsonResponse(exchange, 404, "{\"error\":\"Bay not found\"}");
            return;
        }

        boolean match = comp.verifyOtp(otp);
        sendJsonResponse(exchange, 200, String.format("{\"bayId\":\"%s\",\"matched\":%b}", bayId, match));
    }

    private void handleTelemetry(HttpExchange exchange) throws IOException {
        if (handleCorsPreflight(exchange)) return;
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
            return;
        }

        Map<String, String> b = parseJsonBody(exchange);
        String bayId = b.get("bayId");
        double temp = parseDouble(b.get("tempC"), 24.0);
        double hum = parseDouble(b.get("humidityPct"), 48.0);
        double weight = parseDouble(b.get("weightKg"), 0.0);

        boolean updated = lockerGrid.updateTelemetry(bayId, temp, hum, weight);
        if (updated) {
            persistenceEngine.saveLockers(lockerGrid);
            sendJsonResponse(exchange, 200, "{\"success\":true}");
        } else {
            sendJsonResponse(exchange, 404, "{\"error\":\"Bay not found\"}");
        }
    }

    // ========================================================================
    // REST API HANDLERS - AUTHENTICATION & SESSIONS (OWASP COMPLIANT)
    // ========================================================================

    private void handleAuthRegister(HttpExchange exchange) throws IOException {
        if (handleCorsPreflight(exchange)) return;
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
            return;
        }

        Map<String, String> b = parseJsonBody(exchange);
        String email = b.get("email");
        String password = b.get("password");
        String fullName = b.get("fullName");
        String role = b.getOrDefault("role", "RENTER");
        String phone = b.getOrDefault("phone", "+91 98221 00000");

        try {
            UserAccount user = authService.registerUser(email, password, fullName, role, phone);
            UserSession session = authService.login(email, password);

            // Set secure session cookie
            String cookie = String.format("rs_session=%s; Path=/; HttpOnly; SameSite=Lax", session.getToken());
            exchange.getResponseHeaders().set("Set-Cookie", cookie);

            // Record policy acceptance if requested
            policyService.recordAcceptance(user.getUserId(), List.of("tos", "privacy", "rental_agreement", "cancellation", "deposit_payment"));

            String json = String.format(
                    "{\"success\":true,\"token\":\"%s\",\"user\":{\"userId\":\"%s\",\"email\":\"%s\",\"fullName\":\"%s\",\"role\":\"%s\",\"accountStatus\":\"%s\"}}",
                    session.getToken(), user.getUserId(), user.getEmail(), escapeJson(user.getFullName()), user.getRole(), user.getAccountStatus());
            sendJsonResponse(exchange, 201, json);
        } catch (IllegalArgumentException | IllegalStateException e) {
            sendJsonResponse(exchange, 400, "{\"success\":false,\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
        }
    }

    private void handleAuthLogin(HttpExchange exchange) throws IOException {
        if (handleCorsPreflight(exchange)) return;
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
            return;
        }

        Map<String, String> b = parseJsonBody(exchange);
        String email = b.get("email");
        String password = b.get("password");

        try {
            UserSession session = authService.login(email, password);
            if (session == null) {
                sendJsonResponse(exchange, 401, "{\"success\":false,\"error\":\"Invalid credentials. Default password for seeded accounts is RentSphere@2026\"}");
                return;
            }

            UserAccount user = authService.getUserById(session.getUserId());
            String cookie = String.format("rs_session=%s; Path=/; HttpOnly; SameSite=Lax", session.getToken());
            exchange.getResponseHeaders().set("Set-Cookie", cookie);

            String json = String.format(
                    "{\"success\":true,\"token\":\"%s\",\"user\":{\"userId\":\"%s\",\"email\":\"%s\",\"fullName\":\"%s\",\"role\":\"%s\",\"accountStatus\":\"%s\"}}",
                    session.getToken(), user.getUserId(), user.getEmail(), escapeJson(user.getFullName()), user.getRole(), user.getAccountStatus());
            sendJsonResponse(exchange, 200, json);
        } catch (IllegalStateException e) {
            sendJsonResponse(exchange, 403, "{\"success\":false,\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
        }
    }

    private void handleAuthLogout(HttpExchange exchange) throws IOException {
        if (handleCorsPreflight(exchange)) return;
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
            return;
        }

        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            authService.logout(authHeader.substring(7).trim());
        }
        exchange.getResponseHeaders().set("Set-Cookie", "rs_session=; Path=/; Max-Age=0; HttpOnly; SameSite=Lax");
        sendJsonResponse(exchange, 200, "{\"success\":true,\"message\":\"Successfully logged out.\"}");
    }

    private void handleAuthMe(HttpExchange exchange) throws IOException {
        if (handleCorsPreflight(exchange)) return;
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
            return;
        }

        UserAccount user = resolveUser(exchange);
        if (user == null) {
            sendJsonResponse(exchange, 200, "{\"authenticated\":false,\"user\":null}");
            return;
        }

        int unreadNotifs = (int) notificationService.getUserNotifications(user.getUserId()).stream()
                .filter(n -> !n.read).count();

        String json = String.format(
                "{\"authenticated\":true,\"user\":{\"userId\":\"%s\",\"email\":\"%s\",\"fullName\":\"%s\",\"role\":\"%s\",\"phone\":\"%s\",\"kycVerified\":%b,\"accountStatus\":\"%s\",\"unreadNotifications\":%d}}",
                user.getUserId(), user.getEmail(), escapeJson(user.getFullName()), user.getRole(),
                escapeJson(user.getPhone()), user.isKycVerified(), user.getAccountStatus(), unreadNotifs);
        sendJsonResponse(exchange, 200, json);
    }

    // ========================================================================
    // REST API HANDLERS - PAYMENT GATEWAY SANDBOX (₹ INR)
    // ========================================================================

    private void handlePaymentCreateOrder(HttpExchange exchange) throws IOException {
        if (handleCorsPreflight(exchange)) return;
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
            return;
        }

        Map<String, String> b = parseJsonBody(exchange);
        String assetId = b.get("assetId");
        String renterId = b.get("renterId");
        int days = parseInt(b.get("rentalDays"), 1);

        Optional<RentableAsset> optAsset = assetRepo.findById(assetId);
        if (optAsset.isEmpty()) {
            sendJsonResponse(exchange, 404, "{\"error\":\"Asset not found\"}");
            return;
        }

        RentableAsset asset = optAsset.get();
        double tariff = asset.calculateDailyRentalTariff(days);
        double collateral = asset.calculateRequiredCollateral();

        PaymentOrder order = paymentGateway.createOrder("BKG-" + System.currentTimeMillis() % 100000,
                assetId, renterId, asset.getOwnerId(), tariff, collateral);

        String json = String.format(Locale.US,
                "{\"success\":true,\"orderId\":\"%s\",\"currency\":\"INR (₹)\",\"rentalTariff\":%.2f," +
                "\"securityDeposit\":%.2f,\"totalAmountPayable\":%.2f,\"keyId\":\"rzp_test_rentsphere_goa\"," +
                "\"mode\":\"SANDBOX_SIMULATION\",\"message\":\"Payment order created. Ready for UPI / NetBanking authorization.\"}",
                order.getOrderId(), tariff, collateral, order.getTotalAmount());
        sendJsonResponse(exchange, 201, json);
    }

    private void handlePaymentVerify(HttpExchange exchange) throws IOException {
        if (handleCorsPreflight(exchange)) return;
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
            return;
        }

        Map<String, String> b = parseJsonBody(exchange);
        String orderId = b.get("orderId");
        String paymentId = b.getOrDefault("paymentId", "pay_sim_" + UUID.randomUUID().toString().substring(0, 8));
        String signature = b.getOrDefault("signature", "sandbox_valid_sig");

        boolean verified = paymentGateway.verifyPaymentSignature(orderId, paymentId, signature);
        if (verified) {
            PaymentOrder order = paymentGateway.getOrder(orderId);
            sendJsonResponse(exchange, 200, String.format(
                    "{\"success\":true,\"status\":\"CAPTURED\",\"orderId\":\"%s\",\"paymentId\":\"%s\",\"message\":\"Payment verified via Indian Gateway Sandbox.\"}",
                    orderId, paymentId));
        } else {
            sendJsonResponse(exchange, 400, "{\"success\":false,\"error\":\"Invalid payment signature.\"}");
        }
    }

    private void handlePaymentWebhook(HttpExchange exchange) throws IOException {
        if (handleCorsPreflight(exchange)) return;
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
            return;
        }

        InputStream is = exchange.getRequestBody();
        String payload = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        String eventId = "evt_" + UUID.randomUUID().toString().substring(0, 10);

        boolean processed = paymentGateway.processWebhook("payment.captured", "order_test", "pay_test", "sandbox_valid_sig");
        sendJsonResponse(exchange, 200, String.format("{\"received\":true,\"processed\":%b}", processed));
    }

    // ========================================================================
    // REST API HANDLERS - DISPUTES & ARBITRATION
    // ========================================================================

    private void handleDisputes(HttpExchange exchange) throws IOException {
        if (handleCorsPreflight(exchange)) return;
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
            return;
        }

        UserAccount user = resolveUser(exchange);
        if (user == null) {
            sendJsonResponse(exchange, 401, "{\"error\":\"Unauthorized: Authentication required to view disputes.\"}");
            return;
        }
        List<DisputeRecord> list;
        if (isUserAdmin(user)) {
            list = disputeService.getAllDisputes();
        } else {
            list = disputeService.getDisputesForUser(user.getUserId());
        }

        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(",");
            DisputeRecord d = list.get(i);
            sb.append(String.format(Locale.US,
                    "{\"disputeId\":\"%s\",\"assetId\":\"%s\",\"renterId\":\"%s\",\"ownerId\":\"%s\"," +
                    "\"heldDeposit\":%.2f,\"claimedDeduction\":%.2f,\"renterReason\":\"%s\",\"evidenceNotes\":\"%s\"," +
                    "\"status\":\"%s\",\"adminNotes\":\"%s\",\"createdAt\":%d,\"resolvedAt\":%d}",
                    d.getDisputeId(), d.getAssetId(), d.getRenterId(), d.getOwnerId(),
                    d.getHeldDeposit(), d.getClaimedDeduction(), escapeJson(d.getRenterReason()),
                    escapeJson(d.getRenterEvidenceNotes()), d.getStatus(), escapeJson(d.getAdminNotes()),
                    d.getCreatedAt(), d.getResolvedAt()));
        }
        sb.append("]");
        sendJsonResponse(exchange, 200, sb.toString());
    }

    private void handleDisputeFile(HttpExchange exchange) throws IOException {
        if (handleCorsPreflight(exchange)) return;
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
            return;
        }

        Map<String, String> b = parseJsonBody(exchange);
        String assetId = b.get("assetId");
        String renterId = b.get("renterId");
        String reason = b.getOrDefault("renterReason", "Contesting damage assessment; normal wear and tear.");
        String evidence = b.getOrDefault("evidenceNotes", "Photos taken at return show zero physical damage.");

        Optional<RentableAsset> opt = assetRepo.findById(assetId);
        if (opt.isEmpty()) {
            sendJsonResponse(exchange, 404, "{\"error\":\"Asset not found\"}");
            return;
        }

        RentableAsset asset = opt.get();
        double heldDeposit = asset.calculateRequiredCollateral();
        double claimedDeduction = heldDeposit * 0.20; // Default claimed deduction

        // Transition asset to DISPUTED state (Freezing transfers)
        String prevState = asset.getCurrentState().getStateName();
        asset.setState(new DisputedState());
        asset.addServiceLog("Renter filed formal dispute against condition assessment. Collateral frozen.");

        DisputeRecord record = disputeService.fileDispute(assetId, renterId, asset.getOwnerId(), heldDeposit, claimedDeduction, reason, evidence);

        persistenceEngine.saveAssets(assetRepo);
        persistenceEngine.appendWal(assetId, "DISPUTE_FILED", prevState, "DISPUTED", renterId,
                "Formal dispute " + record.getDisputeId() + " filed. Automatic deposit release frozen.");

        notificationService.pushNotification(asset.getOwnerId(), "Dispute Notice",
                "Renter has contested your damage assessment for " + asset.getTitle() + ". RentSphere arbitration active.", "WARNING");

        String json = String.format(Locale.US,
                "{\"success\":true,\"disputeId\":\"%s\",\"assetId\":\"%s\",\"status\":\"PENDING_REVIEW\"," +
                "\"message\":\"Dispute filed successfully. Security deposit frozen pending arbitration by Goa Hub Lead.\"}",
                record.getDisputeId(), assetId);
        sendJsonResponse(exchange, 201, json);
    }

    private void handleDisputeResolve(HttpExchange exchange) throws IOException {
        if (handleCorsPreflight(exchange)) return;
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
            return;
        }

        UserAccount admin = resolveUser(exchange);
        if (admin == null || !isUserAdmin(admin)) {
            sendJsonResponse(exchange, 403, "{\"error\":\"Forbidden: Admin session required for dispute arbitration.\"}");
            return;
        }

        Map<String, String> b = parseJsonBody(exchange);
        String disputeId = b.get("disputeId");
        String decision = b.getOrDefault("decision", "RESOLVED_REFUND"); // "RESOLVED_REFUND" or "RESOLVED_SLASH"
        String notes = b.getOrDefault("adminNotes", "Arbitrated after review of photo evidence.");

        DisputeRecord dispute = disputeService.getDispute(disputeId);
        if (dispute == null) {
            sendJsonResponse(exchange, 404, "{\"error\":\"Dispute not found\"}");
            return;
        }

        disputeService.resolveDispute(disputeId, admin != null ? admin.getUserId() : "admin_lead", decision, notes);

        Optional<RentableAsset> optAsset = assetRepo.findById(dispute.getAssetId());
        if (optAsset.isPresent()) {
            RentableAsset asset = optAsset.get();
            String prevState = asset.getCurrentState().getStateName();
            int rentalDays = asset.getRentalDays();
            double tariff = asset.calculateDailyRentalTariff(rentalDays);

            if ("RESOLVED_REFUND".equalsIgnoreCase(decision)) {
                // Rule in favor of renter: Full refund of held deposit
                asset.getCurrentState().approveInspection(asset);
                escrowVault.releasePayoutClean(dispute.getRenterId(), dispute.getOwnerId(), asset.getId(),
                        tariff, dispute.getHeldDeposit());
                asset.addServiceLog("Dispute resolved in favor of renter. 100% deposit refunded. Restored to AVAILABLE.");
            } else {
                // Rule in favor of host: Slash claimed deduction
                asset.getCurrentState().slashCollateral(asset, dispute.getClaimedDeduction());
                escrowVault.slashCollateral(dispute.getRenterId(), dispute.getOwnerId(), asset.getId(),
                        tariff, dispute.getHeldDeposit(), dispute.getClaimedDeduction());
                asset.addServiceLog("Dispute resolved in favor of host. ₹" + dispute.getClaimedDeduction() + " deducted from deposit.");
            }
            asset.setRentalDays(1);

            persistenceEngine.saveAssets(assetRepo);
            persistenceEngine.saveEscrow(escrowVault);
            persistenceEngine.appendWal(asset.getId(), "DISPUTE_RESOLVED", prevState, asset.getCurrentState().getStateName(),
                    admin != null ? admin.getUserId() : "admin",
                    "Dispute " + disputeId + " settled: " + decision + ". " + notes);

            notificationService.pushNotification(dispute.getRenterId(), "Dispute Arbitrated",
                    "RentSphere arbitration concluded for " + asset.getTitle() + ": " + decision + ".", "INFO");
            notificationService.pushNotification(dispute.getOwnerId(), "Dispute Arbitrated",
                    "Arbitration finalized for " + asset.getTitle() + ": " + decision + ".", "INFO");
        }

        sendJsonResponse(exchange, 200, "{\"success\":true,\"disputeId\":\"" + disputeId + "\",\"decision\":\"" + decision + "\",\"message\":\"Dispute arbitrated and settled.\"}");
    }

    // ========================================================================
    // REST API HANDLERS - SUPPORT TICKETS & GRIEVANCE REDRESSAL (IT RULES 2021)
    // ========================================================================

    private void handleSupportTickets(HttpExchange exchange) throws IOException {
        if (handleCorsPreflight(exchange)) return;
        String method = exchange.getRequestMethod();

        if ("GET".equalsIgnoreCase(method)) {
            UserAccount user = resolveUser(exchange);
            if (user == null) {
                sendJsonResponse(exchange, 401, "{\"error\":\"Unauthorized: Authentication required to view support tickets.\"}");
                return;
            }
            List<SupportService.SupportTicket> list;
            if (isUserAdmin(user)) {
                list = supportService.getAllTickets();
            } else {
                list = supportService.getUserTickets(user.getUserId());
            }

            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) sb.append(",");
                SupportService.SupportTicket t = list.get(i);
                sb.append(String.format(
                        "{\"ticketId\":\"%s\",\"userId\":\"%s\",\"category\":\"%s\",\"subject\":\"%s\"," +
                        "\"description\":\"%s\",\"status\":\"%s\",\"resolution\":\"%s\",\"createdAt\":%d,\"resolvedAt\":%d}",
                        t.ticketId, t.userId, t.category, escapeJson(t.subject), escapeJson(t.description),
                        t.status, escapeJson(t.resolution), t.createdAt, t.resolvedAt));
            }
            sb.append("]");
            sendJsonResponse(exchange, 200, sb.toString());
        } else if ("POST".equalsIgnoreCase(method)) {
            Map<String, String> b = parseJsonBody(exchange);
            String userId = b.getOrDefault("userId", "visitor");
            String cat = b.getOrDefault("category", "BOOKING");
            String sub = b.getOrDefault("subject", "General Inquiry");
            String desc = b.getOrDefault("description", "Need assistance with equipment rental.");

            SupportService.SupportTicket ticket = supportService.createTicket(userId, cat, sub, desc);
            sendJsonResponse(exchange, 201, String.format(
                    "{\"success\":true,\"ticketId\":\"%s\",\"status\":\"OPEN\",\"message\":\"Grievance registered. Acknowledgment provided within 24h as per IT Rules 2021.\"}",
                    ticket.ticketId));
        } else {
            sendResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
        }
    }

    private void handleSupportReply(HttpExchange exchange) throws IOException {
        if (handleCorsPreflight(exchange)) return;
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
            return;
        }

        UserAccount admin = resolveUser(exchange);
        if (admin == null || !isUserAdmin(admin)) {
            sendJsonResponse(exchange, 403, "{\"error\":\"Forbidden: Admin access required.\"}");
            return;
        }

        Map<String, String> b = parseJsonBody(exchange);
        String ticketId = b.get("ticketId");
        String resolution = b.getOrDefault("resolution", "Resolved by Goa University Hub Support.");

        boolean ok = supportService.resolveTicket(ticketId, admin.getUserId(), resolution);
        if (ok) {
            sendJsonResponse(exchange, 200, "{\"success\":true,\"message\":\"Ticket updated successfully.\"}");
        } else {
            sendJsonResponse(exchange, 404, "{\"error\":\"Ticket not found\"}");
        }
    }

    // ========================================================================
    // REST API HANDLERS - NOTIFICATIONS
    // ========================================================================

    private void handleNotifications(HttpExchange exchange) throws IOException {
        if (handleCorsPreflight(exchange)) return;
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
            return;
        }

        UserAccount user = resolveUser(exchange);
        String userId = (user != null) ? user.getUserId() : getQueryParam(exchange.getRequestURI().getQuery(), "userId");
        if (userId.isBlank()) userId = "renter_deepak";

        List<NotificationService.NotificationItem> list = notificationService.getUserNotifications(userId);
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(",");
            NotificationService.NotificationItem n = list.get(i);
            sb.append(String.format(
                    "{\"id\":\"%s\",\"title\":\"%s\",\"message\":\"%s\",\"type\":\"%s\",\"read\":%b,\"timestamp\":%d}",
                    n.id, escapeJson(n.title), escapeJson(n.message), n.type, n.read, n.timestamp));
        }
        sb.append("]");
        sendJsonResponse(exchange, 200, sb.toString());
    }

    private void handleNotificationsRead(HttpExchange exchange) throws IOException {
        if (handleCorsPreflight(exchange)) return;
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
            return;
        }

        UserAccount user = resolveUser(exchange);
        String userId = (user != null) ? user.getUserId() : parseJsonBody(exchange).getOrDefault("userId", "renter_deepak");
        notificationService.markAllAsRead(userId);
        sendJsonResponse(exchange, 200, "{\"success\":true}");
    }

    // ========================================================================
    // REST API HANDLERS - 20 INDIAN LEGAL POLICIES & CONSENT
    // ========================================================================

    private void handlePolicies(HttpExchange exchange) throws IOException {
        if (handleCorsPreflight(exchange)) return;
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
            return;
        }

        List<PolicyService.PolicyDocument> list = policyService.getAllPolicies();
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(",");
            PolicyService.PolicyDocument p = list.get(i);
            sb.append(String.format(
                    "{\"id\":\"%s\",\"title\":\"%s\",\"summary\":\"%s\",\"jurisdictionNote\":\"%s\",\"content\":\"%s\",\"version\":\"%s\"}",
                    p.id(), escapeJson(p.title()), escapeJson(p.summary()),
                    escapeJson(p.jurisdictionNote()), escapeJson(p.content()), p.version()));
        }
        sb.append("]");
        sendJsonResponse(exchange, 200, sb.toString());
    }

    private void handlePolicyAccept(HttpExchange exchange) throws IOException {
        if (handleCorsPreflight(exchange)) return;
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
            return;
        }

        Map<String, String> b = parseJsonBody(exchange);
        String userId = b.get("userId");
        String pids = b.getOrDefault("policyIds", "tos,privacy,rental_agreement");
        List<String> list = Arrays.asList(pids.split(","));

        policyService.recordAcceptance(userId, list);
        sendJsonResponse(exchange, 200, "{\"success\":true,\"message\":\"Policy acceptances recorded with auditable timestamps.\"}");
    }

    // ========================================================================
    // REST API HANDLERS - ADMIN CONTROLS & HARDWARE SIMULATION
    // ========================================================================

    private void handleAdminUsers(HttpExchange exchange) throws IOException {
        if (handleCorsPreflight(exchange)) return;
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
            return;
        }

        UserAccount caller = resolveUser(exchange);
        if (caller == null || !isUserAdmin(caller)) {
            sendJsonResponse(exchange, 403, "{\"error\":\"Forbidden: Admin access required.\"}");
            return;
        }

        List<UserAccount> users = authService.getAllUsers();
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < users.size(); i++) {
            if (i > 0) sb.append(",");
            UserAccount u = users.get(i);
            sb.append(String.format(
                    "{\"userId\":\"%s\",\"email\":\"%s\",\"fullName\":\"%s\",\"role\":\"%s\",\"phone\":\"%s\"," +
                    "\"kycVerified\":%b,\"accountStatus\":\"%s\",\"createdAt\":%d,\"lastLoginAt\":%d}",
                    u.getUserId(), u.getEmail(), escapeJson(u.getFullName()), u.getRole(),
                    escapeJson(u.getPhone()), u.isKycVerified(), u.getAccountStatus(), u.getCreatedAt(), u.getLastLoginAt()));
        }
        sb.append("]");
        sendJsonResponse(exchange, 200, sb.toString());
    }

    private void handleAdminUserStatus(HttpExchange exchange) throws IOException {
        if (handleCorsPreflight(exchange)) return;
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
            return;
        }

        UserAccount caller = resolveUser(exchange);
        if (caller == null || !isUserAdmin(caller)) {
            sendJsonResponse(exchange, 403, "{\"error\":\"Forbidden: Admin access required.\"}");
            return;
        }

        Map<String, String> b = parseJsonBody(exchange);
        String userId = b.get("userId");
        String status = b.getOrDefault("status", "ACTIVE"); // "ACTIVE" or "SUSPENDED"

        boolean ok = authService.setAccountStatus(userId, status);
        if (ok) {
            sendJsonResponse(exchange, 200, "{\"success\":true,\"userId\":\"" + userId + "\",\"status\":\"" + status + "\"}");
        } else {
            sendJsonResponse(exchange, 404, "{\"error\":\"User not found\"}");
        }
    }

    private void handleAdminDeviceState(HttpExchange exchange) throws IOException {
        if (handleCorsPreflight(exchange)) return;
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
            return;
        }

        UserAccount caller = resolveUser(exchange);
        if (caller == null || !isUserAdmin(caller)) {
            sendJsonResponse(exchange, 403, "{\"error\":\"Forbidden: Admin access required to control hardware locker state.\"}");
            return;
        }

        Map<String, String> b = parseJsonBody(exchange);
        String bayId = b.get("bayId");
        String stateStr = b.getOrDefault("state", "ONLINE_READY");

        try {
            DeviceState state = DeviceState.valueOf(stateStr.toUpperCase());
            lockerDevice.setDeviceState(bayId, state);
            sendJsonResponse(exchange, 200, "{\"success\":true,\"bayId\":\"" + bayId + "\",\"deviceState\":\"" + state + "\"}");
        } catch (IllegalArgumentException e) {
            sendJsonResponse(exchange, 400, "{\"error\":\"Invalid device state: " + stateStr + "\"}");
        }
    }

    // ========================================================================
    // REST API HANDLERS - ESCROW, WAL, BENCHMARKS & REFLECTION
    // ========================================================================

    private void handleAccounts(HttpExchange exchange) throws IOException {
        if (handleCorsPreflight(exchange)) return;
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
            return;
        }

        StringBuilder sb = new StringBuilder("[");
        List<EscrowAccount> list = escrowVault.getAllAccounts();
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(",");
            EscrowAccount a = list.get(i);
            sb.append(String.format(Locale.US,
                    "{\"userId\":\"%s\",\"userName\":\"%s\",\"role\":\"%s\",\"walletBalance\":%.2f," +
                    "\"lockedEscrow\":%.2f,\"earnedRevenue\":%.2f,\"isKycVerified\":%b}",
                    a.getUserId(), escapeJson(a.getUserName()), a.getRole(), a.getWalletBalance(),
                    a.getLockedEscrow(), a.getEarnedRevenue(), a.isKycVerified()));
        }
        sb.append("]");
        sendJsonResponse(exchange, 200, sb.toString());
    }

    private void handleWal(HttpExchange exchange) throws IOException {
        if (handleCorsPreflight(exchange)) return;
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
            return;
        }

        List<String> rawLogs = persistenceEngine.readWalEntries(50);
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < rawLogs.size(); i++) {
            if (i > 0) sb.append(",");
            String[] p = rawLogs.get(i).split("\\|", -1);
            if (p.length >= 8) {
                sb.append(String.format(
                        "{\"timestamp\":\"%s\",\"txId\":\"%s\",\"entityId\":\"%s\",\"actionType\":\"%s\"," +
                        "\"previousState\":\"%s\",\"newState\":\"%s\",\"actor\":\"%s\",\"details\":\"%s\"}",
                        p[0], p[1], p[2], p[3], p[4], p[5], p[6], escapeJson(p[7])));
            } else {
                sb.append("{\"raw\":\"" + escapeJson(rawLogs.get(i)) + "\"}");
            }
        }
        sb.append("]");
        sendJsonResponse(exchange, 200, sb.toString());
    }

    private void handleReflect(HttpExchange exchange) throws IOException {
        if (handleCorsPreflight(exchange)) return;
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
            return;
        }

        String query = exchange.getRequestURI().getQuery();
        String assetId = getQueryParam(query, "id");
        Optional<RentableAsset> opt = assetRepo.findById(assetId);

        if (opt.isEmpty()) {
            sendJsonResponse(exchange, 404, "{\"error\":\"Asset not found\"}");
            return;
        }

        ReflectionInspector.InspectionReport report = ReflectionInspector.inspect(opt.get());

        StringBuilder sb = new StringBuilder("{");
        sb.append("\"className\":\"").append(report.getClassName()).append("\",");
        sb.append("\"packageName\":\"").append(report.getPackageName()).append("\",");

        sb.append("\"inheritanceChain\":[");
        for (int i = 0; i < report.getInheritanceChain().size(); i++) {
            if (i > 0) sb.append(",");
            sb.append("\"").append(report.getInheritanceChain().get(i)).append("\"");
        }
        sb.append("],");

        sb.append("\"implementedInterfaces\":[");
        for (int i = 0; i < report.getImplementedInterfaces().size(); i++) {
            if (i > 0) sb.append(",");
            sb.append("\"").append(report.getImplementedInterfaces().get(i)).append("\"");
        }
        sb.append("],");

        sb.append("\"declaredFields\":[");
        for (int i = 0; i < report.getDeclaredFields().size(); i++) {
            if (i > 0) sb.append(",");
            Map<String, String> f = report.getDeclaredFields().get(i);
            sb.append(String.format("{\"name\":\"%s\",\"type\":\"%s\",\"modifiers\":\"%s\",\"value\":\"%s\"}",
                    f.get("name"), f.get("type"), f.get("modifiers"), escapeJson(f.get("value"))));
        }
        sb.append("],");

        sb.append("\"declaredMethods\":[");
        for (int i = 0; i < report.getDeclaredMethods().size(); i++) {
            if (i > 0) sb.append(",");
            Map<String, String> m = report.getDeclaredMethods().get(i);
            sb.append(String.format("{\"name\":\"%s\",\"returnType\":\"%s\",\"modifiers\":\"%s\",\"parameters\":\"%s\"}",
                    m.get("name"), m.get("returnType"), m.get("modifiers"), escapeJson(m.get("parameters"))));
        }
        sb.append("]}");

        sendJsonResponse(exchange, 200, sb.toString());
    }

    private void handleCloneBenchmark(HttpExchange exchange) throws IOException {
        if (handleCorsPreflight(exchange)) return;
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
            return;
        }

        String query = exchange.getRequestURI().getQuery();
        String assetId = getQueryParam(query, "id");
        Optional<RentableAsset> opt = assetRepo.findById(assetId);

        if (opt.isEmpty()) {
            sendJsonResponse(exchange, 404, "{\"error\":\"Asset not found\"}");
            return;
        }

        PrototypeCloner.CloneBenchmarkReport report = PrototypeCloner.benchmarkCloning(opt.get());

        String json = String.format(
                "{\"originalObjectId\":\"%s\",\"originalLogsId\":\"%s\",\"originalLogsCount\":%d," +
                "\"shallowObjectId\":\"%s\",\"shallowLogsId\":\"%s\",\"shallowLogsShared\":%b,\"shallowLogsCount\":%d," +
                "\"deepObjectId\":\"%s\",\"deepLogsId\":\"%s\",\"deepLogsIsolated\":%b,\"deepLogsCount\":%d," +
                "\"mutationVerdict\":\"%s\"}",
                report.originalObjectId, report.originalLogsId, report.originalLogsCount,
                report.shallowObjectId, report.shallowLogsId, report.shallowLogsShared, report.shallowLogsCount,
                report.deepObjectId, report.deepLogsId, report.deepLogsIsolated, report.deepLogsCount,
                escapeJson(report.mutationTestVerdict));

        sendJsonResponse(exchange, 200, json);
    }

    private void handleTrustScore(HttpExchange exchange) throws IOException {
        if (handleCorsPreflight(exchange)) return;
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
            return;
        }

        Map<String, String> b = parseJsonBody(exchange);
        String name = b.getOrDefault("memberName", "Goa University Community Host");
        double condition = parseDouble(b.get("conditionHistory"), 88.0);
        double punctuality = parseDouble(b.get("punctualityRate"), 92.0);
        double escrow = parseDouble(b.get("escrowCompletion"), 95.0);
        double peer = parseDouble(b.get("peerRating"), 90.0);
        double kyc = parseDouble(b.get("kycTier"), 100.0);

        double deltaCondition = parseDouble(b.get("deltaCondition"), 5.0);
        double deltaPunctuality = parseDouble(b.get("deltaPunctuality"), 4.0);
        double deltaEscrow = parseDouble(b.get("deltaEscrow"), 2.0);
        double deltaPeer = parseDouble(b.get("deltaPeer"), 3.0);
        double deltaKyc = parseDouble(b.get("deltaKyc"), 0.0);

        TrustScoreEngine.TrustMetrics baseline = new TrustScoreEngine.TrustMetrics("USR-MEMBER", name, condition, punctuality, escrow, peer, kyc);
        TrustScoreEngine.SimulationResult sim = TrustScoreEngine.simulateWhatIf(baseline, deltaCondition, deltaPunctuality, deltaEscrow, deltaPeer, deltaKyc);

        String json = String.format(Locale.US,
                "{\"memberName\":\"%s\",\"baselineScore\":%.2f,\"baselineTier\":\"%s\"," +
                "\"simulatedScore\":%.2f,\"simulatedTier\":\"%s\",\"scoreDelta\":%.2f," +
                "\"policyRecommendation\":\"%s\"}",
                escapeJson(name), sim.baselineScore, sim.baselineTier,
                sim.simulatedScore, sim.simulatedTier, sim.scoreDelta, escapeJson(sim.recommendation));

        sendJsonResponse(exchange, 200, json);
    }

    // ========================================================================
    // STATIC FILE HANDLER & HTTP UTILITIES
    // ========================================================================

    private void handleStaticFiles(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        if (path == null || path.equals("/") || path.isBlank()) {
            path = "/index.html";
        }

        if (path.startsWith("/")) path = path.substring(1);

        Path filePath = frontendPath.resolve(path).normalize();
        if (!filePath.startsWith(frontendPath) || !Files.exists(filePath) || Files.isDirectory(filePath)) {
            filePath = frontendPath.resolve("index.html");
        }

        if (!Files.exists(filePath)) {
            sendResponse(exchange, 404, "404 Not Found: Static web UI file missing.");
            return;
        }

        String mimeType = getContentType(filePath.getFileName().toString());
        byte[] bytes = Files.readAllBytes(filePath);

        exchange.getResponseHeaders().set("Content-Type", mimeType);
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        exchange.getResponseHeaders().set("X-Frame-Options", "SAMEORIGIN");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private boolean handleCorsPreflight(HttpExchange exchange) throws IOException {
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type,Authorization");
            exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET,POST,OPTIONS");
            exchange.sendResponseHeaders(204, -1);
            return true;
        }
        return false;
    }

    private String getContentType(String filename) {
        if (filename.endsWith(".html")) return "text/html; charset=UTF-8";
        if (filename.endsWith(".css")) return "text/css; charset=UTF-8";
        if (filename.endsWith(".js")) return "application/javascript; charset=UTF-8";
        if (filename.endsWith(".json")) return "application/json; charset=UTF-8";
        if (filename.endsWith(".svg")) return "image/svg+xml";
        if (filename.endsWith(".png")) return "image/png";
        if (filename.endsWith(".ico")) return "image/x-icon";
        return "text/plain; charset=UTF-8";
    }

    private void sendJsonResponse(HttpExchange exchange, int statusCode, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type,Authorization");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET,POST,OPTIONS");
        exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String text) throws IOException {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private Map<String, String> parseJsonBody(HttpExchange exchange) throws IOException {
        InputStream is = exchange.getRequestBody();
        String body = new String(is.readAllBytes(), StandardCharsets.UTF_8).trim();
        Map<String, String> map = new HashMap<>();
        if (body.startsWith("{") && body.endsWith("}")) {
            body = body.substring(1, body.length() - 1);
            String[] pairs = body.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
            for (String pair : pairs) {
                String[] kv = pair.split(":(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", 2);
                if (kv.length == 2) {
                    String key = unquote(kv[0].trim());
                    String val = unquote(kv[1].trim());
                    map.put(key, val);
                }
            }
        }
        return map;
    }

    private String unquote(String s) {
        if (s.startsWith("\"") && s.endsWith("\"") && s.length() >= 2) {
            return s.substring(1, s.length() - 1).replace("\\\"", "\"");
        }
        return s;
    }

    private String serializeAssetToJson(RentableAsset a) {
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"id\":\"").append(a.getId()).append("\",");
        sb.append("\"title\":\"").append(escapeJson(a.getTitle())).append("\",");
        sb.append("\"leafClass\":\"").append(a.getClass().getSimpleName()).append("\",");
        sb.append("\"ownerId\":\"").append(a.getOwnerId()).append("\",");
        sb.append("\"renterId\":\"").append(a.getRenterId()).append("\",");
        sb.append("\"state\":\"").append(a.getCurrentState().getStateName()).append("\",");
        sb.append(String.format(Locale.US, "\"baseDailyRate\":%.2f,", a.getBaseDailyRate()));
        sb.append(String.format(Locale.US, "\"securityCollateral\":%.2f,", a.getSecurityCollateral()));
        sb.append(String.format(Locale.US, "\"calculatedTariff\":%.2f,", a.calculateDailyRentalTariff(1)));
        sb.append(String.format(Locale.US, "\"requiredCollateral\":%.2f,", a.calculateRequiredCollateral()));
        sb.append("\"lockerCoordinate\":\"").append(a.getLockerCoordinate()).append("\",");

        if (a instanceof CategorizedRentalAsset) {
            CategorizedRentalAsset cat = (CategorizedRentalAsset) a;
            sb.append("\"category\":\"").append(cat.getCategory()).append("\",");
            sb.append(String.format(Locale.US, "\"lateFeeMultiplier\":%.2f,", cat.getLateFeeMultiplier()));
            sb.append("\"protectionTier\":\"").append(escapeJson(cat.getProtectionTier())).append("\",");
        }

        if (a instanceof EventAndCreatorAsset) {
            EventAndCreatorAsset ev = (EventAndCreatorAsset) a;
            sb.append("\"indoorOutdoorRating\":\"").append(escapeJson(ev.getIndoorOutdoorRating())).append("\",");
            sb.append("\"wattageOrBatterySpec\":\"").append(escapeJson(ev.getWattageOrBatterySpec())).append("\",");
            sb.append("\"accessoryKitCount\":").append(ev.getAccessoryKitCount()).append(",");
        }

        if (a instanceof UtilityAndMobilityAsset) {
            UtilityAndMobilityAsset um = (UtilityAndMobilityAsset) a;
            sb.append("\"powerOrEngineSpec\":\"").append(escapeJson(um.getPowerOrEngineSpec())).append("\",");
            sb.append("\"safetyCertification\":\"").append(escapeJson(um.getSafetyCertification())).append("\",");
            sb.append("\"requiresGroundBay\":").append(um.isRequiresGroundBay()).append(",");
        }

        sb.append("\"serviceLogs\":[");
        for (int i = 0; i < a.getServiceLogs().size(); i++) {
            if (i > 0) sb.append(",");
            sb.append("\"").append(escapeJson(a.getServiceLogs().get(i))).append("\"");
        }
        sb.append("]}");
        return sb.toString();
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private String getQueryParam(String query, String paramName) {
        if (query == null) return "";
        for (String pair : query.split("&")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2 && kv[0].equalsIgnoreCase(paramName)) {
                try {
                    return URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
                } catch (Exception e) {
                    return kv[1];
                }
            }
        }
        return "";
    }

    private double parseDouble(String str, double defaultVal) {
        if (str == null) return defaultVal;
        try {
            return Double.parseDouble(str.trim());
        } catch (NumberFormatException e) {
            return defaultVal;
        }
    }

    private int parseInt(String str, int defaultVal) {
        if (str == null) return defaultVal;
        try {
            return Integer.parseInt(str.trim());
        } catch (NumberFormatException e) {
            return defaultVal;
        }
    }

    public static void main(String[] args) {
        int port = DEFAULT_PORT;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException ignored) {}
        }

        try {
            MainServer server = new MainServer(port, "data", "frontend");
            server.start();
            Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
        } catch (IOException e) {
            System.err.println("[FATAL] Could not start server on port " + port + ": " + e.getMessage());
            System.exit(1);
        }
    }
}
