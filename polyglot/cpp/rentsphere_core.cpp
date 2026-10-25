/**
 * RentSphere Core Engine - C++20 Polyglot Reference Implementation
 * Author: Mohammed Faaris Khan (Subsystem 1 Lead)
 * Institution: Goa University - MCA Object-Oriented Technology Lab
 * Currency: Indian Rupees (₹ INR)
 * 
 * Features:
 *  - 5-Tier Domain Inheritance Hierarchy (Realistic Local/Event Categories)
 *  - 4-Stage GoF State Pattern with Mandatory Quarantine Invariant
 *  - 4-Gate Fail-Fast Verification Pipeline (Row H Ground Slot Spatial Invariant)
 */

#include <iostream>
#include <string>
#include <vector>
#include <memory>
#include <chrono>
#include <iomanip>
#include <stdexcept>

namespace rentsphere {

// Tier 1: Abstract Base Domain Entity
class AbstractDomainEntity {
protected:
    std::string id;
    int64_t createdAt;
public:
    AbstractDomainEntity(std::string entityId) 
        : id(std::move(entityId)), 
          createdAt(std::chrono::duration_cast<std::chrono::milliseconds>(
              std::chrono::system_clock::now().time_since_epoch()).count()) {}
    virtual ~AbstractDomainEntity() = default;
    const std::string& getId() const { return id; }
    virtual std::string serializeToPipe() const = 0;
};

// Forward declaration of GoF LeaseState
class LeaseState;

// Tier 2: Rentable Asset with GoF State Context & INR Rates
class RentableAsset : public AbstractDomainEntity {
protected:
    std::string title;
    std::string ownerId;
    std::string renterId;
    double baseDailyRate;        // ₹ INR
    double securityCollateral;    // ₹ INR Refundable Deposit
    std::string lockerCoordinate;
    std::shared_ptr<LeaseState> state;
    std::vector<std::string> serviceLogs;

public:
    RentableAsset(std::string id, std::string title, std::string ownerId, 
                  double baseRate, double collateral, std::string lockerBay);
    virtual ~RentableAsset() = default;

    virtual double calculateDailyRentalTariff(int rentalDays) const {
        return baseDailyRate * rentalDays;
    }
    virtual double calculateRequiredCollateral() const {
        return securityCollateral;
    }
    virtual double calculateDamageSlashingPenalty(double damageSeverityPct) const {
        return securityCollateral * (damageSeverityPct / 100.0);
    }

    void setState(std::shared_ptr<LeaseState> newState) { state = newState; }
    std::shared_ptr<LeaseState> getState() const { return state; }
    const std::string& getOwnerId() const { return ownerId; }
    const std::string& getRenterId() const { return renterId; }
    void setRenterId(const std::string& rId) { renterId = rId; }
    const std::string& getLockerCoordinate() const { return lockerCoordinate; }
    const std::string& getTitle() const { return title; }
};

// Tier 3: Categorized Rental Asset
class CategorizedRentalAsset : public RentableAsset {
protected:
    std::string category; // "CAMERAS", "AUDIO_LIGHTING", "POWER_TOOLS", "MOBILITY_VEHICLES"
    double lateFeeMultiplier;
    std::string protectionTier;
public:
    CategorizedRentalAsset(std::string id, std::string title, std::string ownerId, 
                           double baseRate, double collateral, std::string lockerBay,
                           std::string cat, double multiplier, std::string protection)
        : RentableAsset(id, title, ownerId, baseRate, collateral, lockerBay),
          category(std::move(cat)), lateFeeMultiplier(multiplier), protectionTier(std::move(protection)) {}

    const std::string& getCategory() const { return category; }
};

// Tier 4A: Event and Creator Equipment
class EventAndCreatorAsset : public CategorizedRentalAsset {
protected:
    std::string indoorOutdoorRating;
    std::string wattageOrBatterySpec;
    int accessoryKitCount;
public:
    EventAndCreatorAsset(std::string id, std::string title, std::string ownerId, 
                         double baseRate, double collateral, std::string lockerBay,
                         std::string cat, double multiplier, std::string protection,
                         std::string rating, std::string wattage, int kitCount)
        : CategorizedRentalAsset(id, title, ownerId, baseRate, collateral, lockerBay, cat, multiplier, protection),
          indoorOutdoorRating(std::move(rating)), wattageOrBatterySpec(std::move(wattage)), accessoryKitCount(kitCount) {}
};

// Tier 4B: Utility and Mobility Equipment
class UtilityAndMobilityAsset : public CategorizedRentalAsset {
protected:
    std::string powerOrEngineSpec;
    std::string safetyCertification;
    bool requiresGroundBay;
public:
    UtilityAndMobilityAsset(std::string id, std::string title, std::string ownerId, 
                            double baseRate, double collateral, std::string lockerBay,
                            std::string cat, double multiplier, std::string protection,
                            std::string engine, std::string cert, bool groundBay)
        : CategorizedRentalAsset(id, title, ownerId, baseRate, collateral, lockerBay, cat, multiplier, protection),
          powerOrEngineSpec(std::move(engine)), safetyCertification(std::move(cert)), requiresGroundBay(groundBay) {}

