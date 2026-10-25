# RentSphere: Peer-to-Peer (P2P) Equipment Marketplace & 8×8 Smart Hub (₹ INR)

[![Java Version](https://img.shields.io/badge/Java-SE%2017%2B%20%2F%2025-0D9488.svg)](https://openjdk.org/)
[![Architecture](https://img.shields.io/badge/Architecture-Zero--Database%20Pure%20OOP-0A5C4D.svg)](#zero-database-flat-file-persistence-engine)
[![Currency](https://img.shields.io/badge/Currency-Indian%20Rupees%20(%E2%82%B9%20INR)-F59E0B.svg)](#realistic-catalog-in-indian-rupees--inr)
[![Test Suite](https://img.shields.io/badge/Verification%20Tests-76%20%2F%2076%20PASSED-10B981.svg)](#running-automated-verification-tests)
[![Security Status](https://img.shields.io/badge/Security-OWASP%20Hardened%20%7C%20Zero%20OTP%20Leakage-10B981.svg)](#security-audit--threat-remediation)
[![Legal Compliance](https://img.shields.io/badge/Compliance-DPDP%20Act%202023%20%7C%20IT%20Rules%202021-3B82F6.svg)](#legal-policies--statutory-grievance-redressal)
[![Design System](https://img.shields.io/badge/Theme-Warm%20Horizon%20Marketplace-0D9488.svg)](#5-cohesive-product-views)

**Department of Computer Science & Technology — Goa University**  
*Master of Computer Applications (MCA) — Object-Oriented Technology (OOT) Lab Mini-Project*  

---

## 1. Project Charter & Engineering Philosophy

**RentSphere** is an industry-grade, zero-database, pure Object-Oriented full-stack web application with an integrated 8×8 smart pickup and parking hub matrix. Designed as a real-world, production-ready Peer-to-Peer (P2P) equipment sharing marketplace for **Goa University and the surrounding community scale**, RentSphere combines robust software design patterns with everyday rental categories:

- **Indian Rupees (₹ / INR) Currency Standard**: All asset rental tariffs, refundable security deposits, wallet balances, damage penalties, and audit ledger entries across Java models, flat files, and the Web UI use **Indian Rupees (`₹` INR)** with proper formatting (`₹1,200/day`, `₹5,000`, `₹25,000.00`).
- **Realistic Community Rental Categories**: Everyday gear and mobility categories:
  1. **Cameras & Optics Gear**: Sony Alpha mirrorless cameras, cinema lenses, gimbals, Full-HD event projectors.
  2. **Event Audio & Lighting**: JBL PartyBox club speakers, Pioneer DJ controllers, PA sound systems.
  3. **Power Tools & DIY**: Bosch impact drill kits, Karcher high-pressure washers, electronics toolkits.
  4. **Wedding Cars & Campus Mobility**: Maruti Suzuki Fronx automatic self-drive cars, Ather 450X electric scooters.
- **Pure Java SE Execution (Zero External JARs)**: 100% executable using standard JDK 17+ built-in libraries (`com.sun.net.httpserver.HttpServer`, `javax.crypto.*`, `java.nio.file.*`, `java.lang.reflect.*`, `javax.swing.*`).
- **Zero-Database Rule**: Replaces heavy SQL/ORM dependencies with thread-safe `ConcurrentHashMap` in-memory collections synchronized deterministically with pipe-delimited flat files in `data/` (`assets_ledger.txt`, `locker_grid_8x8.txt`, `escrow_vault.txt`, `users_auth.txt`, `payments_ledger.txt`, `disputes_ledger.txt`, `policy_acceptance.txt`, `support_tickets.txt`, `audit_wal.txt`).
- **Fail-Fast Defense Pipeline**: State transitions and physical locker allocations are guarded by a 4-Gate Pipeline that short-circuits upon the first invariant violation (`KycIdentityGate` $\to$ `StateLegalityGate` $\to$ `EscrowSolvencyGate` $\to$ `LockerSpatialGate`).
- **Mandatory Quarantine Invariant**: Returning an active lease triggers inspection quarantine before collateral release or asset relisting, preventing fraudulent gear returns.
- **Host Studio & Listing Cloner**: Hosts can duplicate listings in 1 click using Prototype Pattern Deep Cloning, ensuring isolated service logs (`System.identityHashCode()`) without shared memory bugs.
- **RentSphere Trust & Reliability Scoring Engine**: A weighted composite metric calculating member trust out of 100 based on physical return condition (30%), return punctuality (30%), escrow history (20%), peer reviews (10%), and KYC level (10%).
- **Hardened Security Architecture**: PBKDF2 password hashing with HMAC-SHA256 (100,000 iterations, 32-byte salt), 256-bit cryptographically secure sessions, Role-Based Access Control (`VISITOR`, `RENTER`, `HOST`, `ADMIN`), Zero OTP leakage to public endpoints (CWE-200 fix), and 3-strike brute-force lockout defense on locker bays (CWE-307 fix).
- **Indian Regulatory Compliance Engine**: 20 distinct Indian regulatory and marketplace policies, versioned consent logging under the Digital Personal Data Protection (DPDP) Act 2023, and statutory Grievance Redressal mechanism under Rule 3(2) of the Information Technology Intermediary Rules 2021.

---

## 2. Team Roster & Subsystem Ownership

| Student Lead | Subsystem & Role | Core Deliverables |
| :--- | :--- | :--- |
| **Mohammed Faaris Khan** | **Subsystem 1: Core Behavioral Engine** | 4-Stage GoF `LeaseState` pattern, `DisputedState`, Mandatory Quarantine Invariant, 4-Gate `FailFastPipeline`, Polyglot C++20 Core (`polyglot/cpp/rentsphere_core.cpp`) |
| **Ketan Gurudas Naik** | **Subsystem 2: Escrow Vault & 5-Tier Domain Model** | 5-Tier Domain Model (`model/`), Double-Entry `EscrowVault` in ₹ INR, Indian Payment Gateway Sandbox with HMAC-SHA256, Dynamic Marketplace Catalog UI, Polyglot C# Core (`polyglot/csharp/RentSphereCore.cs`) |
| **Nikhil Bhaiju** | **Subsystem 3: Trust Engine & Host Diagnostics** | `GenericRepository<T>`, Live RTTI `ReflectionInspector`, Multi-Unit Prototype Cloner, RentSphere Trust & Reliability Engine, Polyglot Python 3.12 Core (`polyglot/python/rentsphere_core.py`) |
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

## 4. Five Cohesive Product Views (`frontend/`)

1. **Tab 1: Explore Marketplace (`Marketplace`)**:
   - Visitor Hero Banner welcoming community members to Goa's peer-to-peer equipment sharing hub.
   - Category filter pills: `All Items`, `Cameras & Optics`, `Event Audio & Lighting`, `Power Tools & DIY`, `Wedding Cars & Mobility`.
   - Search bar, availability state dropdown, and **"+ List Equipment"** modal with category validation.
   - Equipment cards displaying daily rates in `₹`, security deposits in `₹`, assigned Hub Bay tag, protection badge, and action buttons (`Rent Now` / `Reserved` / `Out on Rent` / `In Inspection`).
   - Integrated **Checkout & Payment Sandbox Modal** allowing instant UPI and NetBanking payment simulation with server-authoritative HMAC signature generation.
2. **Tab 2: Smart Pickup Hub & Parking Grid (`8x8 Smart Hub A1–H8`)**:
   - 64 interactive bays and ground parking slots:
     - **Rows A–C (Small Lockers)**: Cameras, Drills, Lenses, Mics.
     - **Rows D–G (Medium/Large Lockers)**: Speakers, Projectors, Pressure Washers.
     - **Row H (Ground Vehicle Slots `H1`–`H8`)**: Wedding Cars, Electric Scooters.
   - Side inspector panel with live temperature (°C), humidity (%), load weight (kg), simulated door solenoids (`ONLINE_READY` / `DOOR_UNLOCKED`), 6-digit pickup OTP, **"Verify OTP & Unlock Pickup"**, and **"Return Gear to Hub"**.
   - Zero OTP leakage: Unauthenticated visitors see `******`, while authenticated active renters see their cleartext 6-digit PIN.
3. **Tab 3: Escrow Wallet, Returns & Inspection (`Escrow & Returns`)**:
   - Post-Return Inspection Queue displaying returned items held in `INSPECTION_QUARANTINE`.
   - Interactive Damage Deduction Slider (0% to 100%) in `₹`.
   - **"Approve Clean Return & Refund Deposit"** vs. **"Deduct Repair Cost from Deposit"**.
   - Renter **Damage Dispute Filing Modal** allowing renters to contest deductions with photos and notes, transitioning assets into `DISPUTED` state.
   - Live Transaction & Escrow Audit Ledger (`data/audit_wal.txt`) in `₹`.
4. **Tab 4: Host Inventory, Multi-Unit Cloner & Trust Score (`Host Studio & Diagnostics`)**:
   - **Multi-Unit Listing Cloner**: Duplicates listings in 1 click; memory comparison cards show live `System.identityHashCode()` proof that Deep Cloning isolates internal service logs.
   - **RentSphere Trust & Asset Reliability Calculator**: Composite metric (Score / 100) with interactive simulation sliders and deep-cloned What-If policy recommendations.
   - **Live JVM Object Inspector (`java.lang.reflect`)**: Inspects runtime class hierarchy, declared fields, and method signatures.
5. **Tab 5: Admin & Grievance Hub (`Admin & Grievance Hub`)**:
   - Restricted to `ADMIN` persona (Dr. A. Almeida).
   - **Damage Dispute Arbitration Queue**: Review claims, evidence, and decide `Refund Full Collateral to Renter` vs. `Remit Damage Deduction to Host`.
   - **Statutory Grievance Redressal Tickets (Rule 3(2) IT Rules 2021)**: Track user complaints, 24h SLA compliance, and mark tickets as `RESOLVED`.
   - **Account Status Management**: Suspend or reinstate user accounts with audit trail.
   - **Audit Trail & System Health Monitor**: Live telemetry, memory usage, and direct access to Write-Ahead Log.

---

## 5. Security & Threat Remediation

RentSphere has been thoroughly audited against the OWASP Top 10:
- **CWE-200 (OTP Credential Leakage):** Fixed. Public `/api/lockers` responses mask `otpPin` as `""`. PINs are only transmitted to authorized administrators or the active verified renter.
- **CWE-307 (Brute-Force Lockout):** Fixed. Locker bays trigger a 15-minute security lockout after 3 consecutive invalid OTP attempts. OTPs expire automatically after 60 minutes.
- **CWE-256 (Password Storage):** Fixed. Implemented PBKDF2 with HMAC-SHA256 (100,000 iterations, 32-byte salt). Passwords stored securely in `data/users_auth.txt`.
- **CWE-347 (Webhook Spoofing):** Fixed. Indian Payment Gateway Sandbox verifies HMAC-SHA256 signatures before capturing payments and updating orders.
- **CWE-284 (Broken Object Authorization):** Fixed. Role-Based Access Control enforced on server endpoints (`VISITOR`, `RENTER`, `HOST`, `ADMIN`).

*Full Security Review:* See [SECURITY_REVIEW.md](file:///c:/Users/user/OneDrive/Desktop/MCA/OOT-MINIPROJECT/SECURITY_REVIEW.md).

---

## 6. Legal & Regulatory Compliance (India Jurisdiction)

RentSphere includes 20 comprehensive regulatory and marketplace policies:
1. Terms of Service, 2. Privacy Policy (DPDP Act 2023), 3. Standard Equipment Bailment Agreement (Indian Contract Act 1872), 4. Cancellation & Refund Policy, 5. Late Return Policy, 6. Damage & Wear-and-Tear Policy, 7. Host Policy, 8. Renter Policy, 9. Security Deposit & Escrow Policy, 10. Dispute Resolution Policy (Arbitration Act 1996), 11. Prohibited Items Policy, 12. Copyright & IP Policy, 13. Listing Content & Image Rights Policy, 14. Community Guidelines, 15. Safety & Equipment Usage Guidance, 16. Cookie & Tracking Disclosures, 17. Data Retention & Account Deletion Policy, 18. Grievance Redressal Mechanism (Rule 3(2) IT Rules 2021), 19. Marketplace Limitations & Liability Disclosures, 20. Digital Accessibility Statement.

- **Consent Logging:** Consent is recorded deterministically in `data/policy_acceptance.txt` with timestamp, user ID, and IP address.
- **Grievance Redressal:** Designated Grievance Officer: Dr. A. Almeida (`grievance@rentsphere.in`).

*Full Legal Review Checklist:* See [POLICY_REVIEW_CHECKLIST.md](file:///c:/Users/user/OneDrive/Desktop/MCA/OOT-MINIPROJECT/POLICY_REVIEW_CHECKLIST.md).

---

## 7. How to Build, Test & Run

### Step 1: Compile All Modules (Zero External Dependencies)
```powershell
if (!(Test-Path "bin")) { New-Item -ItemType Directory -Path "bin" }
Get-ChildItem -Path "backend/src", "tests" -Filter "*.java" -Recurse | ForEach-Object { $_.FullName } | Out-File -Encoding ASCII sources.txt
javac -d bin -sourcepath "backend/src;tests" @sources.txt
```

### Step 2: Run Automated Verification Test Suite (76 Tests)
```powershell
java -cp bin tests.RentSphereVerificationTest
```
*Expected Output:*
```
=================================================================
  VERIFICATION SUMMARY: 76 / 76 TESTS PASSED (100% SUCCESS)
=================================================================
```

### Step 3: Launch Full-Stack Web Server (Port 8080)
```powershell
java -cp bin rentsphere.api.MainServer 8080
```
Open your browser and navigate to: **`http://localhost:8080/`**

### Step 4: Pre-Seeded Community Personas
All seeded accounts share the default password: **`RentSphere@2026`**
- **Admin & Grievance Officer:** `almeida@unigoa.ac.in` (User ID: `inspector_almeida`)
- **Equipment Hosts:** `faaris@rentsphere.in` (`host_faaris`), `ketan@rentsphere.in` (`host_ketan`)
- **Community Renters:** `deepak@unigoa.ac.in` (`renter_deepak`), `nikhil@rentsphere.in` (`renter_nikhil`), `priya@rentsphere.in` (`renter_priya`)

---

## 8. Complete Project Documentation Index

- [PROJECT_AUDIT.md](file:///c:/Users/user/OneDrive/Desktop/MCA/OOT-MINIPROJECT/PROJECT_AUDIT.md) — Architectural assessment, initial state, and risk classification.
- [IMPLEMENTATION_REPORT.md](file:///c:/Users/user/OneDrive/Desktop/MCA/OOT-MINIPROJECT/IMPLEMENTATION_REPORT.md) — Detailed changelog of all implemented features.
- [TEST_REPORT.md](file:///c:/Users/user/OneDrive/Desktop/MCA/OOT-MINIPROJECT/TEST_REPORT.md) — Test commands, 76 automated test results, and browser verification artifacts.
- [SECURITY_REVIEW.md](file:///c:/Users/user/OneDrive/Desktop/MCA/OOT-MINIPROJECT/SECURITY_REVIEW.md) — Threat modeling, OWASP remediation, and residual risk analysis.
- [POLICY_REVIEW_CHECKLIST.md](file:///c:/Users/user/OneDrive/Desktop/MCA/OOT-MINIPROJECT/POLICY_REVIEW_CHECKLIST.md) — 20 Indian legal policies, compliance matrix, and counsel sign-off checklist.
- [DEPLOYMENT.md](file:///c:/Users/user/OneDrive/Desktop/MCA/OOT-MINIPROJECT/DEPLOYMENT.md) — Operations, recovery, reverse proxy setup, and disaster recovery procedures.
- [DEVELOPMENT_LOG.md](file:///c:/Users/user/OneDrive/Desktop/MCA/OOT-MINIPROJECT/DEVELOPMENT_LOG.md) — Complete iterative development history and viva-voce notes.

---

*Department of Computer Science & Technology — Goa University*  
*MCA OOT Mini-Project (Academic Year 2025–2026)*
