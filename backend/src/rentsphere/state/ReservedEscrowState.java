package rentsphere.state;

import rentsphere.model.RentableAsset;

/**
 * State: RESERVED_ESCROW
 * Escrow is locked, OTP generated, awaiting physical pickup.
 */
public class ReservedEscrowState implements LeaseState {

    @Override
    public String getStateName() {
        return "RESERVED_ESCROW";
    }

    @Override
    public boolean canReserve() {
        return false;
    }

    @Override
    public boolean canPickup() {
        return true;
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
        throw new IllegalStateException("Asset is already reserved.");
    }

    @Override
    public void pickup(RentableAsset asset) {
        asset.setState(new ActiveLeaseState());
    }

    @Override
    public void initiateReturn(RentableAsset asset) {
        throw new IllegalStateException("Cannot return asset before it has been picked up.");
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