    bool isRequiresGroundBay() const { return requiresGroundBay; }
};

// Tier 5: Concrete Leaf - Camera & Optics Gear
class CameraAndOpticsGear : public EventAndCreatorAsset {
private:
    double sensorResolutionMp;
    bool is4kOr8kVideo;
public:
    CameraAndOpticsGear(std::string id, std::string title, std::string ownerId, 
                        double baseRate, double collateral, std::string lockerBay,
                        double multiplier, std::string protection, double sensorMp, bool is4k)
        : EventAndCreatorAsset(id, title, ownerId, baseRate, collateral, lockerBay, "CAMERAS", multiplier, protection,
                               "Studio/Outdoor", "Dual NP-FZ100", 4),
          sensorResolutionMp(sensorMp), is4kOr8kVideo(is4k) {}

    double calculateDailyRentalTariff(int rentalDays) const override {
        double resolutionFactor = is4kOr8kVideo ? 1.15 : 1.0;
        return baseDailyRate * resolutionFactor * rentalDays;
    }
    double calculateRequiredCollateral() const override {
        return (sensorResolutionMp >= 30.0) ? securityCollateral * 1.20 : securityCollateral;
    }
    std::string serializeToPipe() const override {
        return id + "|" + title + "|CAMERAS|CameraAndOpticsGear|" + ownerId + "|" + 
               renterId + "|" + lockerCoordinate + "|" + std::to_string(baseDailyRate);
    }
};

// Tier 5: Concrete Leaf - Event & Mobility Vehicle (Row H Ground Slot)
class EventAndMobilityVehicle : public UtilityAndMobilityAsset {
private:
    int seatingCapacity;
    std::string fuelOrChargingType;
public:
    EventAndMobilityVehicle(std::string id, std::string title, std::string ownerId, 
                           double baseRate, double collateral, std::string lockerBay,
                           double multiplier, std::string protection, int seats, std::string fuel)
        : UtilityAndMobilityAsset(id, title, ownerId, baseRate, collateral, lockerBay, "MOBILITY_VEHICLES",
                                 multiplier, protection, "1.2L Engine", "ARAI 5-Star", true),
          seatingCapacity(seats), fuelOrChargingType(std::move(fuel)) {}

    double calculateDailyRentalTariff(int rentalDays) const override {
        return baseDailyRate * rentalDays;
    }
    double calculateRequiredCollateral() const override {
        return securityCollateral * 1.20; // 20% insurance deposit
    }
    std::string serializeToPipe() const override {
        return id + "|" + title + "|MOBILITY_VEHICLES|EventAndMobilityVehicle|" + ownerId + "|" + 
               renterId + "|" + lockerCoordinate + "|" + std::to_string(baseDailyRate);
    }
};

// ============================================================================
// 4-STAGE GoF STATE PATTERN
// ============================================================================
class LeaseState {
public:
    virtual ~LeaseState() = default;
    virtual std::string getStateName() const = 0;
    virtual bool canReserve() const { return false; }
    virtual bool canPickup() const { return false; }
    virtual bool canReturn() const { return false; }
    virtual bool canApproveInspection() const { return false; }
    virtual bool canSlashInspection() const { return false; }

