package rentsphere.state;

import rentsphere.model.RentableAsset;

/**
 * State: ACTIVE_LEASE
 * Gear is in use by renter.
 * 
 * MANDATORY QUARANTINE INVARIANT:
 * Returning gear from ACTIVE_LEASE MUST transition to InspectionQuarantineState,
 * never directly to AvailableState!
 */
public class ActiveLeaseState implements LeaseState {

    @Override
    public String getStateName() {
        return "ACTIVE_LEASE";
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
        return true;
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
        throw new IllegalStateException("Asset is currently on an active lease and cannot be reserved.");
    }

    @Override
    public void pickup(RentableAsset asset) {
        throw new IllegalStateException("Asset has already been picked up.");
    }

    @Override
    public void initiateReturn(RentableAsset asset) {
        // Enforce Mandatory Quarantine Invariant
        asset.setState(new InspectionQuarantineState());
    }

    @Override
    public void approveInspection(RentableAsset asset) {
        throw new IllegalStateException("Asset is still on active lease. Must be returned to quarantine first.");
    }

    @Override
    public void slashCollateral(RentableAsset asset, double penalty) {
        throw new IllegalStateException("Asset is still on active lease. Must be returned to quarantine first.");
    }

    @Override
    public String toString() {
        return getStateName();
    }
}
