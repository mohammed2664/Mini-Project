package rentsphere.locker;

/**
 * Physical Locker Hardware Device Abstraction Interface.
 * Defines commands for micro-storage solenoid locks, telemetry sensors, and door statuses.
 */
public interface LockerDevice {

    enum DeviceState {
        ONLINE_READY,
        DOOR_UNLOCKED,
        DOOR_JAMMED,
        OFFLINE,
        MAINTENANCE_MODE
    }

    boolean sendUnlockCommand(String bayId, String otp);
    boolean sendLockCommand(String bayId);
    DeviceState getDeviceState(String bayId);
    void setDeviceState(String bayId, DeviceState state);
    boolean isConnected();
}
