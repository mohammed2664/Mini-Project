package rentsphere.locker;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * High-Fidelity Simulated Locker Hardware Device.
 * Models door solenoids, jammed conditions, offline states, and sensor feedback.
 */
public class SimulatedLockerDevice implements LockerDevice {

    private final Map<String, DeviceState> bayDeviceStates = new ConcurrentHashMap<>();
    private boolean hardwareConnected = false; // Clearly distinguished as simulation

    public SimulatedLockerDevice() {
        // Initialize 64 bays to ONLINE_READY
        for (char r = 'A'; r <= 'H'; r++) {
            for (int c = 1; c <= 8; c++) {
                bayDeviceStates.put("" + r + c, DeviceState.ONLINE_READY);
            }
        }
    }

    @Override
    public boolean sendUnlockCommand(String bayId, String otp) {
        DeviceState state = bayDeviceStates.getOrDefault(bayId, DeviceState.OFFLINE);
        if (state == DeviceState.OFFLINE || state == DeviceState.DOOR_JAMMED || state == DeviceState.MAINTENANCE_MODE) {
            return false;
        }
        bayDeviceStates.put(bayId, DeviceState.DOOR_UNLOCKED);
        return true;
    }

    @Override
    public boolean sendLockCommand(String bayId) {
        DeviceState state = bayDeviceStates.getOrDefault(bayId, DeviceState.OFFLINE);
        if (state == DeviceState.OFFLINE || state == DeviceState.MAINTENANCE_MODE) {
            return false;
        }
        bayDeviceStates.put(bayId, DeviceState.ONLINE_READY);
        return true;
    }

    @Override
    public DeviceState getDeviceState(String bayId) {
        return bayDeviceStates.getOrDefault(bayId, DeviceState.OFFLINE);
    }

    @Override
    public void setDeviceState(String bayId, DeviceState state) {
        bayDeviceStates.put(bayId, state);
    }

    @Override
    public boolean isConnected() {
        return hardwareConnected;
    }

    public void setHardwareConnected(boolean connected) {
        this.hardwareConnected = connected;
    }
}
