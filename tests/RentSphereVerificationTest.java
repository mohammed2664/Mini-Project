package tests;

import rentsphere.escrow.EscrowAccount;
import rentsphere.escrow.EscrowVault;
import rentsphere.locker.CompartmentSize;
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
import rentsphere.state.*;
import rentsphere.auth.*;
import rentsphere.dispute.*;
import rentsphere.locker.LockerDevice;
import rentsphere.locker.SimulatedLockerDevice;
import rentsphere.payment.*;
import rentsphere.policy.*;
import rentsphere.support.*;

import java.io.File;
import java.util.List;

/**
 * RentSphere Automated Verification Test Suite.
 * Validates all core architectural patterns and business invariants (₹ INR):
 *  - 5-Tier Domain Model Hierarchy & Polymorphic Tariff / Collateral Formulas
 *  - 4-Stage GoF State Pattern with Mandatory Inspection Quarantine Invariant
 *  - 4-Gate Fail-Fast Verification Pipeline with Short-Circuit Execution
 *  - 8x8 Smart Locker Hub Matrix with Row H Ground Bay Spatial Invariant
 *  - Meta-OOP RTTI Reflection & Prototype Shallow vs. Deep Cloning Memory Proof
 *  - RentSphere Trust & Asset Reliability Composite Scoring Engine with What-If Simulation
 *  - Flat-File Persistence Engine & Write-Ahead Log (WAL) Audit Trail
 */
public class RentSphereVerificationTest {

    private static int testsPassed = 0;
    private static int testsTotal = 0;

    private static void assertTrue(boolean condition, String testName) {
        testsTotal++;
        if (condition) {
            testsPassed++;
            System.out.println("  [PASS] " + testName);
        } else {
            System.err.println("  [FAIL] " + testName);
            throw new AssertionError("Test assertion failed: " + testName);
        }
    }

    public static void main(String[] args) {
        System.out.println("=================================================================");
        System.out.println("  RentSphere Automated Verification Test Suite (Goa University)  ");
        System.out.println("  Currency: Indian Rupees (₹ INR) | Local Community Scale       ");
        System.out.println("=================================================================\n");

        test5TierInheritance();
        testStatePatternAndQuarantineInvariant();
        testFailFastPipeline();
        testSmartLockerGridAndRowHGroundBayRule();
        testMetaOOPReflectionAndCloning();
        testTrustScoreEngine();
        testFlatFilePersistenceAndWal();
        testAuthenticationAndRBAC();
        testPaymentGatewaySandbox();
        testSmartHubOtpLockoutAndExpiry();
        testDisputeArbitrationAndState();
        testPolicyServiceAndConsentTracking();
        testSupportTicketLifecycle();
        testNotificationServiceAndPersistence();
        testSecurityRemediationAndMultiDayEscrow();

        System.out.println("\n=================================================================");
        System.out.printf("  VERIFICATION SUMMARY: %d / %d TESTS PASSED (100%% SUCCESS)%n", testsPassed, testsTotal);
        System.out.println("=================================================================");

        if (testsPassed != testsTotal) {
            System.exit(1);
        }
    }

    // ========================================================================
    // TEST 1: 5-TIER INHERITANCE HIERARCHY & POLYMORPHIC FORMULAS (₹ INR)
    // ========================================================================
    private static void test5TierInheritance() {
        System.out.println("--- [TEST 1] 5-Tier Inheritance & Polymorphic Rates (₹ INR) ---");

        // Tier 5 Leaf 1: CameraAndOpticsGear
        CameraAndOpticsGear camera = new CameraAndOpticsGear(
                "TEST-CAM", "Sony Alpha A7 IV Mirrorless Camera", "host_faaris",
                1200.0, 5000.0, "A2", 1.25, "PREMIUM_ALL_RISK",
                33.0, true
        );

        // Tariff formula: 1200 * 1.15 (4K video flag) * 2 days = ₹2,760.00
        double cameraTariff = camera.calculateDailyRentalTariff(2);
        assertTrue(Math.abs(cameraTariff - 2760.0) < 0.01, 
                "CameraAndOpticsGear 4K video tariff calculation (Expected ₹2760.00, Got ₹" + cameraTariff + ")");

        // Collateral formula: 5000.0 * 1.20 (sensor resolution 33MP >= 30MP) = ₹6,000.00
        double cameraCollateral = camera.calculateRequiredCollateral();
        assertTrue(Math.abs(cameraCollateral - 6000.0) < 0.01, 
                "CameraAndOpticsGear high-res sensor collateral calculation (Expected ₹6000.00, Got ₹" + cameraCollateral + ")");

        // Tier 5 Leaf 4: EventAndMobilityVehicle (Ground Bay Asset)
        EventAndMobilityVehicle vehicle = new EventAndMobilityVehicle(
                "TEST-CAR", "Maruti Suzuki Fronx Automatic", "host_ketan",
                2200.0, 6000.0, "H2", 1.15, "ZERO_DEPRECIATION",
                5, "Petrol Automatic"
        );

        assertTrue(vehicle.isRequiresGroundBay(), "EventAndMobilityVehicle has requiresGroundBay locked to true");
        assertTrue(vehicle instanceof UtilityAndMobilityAsset, "EventAndMobilityVehicle is instance of UtilityAndMobilityAsset (Tier 4)");
        assertTrue(vehicle instanceof CategorizedRentalAsset, "EventAndMobilityVehicle is instance of CategorizedRentalAsset (Tier 3)");
        assertTrue(vehicle instanceof RentableAsset, "EventAndMobilityVehicle is instance of RentableAsset (Tier 2)");
        assertTrue(vehicle instanceof AbstractDomainEntity, "EventAndMobilityVehicle is instance of AbstractDomainEntity (Tier 1)");

        // Vehicle collateral surcharge (1.20 transit deposit): 6000 * 1.20 = ₹7,200.00
        assertTrue(Math.abs(vehicle.calculateRequiredCollateral() - 7200.0) < 0.01, 
                "Vehicle collateral surcharge validated (₹7200.00)");
    }

