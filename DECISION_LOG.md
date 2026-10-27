# RentSphere: Architectural & Engineering Decision Log (ADR)

**Project Title**: RentSphere: Peer-to-Peer (P2P) Equipment Rental Marketplace & 8×8 Smart Pickup Hub  
**Institution**: Goa University — Department of Computer Science & Technology  
**Program**: Master of Computer Applications (MCA) — Object-Oriented Technology (OOT) Lab Mini-Project  
**Document Classification**: Architectural Decision Records (ADR)  
**Standard**: Documenting observed technical choices, context, trade-offs, and status  

---

## Decision Index

| ID | Topic | Decision Summary | Status | Date |
| :--- | :--- | :--- | :--- | :--- |
| **ADR-001** | Zero External Dependencies | Build backend using pure Java SE 17+ standard library without external JARs | **ACCEPTED** | 2026-10-25 |
| **ADR-002** | Zero Database Persistence | Use in-memory ConcurrentHashMaps backed by pipe-delimited flat files & WAL | **ACCEPTED** | 2026-10-25 |
| **ADR-003** | Indian Currency Standard (₹ INR) | Standardize all pricing, tariffs, deposits, and financial transactions in INR | **ACCEPTED** | 2026-10-25 |
| **ADR-004** | Embedded HTTP Server | Use JDK built-in `com.sun.net.httpserver.HttpServer` on port 8080 | **ACCEPTED** | 2026-10-25 |
| **ADR-005** | Domain Model Evolution | Transition from marine/agriculture to realistic community equipment categories | **SUPERSEDED** (Early draft replaced) | 2026-10-25 |
| **ADR-006** | Mandatory Quarantine Invariant | Active leases returned to hub must enter quarantine prior to relisting | **ACCEPTED** | 2026-10-25 |
| **ADR-007** | 4-Gate Fail-Fast Verification | Short-circuit reservation requests on first invariant violation | **ACCEPTED** | 2026-10-25 |
| **ADR-008** | Cryptographic Authentication | Use PBKDF2-HMAC-SHA256 (100k iterations, 32-byte salt) & 256-bit session tokens | **ACCEPTED** | 2026-10-25 |
| **ADR-009** | OTP Security Defenses | Mask OTPs on public endpoints (CWE-200) and enforce 3-strike lockout (CWE-307) | **ACCEPTED** | 2026-10-25 |
| **ADR-010** | Prototype Pattern Deep Cloning | Use deep cloning with isolated ArrayList references for listing duplication | **ACCEPTED** | 2026-10-25 |
| **ADR-011** | Trust Composite Score vs NIRF | Refactor NIRF institutional portal into community member trust score engine | **SUPERSEDED** (Academic NIRF adapted to marketplace) | 2026-10-25 |
| **ADR-012** | Multi-Day Duration Tracking | Track rental duration in asset model and charge host full multi-day tariff | **ACCEPTED** | 2026-10-25 |
| **ADR-013** | Atomic Flat-File Staging | Write to temporary `.tmp` files before atomic rename to prevent corruption | **ACCEPTED** | 2026-10-25 |
| **ADR-014** | Payment Gateway Architecture | Implement authentic Indian gateway sandbox (HMAC-SHA256) rather than live bank API | **ACCEPTED (Pilot Standard)** | 2026-10-25 |
| **ADR-015** | Physical IoT Hardware Abstraction | Implement in-memory simulated locker device instead of physical microcontrollers | **ACCEPTED (Lab Standard)** | 2026-10-25 |

---

## Detailed Decision Records

### ADR-001: Zero External Dependencies (Pure Java SE 17+)
- **Status**: ACCEPTED
- **Context**: The project was assigned as an MCA Object-Oriented Technology (OOT) Lab mini-project at Goa University. Academic lab evaluation machines may lack internet access, Maven repos, or specific build tool versions.
- **Decision**: Implement all networking, JSON processing, reflection, cryptography, file I/O, and test suites using only standard JDK built-in packages (`java.net.*`, `com.sun.net.httpserver.*`, `javax.crypto.*`, `java.lang.reflect.*`, `java.nio.file.*`).
- **Consequences**:
  - *Positive*: Zero dependency download overhead; compiles instantly with standard `javac` on any computer with JDK 17+; completely immune to third-party dependency vulnerabilities.
  - *Trade-off*: Custom JSON parsing utilities and HTTP dispatchers must be maintained within `MainServer.java`.

