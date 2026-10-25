package rentsphere.persistence;

import rentsphere.escrow.EscrowAccount;
import rentsphere.escrow.EscrowVault;
import rentsphere.locker.CompartmentSize;
import rentsphere.locker.LockerCompartment;
import rentsphere.locker.SmartLockerGrid8x8;
import rentsphere.meta.GenericRepository;
import rentsphere.model.*;
import rentsphere.state.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;

/**
 * Deterministic Zero-Database Flat File Persistence Engine.
 * Synchronizes domain state deterministically with human-readable, pipe-delimited
 * flat files in data/ (*.txt) and an append-only Write-Ahead Log (WAL).
 * MCA OOT Lab - Assignment 5 (File I/O & Audit Logging)
 */
public class FlatFilePersistenceEngine {
    private final Path assetsFile;
    private final Path lockerGridFile;
    private final Path escrowVaultFile;
    private final Path auditWalFile;
    private final Object fileLock = new Object();

    public FlatFilePersistenceEngine(String baseDataDir) {
        Path dataDir = Paths.get(baseDataDir != null ? baseDataDir : "data");
        try {
            if (!Files.exists(dataDir)) {
                Files.createDirectories(dataDir);
            }
        } catch (IOException e) {
            System.err.println("[PERSISTENCE] Error creating data directory: " + e.getMessage());
        }

        this.assetsFile = dataDir.resolve("assets_ledger.txt");
        this.lockerGridFile = dataDir.resolve("locker_grid_8x8.txt");
        this.escrowVaultFile = dataDir.resolve("escrow_vault.txt");
        this.auditWalFile = dataDir.resolve("audit_wal.txt");
    }