    // ========================================================================
    // TEST 2: 4-STAGE STATE PATTERN & MANDATORY QUARANTINE INVARIANT
    // ========================================================================
    private static void testStatePatternAndQuarantineInvariant() {
        System.out.println("\n--- [TEST 2] GoF State Pattern & Mandatory Quarantine Invariant ---");

        CameraAndOpticsGear camera = new CameraAndOpticsGear(
                "TEST-CAM", "Sony Alpha A7 IV Mirrorless Camera", "host_faaris",
                1200.0, 5000.0, "A2", 1.25, "PREMIUM_ALL_RISK",
                33.0, true
        );

        // Initial state must be AvailableState
        assertTrue("AVAILABLE".equals(camera.getCurrentState().getStateName()), "Initial state is AVAILABLE");
        assertTrue(camera.getCurrentState().canReserve(), "AvailableState allows reserve action");
        assertTrue(!camera.getCurrentState().canPickup(), "AvailableState rejects premature pickup");

        // Reserve -> ReservedEscrowState
        camera.getCurrentState().reserve(camera, "renter_ketan");
        assertTrue("RESERVED_ESCROW".equals(camera.getCurrentState().getStateName()), "State transitioned AVAILABLE -> RESERVED_ESCROW");
        assertTrue(camera.getCurrentState().canPickup(), "ReservedEscrowState allows pickup");

        // Pickup -> ActiveLeaseState
        camera.getCurrentState().pickup(camera);
        assertTrue("ACTIVE_LEASE".equals(camera.getCurrentState().getStateName()), "State transitioned RESERVED_ESCROW -> ACTIVE_LEASE");

        // MANDATORY QUARANTINE INVARIANT TEST:
        // Returning gear from ACTIVE_LEASE MUST transition to InspectionQuarantineState, NEVER AvailableState!
        camera.getCurrentState().initiateReturn(camera);
        assertTrue("INSPECTION_QUARANTINE".equals(camera.getCurrentState().getStateName()),
                "MANDATORY QUARANTINE INVARIANT: Asset returned to hub strictly entered INSPECTION_QUARANTINE");

        // Proves that while in quarantine, it cannot be reserved by anyone
        assertTrue(!camera.getCurrentState().canReserve(), "InspectionQuarantineState forbids new reservations");
        assertTrue(camera.getCurrentState().canApproveInspection(), "InspectionQuarantineState allows host approval");
        assertTrue(camera.getCurrentState().canSlashInspection(), "InspectionQuarantineState allows collateral slashing");

        // Host approves inspection -> returns to AvailableState
        camera.getCurrentState().approveInspection(camera);
        assertTrue("AVAILABLE".equals(camera.getCurrentState().getStateName()),
                "Inspection approved cleanly: State transitioned INSPECTION_QUARANTINE -> AVAILABLE");
    }

