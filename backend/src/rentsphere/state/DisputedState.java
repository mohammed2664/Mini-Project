package rentsphere.state;

import rentsphere.model.RentableAsset;

/**
 * State: DISPUTED
 * Represents an asset whose return condition or damage claim is actively contested by the renter.
 * All automatic collateral transfers remain frozen until resolved by Goa University / Hub Administration.
 */
public class DisputedState implements LeaseState {

    @Override
    public String getStateName() {
        return "DISPUTED";
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
        return false;
    }

    @Override
    public boolean canSlashInspection() {
        return false;
    }

    @Override
    public void reserve(RentableAsset asset, String renterId) {
        throw new IllegalStateException("Asset is frozen in DISPUTED state pending administrative arbitration.");
    }

    @Override
    public void pickup(RentableAsset asset) {
        throw new IllegalStateException("Asset cannot be picked up while in DISPUTED state.");
    }

    @Override
    public void initiateReturn(RentableAsset asset) {
        throw new IllegalStateException("Asset has already been returned and is in dispute.");
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