    // ========================================================================
    // ASSETS LEDGER FLAT FILE I/O
    // ========================================================================
    public void loadAssets(GenericRepository<RentableAsset> repo) {
        synchronized (fileLock) {
            if (!Files.exists(assetsFile)) return;

            try (BufferedReader reader = Files.newBufferedReader(assetsFile, StandardCharsets.UTF_8)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) continue;

                    String[] parts = line.split("\\|", -1);
                    if (parts.length < 10) continue;

                    String id = parts[0];
                    String title = parts[1];
                    String sector = parts[2];
                    String leafClass = parts[3];
                    String ownerId = parts[4];
                    String renterId = parts[5];
                    String stateStr = parts[6];
                    double baseRate = parseDouble(parts[7], 500.0);
                    double collateral = parseDouble(parts[8], 1500.0);
                    String lockerBay = parts[9];
                    String extra = (parts.length > 10) ? parts[10] : "";
                    String logs = (parts.length > 11) ? parts[11] : "";

                    Map<String, String> extraMap = parseKeyValueMap(extra);
                    RentableAsset asset = constructAsset(leafClass, id, title, ownerId, baseRate, collateral, lockerBay, extraMap);

                    if (asset != null) {
                        asset.setRenterId(renterId);
                        applyState(asset, stateStr);

                        if (!logs.isBlank()) {
                            for (String logEntry : logs.split(",")) {
                                if (!logEntry.isBlank()) {
                                    asset.addMaintenanceLog(logEntry.trim());
                                }
                            }
                        }
                        repo.save(asset);
                    }
                }
            } catch (IOException e) {
                System.err.println("[PERSISTENCE] Error reading assets_ledger.txt: " + e.getMessage());
            }
        }
    }

    public void saveAssets(GenericRepository<RentableAsset> repo) {
        synchronized (fileLock) {
            try (BufferedWriter writer = Files.newBufferedWriter(assetsFile, StandardCharsets.UTF_8)) {
                for (RentableAsset asset : repo.findAll()) {
                    writer.write(asset.serializeToPipe());
                    writer.newLine();
                }
            } catch (IOException e) {
                System.err.println("[PERSISTENCE] Error saving assets_ledger.txt: " + e.getMessage());
            }
        }
    }

    // ========================================================================
    // 8x8 SMART LOCKER GRID FLAT FILE I/O
    // ========================================================================
    public void loadLockers(SmartLockerGrid8x8 grid) {
        synchronized (fileLock) {
            if (!Files.exists(lockerGridFile)) return;

            try (BufferedReader reader = Files.newBufferedReader(lockerGridFile, StandardCharsets.UTF_8)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) continue;

                    String[] parts = line.split("\\|", -1);
                    if (parts.length < 11) continue;

                    String bayId = parts[0];
                    int row = parseInt(parts[1], 0);
                    int col = parseInt(parts[2], 0);
                    CompartmentSize size = parseSize(parts[3], row);
                    boolean occupied = Boolean.parseBoolean(parts[4]);
                    String assetId = parts[5];
                    String status = parts[6];
                    String otp = parts[7];
                    double temp = parseDouble(parts[8], 23.5);
                    double hum = parseDouble(parts[9], 45.0);
                    double weight = parseDouble(parts[10], 0.0);

                    LockerCompartment comp = new LockerCompartment(
                            bayId, row, col, size, occupied, assetId, status, otp, temp, hum, weight
                    );
                    grid.loadCompartment(comp);
                }
            } catch (IOException e) {
                System.err.println("[PERSISTENCE] Error reading locker_grid_8x8.txt: " + e.getMessage());
            }
        }
    }

    public void saveLockers(SmartLockerGrid8x8 grid) {
        synchronized (fileLock) {
            try (BufferedWriter writer = Files.newBufferedWriter(lockerGridFile, StandardCharsets.UTF_8)) {
                for (LockerCompartment comp : grid.getAllCompartments()) {
                    writer.write(comp.serializeToPipe());
                    writer.newLine();
                }
            } catch (IOException e) {
                System.err.println("[PERSISTENCE] Error saving locker_grid_8x8.txt: " + e.getMessage());
            }
        }
    }

    // ========================================================================
    // ESCROW VAULT FLAT FILE I/O
    // ========================================================================
    public void loadEscrow(EscrowVault vault) {
        synchronized (fileLock) {
            if (!Files.exists(escrowVaultFile)) return;

            try (BufferedReader reader = Files.newBufferedReader(escrowVaultFile, StandardCharsets.UTF_8)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) continue;

                    String[] parts = line.split("\\|", -1);
                    if (parts.length < 7) continue;

                    String userId = parts[0];
                    String userName = parts[1];
                    String role = parts[2];
                    double balance = parseDouble(parts[3], 0.0);
                    double locked = parseDouble(parts[4], 0.0);
                    double earned = parseDouble(parts[5], 0.0);
                    boolean kyc = Boolean.parseBoolean(parts[6]);

                    vault.registerAccount(new EscrowAccount(userId, userName, role, balance, locked, earned, kyc));
                }
            } catch (IOException e) {
                System.err.println("[PERSISTENCE] Error reading escrow_vault.txt: " + e.getMessage());
            }
        }
    }

    public void saveEscrow(EscrowVault vault) {
        synchronized (fileLock) {
            try (BufferedWriter writer = Files.newBufferedWriter(escrowVaultFile, StandardCharsets.UTF_8)) {
                for (EscrowAccount acc : vault.getAllAccounts()) {
                    writer.write(acc.serializeToPipe());
                    writer.newLine();
                }
            } catch (IOException e) {
                System.err.println("[PERSISTENCE] Error saving escrow_vault.txt: " + e.getMessage());
            }
        }
    }

    // ========================================================================
    // APPEND-ONLY WRITE-AHEAD LOG (WAL)
    // ========================================================================
    public void appendWal(String entityId, String actionType, String previousState, 
                          String newState, String actor, String details) {
        synchronized (fileLock) {
            String timestamp = Instant.now().toString();
            String txId = "WAL-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            String entry = String.format("%s|%s|%s|%s|%s|%s|%s|%s",
                    timestamp, txId, entityId, actionType, previousState, newState, actor, details);

            try (BufferedWriter writer = Files.newBufferedWriter(auditWalFile, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
                writer.write(entry);
                writer.newLine();
            } catch (IOException e) {
                System.err.println("[PERSISTENCE] Error appending to audit_wal.txt: " + e.getMessage());
            }
        }
    }

    public List<String> readWalEntries(int limit) {
        synchronized (fileLock) {
            List<String> entries = new ArrayList<>();
            if (!Files.exists(auditWalFile)) return entries;

            try (BufferedReader reader = Files.newBufferedReader(auditWalFile, StandardCharsets.UTF_8)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.isBlank()) {
                        entries.add(line);
                    }
                }
            } catch (IOException e) {
                System.err.println("[PERSISTENCE] Error reading audit_wal.txt: " + e.getMessage());
            }

            if (entries.size() > limit && limit > 0) {
                return entries.subList(entries.size() - limit, entries.size());
            }
            return entries;
        }
    }

    // ========================================================================
    // HELPER FACTORIES
    // ========================================================================
    private RentableAsset constructAsset(String leafClass, String id, String title, String ownerId, 
                                         double baseRate, double collateral, String lockerBay, 
                                         Map<String, String> extra) {
        double multiplier = parseDouble(extra.get("multiplier"), 1.15);

        switch (leafClass) {
            case "AutonomousMarineDrone":
                double depth = parseDouble(extra.get("depth"), 100.0);
                boolean sonar = Boolean.parseBoolean(extra.get("sonar"));
                String ip = extra.getOrDefault("ip", "IP68");
                double corrosion = parseDouble(extra.get("corrosion"), 1.10);
                return new AutonomousMarineDrone(id, title, ownerId, baseRate, collateral, lockerBay,
                        multiplier, "Tier-1 Marine Comprehensive", ip, corrosion, depth, sonar);

            case "SubmersibleActionRig":
                String stabilization = extra.getOrDefault("stabilization", "GimbalTriple");
                double rigDepth = parseDouble(extra.get("depth"), 50.0);
                boolean is8k = Boolean.parseBoolean(extra.get("is8k"));
                String rigIp = extra.getOrDefault("ip", "IP68");
                double rigCorrosion = parseDouble(extra.get("corrosion"), 1.05);
                return new SubmersibleActionRig(id, title, ownerId, baseRate, collateral, lockerBay,
                        multiplier, "Standard Tourism Cover", rigIp, rigCorrosion, stabilization, is8k, rigDepth);

            case "PrecisionRotaryTiller":
                int rpm = parseInt(extra.get("rpm"), 2500);
                double width = parseDouble(extra.get("width"), 160.0);
                String soil = extra.getOrDefault("soil", "Laterite");
                double kw = parseDouble(extra.get("kw"), 35.0);
                return new PrecisionRotaryTiller(id, title, ownerId, baseRate, collateral, lockerBay,
                        multiplier, "Agri Machinery Protection", soil, kw, rpm, width);

            case "OpticalSoilSpectrometer":
                int bands = parseInt(extra.get("bands"), 16);
                double accuracy = parseDouble(extra.get("accuracy"), 98.0);
                String specSoil = extra.getOrDefault("soil", "Alluvial");
                double specKw = parseDouble(extra.get("kw"), 2.0);
                return new OpticalSoilSpectrometer(id, title, ownerId, baseRate, collateral, lockerBay,
                        multiplier, "Sensor Precision Bond", specSoil, specKw, bands, accuracy);

            default:
                return null;
        }
    }

    private void applyState(RentableAsset asset, String stateStr) {
        switch (stateStr.toUpperCase()) {
            case "RESERVED_ESCROW":
                asset.setState(new ReservedEscrowState());
                break;
            case "ACTIVE_LEASE":
                asset.setState(new ActiveLeaseState());
                break;
            case "INSPECTION_QUARANTINE":
                asset.setState(new InspectionQuarantineState());
                break;
            case "AVAILABLE":
            default:
                asset.setState(new AvailableState());
                break;
        }
    }

    private Map<String, String> parseKeyValueMap(String extra) {
        Map<String, String> map = new HashMap<>();
        if (extra == null || extra.isBlank()) return map;
        for (String pair : extra.split(";")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2) {
                map.put(kv[0].trim(), kv[1].trim());
            }
        }
        return map;
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

    private CompartmentSize parseSize(String str, int row) {
        if (str != null) {
            try {
                return CompartmentSize.valueOf(str.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }
        if (row <= 2) return CompartmentSize.SMALL;
        if (row <= 4) return CompartmentSize.MEDIUM;
        if (row <= 6) return CompartmentSize.LARGE;
        return CompartmentSize.XL_HEAVY_AGRI;
    }
}