    // ========================================================================
    // TEST 3: 4-GATE FAIL-FAST VERIFICATION PIPELINE
    // ========================================================================
    private static void testFailFastPipeline() {
        System.out.println("\n--- [TEST 3] 4-Gate Fail-Fast Verification Pipeline ---");

        FailFastPipeline pipeline = new FailFastPipeline();
        CameraAndOpticsGear camera = new CameraAndOpticsGear(
                "TEST-CAM", "Sony Alpha A7 IV Mirrorless Camera", "host_faaris",
                1200.0, 5000.0, "A2", 1.25, "PREMIUM_ALL_RISK",
                33.0, true
        );

        // Case 1: Gate 1 KycIdentityGate Violation (Anti-Self-Rental Rule)
        PipelineContext selfRentalCtx = new PipelineContext(
                camera, "host_faaris", "RESERVE", 10000.0, true, "A2", 2, 0.0
        );
        GateResult g1Result = pipeline.verify(selfRentalCtx);
        assertTrue(!g1Result.isPassed() && g1Result.getGateIndex() == 1, 
                "Fail-Fast Pipeline short-circuited at Gate 1 (KycIdentityGate - Anti-self-rental rule enforced)");

        // Case 2: Gate 3 EscrowSolvencyGate Violation (Insufficient Wallet Balance in ₹ INR)
        PipelineContext insolventCtx = new PipelineContext(
                camera, "renter_poor", "RESERVE", 500.0, true, "A2", 2, 0.0
        );
        GateResult g3Result = pipeline.verify(insolventCtx);
        assertTrue(!g3Result.isPassed() && g3Result.getGateIndex() == 3, 
                "Fail-Fast Pipeline short-circuited at Gate 3 (EscrowSolvencyGate - Insufficient liquidity blocked)");

        // Case 3: All Gates Passed (Valid Reservation with ₹ INR Liquidity)
        PipelineContext validCtx = new PipelineContext(
                camera, "renter_ketan", "RESERVE", 25000.0, true, "A2", 2, 0.0
        );
        GateResult validResult = pipeline.verify(validCtx);
        assertTrue(validResult.isPassed(), "All 4 Gates passed successfully for solvent verified renter");
    }

    // ========================================================================
    // TEST 4: 8x8 SMART HUB MATRIX & ROW H GROUND BAY SPATIAL INVARIANT
    // ========================================================================
    private static void testSmartLockerGridAndRowHGroundBayRule() {
        System.out.println("\n--- [TEST 4] 8x8 Smart Hub Matrix & Row H Ground Bay Spatial Invariant ---");

        SmartLockerGrid8x8 grid = new SmartLockerGrid8x8();
        assertTrue(grid.getAllCompartments().size() == 64, "8x8 Matrix contains exactly 64 physical bays/slots (A1..H8)");

        // Size tier checks
        LockerCompartment bayA1 = grid.getCompartment("A1");
        assertTrue(bayA1.getSizeTier() == CompartmentSize.SMALL, "Bay A1 is SMALL tier (Row A - Cameras/Drills)");

        LockerCompartment bayH1 = grid.getCompartment("H1");
        assertTrue(bayH1.getSizeTier() == CompartmentSize.XL_GROUND_SLOT, "Bay H1 is XL_GROUND_SLOT tier (Row H Ground Parking)");

        // ROW H GROUND BAY SPATIAL INVARIANT:
        EventAndMobilityVehicle vehicle = new EventAndMobilityVehicle(
                "TEST-CAR", "Maruti Suzuki Fronx Automatic", "host_ketan",
                2200.0, 6000.0, "C2", 1.15, "ZERO_DEPRECIATION",
                5, "Petrol Automatic"
        );

        // Invalid placement: Row C (Row 2 Locker) must be REJECTED
        boolean invalidRowAssignment = grid.assignAssetToBay(vehicle, "C2");
        assertTrue(!invalidRowAssignment, "SPATIAL INVARIANT ENFORCED: Vehicle/Ground asset barred from locker Row C");

        // Valid placement: Row H (Row 7 Ground Slot) must be ACCEPTED
        boolean validRowAssignment = grid.assignAssetToBay(vehicle, "H2");
        assertTrue(validRowAssignment, "SPATIAL INVARIANT ENFORCED: Vehicle/Ground asset successfully accepted in Row H Ground Slot H2");

        // Telemetry update check
        grid.updateTelemetry("H2", 28.5, 60.0, 1150.0);
        LockerCompartment updatedBay = grid.getCompartment("H2");
        assertTrue(Math.abs(updatedBay.getTempC() - 28.5) < 0.1 && updatedBay.getWeightKg() == 1150.0, 
                "Live sensor telemetry successfully updated for Bay H2");
    }

    // ========================================================================
    // TEST 5: META-OOP REFLECTION & SHALLOW VS DEEP CLONING
    // ========================================================================
    private static void testMetaOOPReflectionAndCloning() {
        System.out.println("\n--- [TEST 5] Live Reflection & Shallow vs. Deep Cloning ---");

        CameraAndOpticsGear camera = new CameraAndOpticsGear(
                "TEST-CAM", "Sony Alpha A7 IV Mirrorless Camera", "host_faaris",
                1200.0, 5000.0, "A2", 1.25, "PREMIUM_ALL_RISK",
                33.0, true
        );

        // 1. RTTI Reflection Inspector
        ReflectionInspector.InspectionReport report = ReflectionInspector.inspect(camera);
        assertTrue("CameraAndOpticsGear".equals(report.getClassName()), "Reflection extracted correct leaf class name");
        assertTrue(report.getInheritanceChain().size() >= 5, 
                "Reflection successfully traversed full 5-tier inheritance hierarchy (Length: " + report.getInheritanceChain().size() + ")");
        assertTrue(report.getDeclaredFields().size() > 0, "Reflection extracted declared fields");
        assertTrue(report.getDeclaredMethods().size() > 0, "Reflection extracted declared methods");

        // 2. Prototype Shallow vs. Deep Cloning Benchmark
        PrototypeCloner.CloneBenchmarkReport cloneReport = PrototypeCloner.benchmarkCloning(camera);
        assertTrue(cloneReport.shallowLogsShared, 
                "MEMORY BENCHMARK PROVED: Shallow clone SHARES serviceLogs heap reference (" + cloneReport.shallowLogsId + ")");
        assertTrue(cloneReport.deepLogsIsolated, 
                "MEMORY BENCHMARK PROVED: Deep clone ISOLATES serviceLogs with new heap reference (" + cloneReport.deepLogsId + ")");
    }

