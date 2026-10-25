package rentsphere.locker;

/**
 * Size tiers for physical micro-storage compartments.
 * MCA OOT Lab - Assignment 5
 */
public enum CompartmentSize {
    SMALL("Small (Cameras & Sensors, Rows A-C)"),
    MEDIUM("Medium (Handheld Rig, Rows D-E)"),
    LARGE("Large (Survey Drones, Rows F-G)"),
    XL_HEAVY_AGRI("Extra Large Heavy Agri (Tillers & Heavy Implements, Row H Only)");

    private final String description;

    CompartmentSize(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