    virtual void reserve(RentableAsset& ctx, const std::string& renterId) {
        throw std::runtime_error("Cannot reserve in state: " + getStateName());
    }
    virtual void pickup(RentableAsset& ctx) {
        throw std::runtime_error("Cannot pickup in state: " + getStateName());
    }
    virtual void initiateReturn(RentableAsset& ctx) {
        throw std::runtime_error("Cannot return in state: " + getStateName());
    }
    virtual void approveInspection(RentableAsset& ctx) {
        throw std::runtime_error("Cannot approve in state: " + getStateName());
    }
    virtual void slashInspection(RentableAsset& ctx, double penalty) {
        throw std::runtime_error("Cannot slash in state: " + getStateName());
    }
};

class AvailableState : public LeaseState {
public:
    std::string getStateName() const override { return "AVAILABLE"; }
    bool canReserve() const override { return true; }
    void reserve(RentableAsset& ctx, const std::string& renterId) override;
};

class ReservedEscrowState : public LeaseState {
public:
    std::string getStateName() const override { return "RESERVED_ESCROW"; }
    bool canPickup() const override { return true; }
    void pickup(RentableAsset& ctx) override;
};

class ActiveLeaseState : public LeaseState {
public:
    std::string getStateName() const override { return "ACTIVE_LEASE"; }
    bool canReturn() const override { return true; }
    void initiateReturn(RentableAsset& ctx) override;
};

class InspectionQuarantineState : public LeaseState {
public:
    std::string getStateName() const override { return "INSPECTION_QUARANTINE"; }
    bool canApproveInspection() const override { return true; }
    bool canSlashInspection() const override { return true; }
    void approveInspection(RentableAsset& ctx) override;
    void slashInspection(RentableAsset& ctx, double penalty) override;
};

inline void AvailableState::reserve(RentableAsset& ctx, const std::string& renterId) {
    ctx.setRenterId(renterId);
    ctx.setState(std::make_shared<ReservedEscrowState>());
    std::cout << "[STATE] " << ctx.getTitle() << " transitioned AVAILABLE -> RESERVED_ESCROW for " << renterId << std::endl;
}

inline void ReservedEscrowState::pickup(RentableAsset& ctx) {
    ctx.setState(std::make_shared<ActiveLeaseState>());
    std::cout << "[STATE] " << ctx.getTitle() << " transitioned RESERVED_ESCROW -> ACTIVE_LEASE (In-Field)" << std::endl;
}

inline void ActiveLeaseState::initiateReturn(RentableAsset& ctx) {
    ctx.setState(std::make_shared<InspectionQuarantineState>());
    std::cout << "[STATE INVARIANT] " << ctx.getTitle() << " returned to hub. MANDATORY QUARANTINE ACTIVE (Deposit held in ₹)!" << std::endl;
}

inline void InspectionQuarantineState::approveInspection(RentableAsset& ctx) {
    ctx.setRenterId("");
    ctx.setState(std::make_shared<AvailableState>());
    std::cout << "[STATE] Inspection approved clean. Full deposit refunded. Transitioned -> AVAILABLE" << std::endl;
}

inline void InspectionQuarantineState::slashInspection(RentableAsset& ctx, double penalty) {
    ctx.setRenterId("");
    ctx.setState(std::make_shared<AvailableState>());
    std::cout << "[STATE] Damage flagged. Deducted ₹" << penalty << " from deposit. Transitioned -> AVAILABLE" << std::endl;
}

inline RentableAsset::RentableAsset(std::string id, std::string title, std::string ownerId, 
                                   double baseRate, double collateral, std::string lockerBay)
    : AbstractDomainEntity(std::move(id)), title(std::move(title)), ownerId(std::move(ownerId)),
      baseDailyRate(baseRate), securityCollateral(collateral), lockerCoordinate(std::move(lockerBay)),
      state(std::make_shared<AvailableState>()) {}

// ============================================================================
// 4-GATE FAIL-FAST VERIFICATION PIPELINE
// ============================================================================
struct PipelineContext {
    std::shared_ptr<RentableAsset> asset;
    std::string renterId;
    std::string requestedAction;
    double renterWalletBalance;
    std::string targetLockerBay;
    bool isRenterKycVerified;
    int rentalDays;
};

struct GateResult {
    bool passed;
    int gateIndex;
    std::string gateName;
    std::string diagnostic;
    static GateResult success(int idx, std::string name) {
        return {true, idx, std::move(name), "Gate passed successfully"};
    }
    static GateResult failure(int idx, std::string name, std::string reason) {
        return {false, idx, std::move(name), std::move(reason)};
    }
};

class VerificationGate {
public:
    virtual ~VerificationGate() = default;
    virtual GateResult execute(const PipelineContext& ctx) = 0;
};

class KycIdentityGate : public VerificationGate {
public:
    GateResult execute(const PipelineContext& ctx) override {
        if (!ctx.isRenterKycVerified) {
            return GateResult::failure(1, "KycIdentityGate", "Renter KYC identity is not verified");
        }
        if (ctx.renterId == ctx.asset->getOwnerId()) {
            return GateResult::failure(1, "KycIdentityGate", "Anti-self-rental rule: Host cannot rent own item");
        }
        return GateResult::success(1, "KycIdentityGate");
    }
};

class StateLegalityGate : public VerificationGate {
public:
    GateResult execute(const PipelineContext& ctx) override {
        auto state = ctx.asset->getState();
        if (ctx.requestedAction == "RESERVE" && !state->canReserve()) {
            return GateResult::failure(2, "StateLegalityGate", "Item is not in AVAILABLE state (Current: " + state->getStateName() + ")");
        }
        return GateResult::success(2, "StateLegalityGate");
    }
};

class EscrowSolvencyGate : public VerificationGate {
public:
    GateResult execute(const PipelineContext& ctx) override {
        double tariff = ctx.asset->calculateDailyRentalTariff(ctx.rentalDays);
        double deposit = ctx.asset->calculateRequiredCollateral();
        double totalRequired = tariff + deposit;

        if (ctx.renterWalletBalance < totalRequired) {
            return GateResult::failure(3, "EscrowSolvencyGate", 
                "Insufficient funds: Required ₹" + std::to_string(totalRequired) + 
                ", but wallet has ₹" + std::to_string(ctx.renterWalletBalance));
        }
        return GateResult::success(3, "EscrowSolvencyGate");
    }
};

class LockerSpatialGate : public VerificationGate {
public:
    GateResult execute(const PipelineContext& ctx) override {
        if (ctx.targetLockerBay.empty()) {
            return GateResult::failure(4, "LockerSpatialGate", "Bay coordinate cannot be empty");
        }
        char rowChar = ctx.targetLockerBay[0];
        int rowIndex = rowChar - 'A';

        auto util = std::dynamic_pointer_cast<UtilityAndMobilityAsset>(ctx.asset);
        if (util && util->isRequiresGroundBay()) {
            if (rowIndex != 7) { // Row 7 corresponds to Row H Ground Slot
                return GateResult::failure(4, "LockerSpatialGate", 
                    "Ground Slot Spatial Invariant: Vehicle/Heavy gear must be parked in Row H (H1–H8). Specified: " + ctx.targetLockerBay);
            }
        }
        return GateResult::success(4, "LockerSpatialGate");
    }
};

class FailFastPipeline {
private:
    std::vector<std::unique_ptr<VerificationGate>> gates;
public:
    FailFastPipeline() {
        gates.push_back(std::make_unique<KycIdentityGate>());
        gates.push_back(std::make_unique<StateLegalityGate>());
        gates.push_back(std::make_unique<EscrowSolvencyGate>());
        gates.push_back(std::make_unique<LockerSpatialGate>());
    }