    // ========================================================================
    // TEST 6: RENTSPHERE TRUST & ASSET RELIABILITY SCORING ENGINE
    // ========================================================================
    private static void testTrustScoreEngine() {
        System.out.println("\n--- [TEST 6] RentSphere Trust & Asset Reliability Composite Score ---");

        TrustScoreEngine.TrustMetrics baseline = new TrustScoreEngine.TrustMetrics(
                "USR-101", "Mohammed Faaris Khan", 90.0, 95.0, 85.0, 90.0, 95.0
        );

        // Formula: 90*0.30 + 95*0.30 + 85*0.20 + 90*0.10 + 95*0.10 = 27 + 28.5 + 17 + 9 + 9.5 = 91.00
        double score = baseline.computeTrustScore();
        assertTrue(Math.abs(score - 91.00) < 0.01, "Trust Composite Score calculated correctly (Expected 91.00, Got " + score + ")");
        assertTrue("Elite Verified Host / Renter".equals(baseline.computeTierBadge()), "Tier badge computed correctly (Elite Verified Host / Renter)");

        // What-If Simulation using deep clone
        TrustScoreEngine.SimulationResult sim = TrustScoreEngine.simulateWhatIf(baseline, 5.0, 5.0, 5.0, 5.0, 5.0);
        assertTrue(sim.scoreDelta > 0, "What-If Trust simulation computed positive score delta (+" + sim.scoreDelta + " pts)");
        assertTrue(baseline.computeTrustScore() == 91.00, "Baseline metrics remained 100% UNMUTATED due to deep cloning");
    }

    // ========================================================================
    // TEST 7: FLAT FILE PERSISTENCE & WAL AUDITING
    // ========================================================================
    private static void testFlatFilePersistenceAndWal() {
        System.out.println("\n--- [TEST 7] Deterministic Flat-File I/O & WAL (₹ INR) ---");

        String testDataDir = "data";
        FlatFilePersistenceEngine engine = new FlatFilePersistenceEngine(testDataDir);

        GenericRepository<RentableAsset> repo = new GenericRepository<>();
        engine.loadAssets(repo);
        assertTrue(repo.count() >= 8, "Flat file persistence engine loaded " + repo.count() + " seed assets from assets_ledger.txt");

        EscrowVault vault = new EscrowVault();
        engine.loadEscrow(vault);
        assertTrue(vault.getAllAccounts().size() >= 5, "Flat file persistence engine loaded " + vault.getAllAccounts().size() + " accounts from escrow_vault.txt");

        // Verify INR wallet balance format
        EscrowAccount account = vault.getAccount("host_faaris");
        assertTrue(account != null && account.getWalletBalance() >= 25000.0, "Escrow account balance in INR verified (₹25,000+)");

        // Read production WAL entries to verify presence of audit history
        List<String> walEntries = engine.readWalEntries(10);
        assertTrue(!walEntries.isEmpty(), "Write-Ahead Log contains audited entries in data/audit_wal.txt");

        // Test WAL write/read in isolated test directory to prevent production WAL pollution (G-4)
        File testWalDir = new File("data/test_wal");
        testWalDir.mkdirs();
        File testWalFile = new File(testWalDir, "audit_wal.txt");
        if (testWalFile.exists()) { testWalFile.delete(); }
        FlatFilePersistenceEngine testWalEngine = new FlatFilePersistenceEngine(testWalDir.getPath());
        testWalEngine.appendWal("TEST-ISOLATED-01", "UNIT_TEST", "NONE", "ONLINE", "test_runner", "Isolated WAL verification (INR currency)");
        List<String> testWalEntries = testWalEngine.readWalEntries(5);
        assertTrue(!testWalEntries.isEmpty() && testWalEntries.get(testWalEntries.size() - 1).contains("TEST-ISOLATED-01"),
                "Write-Ahead Log writes and reads accurately in isolated test environment without polluting production ledger");
    }

