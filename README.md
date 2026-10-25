# RentSphere: Local Peer-to-Peer (P2P) Rental & Smart Pickup Hub Engine (₹ INR)

[![Java Version](https://img.shields.io/badge/Java-SE%2017%2B%20%2F%2025-0D9488.svg)](https://openjdk.org/)
[![Architecture](https://img.shields.io/badge/Architecture-Zero--Database%20Pure%20OOP-0A5C4D.svg)](#zero-database-flat-file-persistence-engine)
[![Currency](https://img.shields.io/badge/Currency-Indian%20Rupees%20(%E2%82%B9%20INR)-F59E0B.svg)](#realistic-catalog-in-indian-rupees--inr)
[![Test Suite](https://img.shields.io/badge/Verification%20Tests-42%20%2F%2042%20PASSED-10B981.svg)](#running-automated-verification-tests)
[![Design System](https://img.shields.io/badge/Theme-Warm%20Horizon%20Marketplace-0D9488.svg)](#4-cohesive-product-views)

**Department of Computer Science & Technology — Goa University**  
*Master of Computer Applications (MCA) — Object-Oriented Technology (OOT) Lab Mini-Project*  

---

## 1. Project Charter & Engineering Philosophy

**RentSphere** is an industry-grade, zero-database, pure Object-Oriented full-stack web application with an integrated 8x8 smart pickup and parking hub matrix. Designed as a real-world, production-ready Peer-to-Peer (P2P) equipment sharing platform for **Goa University and the surrounding community scale**, RentSphere combines robust software design patterns with everyday rental categories:

- **Indian Rupees (₹ / INR) Currency Standard**: All asset rental tariffs, refundable security deposits, wallet balances, damage penalties, and audit ledger entries across Java models, flat files, and the Web UI use **Indian Rupees (`₹` INR)** with proper formatting (`₹1,200/day`, `₹5,000`, `₹25,000.00`).
- **Realistic Community Rental Categories**: Everyday gear and mobility categories:
  1. **Cameras & Optics Gear**: Sony Alpha mirrorless cameras, cinema lenses, gimbals, Full-HD event projectors.
  2. **Event Audio & Lighting**: JBL PartyBox club speakers, Pioneer DJ controllers, PA sound systems.
  3. **Power Tools & DIY**: Bosch impact drill kits, Karcher high-pressure washers, electronics toolkits.
  4. **Wedding Cars & Campus Mobility**: Maruti Suzuki Fronx automatic self-drive cars, Ather 450X electric scooters.
- **Pure Java SE Execution (Zero External JARs)**: 100% executable using standard JDK 17+ built-in libraries (`com.sun.net.httpserver.HttpServer`, `java.nio.file.*`, `java.lang.reflect.*`, `javax.swing.*`).
- **Zero-Database Rule**: Replaces heavy SQL/ORM dependencies with thread-safe `ConcurrentHashMap` in-memory collections synchronized deterministically with pipe-delimited flat files in `data/` (`assets_ledger.txt`, `locker_grid_8x8.txt`, `escrow_vault.txt`, `audit_wal.txt`).
- **Fail-Fast Defense Pipeline**: State transitions and physical locker allocations are guarded by a 4-Gate Pipeline that short-circuits upon the first invariant violation.
- **Mandatory Quarantine Invariant**: Returning an active lease triggers inspection quarantine before collateral release or asset relisting, preventing fraudulent gear returns.
- **Host Studio & Listing Cloner**: Hosts can duplicate listings in 1 click using Prototype Pattern Deep Cloning, ensuring isolated service logs (`System.identityHashCode()`) without shared memory bugs.
- **RentSphere Trust & Reliability Scoring Engine**: A weighted composite metric calculating member trust out of 100 based on physical return condition (30%), return punctuality (30%), escrow history (20%), peer reviews (10%), and KYC level (10%).

---

## 2. Team Roster & Subsystem Ownership

| Student Lead | Subsystem & Role | Core Deliverables |
| :--- | :--- | :--- |
| **Mohammed Faaris Khan** | **Subsystem 1: Core Behavioral Engine** | 4-Stage GoF `LeaseState` pattern, Mandatory Quarantine Invariant, 4-Gate `FailFastPipeline`, Polyglot C++20 Core (`polyglot/cpp/rentsphere_core.cpp`) |
| **Ketan Gurudas Naik** | **Subsystem 2: Escrow Vault & 5-Tier Domain Model** | 5-Tier Domain Model (`model/`), Double-Entry `EscrowVault` in ₹ INR, Dynamic Marketplace Catalog UI, Polyglot C# Core (`polyglot/csharp/RentSphereCore.cs`) |
| **Nikhil Bhaiju** | **Subsystem 3: Trust Engine & Host Diagnostics** | `GenericRepository<T>`, Live RTTI `ReflectionInspector`, Multi-Unit Prototype Cloner, RentSphere Trust & Reliability Engine, 5 Mermaid UML Diagrams, Polyglot Python 3.12 Core (`polyglot/python/rentsphere_core.py`) |
| **Deepak Dev** | **Subsystem 4: 8x8 Smart Hub & Persistence** | 64-Bay `SmartLockerGrid8x8`, Standalone Java Swing Desktop Console (`SwingLockerConsole.java`), Row H Ground Bay Spatial Invariant, Deterministic `FlatFilePersistenceEngine` (`data/*.txt`) |

---

## 3. Architecture & Domain Model Hierarchy

### 5-Tier Java Inheritance Hierarchy (`backend/src/rentsphere/model/`)
- **Tier 1 (`AbstractDomainEntity.java`)**: Encapsulates `String id`, `long createdAt`, `long updatedAt`, and pipe serialization contracts.
- **Tier 2 (`RentableAsset.java`)**: Encapsulates `LeaseState currentState`, `double baseDailyRate` (₹ INR), `double securityCollateral` (₹ INR), `String lockerCoordinate`, `List<String> serviceLogs`, implements `Cloneable`. Polymorphic tariff, deposit, and damage penalty methods.
- **Tier 3 (`CategorizedRentalAsset.java`)**: Encapsulates category classification, late-fee multiplier, and protection tier.
- **Tier 4 (`EventAndCreatorAsset.java` & `UtilityAndMobilityAsset.java`)**:
  - `EventAndCreatorAsset`: Indoor/outdoor rating, wattage/battery spec, accessory kit count.
  - `UtilityAndMobilityAsset`: Power/engine spec, safety certification, and `boolean requiresGroundBay`.
- **Tier 5 (4 Concrete Rental Classes)**:
  1. `CameraAndOpticsGear.java` (`CAMERAS`): 4K/8K resolution tariff factor (1.15x), sensor resolution deposit surcharge (1.20x for >= 30MP).
  2. `EventAudioAndLighting.java` (`AUDIO_LIGHTING`): Sound output dB, wireless mic count, RGB stage lighting flag.
  3. `PowerToolEquipment.java` (`POWER_TOOLS`): Cordless flag, drill bit / accessory count, impact power spec.
  4. `EventAndMobilityVehicle.java` (`MOBILITY_VEHICLES`): Seating capacity, fuel/EV type, auto transmission, `requiresGroundBay` locked to `true` (enforces Row H Ground Slots `H1`–`H8`).

---

## 4. Four Cohesive Product Views (`frontend/`)

1. **Tab 1: Explore Marketplace (`Marketplace`)**:
   - Category filter pills: `All Items`, `Cameras & Optics`, `Event Audio & Lighting`, `Power Tools & DIY`, `Wedding Cars & Mobility`.
   - Search bar, availability state dropdown, and **"+ List Equipment"** modal.
   - Equipment cards displaying daily rates in `₹`, security deposits in `₹`, assigned Hub Bay tag, protection badge, and action buttons (`Rent Now` / `Reserved` / `Out on Rent` / `In Inspection`).
2. **Tab 2: Smart Pickup Hub & Parking Grid (`8x8 Smart Hub A1–H8`)**:
   - 64 interactive bays and ground parking slots:
     - **Rows A–C (Small Lockers)**: Cameras, Drills, Lenses, Mics.
     - **Rows D–G (Medium/Large Lockers)**: Speakers, Projectors, Pressure Washers.
     - **Row H (Ground Vehicle Slots `H1`–`H8`)**: Wedding Cars, Electric Scooters.
   - Side inspector panel with live temperature (°C), humidity (%), load weight (kg), 6-digit pickup OTP, **"Verify OTP & Unlock Pickup"**, and **"Return Gear to Hub"**.
3. **Tab 3: Escrow Wallet, Returns & Inspection (`Escrow & Returns`)**:
   - Post-Return Inspection Queue displaying returned items held in `INSPECTION_QUARANTINE`.
   - Interactive Damage Deduction Slider (0% to 100%) in `₹`.
   - **"Approve Clean Return & Refund Deposit"** vs. **"Deduct Repair Cost from Deposit"**.
   - Live Transaction & Escrow Audit Ledger (`data/audit_wal.txt`) in `₹`.
4. **Tab 4: Host Inventory, Multi-Unit Cloner & Trust Score (`Host Studio & Diagnostics`)**:
   - **Multi-Unit Listing Cloner**: Duplicates listings in 1 click; memory comparison cards show live `System.identityHashCode()` proof that Deep Cloning isolates internal service logs.
   - **RentSphere Trust & Asset Reliability Calculator**: Composite metric (Score / 100) with interactive simulation sliders and deep-cloned What-If policy recommendations.
   - **Live JVM Object Inspector (`java.lang.reflect`)**: Inspects runtime class hierarchy, declared fields, and method signatures.

---

## 5. How to Build & Run

### Prerequisites
- Java SE 17 or higher (tested on Java SE 25.0.3 LTS).
- Zero external JAR dependencies required.

### 1. Compile All Modules
```powershell
Get-ChildItem -Recurse -Include *.java -Path backend\src, tests | Select-Object -ExpandProperty FullName | Out-File -Encoding ascii sources.txt
javac -encoding UTF-8 -d bin "@sources.txt"
```

### 2. Run Automated Verification Test Suite (42 Tests)
```powershell
java -cp bin tests.RentSphereVerificationTest
```

### 3. Launch Full-Stack Web Server (Port 8080)
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

## 6. Live Viva-Voce Defense Quick-Reference

### Question 1: How does the 4-Gate Pipeline enforce Fail-Fast validation?
*Answer*: In `FailFastPipeline.verify(PipelineContext ctx)`, gates are evaluated sequentially: `KycIdentityGate` (Gate 1) $\to$ `StateLegalityGate` (Gate 2) $\to$ `EscrowSolvencyGate` (Gate 3) $\to$ `LockerSpatialGate` (Gate 4). If any gate returns `isPassed() == false`, the method immediately halts and returns that `GateResult` without evaluating subsequent gates.

### Question 2: Why can gear never return directly from `ACTIVE_LEASE` to `AVAILABLE`?
*Answer*: This enforces the **Mandatory Quarantine Invariant** modeled in `ActiveLeaseState.initiateReturn()`. In peer-to-peer equipment sharing, returning gear must be locked in `INSPECTION_QUARANTINE` mode until an equipment host or hub inspector verifies physical condition. Collateral deposits remain held in `EscrowVault` until clean approval or damage slashing occurs.

### Question 3: How does the Prototype Pattern isolate multi-unit inventory in memory?
*Answer*: In `RentableAsset`, `shallowClone()` calls `super.clone()`, which creates a new parent instance but copies the mutable object reference to `serviceLogs`. `deepClone()` explicitly allocates a new `ArrayList<>(this.serviceLogs)`. In the Host Studio, `PrototypeCloner` displays `System.identityHashCode()`, showing that `shallow.serviceLogs` shares the exact same memory address as `original.serviceLogs`, whereas `deep.serviceLogs` has a completely distinct heap address.

### Question 4: How is the Row H Ground Bay spatial rule enforced?
*Answer*: `UtilityAndMobilityAsset` declares `boolean requiresGroundBay`, which is locked to `true` in `EventAndMobilityVehicle`. In `SmartLockerGrid8x8.assignAssetToBay()` and Gate 4 (`LockerSpatialGate`), if `asset.isRequiresGroundBay()` is true, the coordinate must have `row == 7` (`H1`–`H8`). Attempting to store vehicles in locker Rows A–G immediately halts at Gate 4.
