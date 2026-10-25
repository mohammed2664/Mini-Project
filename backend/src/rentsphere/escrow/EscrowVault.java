package rentsphere.escrow;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe Double-Entry Escrow Vault Subsystem.
 * Manages user wallets, locked escrow collateral, and automated damage penalty distribution.
 * MCA OOT Lab - Assignment 2 (Escrow Vault)
 */
public class EscrowVault {
    private final Map<String, EscrowAccount> accounts = new ConcurrentHashMap<>();
    private final List<EscrowTransaction> transactionLedger = Collections.synchronizedList(new ArrayList<>());
    private final Object vaultLock = new Object();

    public void registerAccount(EscrowAccount account) {
        accounts.put(account.getUserId(), account);
    }

    public EscrowAccount getAccount(String userId) {
        return accounts.get(userId);
    }

    public List<EscrowAccount> getAllAccounts() {
        return new ArrayList<>(accounts.values());
    }

    public List<EscrowTransaction> getTransactionLedger() {
        synchronized (transactionLedger) {
            return new ArrayList<>(transactionLedger);
        }
    }

    /**
     * Locks rental tariff + security collateral into escrow on reservation.
     */
    public boolean lockEscrow(String renterId, String ownerId, String assetId, 
                              double rentalTariff, double securityCollateral) {
        synchronized (vaultLock) {
            EscrowAccount renter = accounts.get(renterId);
            if (renter == null) return false;

            double total = Math.round((rentalTariff + securityCollateral) * 100.0) / 100.0;
            if (renter.getWalletBalance() < total) return false;

            renter.debitWallet(total);
            renter.creditLocked(total);

            transactionLedger.add(new EscrowTransaction(
                    renterId, ownerId, assetId, total, "LOCK",
                    String.format("Locked $%.2f ($%.2f rental fee + $%.2f security deposit)", total, rentalTariff, securityCollateral)
            ));
            return true;
        }
    }

    /**
     * Releases locked escrow upon clean quarantine inspection approval:
     * - Returns 100% of collateral deposit to renter.
     * - Transfers rental tariff to equipment owner.
     */
    public boolean releasePayoutClean(String renterId, String ownerId, String assetId, 
                                      double rentalTariff, double securityCollateral) {
        synchronized (vaultLock) {
            EscrowAccount renter = accounts.get(renterId);
            EscrowAccount owner = accounts.get(ownerId);
            if (renter == null || owner == null) return false;

            double total = Math.round((rentalTariff + securityCollateral) * 100.0) / 100.0;
            renter.debitLocked(total);
            renter.creditWallet(securityCollateral); // Full deposit refunded to renter

            owner.creditWallet(rentalTariff);       // Rental income transferred to owner
            owner.creditEarned(rentalTariff);

            transactionLedger.add(new EscrowTransaction(
                    renterId, ownerId, assetId, total, "PAYOUT_CLEAN",
                    String.format("Clean inspection passed: Refunded $%.2f deposit to %s, paid $%.2f tariff to %s",
                            securityCollateral, renter.getUserName(), rentalTariff, owner.getUserName())
            ));
            return true;
        }
    }

    /**
     * Slashes collateral upon damage flagged during quarantine inspection:
     * - Slashes damage penalty from renter's deposit and awards it to owner.
     * - Refunds net remaining deposit to renter.
     * - Transfers rental tariff to owner.
     */
    public boolean slashCollateral(String renterId, String ownerId, String assetId, 
                                   double rentalTariff, double securityCollateral, double damagePenalty) {
        synchronized (vaultLock) {
            EscrowAccount renter = accounts.get(renterId);
            EscrowAccount owner = accounts.get(ownerId);
            if (renter == null || owner == null) return false;

            double total = Math.round((rentalTariff + securityCollateral) * 100.0) / 100.0;
            renter.debitLocked(total);

            double penalty = Math.min(damagePenalty, securityCollateral);
            double netDepositRefund = Math.round((securityCollateral - penalty) * 100.0) / 100.0;
            double ownerTotalPayout = Math.round((rentalTariff + penalty) * 100.0) / 100.0;

            if (netDepositRefund > 0.0) {
                renter.creditWallet(netDepositRefund);
            }

            owner.creditWallet(ownerTotalPayout);
            owner.creditEarned(ownerTotalPayout);

            transactionLedger.add(new EscrowTransaction(
                    renterId, ownerId, assetId, penalty, "PAYOUT_SLASHED",
                    String.format("Damage Slashing: Deducted $%.2f penalty from deposit to %s, refunded $%.2f net deposit to %s",
                            penalty, owner.getUserName(), netDepositRefund, renter.getUserName())
            ));
            return true;
        }
    }
}