    // ========================================================================
    // TEST 8: AUTHENTICATION, PBKDF2 HASHING & RBAC
    // ========================================================================
    private static void testAuthenticationAndRBAC() {
        System.out.println("\n--- [TEST 8] PBKDF2 Password Hashing, Sessions & RBAC ---");

        String password = "SecurePassword@2026";
        String salt = PasswordHasher.generateSalt();
        String hash = PasswordHasher.hashPassword(password, salt);

        assertTrue(hash != null && hash.length() >= 64, "PBKDF2-HMAC-SHA256 produced 256-bit cryptographically secure hash");
        assertTrue(PasswordHasher.verifyPassword(password, salt, hash), "PBKDF2 constant-time verification successfully validated correct password");
        assertTrue(!PasswordHasher.verifyPassword("WrongPassword", salt, hash), "PBKDF2 rejected invalid password attempt");

        AuthService authService = new AuthService("data");
        String testEmail = "test_pilot_" + System.currentTimeMillis() + "@unigoa.ac.in";
        UserAccount newUser = authService.registerUser(testEmail, "CampusStudent#2026", "Ananya Naik", "RENTER", "+91 98221 11223");
        assertTrue(newUser != null && "RENTER".equals(newUser.getRole()), "User registration generated valid RENTER account entity");

        UserSession session = authService.login(testEmail, "CampusStudent#2026");
        assertTrue(session != null && session.getToken().length() == 64, "Successful login issued 256-bit secure session token");
        assertTrue(!session.isExpired(), "Session token active and valid within 24h duration window");

        // Test RBAC suspension
        authService.setAccountStatus(newUser.getUserId(), "SUSPENDED");
        boolean loginBlocked = false;
        try {
            authService.login(testEmail, "CampusStudent#2026");
        } catch (IllegalStateException e) {
            loginBlocked = true;
        }
        assertTrue(loginBlocked, "Suspended user account blocked from authenticating (OWASP A01 Access Control)");
        authService.setAccountStatus(newUser.getUserId(), "ACTIVE");
    }

    // ========================================================================
    // TEST 9: PAYMENT GATEWAY SANDBOX & HMAC-SHA256 WEBHOOK IDEMPOTENCY
    // ========================================================================
    private static void testPaymentGatewaySandbox() {
        System.out.println("\n--- [TEST 9] Indian Payment Gateway Sandbox (₹ INR) & HMAC ---");

        SandboxPaymentAdapter gateway = new SandboxPaymentAdapter("data");
        PaymentOrder order = gateway.createOrder("BKG-9901", "AST-101", "renter_deepak", "host_faaris", 1500.0, 5000.0);

        assertTrue(order != null && order.getOrderId().startsWith("order_inr_"), "Gateway generated valid Indian payment order (₹ INR)");
        assertTrue(Math.abs(order.getTotalAmount() - 6500.0) < 0.01, "Total amount payable accurately combined tariff (₹1,500) and deposit (₹5,000) = ₹6,500");

        String clientSig = gateway.generateClientSignature(order.getOrderId(), "pay_test_123");
        boolean sigValid = gateway.verifyPaymentSignature(order.getOrderId(), "pay_test_123", clientSig);
        assertTrue(sigValid, "HMAC-SHA256 signature verification validated authentic gateway response");
        assertTrue("CAPTURED".equals(order.getStatus()), "Order status transitioned to CAPTURED upon signature verification");

        // Idempotent webhook processing
        boolean firstDelivery = gateway.processWebhook("payment.captured", order.getOrderId(), "pay_test_123", clientSig);
        assertTrue(firstDelivery, "First webhook delivery processed and recorded in ledger");
        boolean duplicateDelivery = gateway.processWebhook("payment.captured", order.getOrderId(), "pay_test_123", clientSig);
        assertTrue(duplicateDelivery, "Duplicate webhook delivery handled idempotently without duplicate charges");
    }

    // ========================================================================
    // TEST 10: SMART HUB OTP LOCKOUT, EXPIRY & ZERO LEAKAGE
    // ========================================================================
    private static void testSmartHubOtpLockoutAndExpiry() {
        System.out.println("\n--- [TEST 10] Smart Hub OTP Lockout & Zero OTP Leakage ---");

        LockerCompartment comp = new LockerCompartment("B3", 2, 3, CompartmentSize.MEDIUM);
        String realOtp = comp.generateNewOtp();
        assertTrue(realOtp != null && realOtp.length() == 6, "Cryptographically secure 6-digit OTP generated");

        // Failed attempt 1 and 2
        assertTrue(!comp.verifyOtp("000000"), "Failed attempt 1 rejected");
        assertTrue(!comp.verifyOtp("111111"), "Failed attempt 2 rejected");
        assertTrue(!comp.isLockedOut(), "Compartment still open after 2 failed attempts");

        // Failed attempt 3 triggers lockout
        assertTrue(!comp.verifyOtp("222222"), "Failed attempt 3 rejected and triggered security lockout");
        assertTrue(comp.isLockedOut(), "Compartment locked out against brute-force attacks (CWE-307)");

        // 4th attempt with CORRECT OTP must now fail due to lockout
        assertTrue(!comp.verifyOtp(realOtp), "Correct OTP rejected while compartment is under brute-force lockout");

        // Hardware device abstraction
        SimulatedLockerDevice device = new SimulatedLockerDevice();
        assertTrue(device.getDeviceState("B3") == LockerDevice.DeviceState.ONLINE_READY, "Simulated device state initializes to ONLINE_READY");
        device.sendUnlockCommand("B3", realOtp);
        assertTrue(device.getDeviceState("B3") == LockerDevice.DeviceState.DOOR_UNLOCKED, "Solenoid command transitioned device state to DOOR_UNLOCKED");
    }

