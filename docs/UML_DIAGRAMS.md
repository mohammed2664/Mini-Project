# RentSphere Formal UML Architecture Specifications
**Project**: RentSphere: Decentralized P2P Asset Rental & 8x8 Micro-Storage Engine  
**Institution**: Goa University — Department of Computer Science & Technology  
**Program**: Master of Computer Applications (MCA) — Object-Oriented Technology Lab (Assignment 3)  
**Author**: Nikhil Bhaiju & Mohammed Faaris Khan  
**Diagram Format**: Mermaid.js (Standard Markdown Embedded)

---

## 1. Use Case Diagram
Models platform actors (Renter, Equipment Host, Platform Inspector, WAL Engine) and their system interactions.

```mermaid
flowchart LR
    subgraph RentSphere System Boundary
        UC1(["Browse & Filter 5-Tier Asset Catalog"])
        UC2(["Reserve Asset with Fail-Fast Pipeline"])
        UC3(["Lock Escrow & Generate Locker OTP"])
        UC4(["Unlock Locker & Complete Pickup"])
        UC5(["Return Gear to Mandatory Quarantine Bay"])
        UC6(["Inspect Gear & Approve Clean Return"])
        UC7(["Flag Damage & Slash Collateral"])
        UC8(["Onboard New Asset in 8x8 Grid"])
        UC9(["Live RTTI Reflection & Memory Benchmark"])
        UC10(["NIRF Composite Rank Simulation"])
        UC11(["Append-Only Flat File WAL Audit"])
    end

    Renter(["🧑‍💼 Renter"])
    Host(["👨‍🌾 Equipment Host"])
    Inspector(["🛡️ Platform Inspector"])
    WAL(["⚙️ WAL Persistence Daemon"])

    Renter --> UC1
    Renter --> UC2
    Renter --> UC4
    Renter --> UC5
    Renter --> UC9
    Renter --> UC10

    Host --> UC8
    Host --> UC6
    Host --> UC7

    Inspector --> UC6
    Inspector --> UC7

    UC2 -.->|includes| UC3
    UC2 -.->|triggers| UC11
    UC5 -.->|triggers| UC11
    UC6 -.->|triggers| UC11
    UC7 -.->|triggers| UC11
    WAL --- UC11
```

---

## 2. 5-Tier Object-Oriented Class Hierarchy Diagram
Models the complete 5-tier inheritance architecture, GoF state composition, and escrow/locker associations.

```mermaid
classDiagram
    %% Tier 1
    class AbstractDomainEntity {
        <<Abstract>>
        #String id
        #long createdAt
        #long updatedAt
        +getId() String
        +getCreatedAt() long
        +serializeToPipe()* String
    }

    %% Tier 2
    class RentableAsset {
        #String title
        #String ownerId
        #String renterId
        #double baseDailyRate
        #double securityCollateral
        #String lockerCoordinate
        #LeaseState currentState
        #List~String~ maintenanceLogs
        +calculateDailyRentalTariff(int days)* double
        +calculateRequiredCollateral()* double
        +calculateDamageSlashingPenalty(double pct)* double
        +shallowClone() RentableAsset
        +deepClone() RentableAsset
        +setState(LeaseState state) void
    }

    %% Tier 3
    class SectoralAsset {
        #String sector
        #double seasonalDemandMultiplier
        #String insuranceTier
        +getSector() String
        +getSeasonalMultiplier() double
    }

    %% Tier 4
    class TourismAsset {
        #String waterproofIpRating
        #double saltwaterCorrosionFactor
        +getIpRating() String
        +getCorrosionFactor() double
    }

    class AgricultureAsset {
        #String compatibleSoilType
        #double powerKwRating
        #boolean heavyMachineryFlag
        +isHeavyMachinery() boolean
        +getPowerKw() double
    }

    %% Tier 5 Concrete Leaves
    class AutonomousMarineDrone {
        -double maxDepthMeters
        -boolean sonarEquipped
        +calculateDailyRentalTariff(int days) double
        +calculateRequiredCollateral() double
    }

    class SubmersibleActionRig {
        -String lensStabilizationTier
        -boolean 8kRecordingCapable
        +calculateDailyRentalTariff(int days) double
    }

    class PrecisionRotaryTiller {
        -int bladeRpm
        -double workingWidthCm
        +calculateDailyRentalTariff(int days) double
    }

    class OpticalSoilSpectrometer {
        -int spectralBandsCount
        -double nitrogenAccuracyPct
        +calculateDailyRentalTariff(int days) double
    }

    %% GoF State Pattern
    class LeaseState {
        <<Interface>>
        +getStateName() String
        +canReserve() boolean
        +canPickup() boolean
        +canReturn() boolean
        +canApproveInspection() boolean
        +canSlashInspection() boolean
        +reserve(RentableAsset, String) void
        +pickup(RentableAsset) void
        +initiateReturn(RentableAsset) void
        +approveInspection(RentableAsset) void
        +slashCollateral(RentableAsset, double) void
    }

    class AvailableState
    class ReservedEscrowState
    class ActiveLeaseState
    class InspectionQuarantineState

    %% Inheritance Relationships
    AbstractDomainEntity <|-- RentableAsset
    RentableAsset <|-- SectoralAsset
    SectoralAsset <|-- TourismAsset
    SectoralAsset <|-- AgricultureAsset

    TourismAsset <|-- AutonomousMarineDrone
    TourismAsset <|-- SubmersibleActionRig

    AgricultureAsset <|-- PrecisionRotaryTiller
    AgricultureAsset <|-- OpticalSoilSpectrometer

    %% State Pattern Realizations & Association
    LeaseState <|.. AvailableState
    LeaseState <|.. ReservedEscrowState
    LeaseState <|.. ActiveLeaseState
    LeaseState <|.. InspectionQuarantineState
    RentableAsset o-- LeaseState : currentState
```

