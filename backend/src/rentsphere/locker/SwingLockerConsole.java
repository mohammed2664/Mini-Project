package rentsphere.locker;

import javax.swing.*;
import java.awt.*;

/**
 * Standalone Java Swing 8x8 Smart Locker Desktop GUI Console.
 * Offline lab evaluation backup window rendering the 64-bay color-coded GridLayout.
 * MCA OOT Lab - Assignment 5
 */
public class SwingLockerConsole extends JFrame {
    private final SmartLockerGrid8x8 grid;
    private final JButton[][] bayButtons = new JButton[8][8];

    public SwingLockerConsole(SmartLockerGrid8x8 grid) {
        this.grid = grid;
        setTitle("RentSphere - 8x8 Smart Micro-Storage Desktop Console (Goa University MCA)");
        setSize(900, 750);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        initComponents();
    }

    private void initComponents() {
        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        root.setBackground(new Color(244, 247, 251));

        // Header Panel
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(13, 148, 136));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));

        JLabel titleLabel = new JLabel("RentSphere: 8x8 Smart Locker Matrix (64 Micro-Storage Bays)");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        titleLabel.setForeground(Color.WHITE);

        JLabel subtitleLabel = new JLabel("Click any bay (A1–H8) to inspect telemetry, size tiers, and active assets. Row H reserved for Heavy Agri.");
        subtitleLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        subtitleLabel.setForeground(new Color(204, 251, 241));

        headerPanel.add(titleLabel, BorderLayout.NORTH);
        headerPanel.add(subtitleLabel, BorderLayout.SOUTH);
        root.add(headerPanel, BorderLayout.NORTH);

        // 8x8 Matrix Grid Panel
        JPanel gridPanel = new JPanel(new GridLayout(8, 8, 6, 6));
        gridPanel.setBackground(new Color(244, 247, 251));

        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                LockerCompartment comp = grid.getCompartment(r, c);
                JButton btn = new JButton(comp.getBayId());
                btn.setFont(new Font("SansSerif", Font.BOLD, 13));
                btn.setFocusPainted(false);
                btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
                styleButton(btn, comp);

                final int row = r;
                final int col = c;
                btn.addActionListener(e -> showCompartmentDialog(grid.getCompartment(row, col)));

                bayButtons[r][c] = btn;
                gridPanel.add(btn);
            }
        }
        root.add(gridPanel, BorderLayout.CENTER);

        // Legend Footer Panel
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        footer.setBackground(Color.WHITE);
        footer.setBorder(BorderFactory.createLineBorder(new Color(226, 232, 240)));

        footer.add(createLegendPill("Ready / Available", new Color(16, 185, 129)));
        footer.add(createLegendPill("Reserved / Escrow", new Color(245, 158, 11)));
        footer.add(createLegendPill("Active Lease (Field)", new Color(100, 116, 139)));
        footer.add(createLegendPill("Quarantine Locked", new Color(239, 68, 68)));
        footer.add(createLegendPill("Row H: Heavy Agri XL", new Color(10, 92, 77)));

        root.add(footer, BorderLayout.SOUTH);
        setContentPane(root);
    }

    private void styleButton(JButton btn, LockerCompartment comp) {
        String status = comp.getStatus().toUpperCase();
        Color bg;
        Color fg = Color.WHITE;

        if (comp.getRow() == 7) {
            // Row 7 Heavy Agri indicator
            if ("QUARANTINE_LOCKED".equals(status)) {
                bg = new Color(239, 68, 68);
            } else if ("RESERVED_LOCKED".equals(status)) {
                bg = new Color(245, 158, 11);
            } else {
                bg = new Color(10, 92, 77); // Deep teal for Heavy Agri Row H
            }
        } else {
            switch (status) {
                case "RESERVED_LOCKED":
                    bg = new Color(245, 158, 11); // Warm Amber
                    break;
                case "DISPATCHED_ACTIVE":
                    bg = new Color(100, 116, 139); // Slate
                    break;
                case "QUARANTINE_LOCKED":
                    bg = new Color(239, 68, 68); // Crimson
                    break;
                case "ONLINE_READY":
                case "AVAILABLE":
                default:
                    bg = comp.isOccupied() ? new Color(13, 148, 136) : new Color(16, 185, 129);
                    break;
            }
        }

        btn.setBackground(bg);
        btn.setForeground(fg);
        btn.setToolTipText("Bay: " + comp.getBayId() + " | " + comp.getSizeTier().name() + " | Status: " + status);
    }

    private JPanel createLegendPill(String text, Color color) {
        JPanel pill = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
        pill.setOpaque(false);
        JPanel box = new JPanel();
        box.setPreferredSize(new Dimension(14, 14));
        box.setBackground(color);
        box.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
        JLabel label = new JLabel(text);
        label.setFont(new Font("SansSerif", Font.PLAIN, 11));
        pill.add(box);
        pill.add(label);
        return pill;
    }

    private void showCompartmentDialog(LockerCompartment comp) {
        String assetDisplay = comp.getCurrentAssetId().isBlank() ? "(Empty / Unoccupied)" : comp.getCurrentAssetId();
        String message = String.format(
                "Bay Identifier: %s\n" +
                "Matrix Coordinate: Row %d (Char '%c'), Col %d\n" +
                "Size Tier: %s\n" +
                "Status: %s\n" +
                "Occupied: %b\n" +
                "Housed Asset ID: %s\n" +
                "6-Digit Unlock OTP: %s\n\n" +
                "--- Live Compartment Telemetry ---\n" +
                "Internal Temperature: %.1f °C\n" +
                "Relative Humidity: %.1f %%\n" +
                "Floor Weight Sensor: %.1f kg\n\n" +
                "%s",
                comp.getBayId(), comp.getRow(), (char) ('A' + comp.getRow()), comp.getCol(),
                comp.getSizeTier().getDescription(), comp.getStatus(), comp.isOccupied(),
                assetDisplay, comp.getOtpPin(), comp.getTempC(), comp.getHumidityPct(), comp.getWeightKg(),
                (comp.getRow() == 7 ? "NOTE: Row 7 (Row H) is strictly reserved for Heavy-Agri machinery." : "Standard equipment compartment.")
        );

        JOptionPane.showMessageDialog(this, message, "Compartment Inspector — Bay " + comp.getBayId(), JOptionPane.INFORMATION_MESSAGE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            SmartLockerGrid8x8 grid = new SmartLockerGrid8x8();
            SwingLockerConsole console = new SwingLockerConsole(grid);
            console.setVisible(true);
        });
    }
}
