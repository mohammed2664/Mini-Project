# 📘 RentSphere: Mini-Project Report

**Project Title**: RentSphere: Peer-to-Peer Asset Rental & 8×8 Smart Micro-Storage Management System  
**Course**: Master of Computer Applications (MCA) — Object-Oriented Technology (OOT) Lab  
**Academic Year**: 2026–2027  
**Department**: Department of Computer Science & Technology  
**Institution**: Goa University, Taleigao Plateau, Goa 403206  

---

## 👥 Student Project Team

| Roll Number | Full Name | Academic Subsystem & Contribution |
|:---:|:---|:---|
| **2664** | **Mohammed Faaris Khan** | **Tech Lead & Subsystem 1**: 4-Gate Fail-Fast Verification Pipeline, GoF State Machine Engine, Polyglot C++20 Core, and End-to-End System Integration. |
| **2623** | **Ketan Naik** | **Subsystem 2**: Double-Entry Escrow Vault, Financial Settlements, 5-Tier Domain Model Hierarchy, Polymorphic INR Rates, and C# Core Engine. |
| **2616** | **Deepak Devan** | **Subsystem 3**: Deterministic Flat-File Persistence Engine, Append-Only Write-Ahead Log (WAL), Atomic `.tmp` OS Swapping, and Zero-Database Architecture. |
| **2665** | **Nikhil Baiju** | **Subsystem 4**: 8×8 Smart Micro-Storage Grid (64 bays), Row H Ground Bay Spatial Invariant, RTTI Live Reflection, Prototype Deep vs. Shallow Cloner, and Python Core. |

---