---

## 3. GoF 4-Stage Statechart Diagram (Mandatory Quarantine Invariant)
Models state transitions, showing that returning active gear **must always** transition to `INSPECTION_QUARANTINE` first. Returning directly to `AVAILABLE` is strictly forbidden.

```mermaid
stateDiagram-v2
    [*] --> AVAILABLE : Asset Onboarded in Locker Bay

    AVAILABLE --> RESERVED_ESCROW : 4-Gate Pipeline Passed (Gate 1..4)\n[Escrow Locked, OTP Generated]
    
    RESERVED_ESCROW --> AVAILABLE : Reservation Cancelled\n[Escrow Refunded]
    RESERVED_ESCROW --> ACTIVE_LEASE : Bay Unlocked via 6-Digit OTP\n[Gear Picked Up by Renter]

    %% CRITICAL INVARIANT: NEVER DIRECT TO AVAILABLE
    ACTIVE_LEASE --> INSPECTION_QUARANTINE : Return Deposited in Locker Bay\n[Mandatory Quarantine Gate Locked]

    note right of INSPECTION_QUARANTINE
        MANDATORY QUARANTINE INVARIANT:
        Assets can NEVER transition from ACTIVE_LEASE
        directly to AVAILABLE. Host/Inspector inspection
        is strictly required before collateral release.
    end note

    INSPECTION_QUARANTINE --> AVAILABLE : Clean Inspection Approved\n[Rental Fee to Host, Collateral to Renter]
    INSPECTION_QUARANTINE --> AVAILABLE : Damage Flagged & Slashed\n[Penalty Slashed to Host, Net Collateral to Renter]
```

---

## 4. 4-Gate Fail-Fast Verification Pipeline Sequence Diagram
Demonstrates the short-circuiting sequential execution across `KycIdentityGate`, `StateLegalityGate`, `EscrowSolvencyGate`, and `LockerSpatialGate`.

