package rentsphere.locker;

/**
 * Size tiers for physical micro-storage lockers and ground vehicle parking slots.
 * MCA OOT Lab - Assignment 5
 */
public enum CompartmentSize {
    SMALL("Small Locker (Cameras, Lenses, Mics, Drills, Rows A-C)"),
    MEDIUM("Medium Equipment Locker (Projectors, DJ Kits, Tools, Rows D-E)"),
    LARGE("Large Equipment Bay (Party Speakers, Sound Systems, Washers, Rows F-G)"),
    XL_GROUND_SLOT("Row H Ground Vehicle & Heavy Gear Parking Slot (Cars, Scooters, Rigs, Slots H1-H8)");

    private final String description;

    CompartmentSize(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
