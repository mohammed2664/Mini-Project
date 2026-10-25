/**
 * RentSphere Core Engine - C# Polyglot Reference Implementation
 * Author: Ketan Gurudas Naik (Subsystem 2 Lead)
 * Institution: Goa University - MCA Object-Oriented Technology Lab
 * 
 * Features:
 *  - 5-Tier Domain Model with Polymorphic Formulas
 *  - Double-Entry Escrow Vault with Thread-Safe Locking & Slashing
 *  - LINQ-Based Catalog Queries
 */

using System;
using System.Collections.Generic;
using System.Linq;

namespace RentSphere.Core
{
    // ========================================================================
    // TIER 1 - TIER 5 INHERITANCE HIERARCHY
    // ========================================================================
    public abstract class AbstractDomainEntity
    {
        public string Id { get; }
        public long CreatedAt { get; }

        protected AbstractDomainEntity(string id)
        {
            Id = id;
            CreatedAt = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();
        }

        public abstract string SerializeToPipe();
    }

    public class RentableAsset : AbstractDomainEntity, ICloneable
    {
        public string Title { get; }
        public string OwnerId { get; }
        public string RenterId { get; set; } = string.Empty;
        public double BaseDailyRate { get; }
        public double SecurityCollateral { get; }
        public string LockerCoordinate { get; }
        public string State { get; set; } = "AVAILABLE";
        public List<string> MaintenanceLogs { get; } = new List<string>();

        public RentableAsset(string id, string title, string ownerId, double baseDailyRate, 
                             double securityCollateral, string lockerCoordinate)
            : base(id)
        {
            Title = title;
            OwnerId = ownerId;
            BaseDailyRate = baseDailyRate;
            SecurityCollateral = securityCollateral;
            LockerCoordinate = lockerCoordinate;
        }

        public virtual double CalculateDailyTariff(int rentalDays) => BaseDailyRate * rentalDays;
        public virtual double CalculateRequiredCollateral() => SecurityCollateral;
        public virtual double CalculateDamagePenalty(double damageSeverityPct) 
            => SecurityCollateral * (damageSeverityPct / 100.0);

        public object Clone()
        {
            var clone = (RentableAsset)MemberwiseClone();
            // Shallow clone shares list reference; deep clone allocates new list
            return clone;
        }

        public RentableAsset DeepClone()
        {
            var clone = (RentableAsset)MemberwiseClone();
            clone.MaintenanceLogs.Clear();
            clone.MaintenanceLogs.AddRange(this.MaintenanceLogs);
            return clone;
        }

        public override string SerializeToPipe() => $"{Id}|{Title}|{OwnerId}|{State}|{BaseDailyRate}";
    }

    public class SectoralAsset : RentableAsset
    {
        public string Sector { get; } // "TOURISM" or "AGRICULTURE"
        public double SeasonalDemandMultiplier { get; }

        public SectoralAsset(string id, string title, string ownerId, double baseDailyRate,
                             double securityCollateral, string lockerCoordinate, string sector, double multiplier)
            : base(id, title, ownerId, baseDailyRate, securityCollateral, lockerCoordinate)
        {
            Sector = sector;
            SeasonalDemandMultiplier = multiplier;
        }

        public override double CalculateDailyTariff(int rentalDays) 
            => BaseDailyRate * SeasonalDemandMultiplier * rentalDays;
    }

    public class TourismAsset : SectoralAsset
    {
        public string IpRating { get; }
        public double SaltwaterCorrosionFactor { get; }

        public TourismAsset(string id, string title, string ownerId, double baseDailyRate,
                            double securityCollateral, string lockerCoordinate, double multiplier,
                            string ipRating, double corrosionFactor)
            : base(id, title, ownerId, baseDailyRate, securityCollateral, lockerCoordinate, "TOURISM", multiplier)
        {
            IpRating = ipRating;
            SaltwaterCorrosionFactor = corrosionFactor;
        }
    }

    public class AgricultureAsset : SectoralAsset
    {
        public string SoilType { get; }
        public double PowerKw { get; }
        public bool IsHeavyMachinery { get; }

        public AgricultureAsset(string id, string title, string ownerId, double baseDailyRate,
                                double securityCollateral, string lockerCoordinate, double multiplier,
                                string soilType, double powerKw, bool isHeavy)
            : base(id, title, ownerId, baseDailyRate, securityCollateral, lockerCoordinate, "AGRICULTURE", multiplier)
        {
            SoilType = soilType;
            PowerKw = powerKw;
            IsHeavyMachinery = isHeavy;
        }
    }

    public class AutonomousMarineDrone : TourismAsset
    {
        public double MaxDepthMeters { get; }
        public bool SonarEquipped { get; }

        public AutonomousMarineDrone(string id, string title, string ownerId, double baseDailyRate,
                                     double securityCollateral, string lockerCoordinate, double multiplier,
                                     string ipRating, double corrosionFactor, double depth, bool sonar)
            : base(id, title, ownerId, baseDailyRate, securityCollateral, lockerCoordinate, multiplier, ipRating, corrosionFactor)
        {
            MaxDepthMeters = depth;
            SonarEquipped = sonar;
        }

