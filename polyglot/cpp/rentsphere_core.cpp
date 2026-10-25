/**
 * RentSphere Core Engine - C++20 Polyglot Reference Implementation
 * Author: Mohammed Faaris Khan (Subsystem 1 Lead)
 * Institution: Goa University - MCA Object-Oriented Technology Lab
 * 
 * Features:
 *  - 5-Tier Domain Inheritance Hierarchy
 *  - 4-Stage GoF State Pattern with Mandatory Quarantine Invariant
 *  - 4-Gate Fail-Fast Verification Pipeline
 */

#include <iostream>
#include <string>
#include <vector>
#include <memory>
#include <chrono>
#include <iomanip>
#include <stdexcept>

namespace rentsphere {

// ============================================================================
// ASSIGNMENT 2: 5-TIER DOMAIN INHERITANCE HIERARCHY
// ============================================================================

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

// Tier 2: Rentable Asset with GoF State Context
class RentableAsset : public AbstractDomainEntity {
protected:
    std::string title;
    std::string ownerId;
    std::string renterId;
    double baseDailyRate;
    double securityCollateral;
    std::string lockerCoordinate;
    std::shared_ptr<LeaseState> state;
    std::vector<std::string> maintenanceLogs;

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

// Tier 3: Sectoral Asset (Tourism vs Agriculture classification)
class SectoralAsset : public RentableAsset {
protected:
    std::string sector; // "TOURISM" or "AGRICULTURE"
    double seasonalDemandMultiplier;
public:
    SectoralAsset(std::string id, std::string title, std::string ownerId, 
                  double baseRate, double collateral, std::string lockerBay,
                  std::string sectorType, double multiplier)
        : RentableAsset(id, title, ownerId, baseRate, collateral, lockerBay),
          sector(std::move(sectorType)), seasonalDemandMultiplier(multiplier) {}

    double calculateDailyRentalTariff(int rentalDays) const override {
        return baseDailyRate * seasonalDemandMultiplier * rentalDays;
    }
    const std::string& getSector() const { return sector; }
};

// Tier 4A: Tourism Asset (Marine & Coastal Operations)
class TourismAsset : public SectoralAsset {
protected:
    std::string ipRating;
    double saltwaterCorrosionFactor;
public:
    TourismAsset(std::string id, std::string title, std::string ownerId, 
                 double baseRate, double collateral, std::string lockerBay,
                 double multiplier, std::string ip, double corrosionFactor)
        : SectoralAsset(id, title, ownerId, baseRate, collateral, lockerBay, "TOURISM", multiplier),
          ipRating(std::move(ip)), saltwaterCorrosionFactor(corrosionFactor) {}
};

// Tier 4B: Agriculture Asset (Soil & Farming Operations)
class AgricultureAsset : public SectoralAsset {
protected:
    std::string soilType;
    double powerKwRating;
    bool heavyMachineryFlag;
public:
    AgricultureAsset(std::string id, std::string title, std::string ownerId, 
                     double baseRate, double collateral, std::string lockerBay,
                     double multiplier, std::string soil, double kw, bool heavy)
        : SectoralAsset(id, title, ownerId, baseRate, collateral, lockerBay, "AGRICULTURE", multiplier),
          soilType(std::move(soil)), powerKwRating(kw), heavyMachineryFlag(heavy) {}

    bool isHeavyMachinery() const { return heavyMachineryFlag; }
};

// Tier 5: Concrete Leaf - Autonomous Marine Drone
class AutonomousMarineDrone : public TourismAsset {
private:
    double maxDepthMeters;
    bool sonarEquipped;
public:
    AutonomousMarineDrone(std::string id, std::string title, std::string ownerId, 
                          double baseRate, double collateral, std::string lockerBay,
                          double multiplier, std::string ip, double corrosionFactor,
                          double depth, bool sonar)
        : TourismAsset(id, title, ownerId, baseRate, collateral, lockerBay, multiplier, ip, corrosionFactor),
          maxDepthMeters(depth), sonarEquipped(sonar) {}

    double calculateDailyRentalTariff(int rentalDays) const override {
        double depthFactor = 1.0 + (maxDepthMeters / 500.0);
        return baseDailyRate * seasonalDemandMultiplier * depthFactor * rentalDays;
    }
    std::string serializeToPipe() const override {
        return id + "|" + title + "|TOURISM|AutonomousMarineDrone|" + ownerId + "|" + 
               renterId + "|" + lockerCoordinate + "|" + std::to_string(baseDailyRate);
    }
};

// Tier 5: Concrete Leaf - Precision Rotary Tiller (Heavy Agri)
class PrecisionRotaryTiller : public AgricultureAsset {
private:
    int bladeRpm;
    double workingWidthCm;
public:
    PrecisionRotaryTiller(std::string id, std::string title, std::string ownerId, 
                          double baseRate, double collateral, std::string lockerBay,
                          double multiplier, std::string soil, double kw,
                          int rpm, double widthCm)
        : AgricultureAsset(id, title, ownerId, baseRate, collateral, lockerBay, multiplier, soil, kw, true),
          bladeRpm(rpm), workingWidthCm(widthCm) {}