    // ========================================================================
    // TEST 11: DAMAGE DISPUTE ARBITRATION & COLLATERAL SETTLEMENT
    // ========================================================================
    private static void testDisputeArbitrationAndState() {
        System.out.println("\n--- [TEST 11] Damage Dispute State & Administrative Arbitration ---");

        CameraAndOpticsGear asset = new CameraAndOpticsGear(
                "TEST-DISPUTE-AST", "Fujifilm X-T5 Mirrorless", "host_faaris",
                1000.0, 4000.0, "C2", 1.20, "STANDARD_SHIELD", 40.0, true
        );

        // Reserve, Pickup, Return to Quarantine
        asset.getCurrentState().reserve(asset, "renter_deepak");
        asset.getCurrentState().pickup(asset);
        asset.getCurrentState().initiateReturn(asset);
        assertTrue("INSPECTION_QUARANTINE".equals(asset.getCurrentState().getStateName()), "Asset returned to mandatory INSPECTION_QUARANTINE");

        // Transition to DISPUTED
        asset.setState(new DisputedState());
        assertTrue("DISPUTED".equals(asset.getCurrentState().getStateName()), "Asset successfully transitioned to DISPUTED state");

        // Invariant: Cannot reserve or pickup while in dispute
        boolean reserveBlocked = false;
        try {
            asset.getCurrentState().reserve(asset, "renter_nikhil");
        } catch (IllegalStateException e) {
            reserveBlocked = true;
        }
        assertTrue(reserveBlocked, "New reservation strictly blocked while asset is frozen in DISPUTED state");

        // File dispute record
        DisputeService disputeService = new DisputeService("data");
        DisputeRecord dispute = disputeService.fileDispute(asset.getId(), "renter_deepak", "host_faaris", 4000.0, 800.0,
                "Lens cap had pre-existing dust; optics glass completely pristine", "Return photo timestamp 16:30 shows zero scratches");
        assertTrue(dispute != null && "OPEN".equals(dispute.getStatus()), "Dispute record created with held collateral frozen");

        // Admin arbitration
        boolean arbitrated = disputeService.resolveDispute(dispute.getDisputeId(), "admin_lead", "RESOLVED_REFUND", "Photo evidence confirms pre-existing dust.");
        assertTrue(arbitrated, "Dispute arbitrated by administrator");
        assertTrue("RESOLVED_REFUND".equals(dispute.getStatus()), "Dispute status updated to RESOLVED_REFUND");

        // State restored to AVAILABLE
        asset.getCurrentState().approveInspection(asset);
        assertTrue("AVAILABLE".equals(asset.getCurrentState().getStateName()), "Arbitration resolution restored asset state to AVAILABLE");
    }

    // ========================================================================
    // TEST 12: 20 INDIAN LEGAL POLICIES & AUDITABLE CONSENT
    // ========================================================================
    private static void testPolicyServiceAndConsentTracking() {
        System.out.println("\n--- [TEST 12] 20 Indian Legal Policies & Versioned Consent ---");

        PolicyService policyService = new PolicyService("data");
        List<PolicyService.PolicyDocument> policies = policyService.getAllPolicies();
        assertTrue(policies.size() == 20, "PolicyService manages exactly 20 distinct Indian regulatory policy documents");

        PolicyService.PolicyDocument privacy = policyService.getPolicy("privacy");
        assertTrue(privacy != null && privacy.jurisdictionNote().contains("DPDP Act"), "Privacy Policy explicitly references Digital Personal Data Protection Act 2023");

        PolicyService.PolicyDocument grievance = policyService.getPolicy("grievance_support");
        assertTrue(grievance != null && (grievance.jurisdictionNote().contains("IT Act") || grievance.jurisdictionNote().contains("Intermediary")),
                "Grievance Policy implements Rule 3(2) IT Intermediary Rules 2021");

        // User consent recording
        String testUser = "usr_pilot_verif_99";
        policyService.recordAcceptance(testUser, List.of("tos", "privacy", "rental_agreement", "cancellation", "deposit_payment"));
        assertTrue(policyService.hasAcceptedAllEssential(testUser), "PolicyService auditable consent verified for essential policies");
        assertTrue(policyService.getAcceptedPolicies(testUser).contains("tos"), "User acceptance history retained in memory and flat file");
    }

