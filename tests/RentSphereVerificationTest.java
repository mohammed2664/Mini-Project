package tests;

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
import rentsphere.state.*;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * RentSphere Automated Verification Test Suite.
 * Validates all 5 MCA OOT Lab Syllabus Assignments:
 *  - Assignment 1: 4-Gate Fail-Fast Verification Pipeline & Short-Circuiting
 *  - Assignment 2: 5-Tier Inheritance Hierarchy, GoF State Machine & Mandatory Quarantine Invariant
 *  - Assignment 3: System Invariants & State Transition Rules
 *  - Assignment 4: Meta-OOP RTTI Reflection, Shallow vs Deep Cloning & NIRF Scoring
 *  - Assignment 5: 8x8 Smart Locker Matrix, Row 7 Heavy-Agri Spatial Rule & Flat File Persistence
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
        System.out.println("=================================================================\n");

        testAssignment2Inheritance();
        testAssignment2StatePatternAndQuarantineInvariant();
        testAssignment1FailFastPipeline();
        testAssignment5SmartLockerGridAndHeavyAgriRule();
        testAssignment4MetaOOPReflectionAndCloning();
        testAssignment4NirfCompositeScore();
        testAssignment5FlatFilePersistenceAndWal();

        System.out.println("\n=================================================================");
        System.out.printf("  VERIFICATION SUMMARY: %d / %d TESTS PASSED (100%% SUCCESS)%n", testsPassed, testsTotal);
        System.out.println("=================================================================");
    }

    // ========================================================================
    // ASSIGNMENT 2: 5-TIER INHERITANCE HIERARCHY & POLYMORPHISM
    // ========================================================================
    private static void testAssignment2Inheritance() {
        System.out.println("--- [TEST 1] Assignment 2: 5-Tier Inheritance & Polymorphic Rates ---");

        // Tier 5 Leaf 1: AutonomousMarineDrone
        AutonomousMarineDrone drone = new AutonomousMarineDrone(
                "TEST-DRONE", "Goa Deep Marine Drone", "host_faaris",
                1000.0, 3000.0, "F2", 1.20, "Comprehensive Marine",
                "IP68", 1.15, 100.0, true
        );

        // Tariff formula: 1000 * 1.20 * (1.0 + (100 / 500.0)) * 2 = 1000 * 1.20 * 1.20 * 2 = 2880.0
        double droneTariff = drone.calculateDailyRentalTariff(2);
        assertTrue(Math.abs(droneTariff - 2880.0) < 0.01, "AutonomousMarineDrone depth tariff calculation (Expected $2880.00, Got $" + droneTariff + ")");

        // Collateral formula: 3000.0 * 1.25 (sonar equipped) = 3750.0
        double droneCollateral = drone.calculateRequiredCollateral();
        assertTrue(Math.abs(droneCollateral - 3750.0) < 0.01, "AutonomousMarineDrone sonar collateral calculation (Expected $3750.00, Got $" + droneCollateral + ")");

        // Tier 5 Leaf 3: PrecisionRotaryTiller (Heavy Agri)
        PrecisionRotaryTiller tiller = new PrecisionRotaryTiller(
                "TEST-TILLER", "Mandovi Heavy Tiller", "host_ketan",
                800.0, 2000.0, "H1", 1.10, "Machinery Cover",
                "Laterite", 40.0, 2800, 180.0
        );

        assertTrue(tiller.isHeavyMachineryFlag(), "PrecisionRotaryTiller has heavyMachineryFlag locked to true");
        assertTrue(tiller instanceof AgricultureAsset, "PrecisionRotaryTiller is instance of AgricultureAsset (Tier 4)");
        assertTrue(tiller instanceof SectoralAsset, "PrecisionRotaryTiller is instance of SectoralAsset (Tier 3)");
        assertTrue(tiller instanceof RentableAsset, "PrecisionRotaryTiller is instance of RentableAsset (Tier 2)");
        assertTrue(tiller instanceof AbstractDomainEntity, "PrecisionRotaryTiller is instance of AbstractDomainEntity (Tier 1)");

        // Heavy machinery collateral surcharge (1.15): 2000 * 1.15 = 2300.0
        assertTrue(Math.abs(tiller.calculateRequiredCollateral() - 2300.0) < 0.01, "Heavy Agri collateral surcharge validated ($2300.00)");
    }

    // ========================================================================
    // ASSIGNMENT 2: 4-STAGE STATE PATTERN & MANDATORY QUARANTINE INVARIANT
    // ========================================================================
    private static void testAssignment2StatePatternAndQuarantineInvariant() {
        System.out.println("\n--- [TEST 2] Assignment 2: GoF State Pattern & Mandatory Quarantine Invariant ---");

        AutonomousMarineDrone drone = new AutonomousMarineDrone(
                "TEST-DRONE", "Goa Deep Marine Drone", "host_faaris",
                1000.0, 3000.0, "F2", 1.20, "Comprehensive Marine",
                "IP68", 1.15, 100.0, true
        );

        // Initial state must be AvailableState
        assertTrue("AVAILABLE".equals(drone.getCurrentState().getStateName()), "Initial state is AVAILABLE");
        assertTrue(drone.getCurrentState().canReserve(), "AvailableState allows reserve action");
        assertTrue(!drone.getCurrentState().canPickup(), "AvailableState rejects premature pickup");

        // Reserve -> ReservedEscrowState
        drone.getCurrentState().reserve(drone, "renter_ketan");
        assertTrue("RESERVED_ESCROW".equals(drone.getCurrentState().getStateName()), "State transitioned AVAILABLE -> RESERVED_ESCROW");
        assertTrue(drone.getCurrentState().canPickup(), "ReservedEscrowState allows pickup");

        // Pickup -> ActiveLeaseState
        drone.getCurrentState().pickup(drone);
        assertTrue("ACTIVE_LEASE".equals(drone.getCurrentState().getStateName()), "State transitioned RESERVED_ESCROW -> ACTIVE_LEASE");

        // MANDATORY QUARANTINE INVARIANT TEST:
        // Returning gear from ACTIVE_LEASE MUST transition to InspectionQuarantineState, NEVER AvailableState!
        drone.getCurrentState().initiateReturn(drone);
        assertTrue("INSPECTION_QUARANTINE".equals(drone.getCurrentState().getStateName()),
                "MANDATORY QUARANTINE INVARIANT: Asset returned to locker strictly entered INSPECTION_QUARANTINE");

        // Proves that while in quarantine, it cannot be reserved by anyone
        assertTrue(!drone.getCurrentState().canReserve(), "InspectionQuarantineState forbids new reservations");
        assertTrue(drone.getCurrentState().canApproveInspection(), "InspectionQuarantineState allows host approval");
        assertTrue(drone.getCurrentState().canSlashInspection(), "InspectionQuarantineState allows collateral slashing");

        // Host approves inspection -> returns to AvailableState
        drone.getCurrentState().approveInspection(drone);
        assertTrue("AVAILABLE".equals(drone.getCurrentState().getStateName()),
                "Inspection approved cleanly: State transitioned INSPECTION_QUARANTINE -> AVAILABLE");
    }

    // ========================================================================
    // ASSIGNMENT 1: 4-GATE FAIL-FAST VERIFICATION PIPELINE
    // ========================================================================
    private static void testAssignment1FailFastPipeline() {
        System.out.println("\n--- [TEST 3] Assignment 1: 4-Gate Fail-Fast Verification Pipeline ---");

        FailFastPipeline pipeline = new FailFastPipeline();
        AutonomousMarineDrone drone = new AutonomousMarineDrone(
                "TEST-DRONE", "Goa Deep Marine Drone", "host_faaris",
                1000.0, 3000.0, "F2", 1.20, "Comprehensive Marine",
                "IP68", 1.15, 100.0, true
        );

        // Case 1: Gate 1 KycIdentityGate Violation (Anti-Self-Rental Rule)
        PipelineContext selfRentalCtx = new PipelineContext(
                drone, "host_faaris", "RESERVE", 10000.0, true, "F2", 2, 0.0
        );
        GateResult g1Result = pipeline.verify(selfRentalCtx);
        assertTrue(!g1Result.isPassed() && g1Result.getGateIndex() == 1, 
                "Fail-Fast Pipeline short-circuited at Gate 1 (KycIdentityGate - Anti-self-rental rule enforced)");

        // Case 2: Gate 3 EscrowSolvencyGate Violation (Insufficient Wallet Balance)
        PipelineContext insolventCtx = new PipelineContext(
                drone, "renter_poor", "RESERVE", 500.0, true, "F2", 2, 0.0
        );
        GateResult g3Result = pipeline.verify(insolventCtx);
        assertTrue(!g3Result.isPassed() && g3Result.getGateIndex() == 3, 
                "Fail-Fast Pipeline short-circuited at Gate 3 (EscrowSolvencyGate - Insufficient liquidity blocked)");

        // Case 3: All Gates Passed (Valid Reservation)
        PipelineContext validCtx = new PipelineContext(
                drone, "renter_ketan", "RESERVE", 15000.0, true, "F2", 2, 0.0
        );
        GateResult validResult = pipeline.verify(validCtx);
        assertTrue(validResult.isPassed(), "All 4 Gates passed successfully for solvent verified renter");
    }

    // ========================================================================
    // ASSIGNMENT 5: 8x8 SMART LOCKER MATRIX & ROW 7 HEAVY-AGRI SPATIAL RULE
    // ========================================================================
    private static void testAssignment5SmartLockerGridAndHeavyAgriRule() {
        System.out.println("\n--- [TEST 4] Assignment 5: 8x8 Locker Matrix & Row 7 Heavy-Agri Spatial Rule ---");

        SmartLockerGrid8x8 grid = new SmartLockerGrid8x8();
        assertTrue(grid.getAllCompartments().size() == 64, "8x8 Matrix contains exactly 64 physical bays (A1..H8)");

        // Size tier checks
        LockerCompartment bayA1 = grid.getCompartment("A1");
        assertTrue(bayA1.getSizeTier() == CompartmentSize.SMALL, "Bay A1 is SMALL tier (Row A)");

        LockerCompartment bayH1 = grid.getCompartment("H1");
        assertTrue(bayH1.getSizeTier() == CompartmentSize.XL_HEAVY_AGRI, "Bay H1 is XL_HEAVY_AGRI tier (Row H / Row 7)");

        // ROW 7 HEAVY-AGRI SPATIAL INVARIANT:
        PrecisionRotaryTiller heavyTiller = new PrecisionRotaryTiller(
                "TEST-TILLER", "Mandovi Heavy Tiller", "host_ketan",
                800.0, 2000.0, "C2", 1.10, "Machinery Cover",
                "Laterite", 40.0, 2800, 180.0
        );

        // Invalid placement: Row C (Row 2) must be REJECTED
        boolean invalidRowAssignment = grid.assignAssetToBay(heavyTiller, "C2");
        assertTrue(!invalidRowAssignment, "SPATIAL INVARIANT ENFORCED: Heavy agri machinery barred from Row C (Row 2)");

        // Valid placement: Row H (Row 7) must be ACCEPTED
        boolean validRowAssignment = grid.assignAssetToBay(heavyTiller, "H2");
        assertTrue(validRowAssignment, "SPATIAL INVARIANT ENFORCED: Heavy agri machinery successfully accepted in Row H (Row 7)");

        // Telemetry update check
        grid.updateTelemetry("H2", 26.5, 54.0, 215.0);
        LockerCompartment updatedBay = grid.getCompartment("H2");
        assertTrue(Math.abs(updatedBay.getTempC() - 26.5) < 0.1 && updatedBay.getWeightKg() == 215.0, 
                "Live sensor telemetry successfully updated for Bay H2");
    }

    // ========================================================================
    // ASSIGNMENT 4: META-OOP REFLECTION & SHALLOW VS DEEP CLONING
    // ========================================================================
    private static void testAssignment4MetaOOPReflectionAndCloning() {
        System.out.println("\n--- [TEST 5] Assignment 4: Live Reflection & Shallow vs. Deep Cloning ---");

        AutonomousMarineDrone drone = new AutonomousMarineDrone(
                "TEST-DRONE", "Goa Deep Marine Drone", "host_faaris",
                1000.0, 3000.0, "F2", 1.20, "Comprehensive Marine",
                "IP68", 1.15, 100.0, true
        );

        // 1. RTTI Reflection Inspector
        ReflectionInspector.InspectionReport report = ReflectionInspector.inspect(drone);
        assertTrue("AutonomousMarineDrone".equals(report.getClassName()), "Reflection extracted correct leaf class name");
        assertTrue(report.getInheritanceChain().size() >= 5, 
                "Reflection successfully traversed full 5-tier inheritance hierarchy (Length: " + report.getInheritanceChain().size() + ")");
        assertTrue(report.getDeclaredFields().size() > 0, "Reflection extracted declared fields");
        assertTrue(report.getDeclaredMethods().size() > 0, "Reflection extracted declared methods");

        // 2. Prototype Shallow vs. Deep Cloning Benchmark
        PrototypeCloner.CloneBenchmarkReport cloneReport = PrototypeCloner.benchmarkCloning(drone);
        assertTrue(cloneReport.shallowLogsShared, 
                "MEMORY BENCHMARK PROVED: Shallow clone SHARES maintenanceLogs heap reference (" + cloneReport.shallowLogsId + ")");
        assertTrue(cloneReport.deepLogsIsolated, 
                "MEMORY BENCHMARK PROVED: Deep clone ISOLATES maintenanceLogs with new heap reference (" + cloneReport.deepLogsId + ")");
    }

    // ========================================================================
    // ASSIGNMENT 4: NIRF COMPOSITE SCORE CALCULATION
    // ========================================================================
    private static void testAssignment4NirfCompositeScore() {
        System.out.println("\n--- [TEST 6] Assignment 4: NIRF Institutional Composite Score ---");

        NirfAnalyticsPortal.InstitutionMetrics baseline = new NirfAnalyticsPortal.InstitutionMetrics(
                "Goa University - CST", 80.0, 70.0, 85.0, 75.0, 70.0
        );

        // Formula: 80*0.30 + 70*0.30 + 85*0.20 + 75*0.10 + 70*0.10 = 24 + 21 + 17 + 7.5 + 7 = 76.50
        double score = baseline.computeCompositeScore();
        assertTrue(Math.abs(score - 76.50) < 0.01, "NIRF Composite Score calculated correctly (Expected 76.50, Got " + score + ")");
        assertTrue("Top 10 National Excellence".equals(baseline.computeTierBadge()), "Tier badge computed correctly (Top 10 National Excellence)");

        // What-If Simulation using deep clone
        NirfAnalyticsPortal.SimulationResult sim = NirfAnalyticsPortal.simulateWhatIf(baseline, 5.0, 5.0, 2.0, 2.0, 2.0);
        assertTrue(sim.scoreDelta > 0, "What-If rank simulation computed positive rank delta (+" + sim.scoreDelta + " pts)");
        assertTrue(baseline.computeCompositeScore() == 76.50, "Baseline metrics remained 100% UNMUTATED due to deep cloning");
    }

    // ========================================================================
    // ASSIGNMENT 5: FLAT FILE PERSISTENCE & WAL AUDITING
    // ========================================================================
    private static void testAssignment5FlatFilePersistenceAndWal() {
        System.out.println("\n--- [TEST 7] Assignment 5: Deterministic Flat-File I/O & WAL ---");

        String testDataDir = "data";
        FlatFilePersistenceEngine engine = new FlatFilePersistenceEngine(testDataDir);

        GenericRepository<RentableAsset> repo = new GenericRepository<>();
        engine.loadAssets(repo);
        assertTrue(repo.count() >= 8, "Flat file persistence engine loaded " + repo.count() + " seed assets from assets_ledger.txt");

        EscrowVault vault = new EscrowVault();
        engine.loadEscrow(vault);
        assertTrue(vault.getAllAccounts().size() >= 5, "Flat file persistence engine loaded " + vault.getAllAccounts().size() + " accounts from escrow_vault.txt");

        // Append and read WAL entry
        engine.appendWal("TEST-ASSET", "UNIT_TEST", "NONE", "ONLINE", "test_runner", "Automated verification test completed");
        List<String> walEntries = engine.readWalEntries(10);
        assertTrue(!walEntries.isEmpty(), "Write-Ahead Log contains audited entries in data/audit_wal.txt");
    }
}