    GateResult verify(const PipelineContext& ctx) {
        for (const auto& gate : gates) {
            GateResult result = gate->execute(ctx);
            if (!result.passed) {
                std::cout << "[FAIL-FAST SHORT CIRCUIT] Gate " << result.gateIndex << " (" 
                          << result.gateName << ") FAILED: " << result.diagnostic << std::endl;
                return result;
            }
        }
        return GateResult::success(0, "AllGatesPassed");
    }
};

} // namespace rentsphere

int main() {
    std::cout << "=========================================================" << std::endl;
    std::cout << "  RentSphere Core Engine (INR ₹) - C++20 Reference" << std::endl;
    std::cout << "=========================================================" << std::endl;

    using namespace rentsphere;

    auto camera = std::make_shared<CameraAndOpticsGear>(
        "AST-101", "Sony Alpha A7 IV Kit", "host_faaris",
        1200.0, 5000.0, "A2", 1.20, "Creator Comprehensive", 33.0, true
    );

    std::cout << "[ITEM] " << camera->getTitle() << " | Daily Rent: ₹" 
              << camera->calculateDailyRentalTariff(1) << " | Deposit: ₹" 
              << camera->calculateRequiredCollateral() << std::endl;

    FailFastPipeline pipeline;
    PipelineContext ctx{
        camera, "renter_deepak", "RESERVE", 25000.0, "A2", true, 2
    };

    auto verdict = pipeline.verify(ctx);
    if (verdict.passed) {
        camera->getState()->reserve(*camera, ctx.renterId);
        camera->getState()->pickup(*camera);
        camera->getState()->initiateReturn(*camera);
        camera->getState()->approveInspection(*camera);
    }

    std::cout << "\nRentSphere C++20 Verification Complete." << std::endl;
    return 0;
}