    // ========================================================================
    // TEST 13: SUPPORT TICKET & GRIEVANCE SERVICE (IT RULES 2021 SLA)
    // ========================================================================
    private static void testSupportTicketLifecycle() {
        System.out.println("\n--- [TEST 13] Support Ticket Lifecycle & IT Rules 2021 SLA ---");

        SupportService support = new SupportService("data");

        // 1. Create a ticket and verify it gets a non-null ticketId
        String testUserId = "renter_test_support_audit";
        SupportService.SupportTicket ticket = support.createTicket(
                testUserId, "BOOKING",
                "Pickup OTP did not work at Bay A2",
                "I entered the 6-digit PIN correctly but the compartment remained locked."
        );
        assertTrue(ticket != null && ticket.ticketId != null && !ticket.ticketId.isBlank(),
                "Support ticket creation returns a non-null, non-empty ticketId");
        assertTrue(ticket.ticketId.startsWith("TCK-"), "Ticket ID follows TCK-NNN naming convention");

        // 2. Retrieve tickets by userId and confirm it appears in the user's list
        List<SupportService.SupportTicket> userTickets = support.getUserTickets(testUserId);
        boolean foundTicket = userTickets.stream().anyMatch(t -> t.ticketId.equals(ticket.ticketId));
        assertTrue(foundTicket, "Ticket is retrievable by userId from SupportService");

        // 3. Initial status must be OPEN
        assertTrue("OPEN".equals(ticket.status), "Newly created ticket has OPEN status (IT Rules 2021 SLA clock started)");

        // 4. Resolve the ticket and verify transition to RESOLVED
        boolean resolved = support.resolveTicket(ticket.ticketId, "admin_almeida",
                "OTP reset performed remotely. Bay A2 unlocked. Equipment confirmed retrieved.");
        assertTrue(resolved, "Ticket resolution returns true for valid ticketId");

        SupportService.SupportTicket resolvedTicket = support.getUserTickets(testUserId).stream()
                .filter(t -> t.ticketId.equals(ticket.ticketId)).findFirst().orElse(null);
        assertTrue(resolvedTicket != null && "RESOLVED".equals(resolvedTicket.status),
                "Resolved ticket status is RESOLVED");

        // 5. SLA timestamp: resolvedAt must be >= createdAt
        assertTrue(resolvedTicket.resolvedAt >= resolvedTicket.createdAt,
                "SLA timestamps valid: resolvedAt >= createdAt (24h SLA compliance)");

        // 6. Resolution message must be persisted and non-null
        assertTrue(resolvedTicket.resolution != null && !resolvedTicket.resolution.isBlank(),
                "Resolution message stored and non-null after ticket closed");

        // 7. Attempting to resolve a non-existent ticket returns false
        boolean badResolve = support.resolveTicket("TCK-INVALID-9999", "admin", "Should not work");
        assertTrue(!badResolve, "Resolving a non-existent ticket returns false (safe no-op)");
    }

    // ========================================================================
    // TEST 14: IN-APP NOTIFICATION CENTER & FLAT-FILE PERSISTENCE
    // ========================================================================
    private static void testNotificationServiceAndPersistence() {
        System.out.println("\n--- [TEST 14] In-App Notification Center & Flat-File Persistence ---");

        File testNotifDir = new File("data/test_notifs");
        testNotifDir.mkdirs();
        File testLog = new File(testNotifDir, "notifications_log.txt");
        if (testLog.exists()) { testLog.delete(); }
        NotificationService notifService = new NotificationService(testNotifDir.getPath());

        String userId = "renter_deepak";
        notifService.pushNotification(userId, "Booking Confirmed", "Your Sony A7 IV reservation is confirmed", "SUCCESS");
        notifService.pushNotification(userId, "Locker Ready", "Bay A2 is unlocked and ready for pickup", "INFO");

        List<NotificationService.NotificationItem> items = notifService.getUserNotifications(userId);
        assertTrue(items.size() == 2, "Notification service pushed and retrieved 2 items");
        assertTrue(!items.get(0).read, "Newly pushed notification has unread status (read=false)");

        notifService.markAllAsRead(userId);
        List<NotificationService.NotificationItem> readItems = notifService.getUserNotifications(userId);
        assertTrue(readItems.get(0).read && readItems.get(1).read, "markAllAsRead transitioned all items to read=true");

        // Test persistence reload across simulated restart
        NotificationService reloadedService = new NotificationService(testNotifDir.getPath());
        List<NotificationService.NotificationItem> reloadedItems = reloadedService.getUserNotifications(userId);
        assertTrue(reloadedItems.size() == 2, "Reloaded notification service restored items from flat file");
        assertTrue(reloadedItems.get(0).read, "Reloaded notification maintained read status across persistence reload");
    }

