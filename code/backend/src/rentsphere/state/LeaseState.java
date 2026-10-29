package rentsphere.state;

import rentsphere.model.RentableAsset;

/**
 * GoF State Pattern Interface
 * MCA OOT Lab - Assignment 2 (Core Behavioral Engine)
 */
public interface LeaseState {
    String getStateName();
    
    boolean canReserve();
    boolean canPickup();
    boolean canReturn();
    boolean canApproveInspection();
    boolean canSlashInspection();

    void reserve(RentableAsset asset, String renterId);
    void pickup(RentableAsset asset);
    void initiateReturn(RentableAsset asset);
    void approveInspection(RentableAsset asset);
    void slashCollateral(RentableAsset asset, double penalty);
}
