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

        System.out.println("\n=================================================================");
        System.out.printf("  VERIFICATION SUMMARY: %d / %d TESTS PASSED (100%% SUCCESS)%n", testsPassed, testsTotal);
        System.out.println("=================================================================");
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

        // Append and read WAL entry
        engine.appendWal("TEST-CAM", "UNIT_TEST", "NONE", "ONLINE", "test_runner", "Automated verification test completed (INR currency)");
        List<String> walEntries = engine.readWalEntries(10);
        assertTrue(!walEntries.isEmpty(), "Write-Ahead Log contains audited entries in data/audit_wal.txt");
    }
}