### ADR-002: Zero Database Persistence Engine
- **Status**: ACCEPTED
- **Context**: Setting up MySQL, PostgreSQL, or MongoDB on academic demonstration workstations introduces installation friction, credential configuration issues, and database daemon failures.
- **Decision**: Persist all business data deterministically to pipe-delimited flat files in `data/` (`assets_ledger.txt`, `locker_grid_8x8.txt`, `escrow_vault.txt`, `users_auth.txt`, `audit_wal.txt`). Keep runtime state in thread-safe `ConcurrentHashMap` collections.
- **Consequences**:
  - *Positive*: Human-readable, transparent data files that can be inspected, audited, and restored using standard text editors.
  - *Trade-off*: Multi-document transactions lack distributed ACID two-phase commit; consistency relies on in-memory locks, atomic staging, and append-only WAL records.

### ADR-003: Indian Currency Standard (₹ INR)
- **Status**: ACCEPTED
- **Context**: The platform is designed for Goa University and local community sharing in North/South Goa, India.
- **Decision**: Standardize all financial amounts (daily tariffs, security deposits, escrow balances, damage penalties, wallet debits) in **Indian Rupees (`₹` INR)** across backend models, flat files, and web UI.
- **Consequences**:
  - *Positive*: Realistic local market relevance; aligns with Indian E-Commerce rules and local equipment rates (e.g., ₹1,200/day for Sony Alpha, ₹2,200/day for Maruti Fronx).

### ADR-004: Authoritative Frontend Directory (`frontend/`)
- **Status**: ACCEPTED (Observed Implementation Decision)
- **Context**: Two copies of frontend assets existed in the repository (`frontend/` and `src/main/resources/static/`).
- **Decision**: `MainServer.java` is explicitly configured to resolve static web files from `./frontend` (`Paths.get("frontend")`). Therefore, `frontend/` is the authoritative live directory. The secondary copy in `src/main/resources/static/` is kept synchronized for Maven packaging compatibility.
- **Consequences**:
  - *Positive*: Clear rule for developers on where to make UI modifications.

### ADR-005: Evolution of 5-Tier Domain Model Hierarchy
- **Status**: SUPERSEDED (Historical Transition)
- **Context**: Early documentation (`IMPLEMENTATION_PLAN.md` and `docs/UML_DIAGRAMS.md`) referenced marine and agricultural research equipment (`SectoralAsset`, `TourismAsset`, `AgricultureAsset`, `AutonomousMarineDrone`, `SubmersibleActionRig`, `PrecisionRotaryTiller`, `OpticalSoilSpectrometer`).
- **Decision**: Refactor into realistic consumer and community categories:
  - Tier 1: `AbstractDomainEntity`
  - Tier 2: `RentableAsset`
  - Tier 3: `CategorizedRentalAsset`
  - Tier 4: `EventAndCreatorAsset` & `UtilityAndMobilityAsset`
  - Tier 5: `CameraAndOpticsGear`, `EventAudioAndLighting`, `PowerToolEquipment`, `EventAndMobilityVehicle`
- **Consequences**:
  - *Positive*: Highly relatable gear for campus students, faculty, and local event creators while fully satisfying the 5-tier inheritance requirement.

### ADR-006: Mandatory Quarantine Invariant
- **Status**: ACCEPTED
- **Context**: In P2P rentals, returning gear directly to "Available" allows defective, damaged, or uncleaned equipment to be re-rented, creating dispute chaos.
- **Decision**: Enforce that returning an active lease (`ActiveLeaseState.returnGear()`) transitions strictly into `InspectionQuarantineState`. Direct transition back to `AvailableState` is barred at the type level.
- **Consequences**:
  - *Positive*: Hosts must explicitly review returned items and approve them or slash collateral before equipment can be rented again.

### ADR-007: 4-Gate Fail-Fast Verification Pipeline
- **Status**: ACCEPTED
- **Context**: Booking equipment involves multiple independent invariants (KYC, legal state, wallet solvency, physical compartment geometry).
- **Decision**: Implement a 4-Gate Pipeline (`KycIdentityGate` $\to$ `StateLegalityGate` $\to$ `EscrowSolvencyGate` $\to$ `LockerSpatialGate`) that short-circuits on the first violation.
- **Consequences**:
  - *Positive*: Fast failure with actionable diagnostic messages; prevents partial state updates.

### ADR-008: Cryptographic Authentication Architecture
- **Status**: ACCEPTED
- **Context**: Early iterations relied on client-side persona switching with no server-side security.
- **Decision**: Implement standard Java PBKDF2-HMAC-SHA256 (100k iterations, 32-byte salt) in `PasswordHasher.java` and 256-bit cryptographically secure session tokens in `UserSession.java`.
- **Consequences**:
  - *Positive*: OWASP A02 and A07 compliant credential security.