        public override double CalculateDailyTariff(int rentalDays)
        {
            double depthSurcharge = 1.0 + (MaxDepthMeters / 500.0);
            return BaseDailyRate * SeasonalDemandMultiplier * depthSurcharge * rentalDays;
        }
    }

    // ========================================================================
    // DOUBLE-ENTRY ESCROW VAULT SUBSYSTEM
    // ========================================================================
    public class EscrowAccount
    {
        public string UserId { get; }
        public string UserName { get; }
        public double WalletBalance { get; set; }
        public double LockedEscrow { get; set; }
        public double EarnedRevenue { get; set; }

        public EscrowAccount(string userId, string userName, double balance)
        {
            UserId = userId;
            UserName = userName;
            WalletBalance = balance;
        }
    }

    public class EscrowVault
    {
        private readonly Dictionary<string, EscrowAccount> _accounts = new();
        private readonly object _lock = new();

        public void RegisterAccount(string userId, string userName, double balance)
        {
            lock (_lock)
            {
                _accounts[userId] = new EscrowAccount(userId, userName, balance);
            }
        }

        public EscrowAccount GetAccount(string userId)
        {
            lock (_lock)
            {
                return _accounts.TryGetValue(userId, out var acc) ? acc : null;
            }
        }

        // Lock funds on reservation
        public bool LockEscrow(string renterId, string ownerId, double rentalTariff, double securityCollateral)
        {
            lock (_lock)
            {
                if (!_accounts.TryGetValue(renterId, out var renter)) return false;
                double total = rentalTariff + securityCollateral;
                if (renter.WalletBalance < total) return false;

                renter.WalletBalance -= total;
                renter.LockedEscrow += total;
                Console.WriteLine($"[ESCROW LOCKED] User: {renter.UserName} | Locked: ${total:F2} (Tariff: ${rentalTariff:F2}, Deposit: ${securityCollateral:F2})");
                return true;
            }
        }

        // Release funds upon clean inspection approval
        public bool ReleasePayoutClean(string renterId, string ownerId, double rentalTariff, double securityCollateral)
        {
            lock (_lock)
            {
                if (!_accounts.TryGetValue(renterId, out var renter) || !_accounts.TryGetValue(ownerId, out var owner))
                    return false;

                double total = rentalTariff + securityCollateral;
                renter.LockedEscrow -= total;
                renter.WalletBalance += securityCollateral; // Refund deposit to renter
                owner.WalletBalance += rentalTariff;       // Pay rental fee to owner
                owner.EarnedRevenue += rentalTariff;

                Console.WriteLine($"[ESCROW RELEASE CLEAN] Refunded Deposit ${securityCollateral:F2} to {renter.UserName} | Paid Fee ${rentalTariff:F2} to {owner.UserName}");
                return true;
            }
        }

        // Slash collateral upon damage flagged
        public bool SlashCollateral(string renterId, string ownerId, double rentalTariff, double securityCollateral, double damagePenalty)
        {
            lock (_lock)
            {
                if (!_accounts.TryGetValue(renterId, out var renter) || !_accounts.TryGetValue(ownerId, out var owner))
                    return false;

                double total = rentalTariff + securityCollateral;
                renter.LockedEscrow -= total;

                double penalty = Math.Min(damagePenalty, securityCollateral);
                double refundedDeposit = securityCollateral - penalty;

                renter.WalletBalance += refundedDeposit;
                owner.WalletBalance += (rentalTariff + penalty);
                owner.EarnedRevenue += (rentalTariff + penalty);

                Console.WriteLine($"[ESCROW SLASHED] Slashed ${penalty:F2} damage penalty awarded to {owner.UserName} | Refunded ${refundedDeposit:F2} to {renter.UserName}");
                return true;
            }
        }
    }

    // ========================================================================
    // C# DEMO HARNESS
    // ========================================================================
    public static class Program
    {
        public static void Main()
        {
            Console.WriteLine("=========================================================");
            Console.WriteLine("  RentSphere Core Engine - C# Reference Verification");
            Console.WriteLine("=========================================================");

            var vault = new EscrowVault();
            vault.RegisterAccount("host_faaris", "Mohammed Faaris Khan (Host)", 5000.0);
            vault.RegisterAccount("renter_ketan", "Ketan Naik (Renter)", 8000.0);

            var drone = new AutonomousMarineDrone(
                "AST-001", "Goa Coastal Deep-Survey Drone", "host_faaris",
                1200.0, 3000.0, "F2", 1.25, "IP68", 1.15, 150.0, true
            );

            double tariff = drone.CalculateDailyTariff(2);
            double collateral = drone.CalculateRequiredCollateral();

            Console.WriteLine($"Asset: {drone.Title} | 2-Day Tariff: ${tariff:F2} | Collateral: ${collateral:F2}");

            // Lock Escrow
            vault.LockEscrow("renter_ketan", "host_faaris", tariff, collateral);

            // Simulate Quarantine Return and Slashing 20% Damage
            double damagePenalty = drone.CalculateDamagePenalty(20.0);
            vault.SlashCollateral("renter_ketan", "host_faaris", tariff, collateral, damagePenalty);

            var renter = vault.GetAccount("renter_ketan");
            var host = vault.GetAccount("host_faaris");
            Console.WriteLine($"Final Balances -> Renter: ${renter.WalletBalance:F2} | Host: ${host.WalletBalance:F2}");
        }
    }
}