    double calculateDailyRentalTariff(int rentalDays) const override {
        double powerMultiplier = 1.0 + (powerKwRating / 100.0);
        return baseDailyRate * seasonalDemandMultiplier * powerMultiplier * rentalDays;
    }
    std::string serializeToPipe() const override {
        return id + "|" + title + "|AGRICULTURE|PrecisionRotaryTiller|" + ownerId + "|" + 
               renterId + "|" + lockerCoordinate + "|" + std::to_string(baseDailyRate);
    }
};

// ============================================================================
// ASSIGNMENT 2: 4-STAGE GoF STATE PATTERN & MANDATORY QUARANTINE INVARIANT
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

class AvailableState;
class ReservedEscrowState;
class ActiveLeaseState;
class InspectionQuarantineState;

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
    // MANDATORY QUARANTINE INVARIANT:
    // Returns MUST enter InspectionQuarantineState, NEVER AvailableState directly!
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

// Implementation of state transitions
inline void AvailableState::reserve(RentableAsset& ctx, const std::string& renterId) {
    ctx.setRenterId(renterId);
    ctx.setState(std::make_shared<ReservedEscrowState>());
    std::cout << "[STATE] " << ctx.getTitle() << " transitioned AVAILABLE -> RESERVED_ESCROW for " << renterId << std::endl;
}

inline void ReservedEscrowState::pickup(RentableAsset& ctx) {
    ctx.setState(std::make_shared<ActiveLeaseState>());
    std::cout << "[STATE] " << ctx.getTitle() << " transitioned RESERVED_ESCROW -> ACTIVE_LEASE (In-Use)" << std::endl;
}

inline void ActiveLeaseState::initiateReturn(RentableAsset& ctx) {
    // Enforcement of mandatory quarantine
    ctx.setState(std::make_shared<InspectionQuarantineState>());
    std::cout << "[STATE INVARIANT] " << ctx.getTitle() << " returned to locker. MANDATORY QUARANTINE active!" << std::endl;
}

inline void InspectionQuarantineState::approveInspection(RentableAsset& ctx) {
    ctx.setRenterId("");
    ctx.setState(std::make_shared<AvailableState>());
    std::cout << "[STATE] Inspection approved clean. Collateral released. Transitioned -> AVAILABLE" << std::endl;
}

inline void InspectionQuarantineState::slashInspection(RentableAsset& ctx, double penalty) {
    ctx.setRenterId("");
    ctx.setState(std::make_shared<AvailableState>());
    std::cout << "[STATE] Damage flagged. Slashed $" << penalty << " from escrow. Transitioned -> AVAILABLE" << std::endl;
}

inline RentableAsset::RentableAsset(std::string id, std::string title, std::string ownerId, 
                                   double baseRate, double collateral, std::string lockerBay)
    : AbstractDomainEntity(std::move(id)), title(std::move(title)), ownerId(std::move(ownerId)),
      baseDailyRate(baseRate), securityCollateral(collateral), lockerCoordinate(std::move(lockerBay)),
      state(std::make_shared<AvailableState>()) {}

// ============================================================================
// ASSIGNMENT 1: 4-GATE FAIL-FAST VERIFICATION PIPELINE
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

// Gate 1: KYC Identity Verification & Anti-Self-Rental Gate
class KycIdentityGate : public VerificationGate {
public:
    GateResult execute(const PipelineContext& ctx) override {
        if (!ctx.isRenterKycVerified) {
            return GateResult::failure(1, "KycIdentityGate", "Renter KYC identity is not verified");
        }
        if (ctx.renterId == ctx.asset->getOwnerId()) {
            return GateResult::failure(1, "KycIdentityGate", "Anti-self-rental rule: Owner cannot lease own asset");
        }
        return GateResult::success(1, "KycIdentityGate");
    }
};

// Gate 2: State Legality Gate
class StateLegalityGate : public VerificationGate {
public:
    GateResult execute(const PipelineContext& ctx) override {
        auto state = ctx.asset->getState();
        if (ctx.requestedAction == "RESERVE" && !state->canReserve()) {
            return GateResult::failure(2, "StateLegalityGate", "Asset is not in AVAILABLE state (Current: " + state->getStateName() + ")");
        }
        if (ctx.requestedAction == "PICKUP" && !state->canPickup()) {
            return GateResult::failure(2, "StateLegalityGate", "Asset is not in RESERVED_ESCROW state");
        }
        if (ctx.requestedAction == "RETURN" && !state->canReturn()) {
            return GateResult::failure(2, "StateLegalityGate", "Asset is not in ACTIVE_LEASE state");
        }
        return GateResult::success(2, "StateLegalityGate");
    }
};

// Gate 3: Escrow Solvency Gate
class EscrowSolvencyGate : public VerificationGate {
public:
    GateResult execute(const PipelineContext& ctx) override {
        double tariff = ctx.asset->calculateDailyRentalTariff(ctx.rentalDays);
        double deposit = ctx.asset->calculateRequiredCollateral();
        double totalRequired = tariff + deposit;

        if (ctx.renterWalletBalance < totalRequired) {
            return GateResult::failure(3, "EscrowSolvencyGate", 
                "Insufficient funds: Required $" + std::to_string(totalRequired) + 
                " (Tariff: $" + std::to_string(tariff) + " + Deposit: $" + std::to_string(deposit) + 
                "), but wallet has $" + std::to_string(ctx.renterWalletBalance));
        }
        return GateResult::success(3, "EscrowSolvencyGate");
    }
};

// Gate 4: Locker Spatial Gate & Row 7 Heavy-Agri Spatial Rule
class LockerSpatialGate : public VerificationGate {
public:
    GateResult execute(const PipelineContext& ctx) override {
        if (ctx.targetLockerBay.empty()) {
            return GateResult::failure(4, "LockerSpatialGate", "Locker bay coordinate cannot be empty");
        }
        char rowChar = ctx.targetLockerBay[0];
        int rowIndex = rowChar - 'A';
        if (rowIndex < 0 || rowIndex > 7) {
            return GateResult::failure(4, "LockerSpatialGate", "Invalid bay row: " + std::string(1, rowChar));
        }

        // Heavy-Agri spatial invariant
        auto agriAsset = std::dynamic_pointer_cast<AgricultureAsset>(ctx.asset);
        if (agriAsset && agriAsset->isHeavyMachinery()) {
            if (rowIndex != 7) { // Row 7 corresponds to 'H'
                return GateResult::failure(4, "LockerSpatialGate", 
                    "Heavy Agricultural Machinery violation: Must be located in Row H (Row 7). Specified: " + ctx.targetLockerBay);
            }
        }
        return GateResult::success(4, "LockerSpatialGate");
    }
};

// Sequential Fail-Fast Pipeline Orchestrator
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
                // Short-circuit on first failure
                std::cout << "[FAIL-FAST SHORT CIRCUIT] Gate " << result.gateIndex << " (" 
                          << result.gateName << ") FAILED: " << result.diagnostic << std::endl;
                return result;
            }
            std::cout << "[GATE PASS] Gate " << result.gateIndex << " (" << result.gateName << ") validated." << std::endl;
        }
        return GateResult::success(0, "AllGatesPassed");
    }
};

} // namespace rentsphere