### ADR-009: OTP Masking and Lockout Defense
- **Status**: ACCEPTED
- **Context**: Audit identified CWE-200 (OTP serialized to public `/api/lockers`) and CWE-307 (no brute-force limit on OTP entry).
- **Decision**: Mask OTP as `""` for unauthenticated or non-owning callers in `MainServer.java`. In `LockerCompartment.java`, lock bay for 15 minutes after 3 consecutive failures, and expire OTPs after 60 minutes.
- **Consequences**:
  - *Positive*: Physical locker security hardened against eavesdropping and dictionary attacks.

### ADR-010: Prototype Pattern Deep Cloning Memory Isolation
- **Status**: ACCEPTED
- **Context**: Duplicating listings via shallow cloning shares internal reference collections (`serviceLogs`), leading to cross-asset log pollution.
- **Decision**: Provide both `shallowClone()` (shares references) and `deepClone()` (allocates new `ArrayList<String>`), and prove the heap difference via `System.identityHashCode()`.
- **Consequences**:
  - *Positive*: Academic demonstration of memory mechanics; bug-free host listing duplication.

### ADR-011: Trust Composite Score Engine vs NIRF Analytics Portal
- **Status**: SUPERSEDED (Academic Pivot)
- **Context**: The syllabus assignment required an analytics portal, originally modeled on the Ministry of Education NIRF institutional ranking formula.
- **Decision**: Refactor the analytics engine into a weighted member trust metric (`TrustScoreEngine.java`) combining physical return condition (30%), return punctuality (30%), escrow history (20%), peer reviews (10%), and KYC level (10%) with What-If deep cloning simulation.
- **Consequences**:
  - *Positive*: Perfect functional fit for a P2P rental platform while retaining the exact mathematical composite weighting and What-If simulation concepts.

### ADR-012: Multi-Day Rental Tariff Tracking (Finding F-01)
- **Status**: ACCEPTED
- **Context**: Audit finding F-01 uncovered that `handleReserve` locked multi-day tariffs, but inspect approval and dispute resolution hardcoded a 1-day tariff calculation, trapping remaining funds in escrow and underpaying hosts.
- **Decision**: Introduce `rentalDays` field in `RentableAsset`, serialize to flat files, restore on startup, calculate multi-day tariff on return approval, and reset to 1 day only upon transition back to Available.
- **Consequences**:
  - *Positive*: Exact financial accounting: hosts receive 100% of earned multi-day revenue, renters receive full collateral refunds, zero funds trapped in escrow.

### ADR-013: Atomic Persistence Staging (Finding F-07)
- **Status**: ACCEPTED
- **Context**: Direct writes to production `.txt` files risked truncation and file corruption if the JVM was interrupted mid-write.
- **Decision**: Implement `writeLinesAtomically` in `FlatFilePersistenceEngine.java` using a `.tmp` staging file and `Files.move(..., ATOMIC_MOVE, REPLACE_EXISTING)`.
- **Consequences**:
  - *Positive*: Crash-resilient file persistence without partial file states.

### ADR-014: Payment Gateway Sandbox Implementation
- **Status**: ACCEPTED (Pilot Standard)
- **Context**: Integrating live production payment gateways (Razorpay, Cashfree, Paytm) requires commercial merchant accounts, GSTIN, and active bank KYC.
- **Decision**: Implement an authentic sandbox adapter (`SandboxPaymentAdapter.java`) that mirrors the complete Indian payment flow (order creation, HMAC-SHA256 signature verification, and idempotent webhook processing in ₹ INR) using local test simulation.
- **Consequences**:
  - *Positive*: Testable offline; 100% zero external network dependencies; ready to swap in live merchant keys when transitioning to commercial operations.

### ADR-015: Physical Smart Locker Hardware Abstraction
- **Status**: ACCEPTED (Lab Workstation Standard)
- **Context**: Physical locker solenoids, magnetic sensors, and microcontroller hardware (ESP32/Raspberry Pi) were not physically connected to the development PC.
- **Decision**: Implement `LockerDevice` interface and `SimulatedLockerDevice` managing solenoid states (`ONLINE_READY`, `DOOR_UNLOCKED`, `MAINTENANCE_MODE`) and realistic environmental telemetry (temperature, humidity, load cell weight).
- **Consequences**:
  - *Positive*: Full software verification of locker states and telemetry without blocking on physical hardware availability.
