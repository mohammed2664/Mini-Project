package rentsphere.state;

import rentsphere.model.RentableAsset;

/**
 * State: INSPECTION_QUARANTINE
 * Gear has been returned to the smart locker bay and is held for host inspection.
 * Can transition to AVAILABLE only via approveInspection() or slashCollateral().
 */
public class InspectionQuarantineState implements LeaseState {

    @Override
    public String getStateName() {
        return "INSPECTION_QUARANTINE";
    }

    @Override
    public boolean canReserve() {
        return false;
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
        return true;
    }

    @Override
    public boolean canSlashInspection() {
        return true;
    }

    @Override
    public void reserve(RentableAsset asset, String renterId) {
        throw new IllegalStateException("Asset is currently in Quarantine and cannot be reserved until inspected.");
    }

    @Override
    public void pickup(RentableAsset asset) {
        throw new IllegalStateException("Asset cannot be picked up while in Quarantine.");
    }

    @Override
    public void initiateReturn(RentableAsset asset) {
        throw new IllegalStateException("Asset is already in Quarantine.");
    }

    @Override
    public void approveInspection(RentableAsset asset) {
        asset.setRenterId("");
        asset.setState(new AvailableState());
    }

    @Override
    public void slashCollateral(RentableAsset asset, double penalty) {
        asset.setRenterId("");
        asset.setState(new AvailableState());
    }

    @Override
    public String toString() {
        return getStateName();
    }
}