```mermaid
sequenceDiagram
    autonumber
    actor Renter as 🧑‍💼 Renter (Client)
    participant API as 🌐 REST API Gateway
    participant Pipe as ⚡ FailFastPipeline
    participant Gate1 as 🛡️ Gate 1: KycIdentityGate
    participant Gate2 as ⚖️ Gate 2: StateLegalityGate
    participant Gate3 as 💰 Gate 3: EscrowSolvencyGate
    participant Gate4 as 📍 Gate 4: LockerSpatialGate
    participant Vault as 🏦 EscrowVault
    participant State as 🔄 RentableAsset State
    participant WAL as 📝 audit_wal.txt

    Renter->>API: POST /api/assets/reserve {assetId, renterId, days, bay}
    API->>Pipe: verify(PipelineContext)

    Pipe->>Gate1: execute(ctx)
    alt KYC Missing OR Self-Rental
        Gate1-->>Pipe: GateResult: FAIL (Gate 1)
        Pipe-->>API: Short-Circuit Abort
        API-->>Renter: HTTP 400 "Gate 1 Violation"
    else KYC OK & Renter != Owner
        Gate1-->>Pipe: GateResult: PASS
    end

    Pipe->>Gate2: execute(ctx)
    alt Asset Not in AVAILABLE State
        Gate2-->>Pipe: GateResult: FAIL (Gate 2)
        Pipe-->>API: Short-Circuit Abort
        API-->>Renter: HTTP 400 "Gate 2 Illegal State"
    else State == AVAILABLE
        Gate2-->>Pipe: GateResult: PASS
    end

    Pipe->>Gate3: execute(ctx)
    alt Wallet < Tariff + Collateral
        Gate3-->>Pipe: GateResult: FAIL (Gate 3)
        Pipe-->>API: Short-Circuit Abort
        API-->>Renter: HTTP 400 "Gate 3 Insolvent"
    else Wallet Sufficient
        Gate3-->>Pipe: GateResult: PASS
    end

    Pipe->>Gate4: execute(ctx)
    alt Bay Occupied OR Heavy-Agri Not in Row 7 (H)
        Gate4-->>Pipe: GateResult: FAIL (Gate 4)
        Pipe-->>API: Short-Circuit Abort
        API-->>Renter: HTTP 400 "Gate 4 Spatial Violation"
    else Bay Valid & Online
        Gate4-->>Pipe: GateResult: PASS
    end

    Pipe-->>API: All Gates Passed (Verdicts: 1..4 PASS)
    API->>Vault: lockEscrow(renter, owner, tariff, collateral)
    API->>State: reserve(asset, renterId)
    API->>WAL: appendLog("RESERVE", assetId, "RESERVED_ESCROW")
    API-->>Renter: HTTP 200 {status: "RESERVED", otp: "648291"}
```

---

## 5. 8x8 Smart Locker Bay Allocation Activity Diagram
Models physical bay sizing, coordinate computation, and Row 7 Heavy-Agri rule enforcement.

```mermaid
flowchart TD
    Start([Start: Asset Allocation Request]) --> SizingCheck{Determine Equipment Category}
    
    SizingCheck -->|Tourism Small: Cameras & Spectrometers| SizeS[Target Tier: SMALL Rows A-C]
    SizingCheck -->|Tourism Medium: Drones & Accessories| SizeM[Target Tier: MEDIUM Rows D-E]
    SizingCheck -->|Heavy Tourism / Marine Rigs| SizeL[Target Tier: LARGE Rows F-G]
    SizingCheck -->|Heavy Agricultural Machinery| SizeXL[Target Tier: XL_HEAVY_AGRI Row H]

    SizeS --> CheckCoord{Verify Target Bay [r, c]}
    SizeM --> CheckCoord
    SizeL --> CheckCoord
    SizeXL --> CheckCoord

    CheckCoord --> HeavyCheck{Is Asset Heavy Agri?}
    HeavyCheck -->|Yes| RowCheck{Is Row == 7 (Row H)?}
    HeavyCheck -->|No| OccupiedCheck{Is Target Bay Occupied?}

    RowCheck -->|No (Row A-G)| RejectHeavy[Abort: Gate 4 Heavy-Agri Spatial Violation]
    RowCheck -->|Yes (Row H)| OccupiedCheck

    OccupiedCheck -->|Yes| RejectOccupied[Abort: Bay Already In Use]
    OccupiedCheck -->|No| AssignBay[Assign Asset to Bay [r, c]]

    AssignBay --> GenerateOTP[Generate 6-Digit Random Unlock OTP]
    GenerateOTP --> SetTelemetry[Initialize Sensors: 24.5C, 58% Hum, Weight Kg]
    SetTelemetry --> CommitFile[Flush Bay State to data/locker_grid_8x8.txt]
    CommitFile --> End([Allocation Complete])

    RejectHeavy --> EndFail([Allocation Terminated])
    RejectOccupied --> EndFail
```
