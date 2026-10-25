package rentsphere.state;

import rentsphere.model.RentableAsset;

/**
 * State: AVAILABLE
 * Gear is in locker and ready for reservation.
 */
public class AvailableState implements LeaseState {

    @Override
    public String getStateName() {
        return "AVAILABLE";
    }

    @Override
    public boolean canReserve() {
        return true;
    }

    @Override
    public boolean canPickup() {
        return false;
    }

    @Override
    public boolean canReturn() {
        return false;
    }

    @Override
    public boolean canApproveInspection() {
        return false;
    }

    @Override
    public boolean canSlashInspection() {
        return false;
    }

    @Override
    public void reserve(RentableAsset asset, String renterId) {
        asset.setRenterId(renterId);
        asset.setState(new ReservedEscrowState());
    }

    @Override
    public void pickup(RentableAsset asset) {
        throw new IllegalStateException("Cannot pickup asset in AVAILABLE state. Must be reserved first.");
    }

    @Override
    public void initiateReturn(RentableAsset asset) {
        throw new IllegalStateException("Cannot return asset that is already in AVAILABLE state.");
    }

    @Override
    public void approveInspection(RentableAsset asset) {
        throw new IllegalStateException("Asset is not in quarantine for inspection.");
    }

    @Override
    public void slashCollateral(RentableAsset asset, double penalty) {
        throw new IllegalStateException("Asset is not in quarantine for damage slashing.");
    }

    @Override
    public String toString() {
        return getStateName();
    }
}
