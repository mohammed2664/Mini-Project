package rentsphere.locker;

import rentsphere.model.AgricultureAsset;
import rentsphere.model.RentableAsset;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 8x8 Smart Locker Micro-Storage Matrix Engine.
 * Manages 64 physical bays arranged in Rows 0..7 (A..H) and Cols 0..7 (1..8).
 * Enforces size limits and the Row 7 Heavy-Agri Spatial Invariant.
 * MCA OOT Lab - Assignment 5
 */
public class SmartLockerGrid8x8 {
    public static final int ROWS = 8;
    public static final int COLS = 8;
    public static final int TOTAL_BAYS = 64;

    private final LockerCompartment[][] matrix = new LockerCompartment[ROWS][COLS];
    private final Map<String, LockerCompartment> bayLookup = new ConcurrentHashMap<>();

    public SmartLockerGrid8x8() {
        initializeDefaultGrid();
    }

    private void initializeDefaultGrid() {
        for (int r = 0; r < ROWS; r++) {
            char rowChar = (char) ('A' + r);
            CompartmentSize size;
            if (r <= 2) {
                size = CompartmentSize.SMALL;
            } else if (r <= 4) {
                size = CompartmentSize.MEDIUM;
            } else if (r <= 6) {
                size = CompartmentSize.LARGE;
            } else {
                size = CompartmentSize.XL_HEAVY_AGRI;
            }

            for (int c = 0; c < COLS; c++) {
                String bayId = "" + rowChar + (c + 1);
                LockerCompartment comp = new LockerCompartment(
                        bayId, r, c, size, false, "", "AVAILABLE",
                        "000000", 23.5, 45.0, 0.0
                );
                matrix[r][c] = comp;
                bayLookup.put(bayId, comp);
            }
        }
    }

    public void loadCompartment(LockerCompartment comp) {
        if (comp.getRow() >= 0 && comp.getRow() < ROWS && comp.getCol() >= 0 && comp.getCol() < COLS) {
            matrix[comp.getRow()][comp.getCol()] = comp;
            bayLookup.put(comp.getBayId(), comp);
        }
    }

    public LockerCompartment getCompartment(String bayId) {
        if (bayId == null) return null;
        return bayLookup.get(bayId.trim().toUpperCase());
    }

    public LockerCompartment getCompartment(int row, int col) {
        if (row < 0 || row >= ROWS || col < 0 || col >= COLS) return null;
        return matrix[row][col];
    }

    public List<LockerCompartment> getAllCompartments() {
        List<LockerCompartment> list = new ArrayList<>();
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                list.add(matrix[r][c]);
            }
        }
        return list;
    }

    public LockerCompartment[][] getMatrix() {
        return matrix;
    }

    /**
     * Enforces Row 7 Heavy-Agri Spatial Rule:
     * Heavy agricultural machinery is physically barred from Rows 0..6 (A..G)
     * and MUST be stored in Row 7 (Row H).
     */
    public boolean validateHeavyAgriPlacement(RentableAsset asset, String bayId) {
        if (bayId == null || bayId.trim().length() < 2) return false;
        char rowChar = bayId.trim().toUpperCase().charAt(0);
        int rowIndex = rowChar - 'A';

        if (asset instanceof AgricultureAsset) {
            AgricultureAsset agri = (AgricultureAsset) asset;
            if (agri.isHeavyMachineryFlag()) {
                return rowIndex == 7; // Only Row 7 (H) allowed
            }
        }
        return true;
    }

    /**
     * Assigns an asset to a bay, generates an unlock OTP, and updates compartment state.
     */
    public boolean assignAssetToBay(RentableAsset asset, String bayId) {
        LockerCompartment comp = getCompartment(bayId);
        if (comp == null) return false;

        if (!validateHeavyAgriPlacement(asset, bayId)) {
            return false;
        }

        synchronized (comp) {
            comp.setOccupied(true);
            comp.setCurrentAssetId(asset.getId());
            comp.setStatus("ONLINE_READY");
            comp.generateNewOtp();
            asset.setLockerCoordinate(bayId);
            return true;
        }
    }

    /**
     * Simulates live IoT sensor telemetry readings for temperature, humidity, and load cell weight.
     */
    public boolean updateTelemetry(String bayId, double tempC, double humidityPct, double weightKg) {
        LockerCompartment comp = getCompartment(bayId);
        if (comp == null) return false;

        synchronized (comp) {
            comp.setTempC(tempC);
            comp.setHumidityPct(humidityPct);
            comp.setWeightKg(weightKg);
            return true;
        }
    }
}
