package rentsphere.api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import rentsphere.escrow.EscrowAccount;
import rentsphere.escrow.EscrowVault;
import rentsphere.locker.LockerCompartment;
import rentsphere.locker.SmartLockerGrid8x8;
import rentsphere.meta.GenericRepository;
import rentsphere.meta.PrototypeCloner;
import rentsphere.meta.ReflectionInspector;
import rentsphere.meta.TrustScoreEngine;
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
 * Native Indian Rupee (₹ INR) currency across all transactions.
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

        // Context: API routes
        server.createContext("/api/health", this::handleHealth);
        server.createContext("/api/assets", this::handleAssets);
        server.createContext("/api/assets/reserve", this::handleReserve);
        server.createContext("/api/assets/pickup", this::handlePickup);
        server.createContext("/api/assets/return", this::handleReturn);
        server.createContext("/api/assets/inspect-approve", this::handleInspectApprove);
        server.createContext("/api/assets/inspect-slash", this::handleInspectSlash);
        server.createContext("/api/assets/onboard", this::handleOnboard);
        server.createContext("/api/assets/clone", this::handleCloneListing);
        server.createContext("/api/lockers", this::handleLockers);
        server.createContext("/api/lockers/verify-otp", this::handleVerifyOtp);
        server.createContext("/api/lockers/telemetry", this::handleTelemetry);
        server.createContext("/api/escrow/accounts", this::handleAccounts);
        server.createContext("/api/wal", this::handleWal);
        server.createContext("/api/meta/reflect", this::handleReflect);
        server.createContext("/api/meta/clone", this::handleCloneBenchmark);
        server.createContext("/api/trust-score", this::handleTrustScore);
        // Compatibility alias
        server.createContext("/api/meta/nirf", this::handleTrustScore);

        // Context: Static frontend file delivery (Root handler)
        server.createContext("/", this::handleStaticFiles);

        server.start();
        System.out.println("=================================================================");
        System.out.println("  RentSphere P2P Engine & 8x8 Smart Pickup Hub ACTIVE (INR ₹)");
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
                "{\"status\":\"UP\",\"appName\":\"RentSphere P2P Hub\",\"currency\":\"INR (₹)\",\"totalAssets\":%d," +
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
            persistenceEngine.appendWal(assetId, "RESERVE_BLOCKED", asset.getCurrentState().getStateName(),
                    asset.getCurrentState().getStateName(), renterId, 
                    "Blocked at Gate " + verdict.getGateIndex() + " (" + verdict.getGateName() + "): " + verdict.getDiagnostic());

            String err = String.format("{\"success\":false,\"gateIndex\":%d,\"gateName\":\"%s\",\"message\":\"%s\"}",
                    verdict.getGateIndex(), verdict.getGateName(), escapeJson(verdict.getDiagnostic()));
            sendJsonResponse(exchange, 400, err);
            return;
        }

        double tariff = asset.calculateDailyRentalTariff(days);
        double collateral = asset.calculateRequiredCollateral();

        boolean locked = escrowVault.lockEscrow(renterId, asset.getOwnerId(), assetId, tariff, collateral);
        if (!locked) {
            sendJsonResponse(exchange, 500, "{\"error\":\"Escrow lock failed unexpectedly.\"}");
            return;
        }

        String prevState = asset.getCurrentState().getStateName();
        asset.getCurrentState().reserve(asset, renterId);

        LockerCompartment comp = lockerGrid.getCompartment(asset.getLockerCoordinate());
        String otp = "000000";
        if (comp != null) {
            comp.setStatus("RESERVED_LOCKED");
            otp = comp.generateNewOtp();
        }

        asset.addServiceLog("Reserved for " + days + " days by " + renterId + " (Escrow: ₹" + (tariff + collateral) + ")");

        persistenceEngine.saveAssets(assetRepo);
        persistenceEngine.saveLockers(lockerGrid);
        persistenceEngine.saveEscrow(escrowVault);
        persistenceEngine.appendWal(assetId, "RESERVE", prevState, asset.getCurrentState().getStateName(), renterId,
                String.format("Escrow locked ₹%.2f. OTP %s generated for bay %s", (tariff + collateral), otp, asset.getLockerCoordinate()));

        String res = String.format(Locale.US,
                "{\"success\":true,\"state\":\"%s\",\"assetId\":\"%s\",\"otpPin\":\"%s\",\"lockerBay\":\"%s\"," +
                "\"tariff\":%.2f,\"collateral\":%.2f,\"message\":\"Reservation successful! Escrow locked. Present OTP at Hub Bay to unlock.\"}",
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
            sendJsonResponse(exchange, 400, "{\"success\":false,\"message\":\"Invalid 6-Digit Pickup OTP passcode for Bay " + asset.getLockerCoordinate() + "\"}");
            return;
        }

        String prevState = asset.getCurrentState().getStateName();
        try {
            asset.getCurrentState().pickup(asset);
        } catch (IllegalStateException e) {
            sendJsonResponse(exchange, 400, "{\"success\":false,\"message\":\"" + escapeJson(e.getMessage()) + "\"}");
            return;
        }

        comp.setOccupied(false);
        comp.setStatus("DISPATCHED_ACTIVE");
        comp.setWeightKg(0.0);
        asset.addServiceLog("Asset retrieved from Hub Bay " + comp.getBayId() + " via OTP verification.");

        persistenceEngine.saveAssets(assetRepo);
        persistenceEngine.saveLockers(lockerGrid);
        persistenceEngine.appendWal(assetId, "PICKUP", prevState, asset.getCurrentState().getStateName(), asset.getRenterId(),
                "Unlocked via OTP " + otp + ". Bay " + comp.getBayId() + " vacated for active rental.");

        String res = String.format(
                "{\"success\":true,\"state\":\"%s\",\"assetId\":\"%s\",\"message\":\"Hub compartment unlocked! Equipment retrieved successfully.\"}",
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

        LockerCompartment comp = lockerGrid.getCompartment(asset.getLockerCoordinate());
        if (comp != null) {
            comp.setOccupied(true);
            comp.setStatus("QUARANTINE_LOCKED");
            comp.setWeightKg(8.5);
        }

        asset.addServiceLog("Returned to Hub Bay " + asset.getLockerCoordinate() + ". MANDATORY INSPECTION: Deposit held.");

        persistenceEngine.saveAssets(assetRepo);
        persistenceEngine.saveLockers(lockerGrid);
        persistenceEngine.appendWal(assetId, "RETURN", prevState, asset.getCurrentState().getStateName(), asset.getRenterId(),
                "MANDATORY QUARANTINE: Item returned to Bay " + asset.getLockerCoordinate() + ". Security deposit held pending inspection.");

        String res = String.format(
                "{\"success\":true,\"state\":\"%s\",\"assetId\":\"%s\",\"message\":\"Item returned to Hub Bay. Mandatory Inspection active: Deposit held until host checks condition.\"}",
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

        escrowVault.releasePayoutClean(renterId, asset.getOwnerId(), assetId, tariff, collateral);

        LockerCompartment comp = lockerGrid.getCompartment(asset.getLockerCoordinate());
        if (comp != null) {
            comp.setStatus("ONLINE_READY");
        }

        asset.addServiceLog("Clean Inspection Approved by " + hostId + ". 100% deposit refunded. Restored to AVAILABLE.");

        persistenceEngine.saveAssets(assetRepo);
        persistenceEngine.saveLockers(lockerGrid);
        persistenceEngine.saveEscrow(escrowVault);
        persistenceEngine.appendWal(assetId, "INSPECT_APPROVE", prevState, asset.getCurrentState().getStateName(), hostId,
                String.format("Clean return approved. Slashed ₹0. Refunded ₹%.2f deposit to %s. Paid ₹%.2f to host.",
                        collateral, renterId, tariff));

        String res = String.format(Locale.US,
                "{\"success\":true,\"state\":\"%s\",\"message\":\"Clean Return Approved! Full deposit (₹%.2f) refunded to renter. ₹%.2f rental fee transferred to host.\"}",
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

        escrowVault.slashCollateral(renterId, asset.getOwnerId(), assetId, tariff, collateral, slashingPenalty);

        LockerCompartment comp = lockerGrid.getCompartment(asset.getLockerCoordinate());
        if (comp != null) {
            comp.setStatus("ONLINE_READY");
        }

        double netRefund = Math.max(0.0, collateral - slashingPenalty);
        asset.addServiceLog(String.format(Locale.US, "Damage Slashed: %.1f%% penalty (₹%.2f) deducted from deposit. Restored to AVAILABLE.", damagePct, slashingPenalty));

        persistenceEngine.saveAssets(assetRepo);
        persistenceEngine.saveLockers(lockerGrid);
        persistenceEngine.saveEscrow(escrowVault);
        persistenceEngine.appendWal(assetId, "INSPECT_SLASH", prevState, asset.getCurrentState().getStateName(), hostId,
                String.format(Locale.US, "Damage flagged (%.1f%%). Slashed ₹%.2f to host. Refunded ₹%.2f net deposit to %s.",
                        damagePct, slashingPenalty, netRefund, renterId));

        String res = String.format(Locale.US,
                "{\"success\":true,\"state\":\"%s\",\"slashedPenalty\":%.2f,\"refundedCollateral\":%.2f," +
                "\"message\":\"Damage Slashing Executed: ₹%.2f deducted from deposit for repairs. ₹%.2f net refund returned to renter.\"}",
                asset.getCurrentState().getStateName(), slashingPenalty, netRefund, slashingPenalty, netRefund);
        sendJsonResponse(exchange, 200, res);
    }

    private void handleOnboard(HttpExchange exchange) throws IOException {
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
        // Prototype Deep Clone: Total heap isolation of serviceLogs!
        RentableAsset clone = source.deepClone();
        String newId = "AST-" + (100 + assetRepo.count() + 1);

        // Find available bay if not specified
        if (targetBay.isBlank()) {
            targetBay = lockerGrid.getAllCompartments().stream()
                    .filter(c -> !c.isOccupied() && lockerGrid.validateGroundSlotPlacement(clone, c.getBayId()))
                    .map(LockerCompartment::getBayId)
                    .findFirst().orElse("A1");
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
    // STATIC FILE HANDLER
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
    // JSON & HTTP UTILITIES
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
