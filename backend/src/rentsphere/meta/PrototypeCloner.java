package rentsphere.meta;

import rentsphere.model.RentableAsset;

import java.util.HashMap;
import java.util.Map;

/**
 * Prototype Cloner Benchmark.
 * Demonstrates the fundamental difference between Shallow and Deep cloning
 * using real System.identityHashCode() memory references.
 * MCA OOT Lab - Assignment 4
 */
public class PrototypeCloner {

    public static class CloneBenchmarkReport {
        public String originalObjectId;
        public String originalLogsId;
        public int originalLogsCount;

        public String shallowObjectId;
        public String shallowLogsId;
        public boolean shallowLogsShared;
        public int shallowLogsCount;

        public String deepObjectId;
        public String deepLogsId;
        public boolean deepLogsIsolated;
        public int deepLogsCount;

        public String mutationTestVerdict;
    }

    /**
     * Executes shallow and deep cloning on the target asset, appends a test log
     * entry to both clones, and compares memory addresses to verify isolation.
     */
    public static CloneBenchmarkReport benchmarkCloning(RentableAsset original) {
        if (original == null) {
            throw new IllegalArgumentException("Asset cannot be null.");
        }

        CloneBenchmarkReport report = new CloneBenchmarkReport();

        // 1. Memory identities before cloning
        report.originalObjectId = "0x" + Integer.toHexString(System.identityHashCode(original)).toUpperCase();
        report.originalLogsId = "0x" + Integer.toHexString(System.identityHashCode(original.getMaintenanceLogs())).toUpperCase();
        report.originalLogsCount = original.getMaintenanceLogs().size();

        // 2. Perform Shallow Clone
        RentableAsset shallow = original.shallowClone();
        report.shallowObjectId = "0x" + Integer.toHexString(System.identityHashCode(shallow)).toUpperCase();
        report.shallowLogsId = "0x" + Integer.toHexString(System.identityHashCode(shallow.getMaintenanceLogs())).toUpperCase();
        report.shallowLogsShared = report.originalLogsId.equals(report.shallowLogsId);

        // 3. Perform Deep Clone
        RentableAsset deep = original.deepClone();
        report.deepObjectId = "0x" + Integer.toHexString(System.identityHashCode(deep)).toUpperCase();
        report.deepLogsId = "0x" + Integer.toHexString(System.identityHashCode(deep.getMaintenanceLogs())).toUpperCase();
        report.deepLogsIsolated = !report.originalLogsId.equals(report.deepLogsId);

        // 4. Test Mutation Effects
        shallow.addMaintenanceLog("MUTATION_TEST_SHALLOW_TAG");
        boolean shallowLeaked = original.getMaintenanceLogs().contains("MUTATION_TEST_SHALLOW_TAG");

        deep.addMaintenanceLog("MUTATION_TEST_DEEP_TAG");
        boolean deepIsolated = !original.getMaintenanceLogs().contains("MUTATION_TEST_DEEP_TAG");

        // Cleanup test tag from original to keep state consistent
        original.getMaintenanceLogs().remove("MUTATION_TEST_SHALLOW_TAG");

        report.shallowLogsCount = shallow.getMaintenanceLogs().size();
        report.deepLogsCount = deep.getMaintenanceLogs().size();

        if (shallowLeaked && deepIsolated) {
            report.mutationTestVerdict = "VERIFIED: Shallow Clone shares mutable list reference (" + 
                    report.shallowLogsId + "), whereas Deep Clone created an isolated heap allocation (" + 
                    report.deepLogsId + ").";
        } else {
            report.mutationTestVerdict = "UNEXPECTED: Cloning memory isolation test did not match expectations.";
        }

        return report;
    }
}