// ============================================================================
// C++20 POLYGLOT TEST HARNESS
// ============================================================================
int main() {
    std::cout << "=========================================================" << std::endl;
    std::cout << "  RentSphere Core Engine - C++20 Reference Verification" << std::endl;
    std::cout << "=========================================================" << std::endl;

    using namespace rentsphere;

    // Create Tier 5 Leaf instance: Autonomous Marine Drone
    auto drone = std::make_shared<AutonomousMarineDrone>(
        "AST-001", "Goa Coastal Deep-Survey Drone", "host_faaris",
        1200.0, 3000.0, "F2", 1.25, "IP68", 1.15, 150.0, true
    );

    std::cout << "[ASSET] Created: " << drone->getTitle() << " | Daily Rate: $" 
              << drone->calculateDailyRentalTariff(1) << " | Collateral: $" 
              << drone->calculateRequiredCollateral() << std::endl;

    // Pipeline verification
    FailFastPipeline pipeline;
    PipelineContext ctx{
        drone,
        "renter_ketan",
        "RESERVE",
        5000.0, // Wallet balance
        "F2",
        true,   // KYC verified
        1       // 1 day rental
    };

    std::cout << "\n--- Testing Fail-Fast Pipeline (Valid Case) ---" << std::endl;
    auto verdict = pipeline.verify(ctx);
    if (verdict.passed) {
        drone->getState()->reserve(*drone, ctx.renterId);
        drone->getState()->pickup(*drone);
        // Test mandatory quarantine invariant
        drone->getState()->initiateReturn(*drone);
        drone->getState()->approveInspection(*drone);
    }

    std::cout << "\n--- Testing Gate 4 Short-Circuit (Heavy Agri Spatial Rule) ---" << std::endl;
    auto tiller = std::make_shared<PrecisionRotaryTiller>(
        "AST-003", "Mandovi Power Rotary Tiller 55HP", "host_nikhil",
        800.0, 2500.0, "C3", // Invalid Row: C (Row 2), heavy agri must be Row H (Row 7)
        1.10, "Laterite", 41.0, 2800, 180.0
    );

    PipelineContext invalidAgriCtx{
        tiller, "renter_deepak", "RESERVE", 10000.0, "C3", true, 2
    };
    pipeline.verify(invalidAgriCtx);

    std::cout << "\nRentSphere C++20 Core Verification Complete." << std::endl;
    return 0;
}
