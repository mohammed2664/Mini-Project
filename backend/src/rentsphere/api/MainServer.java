package rentsphere.api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import rentsphere.escrow.EscrowAccount;
import rentsphere.escrow.EscrowVault;
import rentsphere.locker.CompartmentSize;
import rentsphere.locker.LockerCompartment;
import rentsphere.locker.SmartLockerGrid8x8;
import rentsphere.meta.GenericRepository;
import rentsphere.meta.NirfAnalyticsPortal;
import rentsphere.meta.PrototypeCloner;
import rentsphere.meta.ReflectionInspector;
import rentsphere.model.*;
import rentsphere.pipeline.FailFastPipeline;
import rentsphere.pipeline.GateResult;
import rentsphere.pipeline.PipelineContext;
import rentsphere.persistence.FlatFilePersistenceEngine;

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
 * Zero External Dependencies (Runs on standard JDK 17+ / 25).
 * 
 * Serves:
 *  - Single-Page Web Application on http://localhost:8080/
 *  - REST API Endpoints on http://localhost:8080/api/*
 * 
 * MCA OOT Lab - All 5 Syllabus Assignments
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
    private HttpServer server;

    public MainServer(int port, String dataDir, String frontendDir) {
        this.port = port;
        this.assetRepo = new GenericRepository<>();
        this.lockerGrid = new SmartLockerGrid8x8();
        this.escrowVault = new EscrowVault();
        this.pipeline = new FailFastPipeline();
        this.persistenceEngine = new FlatFilePersistenceEngine(dataDir);
        this.frontendPath = Paths.get(frontendDir != null ? frontendDir : "frontend");

        // Load initial state from flat files
        loadPersistentState();
    }

    private void loadPersistentState() {
        System.out.println("[BOOT] Initializing RentSphere Zero-Database Engine...");
        persistenceEngine.loadAssets(assetRepo);
        persistenceEngine.loadLockers(lockerGrid);
        persistenceEngine.loadEscrow(escrowVault);
        System.out.printf("[BOOT] Loaded %d assets, %d locker bays, and %d escrow accounts from flat files.%n",
                assetRepo.count(), lockerGrid.getAllCompartments().size(), escrowVault.getAllAccounts().size());
    }

    public synchronized void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());

        // Context: API routes
        server.createContext("/api/health", this::handleHealth);
        server.createContext("/api/assets", this::handleAssets);
        server.createContext("/api/assets/reserve", this::handleReserve);
        server.createContext("/api/assets/pickup", this::handlePickup);
        server.createContext("/api/assets/return", this::handleReturn);
        server.createContext("/api/assets/inspect-approve", this::handleInspectApprove);
        server.createContext("/api/assets/inspect-slash", this::handleInspectSlash);
        server.createContext("/api/assets/onboard", this::handleOnboard);
        server.createContext("/api/lockers", this::handleLockers);
        server.createContext("/api/lockers/verify-otp", this::handleVerifyOtp);
        server.createContext("/api/lockers/telemetry", this::handleTelemetry);
        server.createContext("/api/escrow/accounts", this::handleAccounts);
        server.createContext("/api/wal", this::handleWal);
        server.createContext("/api/meta/reflect", this::handleReflect);
        server.createContext("/api/meta/clone", this::handleClone);
        server.createContext("/api/meta/nirf", this::handleNirf);

        // Context: Static frontend file delivery (Root handler)
        server.createContext("/", this::handleStaticFiles);

        server.start();
        System.out.println("=================================================================");
        System.out.println("  RentSphere P2P Engine & 8x8 Smart Micro-Storage Grid ACTIVE");
        System.out.println("  Web UI:   http://localhost:" + port + "/");
        System.out.println("  REST API: http://localhost:" + port + "/api/health");
        System.out.println("  Platform: Pure Java SE (Zero External Dependencies)");
        System.out.println("=================================================================");
    }

    public synchronized void stop() {
        if (server != null) {
            server.stop(1);
            System.out.println("[SHUTDOWN] RentSphere server halted.");
        }
    }

    // ========================================================================
    // REST API HANDLERS
    // ========================================================================

    private void handleHealth(HttpExchange exchange) throws IOException {
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
                "{\"status\":\"UP\",\"appName\":\"RentSphere P2P Engine\",\"totalAssets\":%d," +
                "\"activeLeases\":%d,\"inQuarantine\":%d,\"lockedEscrowCapital\":%.2f," +
                "\"totalLockerBays\":%d,\"runtime\":\"Java SE %s\"}",
                assetRepo.count(), activeLeaseCount, inQuarantineCount, lockedTotal,
                lockerGrid.getAllCompartments().size(), System.getProperty("java.version"));

        sendJsonResponse(exchange, 200, json);
    }

    private void handleAssets(HttpExchange exchange) throws IOException {
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
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
            return;
        }

        Map<String, String> body = parseJsonBody(exchange);
        String assetId = body.get("assetId");
        String renterId = body.get("renterId");
        int days = parseInt(body.get("rentalDays"), 1);

        Optional<RentableAsset> optAsset = assetRepo.findById(assetId);
        if (optAsset.isEmpty()) {
            sendJsonResponse(exchange, 404, "{\"error\":\"Asset not found: " + assetId + "\"}");
            return;
        }

        RentableAsset asset = optAsset.get();
        EscrowAccount renter = escrowVault.getAccount(renterId);
        double balance = (renter != null) ? renter.getWalletBalance() : 0.0;
        boolean kyc = (renter != null) && renter.isKycVerified();

        // EXECUTE 4-GATE FAIL-FAST PIPELINE
        PipelineContext ctx = new PipelineContext(
                asset, renterId, "RESERVE", balance, kyc, asset.getLockerCoordinate(), days, 0.0
        );

        GateResult verdict = pipeline.verify(ctx);
        if (!verdict.isPassed()) {
            // Pipeline Short-Circuit
            persistenceEngine.appendWal(assetId, "RESERVE_BLOCKED", asset.getCurrentState().getStateName(),
                    asset.getCurrentState().getStateName(), renterId, 
                    "Blocked at Gate " + verdict.getGateIndex() + " (" + verdict.getGateName() + "): " + verdict.getDiagnostic());

            String err = String.format("{\"success\":false,\"gateIndex\":%d,\"gateName\":\"%s\",\"message\":\"%s\"}",
                    verdict.getGateIndex(), verdict.getGateName(), escapeJson(verdict.getDiagnostic()));
            sendJsonResponse(exchange, 400, err);
            return;
        }

        // All 4 gates passed: Execute state transition and lock escrow
        double tariff = asset.calculateDailyRentalTariff(days);
        double collateral = asset.calculateRequiredCollateral();

        boolean locked = escrowVault.lockEscrow(renterId, asset.getOwnerId(), assetId, tariff, collateral);
        if (!locked) {
            sendJsonResponse(exchange, 500, "{\"error\":\"Escrow lock failed unexpectedly.\"}");
            return;
        }

        String prevState = asset.getCurrentState().getStateName();
        asset.getCurrentState().reserve(asset, renterId);

        // Update locker bay compartment
        LockerCompartment comp = lockerGrid.getCompartment(asset.getLockerCoordinate());
        String otp = "000000";
        if (comp != null) {
            comp.setStatus("RESERVED_LOCKED");
            otp = comp.generateNewOtp();
        }

        asset.addMaintenanceLog("Reserved for " + days + " days by " + renterId + " (Escrow: $" + (tariff + collateral) + ")");

        // Persist to flat files and WAL
        persistenceEngine.saveAssets(assetRepo);
        persistenceEngine.saveLockers(lockerGrid);
        persistenceEngine.saveEscrow(escrowVault);
        persistenceEngine.appendWal(assetId, "RESERVE", prevState, asset.getCurrentState().getStateName(), renterId,
                String.format("Escrow locked $%.2f. OTP %s generated for bay %s", (tariff + collateral), otp, asset.getLockerCoordinate()));

        String res = String.format(Locale.US,
                "{\"success\":true,\"state\":\"%s\",\"assetId\":\"%s\",\"otpPin\":\"%s\",\"lockerBay\":\"%s\"," +
                "\"tariff\":%.2f,\"collateral\":%.2f,\"message\":\"Reservation successful! Escrow locked. Present OTP at locker bay to unlock.\"}",
                asset.getCurrentState().getStateName(), assetId, otp, asset.getLockerCoordinate(), tariff, collateral);

        sendJsonResponse(exchange, 200, res);
    }

    private void handlePickup(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
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
        LockerCompartment comp = lockerGrid.getCompartment(asset.getLockerCoordinate());
        if (comp == null || !comp.verifyOtp(otp)) {
            sendJsonResponse(exchange, 400, "{\"success\":false,\"message\":\"Invalid 6-Digit OTP passcode for Bay " + asset.getLockerCoordinate() + "\"}");
            return;
        }

        // Execute State Transition: RESERVED_ESCROW -> ACTIVE_LEASE
        String prevState = asset.getCurrentState().getStateName();
        try {
            asset.getCurrentState().pickup(asset);
        } catch (IllegalStateException e) {
            sendJsonResponse(exchange, 400, "{\"success\":false,\"message\":\"" + escapeJson(e.getMessage()) + "\"}");
            return;
        }

        // Compartment is now open and vacated
        comp.setOccupied(false);
        comp.setStatus("DISPATCHED_ACTIVE");
        comp.setWeightKg(0.0);
        asset.addMaintenanceLog("Asset picked up from bay " + comp.getBayId() + " via OTP verification.");

        persistenceEngine.saveAssets(assetRepo);
        persistenceEngine.saveLockers(lockerGrid);
        persistenceEngine.appendWal(assetId, "PICKUP", prevState, asset.getCurrentState().getStateName(), asset.getRenterId(),
                "Asset unlocked via OTP " + otp + ". Bay " + comp.getBayId() + " now empty in-field.");

        String res = String.format(
                "{\"success\":true,\"state\":\"%s\",\"assetId\":\"%s\",\"message\":\"Compartment unlocked! Gear successfully retrieved.\"}",
                asset.getCurrentState().getStateName(), assetId);
        sendJsonResponse(exchange, 200, res);
    }

    private void handleReturn(HttpExchange exchange) throws IOException {
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

        // MANDATORY QUARANTINE INVARIANT:
        // Must transition to InspectionQuarantineState, NEVER AvailableState!
        try {
            asset.getCurrentState().initiateReturn(asset);
        } catch (IllegalStateException e) {
            sendJsonResponse(exchange, 400, "{\"success\":false,\"message\":\"" + escapeJson(e.getMessage()) + "\"}");
            return;
        }

        // Lock locker bay into Quarantine mode
        LockerCompartment comp = lockerGrid.getCompartment(asset.getLockerCoordinate());
        if (comp != null) {
            comp.setOccupied(true);
            comp.setStatus("QUARANTINE_LOCKED");
            comp.setWeightKg(15.0); // Simulated return weight
        }

        asset.addMaintenanceLog("Deposited in Bay " + asset.getLockerCoordinate() + ". MANDATORY QUARANTINE: Awaiting host inspection.");

        persistenceEngine.saveAssets(assetRepo);
        persistenceEngine.saveLockers(lockerGrid);
        persistenceEngine.appendWal(assetId, "RETURN", prevState, asset.getCurrentState().getStateName(), asset.getRenterId(),
                "MANDATORY QUARANTINE ENFORCED: Gear returned to bay " + asset.getLockerCoordinate() + ". Collateral held.");

        String res = String.format(
                "{\"success\":true,\"state\":\"%s\",\"assetId\":\"%s\",\"message\":\"Asset returned to locker. MANDATORY QUARANTINE active: Collateral held until host approval.\"}",
                asset.getCurrentState().getStateName(), assetId);
        sendJsonResponse(exchange, 200, res);
    }

    private void handleInspectApprove(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
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
        String renterId = asset.getRenterId();
        double tariff = asset.calculateDailyRentalTariff(1);
        double collateral = asset.calculateRequiredCollateral();

        String prevState = asset.getCurrentState().getStateName();
        try {
            asset.getCurrentState().approveInspection(asset);
        } catch (IllegalStateException e) {
            sendJsonResponse(exchange, 400, "{\"success\":false,\"message\":\"" + escapeJson(e.getMessage()) + "\"}");
            return;
        }

        // Release escrow clean
        escrowVault.releasePayoutClean(renterId, asset.getOwnerId(), assetId, tariff, collateral);

        LockerCompartment comp = lockerGrid.getCompartment(asset.getLockerCoordinate());
        if (comp != null) {
            comp.setStatus("ONLINE_READY");
        }

        asset.addMaintenanceLog("Clean Inspection Approved by " + hostId + ". 100% deposit refunded. Gear restored to AVAILABLE.");

        persistenceEngine.saveAssets(assetRepo);
        persistenceEngine.saveLockers(lockerGrid);
        persistenceEngine.saveEscrow(escrowVault);
        persistenceEngine.appendWal(assetId, "INSPECT_APPROVE", prevState, asset.getCurrentState().getStateName(), hostId,
                String.format("Clean return approved. Slashed $0. Refunded $%.2f collateral to %s. Paid $%.2f to owner.",
                        collateral, renterId, tariff));

        String res = String.format(Locale.US,
                "{\"success\":true,\"state\":\"%s\",\"message\":\"Inspection Approved! 100%% Collateral ($%.2f) refunded to renter. $%.2f rental fee paid to owner.\"}",
                asset.getCurrentState().getStateName(), collateral, tariff);
        sendJsonResponse(exchange, 200, res);
    }

    private void handleInspectSlash(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
            return;
        }

        Map<String, String> body = parseJsonBody(exchange);
        String assetId = body.get("assetId");
        String hostId = body.get("hostId");
        double damagePct = parseDouble(body.get("damagePercentage"), 20.0);

        Optional<RentableAsset> optAsset = assetRepo.findById(assetId);
        if (optAsset.isEmpty()) {
            sendJsonResponse(exchange, 404, "{\"error\":\"Asset not found\"}");
            return;
        }

        RentableAsset asset = optAsset.get();
        String renterId = asset.getRenterId();
        double tariff = asset.calculateDailyRentalTariff(1);
        double collateral = asset.calculateRequiredCollateral();
        double slashingPenalty = asset.calculateDamageSlashingPenalty(damagePct);

        String prevState = asset.getCurrentState().getStateName();
        try {
            asset.getCurrentState().slashCollateral(asset, slashingPenalty);
        } catch (IllegalStateException e) {
            sendJsonResponse(exchange, 400, "{\"success\":false,\"message\":\"" + escapeJson(e.getMessage()) + "\"}");
            return;
        }

        // Execute Escrow Slashing
        escrowVault.slashCollateral(renterId, asset.getOwnerId(), assetId, tariff, collateral, slashingPenalty);

        LockerCompartment comp = lockerGrid.getCompartment(asset.getLockerCoordinate());
        if (comp != null) {
            comp.setStatus("ONLINE_READY");
        }

        double netRefund = Math.max(0.0, collateral - slashingPenalty);
        asset.addMaintenanceLog(String.format(Locale.US, "Damage Slashed: %.1f%% penalty ($%.2f) deducted from collateral. Repair scheduled.", damagePct, slashingPenalty));

        persistenceEngine.saveAssets(assetRepo);
        persistenceEngine.saveLockers(lockerGrid);
        persistenceEngine.saveEscrow(escrowVault);
        persistenceEngine.appendWal(assetId, "INSPECT_SLASH", prevState, asset.getCurrentState().getStateName(), hostId,
                String.format(Locale.US, "Damage flagged (%.1f%%). Slashed $%.2f to owner. Refunded $%.2f net deposit to %s.",
                        damagePct, slashingPenalty, netRefund, renterId));

        String res = String.format(Locale.US,
                "{\"success\":true,\"state\":\"%s\",\"slashedPenalty\":%.2f,\"refundedCollateral\":%.2f," +
                "\"message\":\"Damage Slashing Executed: $%.2f deducted from deposit for repairs. $%.2f net refund returned to renter.\"}",
                asset.getCurrentState().getStateName(), slashingPenalty, netRefund, slashingPenalty, netRefund);
        sendJsonResponse(exchange, 200, res);
    }

    private void handleOnboard(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
            return;
        }

        Map<String, String> b = parseJsonBody(exchange);
        String leafClass = b.getOrDefault("leafClass", "AutonomousMarineDrone");
        String title = b.getOrDefault("title", "New Asset");
        String ownerId = b.getOrDefault("ownerId", "host_faaris");
        String bay = b.getOrDefault("lockerBay", "A1").toUpperCase();
        double baseRate = parseDouble(b.get("baseDailyRate"), 500.0);
        double collateral = parseDouble(b.get("securityCollateral"), 1500.0);

        String assetId = "AST-" + (100 + assetRepo.count() + 1);
        RentableAsset newAsset;

        if ("AutonomousMarineDrone".equalsIgnoreCase(leafClass)) {
            newAsset = new AutonomousMarineDrone(
                    assetId, title, ownerId, baseRate, collateral, bay, 1.20, "Marine Comprehensive",
                    "IP68", 1.10, parseDouble(b.get("depth"), 100.0), Boolean.parseBoolean(b.get("sonar"))
            );
        } else if ("SubmersibleActionRig".equalsIgnoreCase(leafClass)) {
            newAsset = new SubmersibleActionRig(
                    assetId, title, ownerId, baseRate, collateral, bay, 1.10, "Standard Cover",
                    "IP68", 1.05, b.getOrDefault("stabilization", "GimbalTriple"),
                    Boolean.parseBoolean(b.get("is8k")), parseDouble(b.get("depth"), 40.0)
            );
        } else if ("PrecisionRotaryTiller".equalsIgnoreCase(leafClass)) {
            newAsset = new PrecisionRotaryTiller(
                    assetId, title, ownerId, baseRate, collateral, bay, 1.15, "Agri Machinery Protect",
                    b.getOrDefault("soil", "Laterite"), parseDouble(b.get("kw"), 40.0),
                    parseInt(b.get("rpm"), 2800), parseDouble(b.get("width"), 180.0)
            );
        } else {
            newAsset = new OpticalSoilSpectrometer(
                    assetId, title, ownerId, baseRate, collateral, bay, 1.05, "Precision Sensor Bond",
                    b.getOrDefault("soil", "Alluvial"), parseDouble(b.get("kw"), 2.0),
                    parseInt(b.get("bands"), 16), parseDouble(b.get("accuracy"), 98.0)
            );
        }

        // Validate Row 7 Heavy-Agri rule
        if (!lockerGrid.validateHeavyAgriPlacement(newAsset, bay)) {
            sendJsonResponse(exchange, 400, "{\"success\":false,\"message\":\"Heavy Agricultural Machinery must be assigned to Row H (Row 7). Specified bay: " + bay + "\"}");
            return;
        }

        assetRepo.save(newAsset);
        lockerGrid.assignAssetToBay(newAsset, bay);

        persistenceEngine.saveAssets(assetRepo);
        persistenceEngine.saveLockers(lockerGrid);
        persistenceEngine.appendWal(assetId, "ONBOARD_ASSET", "NONE", "AVAILABLE", ownerId,
                "Onboarded new " + leafClass + " in Bay " + bay);

        sendJsonResponse(exchange, 201, "{\"success\":true,\"assetId\":\"" + assetId + "\",\"message\":\"Asset successfully onboarded into 8x8 Smart Locker Matrix!\"}");
    }

    private void handleLockers(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
            return;
        }

        StringBuilder sb = new StringBuilder("[");
        List<LockerCompartment> list = lockerGrid.getAllCompartments();
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(",");
            LockerCompartment c = list.get(i);
            sb.append(String.format(Locale.US,
                    "{\"bayId\":\"%s\",\"row\":%d,\"col\":%d,\"sizeTier\":\"%s\",\"occupied\":%b," +
                    "\"currentAssetId\":\"%s\",\"status\":\"%s\",\"otpPin\":\"%s\",\"tempC\":%.1f," +
                    "\"humidityPct\":%.1f,\"weightKg\":%.1f}",
                    c.getBayId(), c.getRow(), c.getCol(), c.getSizeTier().name(), c.isOccupied(),
                    c.getCurrentAssetId(), c.getStatus(), c.getOtpPin(), c.getTempC(),
                    c.getHumidityPct(), c.getWeightKg()));
        }
        sb.append("]");
        sendJsonResponse(exchange, 200, sb.toString());
    }

    private void handleVerifyOtp(HttpExchange exchange) throws IOException {
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
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
            return;
        }

        Map<String, String> b = parseJsonBody(exchange);
        String bayId = b.get("bayId");
        double temp = parseDouble(b.get("tempC"), 24.0);
        double hum = parseDouble(b.get("humidityPct"), 45.0);
        double weight = parseDouble(b.get("weightKg"), 0.0);

        boolean updated = lockerGrid.updateTelemetry(bayId, temp, hum, weight);
        if (updated) {
            persistenceEngine.saveLockers(lockerGrid);
            sendJsonResponse(exchange, 200, "{\"success\":true}");
        } else {
            sendJsonResponse(exchange, 404, "{\"error\":\"Bay not found\"}");
        }
    }

    private void handleAccounts(HttpExchange exchange) throws IOException {
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

        // Serialize report to JSON
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

    private void handleClone(HttpExchange exchange) throws IOException {
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

    private void handleNirf(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
            return;
        }

        Map<String, String> b = parseJsonBody(exchange);
        String name = b.getOrDefault("institutionName", "Goa University - Dept of CST (MCA)");
        double tlr = parseDouble(b.get("tlr"), 78.5);
        double rp = parseDouble(b.get("rp"), 64.2);
        double go = parseDouble(b.get("go"), 82.0);
        double oi = parseDouble(b.get("oi"), 70.5);
        double pr = parseDouble(b.get("pr"), 65.0);

        double deltaTlr = parseDouble(b.get("deltaTlr"), 5.0);
        double deltaRp = parseDouble(b.get("deltaRp"), 8.0);
        double deltaGo = parseDouble(b.get("deltaGo"), 3.0);
        double deltaOi = parseDouble(b.get("deltaOi"), 2.0);
        double deltaPr = parseDouble(b.get("deltaPr"), 5.0);

        NirfAnalyticsPortal.InstitutionMetrics baseline = new NirfAnalyticsPortal.InstitutionMetrics(name, tlr, rp, go, oi, pr);
        NirfAnalyticsPortal.SimulationResult sim = NirfAnalyticsPortal.simulateWhatIf(baseline, deltaTlr, deltaRp, deltaGo, deltaOi, deltaPr);

        String json = String.format(Locale.US,
                "{\"institutionName\":\"%s\",\"baselineScore\":%.2f,\"baselineTier\":\"%s\"," +
                "\"simulatedScore\":%.2f,\"simulatedTier\":\"%s\",\"scoreDelta\":%.2f," +
                "\"policyRecommendation\":\"%s\"}",
                escapeJson(name), sim.baselineScore, sim.baselineTier,
                sim.simulatedScore, sim.simulatedTier, sim.scoreDelta, escapeJson(sim.policyRecommendation));

        sendJsonResponse(exchange, 200, json);
    }

    // ========================================================================
    // STATIC FILE HANDLER (Single-Page Web Application Delivery)
    // ========================================================================
    private void handleStaticFiles(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        if (path == null || path.equals("/") || path.isBlank()) {
            path = "/index.html";
        }

        // Strip leading slash
        if (path.startsWith("/")) path = path.substring(1);

        Path filePath = frontendPath.resolve(path).normalize();
        if (!filePath.startsWith(frontendPath) || !Files.exists(filePath) || Files.isDirectory(filePath)) {
            // Fallback to index.html for Single Page Application routing
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
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
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

    // ========================================================================
    // JSON & HTTP UTILITIES (Zero External Dependencies)
    // ========================================================================
    private void sendJsonResponse(HttpExchange exchange, int statusCode, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type,Authorization");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET,POST,OPTIONS");
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
            // Simple robust regex-free JSON key-value extractor for flat payloads
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

        if (a instanceof SectoralAsset) {
            SectoralAsset s = (SectoralAsset) a;
            sb.append("\"sector\":\"").append(s.getSector()).append("\",");
            sb.append(String.format(Locale.US, "\"seasonalMultiplier\":%.2f,", s.getSeasonalDemandMultiplier()));
            sb.append("\"insuranceTier\":\"").append(escapeJson(s.getInsuranceTier())).append("\",");
        }

        if (a instanceof TourismAsset) {
            TourismAsset t = (TourismAsset) a;
            sb.append("\"ipRating\":\"").append(t.getWaterproofIpRating()).append("\",");
            sb.append(String.format(Locale.US, "\"corrosionFactor\":%.2f,", t.getSaltwaterCorrosionFactor()));
        }

        if (a instanceof AgricultureAsset) {
            AgricultureAsset ag = (AgricultureAsset) a;
            sb.append("\"soilType\":\"").append(escapeJson(ag.getCompatibleSoilType())).append("\",");
            sb.append(String.format(Locale.US, "\"powerKw\":%.1f,", ag.getPowerKwRating()));
            sb.append("\"heavyMachinery\":").append(ag.isHeavyMachineryFlag()).append(",");
        }

        sb.append("\"maintenanceLogs\":[");
        for (int i = 0; i < a.getMaintenanceLogs().size(); i++) {
            if (i > 0) sb.append(",");
            sb.append("\"").append(escapeJson(a.getMaintenanceLogs().get(i))).append("\"");
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

            // Keep main thread alive
            Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
        } catch (IOException e) {
            System.err.println("[FATAL] Could not start server on port " + port + ": " + e.getMessage());
            System.exit(1);
        }
    }
}