## 📑 Table of Contents
1. [Abstract](#1-abstract)
2. [Introduction & Problem Statement](#2-introduction--problem-statement)
3. [Objectives & Academic Scope](#3-objectives--academic-scope)
4. [System Architecture & Design Decisions](#4-system-architecture--design-decisions)
5. [Object-Oriented Design & Design Patterns](#5-object-oriented-design--design-patterns)
6. [8×8 Smart Micro-Storage Grid & Spatial Invariant](#6-88-smart-micro-storage-grid--spatial-invariant)
7. [Zero-Database Persistence & Double-Entry Escrow](#7-zero-database-persistence--double-entry-escrow)
8. [Statutory Compliance & Security Architecture](#8-statutory-compliance--security-architecture)
9. [Automated Verification & Test Harness](#9-automated-verification--test-harness)
10. [Limitations & Future Roadmap](#10-limitations--future-roadmap)
11. [Conclusion](#11-conclusion)
12. [References](#12-references)

---

## 1. Abstract
The traditional sharing economy relies heavily on informal classifieds (e.g., OLX, Craigslist) or high-overhead courier-based platforms (e.g., SharePal). These systems introduce significant physical friction: coordinating schedules in traffic, disputes over lost or damaged deposits, and fear of asset theft. 

**RentSphere** is a hardware-integrated, peer-to-peer (P2P) asset rental and micro-storage platform designed to automate peer rentals through local smart lockers. Built entirely using pure Java SE (JDK 17+/25) with **Zero External JAR Dependencies** and a **Zero-Database Flat-File Architecture**, the system models complex real-world workflows using core Object-Oriented Technology principles. The platform features a 5-tier domain model hierarchy, an automated 4-Gate fail-fast validation pipeline, a 4-stage GoF state machine with a mandatory return quarantine invariant, an 8×8 (64-bay) smart locker grid with physical spatial constraints, a double-entry escrow vault in Indian Rupees (₹ INR), and live runtime reflection. A comprehensive automated test suite of **17 test suites and 163 tests passes with 100% success**, proving the deterministic correctness and resilience of the system.

---

## 2. Introduction & Problem Statement

### 2.1 The Economic Paradox of Idle Capital
Across academic campuses and local communities in Goa, valuable consumer equipment—such as 4K mirrorless cameras, projectors, sound systems, power tools, and vehicles—sits idle up to 90% of the year. Concurrently, students, tourists, and independent creators frequently require access to this equipment for short durations (1–3 days) but cannot justify the capital expenditure to purchase it outright.

### 2.2 Operational Challenges in Existing Models
1. **Coordination & Meeting Friction**: Direct peer handovers require extensive messaging, synchronizing schedules, and meeting in traffic.
2. **Escrow & Deposit Disputes**: Owners fear asset damage; renters fear unlawful withholding of cash security deposits.
3. **Courier Latency**: Courier-based rental services take 2 to 4 days for shipping and return transit, making impromptu campus or community rentals unviable.
4. **Architectural Fragility**: Modern web architectures frequently obscure object lifecycle and domain invariants behind heavyweight application frameworks (such as Spring Boot or ORM libraries), making fine-grained state enforcement opaque and brittle.

---

## 3. Objectives & Academic Scope

The primary objectives of the RentSphere project are:
1. **Rigorous Object-Oriented Modeling**: Model the rental lifecycle using classic design patterns (State, Chain of Responsibility, Prototype, Factory Method, and Repository).
2. **Deterministic Financial Solvency**: Ensure all rental tariffs and security collateral are tracked using a double-entry ledger in Indian Rupees (₹ INR).
3. **Automated Handovers**: Interface with a 64-bay (8×8) smart locker grid for 24/7 contactless pickup and return using cryptographically secure 6-digit OTP pins.
4. **Enforce Mandatory Quarantine**: Guarantee that returned gear enters quarantine before security deposits are released.
5. **Zero External Dependencies**: Run seamlessly on any workstation with standard Java SE APIs alone.

---

## 4. System Architecture & Design Decisions

### 4.1 Layered Architecture Overview
```text
┌────────────────────────────────────────────────────────┐
│  Presentation Layer: Vanilla HTML5 / CSS3 / ES6+ JS   │
│  (Marketplace, 8x8 Smart Hub, Quarantine, Admin Hub)   │
└───────────────────────────┬────────────────────────────┘
                            │ HTTP REST / JSON (Port 8080)
┌───────────────────────────▼────────────────────────────┐
│  Application Layer: Built-in JDK HttpServer            │
│  (AuthService, DisputeService, NotificationService)   │
└───────────────────────────┬────────────────────────────┘
                            │ Domain Invariants & Policies
┌───────────────────────────▼────────────────────────────┐
│  Business Logic Layer: Core OOT Engines                │
│  (4-Gate Pipeline, GoF State Machine, 5-Tier Domain)   │
└───────────────────────────┬────────────────────────────┘
                            │ Atomic Thread-Safe Sync
┌───────────────────────────▼────────────────────────────┐
│  Persistence Layer: Flat-File Engine & Append-Only WAL │
│  (assets_ledger.txt, escrow_vault.txt, audit_wal.txt) │
└────────────────────────────────────────────────────────┘
```

### 4.2 Key Architectural Defense: Why Pure Java SE?
Rather than relying on Spring Boot or external relational database servers (e.g., MySQL, PostgreSQL), the system uses standard Java SE library components:
- **Zero Database Server Setup**: Flat files in `data/` allow instant, cold-boot startup (<350ms) on any laptop.
- **Transparent Object Lifecycles**: Domain invariants are enforced in explicit Java code rather than obscured by magical framework annotations.
- **Atomic File Swapping**: Crash-resilient file writes use temporary files (`.tmp`) followed by operating system atomic renames (`Files.move(..., ATOMIC_MOVE, REPLACE_EXISTING)`).

---

## 5. Object-Oriented Design & Design Patterns

### 5.1 5-Tier Domain Model Hierarchy
Every rentable item extends from a structured 5-tier inheritance tree:
1. **Tier 1 (`AbstractDomainEntity`)**: Root entity providing identity (`id`), created timestamps, and equality semantics.
2. **Tier 2 (`RentableAsset`)**: Core rental fields (`title`, `ownerId`, `renterId`, `currentState`, `baseDailyRate`, `securityCollateral`).
3. **Tier 3 (`CategorizedRentalAsset`)**: Category metadata, late fee multipliers, and protection tiers.
4. **Tier 4 (`EquipmentSubcategory` / `UtilityAndMobilityAsset`)**: Physical operational specs (indoor/outdoor ratings, wattage, ground bay flags).
5. **Tier 5 (Leaf Domain Classes)**: Concrete implementations:
   - `CameraAndOpticsGear`: Adds sensor resolution (`sensorMp`), 4K video flag (`is4k`), and lens mounts. Overrides `calculateDailyRentalTariff()` with high-resolution video multipliers.
   - `EventAudioAndLighting`: Sound pressure level (`soundDb`), wireless mic count (`mics`), and RGB lighting.
   - `PowerToolEquipment`: Motor power (`powerOrEngineSpec`), chuck size, and ISI safety certifications.
   - `EventAndMobilityVehicle`: Enforces vehicle commercial insurance multipliers and spatial ground bay locking (`requiresGroundBay = true`).

### 5.2 GoF State Pattern & Mandatory Quarantine
Asset lifecycles are governed by polymorphic State classes extending `AssetState`:
* **`AvailableState`**: Allows reservation; strictly rejects pickup or quarantine actions.
* **`ReservedEscrowState`**: Funds locked in escrow; allows OTP-verified pickup; rejects new bookings.
* **`ActiveLeaseState`**: Asset physically dispatched; allows return to locker; transitions strictly to Quarantine.
* **`InspectionQuarantineState`**: **Mandatory Quarantine Invariant**: Returned assets are locked from re-booking until the verified host inspects the physical condition and executes clean release or damage penalty slashing.

### 5.3 4-Gate Fail-Fast Verification Pipeline (Chain of Responsibility)
Before an asset reservation is accepted, the request passes through four sequential gates:
1. **`KycIdentityGate`**: Verifies user KYC status and strictly enforces the **Anti-Self-Rental Invariant** (hosts cannot rent their own registered equipment).
2. **`AssetAvailabilityGate`**: Verifies the asset is currently in `AvailableState`.
3. **`EscrowSolvencyGate`**: Verifies the renter's wallet has sufficient funds to cover both the rental tariff and the required security collateral.
4. **`LockerCapacityGate`**: Verifies that a physical locker bay matching the asset's dimensional requirements is vacant.

---

## 6. 8×8 Smart Micro-Storage Grid & Spatial Invariant

The physical pickup and return hub is modeled as an **8×8 matrix comprising exactly 64 discrete compartments** (labeled `A1` through `H8`):
- **Rows A–C (Small Bays)**: Cameras, lenses, and hand tools (Up to 8 kg).
- **Rows D–E (Medium Bays)**: Studio lighting, power drills, and DJ controllers (Up to 25 kg).
- **Rows F–G (Large Bays)**: Club speakers, projectors, and heavy pressure washers (Up to 50 kg).
- **Row H (Ground Level XL Parking Bays)**: **Spatial Invariant**: Ground vehicles (cars, electric scooters, power tillers) cannot be placed in upper lockers. The system enforces that any asset requiring a ground bay (`requiresGroundBay == true`) is strictly locked to Row H (`H1`–`H8`). Upper bays immediately reject vehicle allocations.

Each compartment maintains simulated real-time IoT telemetry:
- Temperature (°C) and relative humidity (%).
- Load cell weight sensor (kg).
- Solenoid latch status (`ONLINE_READY`, `DOOR_UNLOCKED`, `QUARANTINE_LOCKED`).
- 3-strike OTP lockout security: bay locks for 15 minutes after 3 consecutive failed OTP attempts.

---

## 7. Zero-Database Persistence & Double-Entry Escrow

### 7.1 Human-Auditable Flat Files
All operational data is persisted in pipe-delimited text files within `data/`:
- `assets_ledger.txt`: Equipment inventory, specifications, and state tracking.
- `escrow_vault.txt`: Double-entry balances, locked collateral, and earned payouts.
- `locker_grid_8x8.txt`: 64-bay locker matrix hardware states.
- `users_auth.txt`: PBKDF2 hashed credentials and RBAC roles.
- `audit_wal.txt`: Append-only Write-Ahead Log.

### 7.2 Double-Entry Escrow Mathematics
All financial operations preserve zero-sum balance conservation:
$$\Delta \text{RenterWallet} + \Delta \text{LockedEscrow} + \Delta \text{HostWallet} + \Delta \text{PlatformFee} = 0$$

1. **Reservation**: `RenterWallet` debited by Tariff + Collateral; `LockedEscrow` credited.
2. **Clean Return Approval**: `LockedEscrow` debited by Tariff + Collateral; Host credited Tariff (minus 10% fee); Renter credited 100% of Collateral.
3. **Damage Slashing**: Host receives Tariff + Damage Penalty; Renter refunded Remaining Collateral; Platform takes commission.

---

## 8. Statutory Compliance & Security Architecture

1. **Information Technology Act, 2000 & IT Rules 2021**: Implements a prototype grievance redressal workflow (Rule 3(2)) with 24-hour acknowledgment and 15-day resolution SLAs.
2. **Statutory Legal Policies**: 20 comprehensive policies covering terms of service, equipment liability, Fastag/fuel terms, and damage arbitration.
3. **OWASP Top 10 Security**:
   - Cryptographic password hashing using PBKDF2-HMAC-SHA256 with 256-bit unique salts.
   - Constant-time password hash comparison preventing timing attacks.
   - Strict Role-Based Access Control (RBAC): Renters cannot onboard assets, access admin user lists, or resolve disputes.
   - Strict session expiration (24 hours) and zero OTP leak in public API payloads.

---

## 9. Automated Verification & Test Harness

RentSphere includes an exhaustive automated verification test harness ([`RentSphereVerificationTest.java`](code/tests/RentSphereVerificationTest.java)):
- **Total Test Suites**: 17
- **Total Automated Test Assertions**: 163
- **Test Pass Rate**: **100% (163 / 163 Passed)**
- **Test Categories Covered**:
  - Unit tests for polymorphic inheritance and financial algorithms.
  - State machine transition rules and quarantine invariants.
  - 4-Gate pipeline short-circuit validation.
  - Row H spatial invariant rejection and acceptance.
  - Live RTTI reflection and JVM heap reference memory isolation (Prototype deep vs. shallow cloning).
  - Multi-threaded race condition tests simulating simultaneous dispute resolutions, clean inspection approvals, and damage slashings.

---

## 10. Limitations & Future Roadmap

1. **Microcontroller Hardware Interfacing**: The current version uses an in-process simulation (`SimulatedLockerDevice`). Future iterations will connect to physical ESP32 / Raspberry Pi controllers via MQTT over TLS 1.3.
2. **Live Payment Gateway**: Production deployment requires transitioning from the HMAC sandbox adapter to live Razorpay / UPI webhooks.
3. **Multi-Hub Scaling**: Expanding from the single Goa University central node to distributed nodes across North and South Goa campuses.

---

## 11. Conclusion

RentSphere demonstrates that robust, production-grade software can be engineered using standard Java SE library APIs and core Object-Oriented principles without the overhead of heavy enterprise frameworks. By uniting a 5-tier domain model, GoF state patterns, hardware-constrained spatial storage, and double-entry escrow accounting, the platform provides a complete, reliable, and auditable solution to peer-to-peer equipment sharing.

---

## 12. References
1. Gamma, E., Helm, R., Johnson, R., & Vlissides, J. (1994). *Design Patterns: Elements of Reusable Object-Oriented Software*. Addison-Wesley.
2. Bloch, J. (2018). *Effective Java* (3rd ed.). Addison-Wesley Professional.
3. Ministry of Electronics and Information Technology (MeitY), Government of India. (2021). *Information Technology (Intermediary Guidelines and Digital Media Ethics Code) Rules, 2021*.
4. OWASP Foundation. (2021). *OWASP Top Ten Web Application Security Risks*.
