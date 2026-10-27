# RentSphere: Comprehensive Team Handover & Next-Day Operations Guide

**Project Title**: RentSphere: Peer-to-Peer (P2P) Equipment Rental Marketplace & 8×8 Smart Pickup Hub  
**Institution**: Department of Computer Science & Technology, Goa University  
**Program**: Master of Computer Applications (MCA) — Object-Oriented Technology (OOT) Lab Mini-Project  
**Author**: Senior Software Architect, QA Lead & Technical Documentation Specialist  
**Audience**: MCA Project Teammates (Faaris, Ketan, Nikhil, Deepak), Faculty Evaluators & External Examiners  
**Status**: Ready for Team Handover & Syllabus Viva-Voce Defense  

---

## 🚀 START HERE: 3-Minute Quick Launch

If you are just arriving at the project and need to run it immediately:

1. **Read these 3 core documents first**:
   - [PROJECT_ARCHITECTURE.md](file:///c:/Users/user/OneDrive/Desktop/MCA/OOT-MINIPROJECT/PROJECT_ARCHITECTURE.md) — System components, 5-tier inheritance, GoF state machine, and persistence diagrams.
   - [IMPLEMENTATION_STATUS.md](file:///c:/Users/user/OneDrive/Desktop/MCA/OOT-MINIPROJECT/IMPLEMENTATION_STATUS.md) — What is verified (113 tests), what is simulated, and verified business invariants.
   - [SECURITY_REVIEW.md](file:///c:/Users/user/OneDrive/Desktop/MCA/OOT-MINIPROJECT/SECURITY_REVIEW.md) — Remediated OWASP vulnerabilities (CWE-200, CWE-307, CWE-256, CWE-285).

2. **Run this verified 1-liner to compile and test**:
   ```powershell
   # Step A: Compile all 50 backend Java classes and test suite (Exit Code: 0)
   $files = (Get-ChildItem -Recurse -Filter *.java backend/src, tests | Select-Object -ExpandProperty FullName); javac -d bin -encoding UTF-8 $files

   # Step B: Run the 113-test automated verification suite (Expected: 113 / 113 PASSED)
   java -cp bin tests.RentSphereVerificationTest
   ```

3. **Launch the Web Application Server**:
   ```powershell
   java -cp bin rentsphere.api.MainServer 8080
   ```
   Open your browser to: **`http://localhost:8080/`**

---

## Section A: What is RentSphere?

### 1. The Core Problem It Solves
Across university campuses and local community creators in Goa, high-value specialized equipment (4K/8K cinema cameras, PA club sound systems, cordless rotary drills, event projection screens, and mobility transport) sits idle in closets 90% of the year. Concurrently, students and event creators must rent this gear at exorbitant commercial rates with zero trust safeguards.

### 2. Who Uses It? (Platform Personas)
- **Community Renters**: Students, student researchers, and event organizers who need equipment for 1–90 days, locking tariffs and refundable security collateral in escrow.
- **Equipment Hosts**: Peer lenders who onboard gear, set daily rates in `₹` INR, duplicate listings using Prototype Deep Cloning, and review returns in the inspection quarantine desk.
- **Faculty / Platform Administrator**: Oversees platform health, arbitrates damage disputes, resolves statutory grievance tickets (IT Rules 2021), and manages smart hub device overrides.
- **Public Visitors**: Prospective community members who browse public equipment listings, check real-time availability, and review safety guidelines before signing up.

### 3. The Main User Journey
1. **Browse**: Search catalog by category pill (`Cameras`, `Audio & Lighting`, `Power Tools`, `Wedding & Mobility`).
2. **Book & Escrow**: Select rental duration (1–90 days). The **4-Gate Fail-Fast Pipeline** verifies identity, legal state, wallet solvency, and locker geometry. Collateral + tariff are atomically locked in `EscrowVault`.
3. **Pickup**: Renter visits the physical 8×8 smart hub, provides their 6-digit pickup OTP. The bay validates the OTP (enforcing a 3-strike lockout) and unlocks the solenoid door (`ACTIVE_LEASE`).
4. **Return & Mandatory Quarantine**: Renter returns gear to the hub bay. The asset **strictly enters `INSPECTION_QUARANTINE`**; it cannot bypass inspection.
5. **Inspect & Refund**: Host inspects the gear. If clean, host clicks "Approve Clean Return": host receives the multi-day tariff, and the renter receives a 100% deposit refund.
6. **Dispute Arbitration**: If damage is contested, renter files a dispute attaching evidence notes. An administrator arbitrates the payout from the held escrow.

### 4. Project Scope
- **Inside Scope**: Pure Java SE 17+ backend, zero external JAR dependencies, zero external database (human-readable flat files in `data/`), 5-tier inheritance hierarchy, GoF State Pattern with mandatory quarantine, 8×8 smart locker matrix with Row H ground bay constraints, PBKDF2 authentication, Indian payment gateway sandbox in ₹ INR, 20 statutory policies, and responsive web frontend.
- **Outside Scope (Simulated)**: Physical IoT microcontrollers (ESP32/RS485), live commercial bank payment merchant accounts (Razorpay production keys), and production DigiLocker API credentials.

---

## Section B: Where Are We Right Now?

- **Git Branch**: `main` (clean working tree).
- **Latest Commit**: `2d44a52` (*feat: implement RentSphere P2P rental platform backend models, flat-file persistence, API server, and interactive frontend UI*).
- **Compilation Status**: Clean. `javac` compiles all 50 backend source files and test suite with **0 warnings and 0 errors**.
- **Automated Test Results**: **113 / 113 Tests Passed across 16 Test Suites (100% Success)**.
- **Live HTTP Health**: Endpoint `GET /api/health` returns `status: "UP"`, `runtime: "Java SE 25.0.3"`, `currency: "INR (₹)"`, `totalAssets: 8`, `totalLockerBays: 64`.
- **Known Real Limitations**:
  - The payment flow runs against an authentic HMAC-SHA256 sandbox adapter rather than real credit cards.
  - Smart locker solenoids are simulated in memory by `SimulatedLockerDevice`.
  - Multi-document transactions across flat files rely on in-memory locks, atomic staging (`.tmp` replace), and append-only WAL logs rather than distributed ACID two-phase commit.

---

## Section C: What Has Already Been Done?

### Subsystem Breakdown & Source Map

#### Subsystem 1: Core Behavioral Engine (Mohammed Faaris Khan)
- **GoF State Pattern** (`backend/src/rentsphere/state/`):
  - `LeaseState.java`, `AvailableState.java`, `ReservedEscrowState.java`, `ActiveLeaseState.java`, `InspectionQuarantineState.java`, `DisputedState.java`.
  - **Mandatory Quarantine Invariant**: Active leases returning to the hub must enter `InspectionQuarantineState`.
- **4-Gate Fail-Fast Verification Pipeline** (`backend/src/rentsphere/pipeline/`):
  - `KycIdentityGate` (Gate 1), `StateLegalityGate` (Gate 2), `EscrowSolvencyGate` (Gate 3), `LockerSpatialGate` (Gate 4), and `FailFastPipeline.java`.
- **Polyglot Mirror**: `polyglot/cpp/rentsphere_core.cpp` (C++20 reference).

#### Subsystem 2: Escrow Vault & 5-Tier Domain Model (Ketan Gurudas Naik)
- **5-Tier Inheritance Hierarchy** (`backend/src/rentsphere/model/`):
  - Tier 1: `AbstractDomainEntity.java`
  - Tier 2: `RentableAsset.java` (Cloneable context with `rentalDays` tracking)
  - Tier 3: `CategorizedRentalAsset.java`
  - Tier 4: `EventAndCreatorAsset.java`, `UtilityAndMobilityAsset.java`
  - Tier 5: `CameraAndOpticsGear.java`, `EventAudioAndLighting.java`, `PowerToolEquipment.java`, `EventAndMobilityVehicle.java`
- **Double-Entry Escrow Vault** (`backend/src/rentsphere/escrow/`):
  - `EscrowAccount.java`, `EscrowTransaction.java`, `EscrowVault.java`.
- **Indian Payment Gateway Sandbox** (`backend/src/rentsphere/payment/`):
  - `PaymentGateway.java`, `PaymentOrder.java`, `SandboxPaymentAdapter.java` (HMAC-SHA256 signature verification).
- **Polyglot Mirror**: `polyglot/csharp/RentSphereCore.cs` (C# reference).

#### Subsystem 3: Trust Engine & Host Diagnostics (Nikhil Bhaiju)
- **Meta-OOP & Reflection** (`backend/src/rentsphere/meta/`):
  - `GenericRepository.java`: Parameterized generics with predicate filtering.
  - `ReflectionInspector.java`: Dynamic RTTI class hierarchy, declared field, and method extractor via `java.lang.reflect`.
  - `PrototypeCloner.java`: Deep vs. shallow cloning benchmark proving heap reference isolation via `System.identityHashCode()`.
  - `TrustScoreEngine.java`: Weighted composite member trust metric (30/30/20/10/10) with What-If deep cloning simulation.
- **Polyglot Mirror**: `polyglot/python/rentsphere_core.py` (Python 3.12 reference).

#### Subsystem 4: 8x8 Smart Hub & Flat-File Persistence (Deepak Dev)
- **8×8 Physical Locker Grid** (`backend/src/rentsphere/locker/`):
  - `SmartLockerGrid8x8.java`, `LockerCompartment.java`, `CompartmentSize.java`.
  - **Row H Ground Bay Spatial Invariant**: Ground vehicles locked to Row H slots `H1`–`H8`.
  - **OTP Security**: 3-strike brute-force lockout (15 min) and 60-min expiration timestamp (CWE-307).
  - `SwingLockerConsole.java`: Offline Java Swing desktop 8×8 matrix GUI backup.
- **Deterministic Flat-File Persistence** (`backend/src/rentsphere/persistence/`):
  - `FlatFilePersistenceEngine.java`: Atomic write staging via `.tmp` file move; deterministic pipe serialization; append-only `audit_wal.txt`.

#### Cross-Cutting Security & Regulatory Subsystems
- **Authentication & RBAC** (`backend/src/rentsphere/auth/`):
  - `PasswordHasher.java`: PBKDF2-HMAC-SHA256 (100,000 iterations, 32-byte salt).
  - `UserAccount.java`, `UserSession.java`, `AuthService.java`: 256-bit cryptographically secure session tokens; role enforcement (`VISITOR`, `RENTER`, `HOST`, `ADMIN`).
- **Legal Compliance Engine** (`backend/src/rentsphere/policy/`):
  - `PolicyService.java`: 20 statutory Indian policies; versioned consent logging to `data/policy_acceptance.txt`.
- **Customer Grievance Redressal** (`backend/src/rentsphere/support/`):
  - `SupportService.java`: Statutory grievance tickets under Rule 3(2) IT Rules 2021 with 24h SLA tracking.
  - `NotificationService.java`: Real-time notification queue with flat-file persistence in `data/notifications_log.txt`.

---

## Section D: What Should the Team Complete Tomorrow?

Use this ordered checklist during the lab defense preparation:

- [ ] **Task 1: Pre-Defense Clean Compilation Check**
  - *Why*: Ensure no teammate has added syntax errors or broken imports.
  - *Command*: `$files = (Get-ChildItem -Recurse -Filter *.java backend/src, tests | Select-Object -ExpandProperty FullName); javac -d bin -encoding UTF-8 $files`
  - *Acceptance Criteria*: Process exits with code 0.
- [ ] **Task 2: Full Automated Test Suite Execution**
  - *Why*: Confirm that all 113 tests pass cleanly in the evaluation environment.
  - *Command*: `java -cp bin tests.RentSphereVerificationTest`
  - *Acceptance Criteria*: Terminal displays `VERIFICATION SUMMARY: 113 / 113 TESTS PASSED (100% SUCCESS)`.
- [ ] **Task 3: Server Startup & Health Telemetry Verification**
  - *Why*: Verify HTTP daemon is serving on port 8080.
  - *Command*: `java -cp bin rentsphere.api.MainServer 8080`
  - *Check*: Open `http://localhost:8080/api/health` and verify `status: "UP"`.
- [ ] **Task 4: Interactive Browser Walkthrough (5 Product Views)**
  - *Why*: Prepare the 5-minute live viva demonstration.
  - *Step 1*: Open `http://localhost:8080/`. Review the Visitor Landing Hero.
  - *Step 2*: Switch to `renter_deepak`. Reserve the Sony Alpha A7 IV (Asset AST-101) for 2 days. Complete payment sandbox simulation with HMAC.
  - *Step 3*: Switch to Tab 2 (8x8 Smart Hub). Click Bay A2. Show that authenticated renter sees cleartext 6-digit OTP, but unauthenticated visitor sees masked `******`. Click "Verify OTP & Unlock Pickup". Show status transitions to `ACTIVE_LEASE`.
  - *Step 4*: Click "Return Gear to Hub". Show that asset enters `INSPECTION_QUARANTINE`.
  - *Step 5*: Switch to `host_faaris`. Open Tab 3 (Escrow & Returns). Show asset in inspection deck. Click "Approve Clean Return & Refund Deposit". Show host earns ₹2,760, renter receives ₹5,000 collateral back, and ₹0 is trapped in escrow.
  - *Step 6*: Open Tab 4 (Host Studio). Demonstrate 1-click Listing Cloner and show `System.identityHashCode()` proving memory isolation between shallow and deep clones.
  - *Step 7*: Switch to `inspector_almeida`. Open Tab 5 (Admin & Grievance Hub). Show dispute arbitration queue, grievance tickets, and account suspension controls.
- [ ] **Task 5: Standalone Java Swing GUI Demonstration (Offline Backup)**
  - *Why*: Demonstrates multi-interface Java capability to examiners.
  - *Command*: `java -cp bin rentsphere.locker.SwingLockerConsole`
  - *Acceptance Criteria*: Native 8×8 desktop matrix window launches, displaying live bay states and telemetry.

---

## Section E: How Does the Code Work?

### 1. Folder Architecture
- `backend/src/rentsphere/`: All Java production sources.
- `frontend/`: The **authoritative** web UI served by `MainServer`.
- `data/`: The flat-file persistence directory. Never delete genuine user ledgers here.
- `tests/`: Automated verification suite (`RentSphereVerificationTest.java`).
- `config/`: Application settings (`rentsphere.properties`).

### 2. Frontend-to-Backend HTTP Bridge
The frontend uses standard asynchronous `fetch()` calls in `frontend/js/app.js`:
- Sessions are stored in `localStorage.getItem('rs_session_token')` and transmitted via HTTP `Authorization: Bearer <token>` and `Cookie: rs_session=<token>`.
- `MainServer.java` resolves the session token in `resolveSession(exchange)` before dispatching to handlers.
- Endpoints return JSON responses serialized via helper methods `sendJsonResponse(exchange, status, jsonString)`.

### 3. Key Invariant Enforcement Points
- **Quarantine Invariant**: Enforced in `ActiveLeaseState.java` L26-30.
- **Fail-Fast Defense**: Enforced in `FailFastPipeline.java` L30-50.
- **Row H Ground Bay Rule**: Enforced in `SmartLockerGrid8x8.java` L70-76 and `MainServer.java` L629-632.
- **Zero OTP Leakage**: Enforced in `MainServer.java` L718-729.
- **Admin RBAC Guard**: Enforced in `MainServer.java` L1259, L1301, L1571.
- **Atomic File Staging**: Enforced in `FlatFilePersistenceEngine.java` L306-320.

---

## Section F: How Do We Run and Test It?

### Prerequisites
- Operating System: Windows 10/11, Linux, or macOS.
- Java Development Kit: JDK 17, 21, or 25 installed and available on `PATH`.
- No Maven, Gradle, database servers, or external JARs required.

### Compilation Command
```powershell
$files = (Get-ChildItem -Recurse -Filter *.java backend/src, tests | Select-Object -ExpandProperty FullName); javac -d bin -encoding UTF-8 $files
```

### Verification Test Suite Command
```powershell
java -cp bin tests.RentSphereVerificationTest
```

### Server Launch Command
```powershell
java -cp bin rentsphere.api.MainServer 8080
```

### Pre-Seeded Default Personas
All default community personas share the secure password: **`RentSphere@2026`**
- **Admin & Grievance Officer**: `admin@rentsphere.in` (User ID: `inspector_almeida`, Balance: ₹80,000)
- **Cine Host**: `faaris@rentsphere.in` (User ID: `host_faaris`, Balance: ₹45,000)
- **Mobility Host**: `ketan@rentsphere.in` (User ID: `host_ketan`, Balance: ₹38,000)
- **Community Renter**: `deepak@rentsphere.in` (User ID: `renter_deepak`, Balance: ₹25,000)
- **Community Renter**: `nikhil@rentsphere.in` (User ID: `renter_nikhil`, Balance: ₹18,000)
- **Event Planner Renter**: `priya@rentsphere.in` (User ID: `renter_priya`, Balance: ₹30,000)

> [!CAUTION]
> **Data Persistence Warning**: Do NOT delete or empty files in `data/` (`assets_ledger.txt`, `escrow_vault.txt`, `audit_wal.txt`). The automated tests use isolated temporary storage in `data/test_*/` and do NOT contaminate the master production ledger.

---

## Section G: Decisions & Ideas Teammates Should Know

1. **Why Pure Java SE instead of Spring Boot?**  
   To fulfill the academic requirement of demonstrable Object-Oriented Technology (OOT) principles. Heavy frameworks obscure object lifecycles behind reflection proxies. RentSphere demonstrates pure GoF patterns, standard concurrency, and native Java networking.
2. **Why Flat Files instead of MySQL?**  
   The zero-database rule ensures 100% portability. Anyone can clone the repo and immediately run it without setting up relational database schemas or database users.
3. **What is Deferred to Future Work?**  
   - Connecting physical RS485/MQTT microcontroller solenoids.
   - Connecting live Razorpay production merchant accounts.
   - Automated 24-hour quarantine auto-approval timer.

---

## Section H: What Must NOT Be Claimed During the Demonstration

Be 100% truthful with examiners. Defend the project based on what is genuinely built:

- ❌ **Do NOT claim** that physical locker hardware is connected to the workstation. State clearly: *"The smart hub executes realistic telemetry and door solenoid states via our `LockerDevice` abstraction and `SimulatedLockerDevice` implementation."*
- ❌ **Do NOT claim** that real money is debited from bank accounts. State clearly: *"The platform implements an authentic Indian Payment Gateway Sandbox that models Razorpay/Cashfree order generation, HMAC-SHA256 signature verification, and idempotent webhooks in ₹ INR."*
- ❌ **Do NOT claim** that the 20 legal policies constitute formal attorney-certified contracts. State clearly: *"The 20 policies are drafted to align with the DPDP Act 2023 and IT Rules 2021 for an academic campus pilot, with formal legal counsel review scheduled prior to open commercial launch."*
- ❌ **Do NOT claim** that multi-document updates have distributed two-phase commit. State clearly: *"Individual files use atomic `.tmp` move staging, while multi-document consistency is guaranteed through synchronized in-memory locks and an append-only Write-Ahead Log (`audit_wal.txt`)."*

---

## Section I: What Should Happen After Handover?

1. **Review Git Status**: Run `git status` and verify that all documentation updates are cleanly recognized.
2. **Commit Changes**: Stage documentation updates with the descriptive commit message:  
   `docs: complete comprehensive codebase audit, architecture specifications, and team handover guide`
3. **Viva-Voce Rehearsal**: Have each teammate present their assigned subsystem according to Section C above.
