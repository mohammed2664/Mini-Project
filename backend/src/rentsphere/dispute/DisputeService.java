package rentsphere.dispute;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Dispute Resolution and Evidence Management Service.
 * Allows renters to challenge damage claims with counter-evidence,
 * preserving arbitration records and updating the Write-Ahead Log.
 */
public class DisputeService {

    private final Map<String, DisputeRecord> disputes = new ConcurrentHashMap<>();
    private final Path disputesFile;
    private final Object fileLock = new Object();

    public DisputeService(String baseDataDir) {
        Path dataDir = Paths.get(baseDataDir != null ? baseDataDir : "data");
        this.disputesFile = dataDir.resolve("disputes_ledger.txt");
        loadDisputes();
    }

    public synchronized DisputeRecord fileDispute(String assetId, String renterId, String ownerId, 
                                                 double heldDeposit, double claimedDeduction, 
                                                 String renterReason, String evidenceNotes) {
        String disputeId = "DSP-" + (100 + disputes.size() + 1);
        DisputeRecord dispute = new DisputeRecord(disputeId, assetId, renterId, ownerId, heldDeposit, claimedDeduction, renterReason, evidenceNotes);
        disputes.put(disputeId, dispute);
        saveDisputes();
        return dispute;
    }

    public synchronized boolean resolveDispute(String disputeId, String adminId, String decision, String adminNotes) {
        DisputeRecord d = disputes.get(disputeId);
        if (d == null) return false;

        d.setStatus(decision); // "RESOLVED_REFUND" or "RESOLVED_SLASH"
        d.setAdminNotes("Arbitrated by " + adminId + ": " + (adminNotes != null ? adminNotes : ""));
        d.setResolvedAt(System.currentTimeMillis());
        saveDisputes();
        return true;
    }

    public DisputeRecord getDispute(String disputeId) {
        return disputes.get(disputeId);
    }

    public List<DisputeRecord> getAllDisputes() {
        return new ArrayList<>(disputes.values());
    }

    public List<DisputeRecord> getDisputesForUser(String userId) {
        List<DisputeRecord> list = new ArrayList<>();
        for (DisputeRecord d : disputes.values()) {
            if (d.getRenterId().equals(userId) || d.getOwnerId().equals(userId)) {
                list.add(d);
            }
        }
        return list;
    }

    private void loadDisputes() {
        synchronized (fileLock) {
            if (!Files.exists(disputesFile)) return;
            try (BufferedReader reader = Files.newBufferedReader(disputesFile, StandardCharsets.UTF_8)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) continue;

                    String[] p = line.split("\\|", -1);
                    if (p.length < 12) continue;

                    String disputeId = p[0];
                    String assetId = p[1];
                    String renterId = p[2];
                    String ownerId = p[3];
                    double heldDeposit = parseDouble(p[4], 0.0);
                    double claimedDeduction = parseDouble(p[5], 0.0);
                    String reason = p[6];
                    String evidence = p[7];
                    String status = p[8];
                    String adminNotes = p[9];
                    long resolvedAt = parseLong(p[11], 0L);

                    DisputeRecord d = new DisputeRecord(disputeId, assetId, renterId, ownerId, heldDeposit, claimedDeduction, reason, evidence);
                    d.setStatus(status);
                    d.setAdminNotes(adminNotes);
                    d.setResolvedAt(resolvedAt);
                    disputes.put(disputeId, d);
                }
            } catch (IOException e) {
                System.err.println("[DISPUTES] Error loading disputes_ledger.txt: " + e.getMessage());
            }
        }
    }

    private void saveDisputes() {
        synchronized (fileLock) {
            Path temp = disputesFile.resolveSibling(disputesFile.getFileName() + ".tmp");
            try {
                try (BufferedWriter writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                    writer.write("# RentSphere Dispute & Arbitration Ledger");
                    writer.newLine();
                    for (DisputeRecord d : disputes.values()) {
                        writer.write(d.serializeToPipe());
                        writer.newLine();
                    }
                }
                Files.move(temp, disputesFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException e) {
                try {
                    Files.move(temp, disputesFile, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException ex) {
                    System.err.println("[DISPUTES] Error saving disputes_ledger.txt: " + ex.getMessage());
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
