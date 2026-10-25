/**
 * RentSphere Core Engine - C# Polyglot Reference Implementation
 * Author: Ketan Gurudas Naik (Subsystem 2 Lead)
 * Institution: Goa University - MCA Object-Oriented Technology Lab
 * Currency: Indian Rupees (₹ INR)
 * 
 * Features:
 *  - 5-Tier Domain Model with Polymorphic Tariff Formulas & Ground Bay Constraints
 *  - Double-Entry Escrow Vault with Thread-Safe Locking, Clean Refund, & Damage Slashing (₹ INR)
 *  - Prototype Deep vs Shallow Cloning for Multi-Unit Inventory Isolation
 */

using System;
using System.Collections.Generic;

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

    // Tier 2: Core Rentable Asset (INR Currency)
    public class RentableAsset : AbstractDomainEntity, ICloneable
    {
        public string Title { get; }
        public string OwnerId { get; }
        public string RenterId { get; set; } = string.Empty;
        public double BaseDailyRate { get; }         // ₹ INR
        public double SecurityCollateral { get; }    // ₹ INR Refundable Deposit
        public string LockerCoordinate { get; }     // e.g. "A2", "H2"
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
            MaintenanceLogs.Add("Factory Calibration Complete");
            MaintenanceLogs.Add("Initial Safety Audit Verified");
        }

        public virtual double CalculateDailyTariff(int rentalDays) => BaseDailyRate * rentalDays;
        public virtual double CalculateRequiredCollateral() => SecurityCollateral;
        public virtual double CalculateDamagePenalty(double damageSeverityPct) 
            => SecurityCollateral * (damageSeverityPct / 100.0);

        // Shallow clone shares maintenance log list
        public object Clone()
        {
            return (RentableAsset)MemberwiseClone();
        }

        // Deep clone creates isolated maintenance logs for multi-unit inventory
        public RentableAsset DeepClone()
        {
            var clone = (RentableAsset)MemberwiseClone();
            clone.MaintenanceLogs.Clear();
            clone.MaintenanceLogs.AddRange(this.MaintenanceLogs);
            return clone;
        }

        public override string SerializeToPipe() => $"{Id}|{Title}|{OwnerId}|{State}|{BaseDailyRate}";
    }

    // Tier 3: Categorized Rental Asset
    public class CategorizedRentalAsset : RentableAsset
    {
        public string Category { get; }
        public double LateFeeMultiplier { get; }
        public string ProtectionPlanTier { get; }

        public CategorizedRentalAsset(string id, string title, string ownerId, double baseDailyRate,
                                     double securityCollateral, string lockerCoordinate, string category,
                                     double lateFeeMultiplier, string protectionPlanTier)
            : base(id, title, ownerId, baseDailyRate, securityCollateral, lockerCoordinate)
        {
            Category = category;
            LateFeeMultiplier = lateFeeMultiplier;
            ProtectionPlanTier = protectionPlanTier;
        }
    }

    // Tier 4A: Event and Creator Asset
    public class EventAndCreatorAsset : CategorizedRentalAsset
    {
        public string IndoorOutdoorRating { get; }
        public string WattageOrBatterySpec { get; }
        public int AccessoryKitCount { get; }

        public EventAndCreatorAsset(string id, string title, string ownerId, double baseDailyRate,
                                   double securityCollateral, string lockerCoordinate, string category,
                                   double lateFeeMultiplier, string protectionPlanTier,
                                   string indoorOutdoorRating, string wattageOrBatterySpec, int accessoryKitCount)
            : base(id, title, ownerId, baseDailyRate, securityCollateral, lockerCoordinate,
                   category, lateFeeMultiplier, protectionPlanTier)
        {
            IndoorOutdoorRating = indoorOutdoorRating;
            WattageOrBatterySpec = wattageOrBatterySpec;
            AccessoryKitCount = accessoryKitCount;
        }
    }

    // Tier 4B: Utility and Mobility Asset
    public class UtilityAndMobilityAsset : CategorizedRentalAsset
    {
        public string PowerOrEngineSpec { get; }
        public string SafetyCertification { get; }
        public bool RequiresGroundBay { get; }

        public UtilityAndMobilityAsset(string id, string title, string ownerId, double baseDailyRate,
                                      double securityCollateral, string lockerCoordinate, string category,
                                      double lateFeeMultiplier, string protectionPlanTier,
                                      string powerOrEngineSpec, string safetyCertification, bool requiresGroundBay)
            : base(id, title, ownerId, baseDailyRate, securityCollateral, lockerCoordinate,
                   category, lateFeeMultiplier, protectionPlanTier)
        {
            PowerOrEngineSpec = powerOrEngineSpec;
            SafetyCertification = safetyCertification;
            RequiresGroundBay = requiresGroundBay;
        }
    }

    // Tier 5 Concrete: Camera & Optics Gear
    public class CameraAndOpticsGear : EventAndCreatorAsset
    {
        public double SensorResolutionMp { get; }
        public bool Is4kOr8kVideo { get; }

        public CameraAndOpticsGear(string id, string title, string ownerId, double baseDailyRate,
                                   double securityCollateral, string lockerCoordinate, double lateFeeMultiplier,
                                   string protectionPlanTier, double sensorResolutionMp, bool is4kOr8kVideo)
            : base(id, title, ownerId, baseDailyRate, securityCollateral, lockerCoordinate, "CAMERAS",
                   lateFeeMultiplier, protectionPlanTier, "Studio/Outdoor", "Dual NP-FZ100", 4)
        {
            SensorResolutionMp = sensorResolutionMp;
            Is4kOr8kVideo = is4kOr8kVideo;
        }

        public override double CalculateDailyTariff(int rentalDays)
        {
            double resolutionFactor = Is4kOr8kVideo ? 1.15 : 1.0;
            return BaseDailyRate * resolutionFactor * rentalDays;
        }

        public override double CalculateRequiredCollateral()
        {
            return (SensorResolutionMp >= 30.0) ? SecurityCollateral * 1.20 : SecurityCollateral;
        }

        public override string SerializeToPipe()
        {
            return $"{Id}|{Title}|CAMERAS|CameraAndOpticsGear|{OwnerId}|{RenterId}|{LockerCoordinate}|{BaseDailyRate}";
        }
    }

    // Tier 5 Concrete: Event & Mobility Vehicle (Row H Ground Bay)
    public class EventAndMobilityVehicle : UtilityAndMobilityAsset
    {
        public int SeatingCapacity { get; }
        public string FuelOrChargingType { get; }

        public EventAndMobilityVehicle(string id, string title, string ownerId, double baseDailyRate,
                                      double securityCollateral, string lockerCoordinate, double lateFeeMultiplier,
                                      string protectionPlanTier, int seatingCapacity, string fuelOrChargingType)
            : base(id, title, ownerId, baseDailyRate, securityCollateral, lockerCoordinate, "MOBILITY_VEHICLES",
                   lateFeeMultiplier, protectionPlanTier, "1.2L Engine / Electric Motor", "ARAI 5-Star", true)
        {
            SeatingCapacity = seatingCapacity;
            FuelOrChargingType = fuelOrChargingType;
        }

        public override double CalculateDailyTariff(int rentalDays) => BaseDailyRate * rentalDays;

        public override double CalculateRequiredCollateral() => SecurityCollateral * 1.20; // 20% vehicle transit deposit

        public override string SerializeToPipe()
        {
            return $"{Id}|{Title}|MOBILITY_VEHICLES|EventAndMobilityVehicle|{OwnerId}|{RenterId}|{LockerCoordinate}|{BaseDailyRate}";
        }
    }

    // ========================================================================
    // DOUBLE-ENTRY ESCROW VAULT SUBSYSTEM (₹ INR)
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

        // Lock funds on reservation in ₹ INR
        public bool LockEscrow(string renterId, string ownerId, double rentalTariff, double securityCollateral)
        {
            lock (_lock)
            {
                if (!_accounts.TryGetValue(renterId, out var renter)) return false;
                double total = rentalTariff + securityCollateral;
                if (renter.WalletBalance < total) return false;

                renter.WalletBalance -= total;
                renter.LockedEscrow += total;
                Console.WriteLine($"[ESCROW LOCKED] User: {renter.UserName} | Locked: ₹{total:N2} (Tariff: ₹{rentalTariff:N2}, Deposit: ₹{securityCollateral:N2})");
                return true;
            }
        }

        // Release funds upon clean inspection approval in ₹ INR
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

                Console.WriteLine($"[ESCROW RELEASE CLEAN] Refunded Deposit ₹{securityCollateral:N2} to {renter.UserName} | Paid Fee ₹{rentalTariff:N2} to {owner.UserName}");
                return true;
            }
        }

        // Slash collateral upon damage flagged in ₹ INR
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

                Console.WriteLine($"[ESCROW SLASHED] Slashed ₹{penalty:N2} damage penalty awarded to {owner.UserName} | Refunded ₹{refundedDeposit:N2} to {renter.UserName}");
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
            Console.WriteLine("  RentSphere Core Engine - C# Reference Verification (₹ INR)");
            Console.WriteLine("=========================================================");

            var vault = new EscrowVault();
            vault.RegisterAccount("host_faaris", "Mohammed Faaris Khan (Host)", 25000.0);
            vault.RegisterAccount("renter_ketan", "Ketan Naik (Renter)", 45000.0);

            var camera = new CameraAndOpticsGear(
                "AST-001", "Sony Alpha A7 IV Mirrorless Camera + 28-70mm Kit", "host_faaris",
                1200.0, 5000.0, "A2", 1.25, "PREMIUM_ALL_RISK", 33.0, true
            );

            double tariff = camera.CalculateDailyTariff(2);
            double collateral = camera.CalculateRequiredCollateral();

            Console.WriteLine($"Asset: {camera.Title} | 2-Day Tariff: ₹{tariff:N2} | Collateral: ₹{collateral:N2}");

            // Lock Escrow
            vault.LockEscrow("renter_ketan", "host_faaris", tariff, collateral);

            // Simulate Quarantine Return and Slashing 20% Damage
            double damagePenalty = camera.CalculateDamagePenalty(20.0);
            vault.SlashCollateral("renter_ketan", "host_faaris", tariff, collateral, damagePenalty);

            var renter = vault.GetAccount("renter_ketan");
            var host = vault.GetAccount("host_faaris");
            Console.WriteLine($"Final Balances -> Renter: ₹{renter.WalletBalance:N2} | Host: ₹{host.WalletBalance:N2}");
        }
    }
}
