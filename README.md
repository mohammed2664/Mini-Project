# RentSphere: Decentralized Peer-to-Peer (P2P) Asset Rental & 8x8 Micro-Storage Engine

[![Java Version](https://img.shields.io/badge/Java-SE%2017%2B%20%2F%2025-0D9488.svg)](https://openjdk.org/)
[![Architecture](https://img.shields.io/badge/Architecture-Zero--Database%20Pure%20OOP-0A5C4D.svg)](#zero-database-flat-file-persistence-engine)
[![Syllabus Assignments](https://img.shields.io/badge/OOT%20Lab%20Assignments-1%20through%205%20Verified-10B981.svg)](#syllabus-assignments-mapping)
[![Test Suite](https://img.shields.io/badge/Verification%20Tests-41%20%2F%2041%20PASSED-10B981.svg)](#running-automated-verification-tests)
[![Design System](https://img.shields.io/badge/Theme-Warm%20Horizon%20Marketplace-F59E0B.svg)](#design-system--ui-views)

**Department of Computer Science & Technology — Goa University**  
*Master of Computer Applications (MCA) — Object-Oriented Technology (OOT) Lab Mini-Project*  

---

## 1. Project Charter & Engineering Philosophy

**RentSphere** is an industry-grade, zero-database, pure Object-Oriented full-stack web application with an integrated 8x8 physical micro-storage grid engine. Built specifically to fulfill and exceed all five MCA OOT Lab syllabus assignments for Goa University, RentSphere demonstrates high-fidelity software engineering:

- **Zero-Database Rule**: Replaces opaque ORM bloat with thread-safe `ConcurrentHashMap` in-memory collections synchronized deterministically with human-readable, pipe-delimited flat files in `data/` (`assets_ledger.txt`, `locker_grid_8x8.txt`, `escrow_vault.txt`, `audit_wal.txt`).
- **Pure Java SE Execution**: Runs 100% cleanly using standard JDK built-in classes (`com.sun.net.httpserver.HttpServer`, `java.nio.file.*`, `java.lang.reflect.*`, `javax.swing.*`) with **zero external JAR downloads**.
- **Fail-Fast Defense**: State transitions and physical locker allocations are guarded by a 4-Gate Pipeline short-circuiting on the first invariant violation.
- **Mandatory Quarantine Invariant**: Returning an active lease triggers inspection quarantine before collateral release or asset relisting, preventing fraudulent gear returns.
- **Meta-OOP RTTI & Memory Verification**: Live Java reflection inspector exposes runtime class hierarchies, while deep vs. shallow cloning proves memory reference isolation using real `System.identityHashCode()`.
- **"Warm Horizon Marketplace" Aesthetics**: Modern, responsive web frontend with Primary Brand Teal (`#0D9488`, `#0A5C4D`), Warm Amber (`#F59E0B`), Emerald Green (`#10B981`), Quarantine Crimson (`#EF4444`), and Slate (`#64748B`).

---

## 2. Team Roster & Subsystem Ownership

| Student Lead | Subsystem & Role | Syllabus Assignments Covered | Key Deliverables |
| :--- | :--- | :--- | :--- |
| **Mohammed Faaris Khan** | **Subsystem 1: Core Behavioral Engine** | Assignment 1 (Pipeline Architecture) & Assignment 2 (State Pattern) | 4-Stage GoF `LeaseState` pattern, Mandatory Quarantine Invariant, 4-Gate `FailFastPipeline`, Polyglot C++20 Core (`polyglot/cpp/rentsphere_core.cpp`) |
| **Ketan Gurudas Naik** | **Subsystem 2: Escrow Vault & 5-Tier Domain Model** | Assignment 2 (5-Tier Inheritance Hierarchy & Polymorphic Escrow) | 5-Tier Domain Model (`model/`), Double-Entry `EscrowVault`, Dynamic Marketplace Catalog UI, Polyglot C# Core (`polyglot/csharp/RentSphereCore.cs`) |
| **Nikhil Bhaiju** | **Subsystem 3: 10-Point Meta-OOP & NIRF Portal** | Assignment 4 (Meta-OOP & Generics) & Assignment 3 (Formal UML) | `GenericRepository<T>`, Live RTTI `ReflectionInspector`, Deep vs. Shallow Cloner, NIRF Institutional Composite Score Portal, 5 Mermaid UML Diagrams, Polyglot Python 3.12 Core (`polyglot/python/rentsphere_core.py`) |
| **Deepak Dev** | **Subsystem 4: 8x8 Smart Locker Grid & Persistence** | Assignment 5 (Physical Grid Matrix & File Persistence) | 64-Bay `SmartLockerGrid8x8`, Standalone Java Swing Desktop Console (`SwingLockerConsole.java`), Row 7 Heavy-Agri Spatial Invariant, Deterministic `FlatFilePersistenceEngine` (`data/*.txt`) |

---

## 3. Syllabus Assignments Mapping

### Assignment 1: 4-Gate Fail-Fast Verification Pipeline (`pipeline/`)
Every state transition request (e.g. `RESERVE`, `PICKUP`, `RETURN`, `INSPECT_APPROVE`, `INSPECT_SLASH`) is packaged into a `PipelineContext` and submitted to `FailFastPipeline`. The pipeline executes four ordered gates:
1. **Gate 1 (`KycIdentityGate`)**: Verifies requester KYC verification and enforces the anti-self-rental rule (`renterId != asset.ownerId`).
2. **Gate 2 (`StateLegalityGate`)**: Queries `asset.getState().canTransitionTo(requestedAction)`. If illegal, halts immediately.
3. **Gate 3 (`EscrowSolvencyGate`)**: Computes required total funding `(Tariff + SecurityDeposit)` and verifies `wallet.balance >= Total`.
4. **Gate 4 (`LockerSpatialGate`)**: Checks target bay coordinates `[r, c]`, verifies bay is online/available, and enforces the **Row 7 Heavy-Agri Spatial Rule** (heavy agricultural machinery is barred from Rows 0–6 and must be stored in Row H / Row 7).

*Short-Circuiting*: If any gate fails, subsequent gates are aborted immediately, returning a structured `GateResult` with gate index, name, and exact failure reason.

### Assignment 2: 5-Tier Inheritance Hierarchy & GoF State Pattern (`model/` & `state/`)
The domain hierarchy strictly demonstrates 5 levels of OOP inheritance with polymorphic behavior:
- **Tier 1**: `AbstractDomainEntity` — encapsulates `String id`, `long createdAt`, `long updatedAt`, abstract pipe serialization.
- **Tier 2**: `RentableAsset` — encapsulates `LeaseState currentState`, `double baseDailyRate`, `double securityCollateral`, `String lockerCoordinate`, `List<String> maintenanceLogs`, implements `Cloneable`. Polymorphic tariff, collateral, and damage-slashing methods.
- **Tier 3**: `SectoralAsset` — adds `String sector` (`TOURISM` or `AGRICULTURE`), `double seasonalDemandMultiplier`, `String insuranceTier`.
- **Tier 4**: `TourismAsset` (waterproof IP rating, corrosion factor) and `AgricultureAsset` (compatible soil type, power kW, `heavyMachineryFlag`).
- **Tier 5**: Concrete leaf classes:
  1. `AutonomousMarineDrone`: Depth tariff multiplier: `baseRate * seasonalDemand * (1.0 + (depth / 500.0)) * days`.
  2. `SubmersibleActionRig`: 4K/8K stabilization surcharge.
  3. `PrecisionRotaryTiller`: Power kW factor: `baseRate * seasonalDemand * (1.0 + (powerKw / 100.0)) * days`. Heavy machinery flag locked to `true`.
  4. `OpticalSoilSpectrometer`: High-precision optical sensor bond collateral.

**GoF State Pattern & Mandatory Quarantine Invariant**:
- States: `AvailableState`, `ReservedEscrowState`, `ActiveLeaseState`, `InspectionQuarantineState`.
- **CRITICAL INVARIANT**: When an active lease ends, `ActiveLeaseState.initiateReturn()` transitions strictly to `InspectionQuarantineState`. The asset can NEVER transition directly back to `AvailableState`.
- While in `InspectionQuarantineState`:
  - Locker bay is marked `QUARANTINE_LOCKED`.
  - Escrow deposit remains held in `EscrowVault`.
  - Host/Inspector can choose:
    - **Option A (Approve Clean Inspection)**: Full collateral returned to renter, rental fee transferred to owner, asset transitions to `AvailableState`.
    - **Option B (Flag Damage & Slash Collateral)**: Polymorphic damage penalty calculated; slashed amount deducted from renter's deposit and awarded to owner for repairs; remaining collateral returned to renter; asset transitions to `AvailableState`.

### Assignment 3: Formal UML Architecture Specifications (`docs/UML_DIAGRAMS.md`)
Includes 5 comprehensive diagrams authored in clean Mermaid.js:
1. System Use Case Diagram (Renter, Equipment Host, Platform Inspector, WAL Engine).
2. 5-Tier Object-Oriented Class Hierarchy Diagram.
3. GoF 4-Stage Statechart Diagram featuring the **Mandatory Quarantine Invariant**.
4. 4-Gate Fail-Fast Verification Pipeline Sequence Diagram.
5. 8x8 Smart Locker Bay Allocation Activity Diagram.

### Assignment 4: Meta-OOP Workbench, Reflection & NIRF Analytics (`meta/`)
1. **Generic Repository (`GenericRepository<T extends AbstractDomainEntity>`)**: Parameterized thread-safe storage using `ConcurrentHashMap<String, T>` with CRUD operations and dynamic predicate wildcard filtering (`findWhere(Predicate<T>)`).
2. **Live RTTI Reflection Inspector (`ReflectionInspector`)**: Uses `java.lang.reflect` (`Class`, `Field`, `Method`, `Modifier`) to dynamically inspect live domain objects, exposing the full 5-tier inheritance chain, declared fields with runtime values, and declared method signatures to the web UI.
3. **Prototype Deep vs. Shallow Cloning Benchmark (`PrototypeCloner`)**: Implements `shallowClone()` (standard `super.clone()`) and `deepClone()` (recursively allocating a new `ArrayList<>(maintenanceLogs)`). Visually proves with real `System.identityHashCode()` memory addresses that shallow cloning shares mutable references while deep cloning isolates memory completely.
4. **NIRF Institutional Analytics Portal (`NirfAnalyticsPortal`)**: Computes official Ministry of Education composite ranking scores:
   $$\text{Score} = (\text{TLR} \times 0.30) + (\text{RP} \times 0.30) + (\text{GO} \times 0.20) + (\text{OI} \times 0.10) + (\text{PR} \times 0.10)$$
   Features a What-If policy simulation engine using deep-cloned institutional prototypes so policy experimentation never corrupts baseline records.

### Assignment 5: 8x8 Smart Locker Matrix, Swing GUI & Flat File Persistence (`locker/` & `persistence/`)
1. **8x8 Smart Micro-Storage Grid (`SmartLockerGrid8x8`)**:
   - Manages 64 physical bays arranged in an 8-row by 8-column matrix (`A1` through `H8`).
   - Sizing: Rows A–C (`SMALL`), Rows D–E (`MEDIUM`), Rows F–G (`LARGE`), Row H (`XL_HEAVY_AGRI`).
   - **Row 7 Heavy-Agri Spatial Rule**: Heavy agricultural machinery like `PrecisionRotaryTiller` must be stored exclusively in Row H (Row 7). Allocation in Rows A–G fails at Gate 4.
   - Live telemetry: Simulates internal temperature (°C), relative humidity (%), floor load cell weight (kg), and 6-digit random unlock OTP passcodes.
2. **Standalone Java Swing Desktop Console (`SwingLockerConsole.java`)**: Offline desktop backup application rendering a 64-button color-coded `GridLayout(8, 8)` with bay inspection dialogs for lab examiners.
3. **Deterministic Flat File Persistence Engine (`FlatFilePersistenceEngine`)**: Atomically flushes all state to human-readable flat files in `data/` (`assets_ledger.txt`, `locker_grid_8x8.txt`, `escrow_vault.txt`, `audit_wal.txt`).

---

## 4. How to Build & Run

### Prerequisites
- Java SE 17 or higher (tested on Java SE 25.0.3 LTS).
- Zero external JAR dependencies required.

### 1. Compile All Modules
```powershell
javac -d bin (Get-ChildItem -Recurse -Path backend/src/*.java, tests/*.java | ForEach-Object { $_.FullName })
```

### 2. Run the Automated Verification Test Suite (41 Tests)
```powershell
java -cp bin tests.RentSphereVerificationTest
```

### 3. Launch the Full-Stack Web Server (Port 8080)
```powershell
java -cp bin rentsphere.api.MainServer 8080
```
Open your browser and navigate to:
**`http://localhost:8080/`**

### 4. Launch Standalone Java Swing Desktop Console (Lab Backup)
```powershell
java -cp bin rentsphere.locker.SwingLockerConsole
```

---

## 5. Live Web Application Views Overview

The Single-Page Web Application delivers 5 interactive views under the **Warm Horizon Marketplace** design system:
1. **Marketplace & 5-Tier Catalog**: Real-time asset cards, category filtering (Tourism / Agriculture), and "Rent & Reserve" triggering the 4-Gate Fail-Fast Pipeline.
2. **8x8 Smart Locker Matrix (A1–H8)**: 64 interactive color-coded micro-storage bays, compartment inspector panel with temperature, humidity, weight gauges, and OTP unlock controls.
3. **Host & Quarantine Console**: Mandatory inspection deck with damage penalty slider, "Approve Clean Return", "Flag Damage & Slash", and live flat-file Write-Ahead Log table.
4. **Meta-OOP Workbench (Assignment 4)**: Live RTTI reflection tree inspector and shallow vs. deep clone memory address comparison (`System.identityHashCode`).
5. **NIRF Institutional Ranking (Assignment 4)**: Ministry of Education composite ranking dial, metric adjustment sliders, and deep-cloned What-If policy simulation.

---

## 6. Live Viva-Voce Defense Quick-Reference

### Question 1: How does the 4-Gate Pipeline achieve Fail-Fast short-circuiting?
*Answer*: In `FailFastPipeline.verify(PipelineContext ctx)`, gates are iterated in a strict sequence: `KycIdentityGate` (Gate 1) $\to$ `StateLegalityGate` (Gate 2) $\to$ `EscrowSolvencyGate` (Gate 3) $\to$ `LockerSpatialGate` (Gate 4). If any gate returns `isPassed() == false`, the method immediately halts and returns that `GateResult` without evaluating the remaining gates.

### Question 2: Why can gear never return directly from `ACTIVE_LEASE` to `AVAILABLE`?
*Answer*: This enforces the **Mandatory Quarantine Invariant** modeled in `ActiveLeaseState.initiateReturn()`. In peer-to-peer equipment sharing, returning gear must be locked in `INSPECTION_QUARANTINE` mode until an equipment host or lab inspector verifies physical condition. Collateral deposits remain held in `EscrowVault` until clean approval or damage slashing occurs.

### Question 3: How do you prove Shallow vs. Deep Cloning reference isolation?
*Answer*: In `RentableAsset`, `shallowClone()` calls `super.clone()`, which creates a new parent instance but copies the object reference to `maintenanceLogs`. `deepClone()` explicitly allocates a new `ArrayList<>(this.maintenanceLogs)`. In the Meta-OOP Workbench, `PrototypeCloner` displays `System.identityHashCode()`, showing that `shallow.maintenanceLogs` has the exact same memory address as `original.maintenanceLogs`, whereas `deep.maintenanceLogs` has a completely distinct heap address.

### Question 4: How does the system enforce zero-database persistence?
*Answer*: `FlatFilePersistenceEngine` utilizes `BufferedReader` and `BufferedWriter` with thread-safe synchronized locks to deterministically serialize and deserialize domain entities into human-readable pipe-delimited flat files (`assets_ledger.txt`, `locker_grid_8x8.txt`, `escrow_vault.txt`). All state mutations append an immutable record to `audit_wal.txt`.