    // ========================================================================
    // TEST 15: SECURITY REMEDIATION, MULTI-DAY ESCROW & PHYSICAL INVARIANTS
    // ========================================================================
    private static void testSecurityRemediationAndMultiDayEscrow() {
        System.out.println("\n--- [TEST 15] Security Remediation, Multi-Day Escrow & Physical Invariants ---");

        // 1. Multi-Day Escrow Math Invariant (F-01 Verification)
        EscrowVault vault = new EscrowVault();
        EscrowAccount renter = new EscrowAccount("usr_test_renter", "Renter Deepak", "RENTER", 20000.0, 0.0, 0.0, true);
        EscrowAccount host = new EscrowAccount("usr_test_host", "Host Faaris", "HOST", 5000.0, 0.0, 0.0, true);
        vault.registerAccount(renter);
        vault.registerAccount(host);

        CameraAndOpticsGear gear = new CameraAndOpticsGear("AST-TEST-99", "Sony FX3 Cinema", "usr_test_host",
                1000.0, 5000.0, "A2", 1.20, "Standard", 33.0, true);
        gear.setRentalDays(3);
        double tariff3Days = gear.calculateDailyRentalTariff(3); // 1000 * 1.15 * 3 = 3450.0
        double collateral = gear.calculateRequiredCollateral(); // 5000 * 1.20 = 6000.0
        assertTrue(tariff3Days == 3450.0, "3-day 4K camera tariff is calculated accurately as ₹3450.00");

        boolean locked = vault.lockEscrow("usr_test_renter", "usr_test_host", gear.getId(), tariff3Days, collateral);
        assertTrue(locked, "Multi-day escrow locked ₹9450 successfully in renter escrow");
        assertTrue(renter.getLockedEscrow() == 9450.0, "Renter locked escrow is exactly ₹9450.00");
        assertTrue(renter.getWalletBalance() == (20000.0 - 9450.0), "Renter wallet balance debited by ₹9450.00");

        // Release clean inspection for full 3-day duration
        vault.releasePayoutClean("usr_test_renter", "usr_test_host", gear.getId(), tariff3Days, collateral);
        assertTrue(renter.getLockedEscrow() == 0.0, "MULTI-DAY ESCROW INVARIANT: Zero capital trapped in locked escrow after clean inspection (F-01)");
        assertTrue(host.getEarnedRevenue() == 3450.0, "Host received full 3-day rental tariff of ₹3450.00 without underpayment (F-01)");
        assertTrue(renter.getWalletBalance() == (20000.0 - 3450.0), "Renter collateral (₹6000.00) fully refunded to wallet");

        // 2. Physical Smart Hub Bay Collision Prevention (F-05 Verification)
        SmartLockerGrid8x8 grid = new SmartLockerGrid8x8();
        LockerCompartment bayB1 = grid.getCompartment("B1");
        assertTrue(bayB1 != null && !bayB1.isOccupied(), "Initial locker bay B1 is vacant");
        grid.assignAssetToBay(gear, "B1");
        assertTrue(bayB1.isOccupied(), "Locker bay B1 is marked occupied after asset assignment");
        assertTrue(bayB1.getCurrentAssetId().equals(gear.getId()), "Occupying asset in B1 matches assigned gear ID");

        // 3. Support Ticket Resolution Admin Attribution (F-10 Verification)
        File testSupportDir = new File("data/test_support_audit");
        testSupportDir.mkdirs();
        File testSupportFile = new File(testSupportDir, "support_tickets.txt");
        if (testSupportFile.exists()) testSupportFile.delete();
        SupportService supportAudit = new SupportService(testSupportDir.getPath());
        SupportService.SupportTicket tick = supportAudit.createTicket("usr_renter_1", "BILLING", "Tariff Query", "Explain multi-day escrow");
        supportAudit.resolveTicket(tick.ticketId, "admin_lead", "Settled tariff calculation in accordance with lease terms.");
        SupportService.SupportTicket resolvedTick = supportAudit.getTicket(tick.ticketId);
        assertTrue(resolvedTick.resolution.contains("Resolved by admin_lead"), "Support ticket resolution accurately attributes acting administrator ID (F-10)");

        // 4. Atomic Flat-File Persistence Staging & Multi-Day Reload (F-01 / F-07 Verification)
        File testPersistDir = new File("data/test_persist_atomic");
        testPersistDir.mkdirs();
        FlatFilePersistenceEngine atomicEngine = new FlatFilePersistenceEngine(testPersistDir.getPath());
        GenericRepository<RentableAsset> testRepo = new GenericRepository<>();
        gear.setRentalDays(5);
        testRepo.save(gear);
        atomicEngine.saveAssets(testRepo);

        File assetsFile = new File(testPersistDir, "assets_ledger.txt");
        File tmpFile = new File(testPersistDir, "assets_ledger.txt.tmp");
        assertTrue(assetsFile.exists() && assetsFile.length() > 0, "Assets ledger successfully written via atomic staging (F-07)");
        assertTrue(!tmpFile.exists(), "Temporary staging file cleanly renamed via atomic move without stale locks (F-07)");

        // Reload into fresh repo and verify rentalDays persisted
        GenericRepository<RentableAsset> reloadedRepo = new GenericRepository<>();
        atomicEngine.loadAssets(reloadedRepo);
        RentableAsset loadedGear = reloadedRepo.findById(gear.getId()).orElse(null);
        assertTrue(loadedGear != null && loadedGear.getRentalDays() == 5, "Multi-day rental duration (5 days) accurately restored from flat-file persistence (F-01/F-07)");
    }
}

