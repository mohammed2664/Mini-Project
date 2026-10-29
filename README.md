# RentSphere

**A Java-based peer-to-peer equipment rental marketplace with an 8×8 simulated smart pickup hub.**

[![Java](https://img.shields.io/badge/Java-SE%2021%2B-0D9488)](https://openjdk.org/)
[![UI](https://img.shields.io/badge/Frontend-HTML%20%7C%20CSS%20%7C%20JavaScript-F59E0B)](#technology-stack)
[![Latest reported tests](https://img.shields.io/badge/Tests-163%20passed-10B981)](#build-and-test)
[![Project type](https://img.shields.io/badge/Project-MCA%20OOT%20Mini--Project-64748B)](#project-overview)

RentSphere is an academic project built to demonstrate object-oriented design through a practical rental workflow. Renters can explore equipment and make reservations; hosts can manage listings and inspect returns; administrators can manage accounts, review disputes, handle support tickets, and inspect system diagnostics.

> **Prototype notice:** This is an academic demonstration, not a production rental or payment service. Locker hardware and sensor readings are simulated, payments use a sandbox, and escrow-style balances are maintained in an internal software ledger—not in a bank-held trust account.

## Project overview

The project was developed for the **MCA Object-Oriented Technology (OOT) lab mini-project**, Department of Computer Science & Technology, Goa University. The interface is designed for an INR-based equipment-sharing use case.

### What you can do

- **Explore equipment:** Browse cameras, audio and lighting gear, power tools, and mobility-related listings with rental rates and refundable collateral shown in ₹ INR.
- **Reserve equipment:** Follow a server-validated booking flow with eligibility, state, balance, and locker-placement checks.
- **Use the simulated pickup hub:** View the 8×8 grid of bays (A1–H8), inspect virtual telemetry, and exercise the simulated pickup/return flow. Row H is reserved by the domain rules for ground-access mobility assets.
- **Track rental lifecycle:** Follow reservations through active lease and mandatory return inspection/quarantine before an item becomes available again.
- **Manage host inventory:** Create and manage listings, review active rentals, inspect returned items, and explore prototype cloning and runtime object inspection.
- **Manage disputes and support:** Authorized administrators can review disputes, resolve tickets, moderate accounts, and view diagnostics.
- **Explore OOP demonstrations:** Inspect inheritance, polymorphism, the State Pattern, the fail-fast pipeline, cloning, reflection, and trust scoring.

## Technology stack

| Layer | Technology |
|---|---|
| Backend | Java SE HTTP server and standard Java libraries |
| Frontend | HTML, CSS, and browser JavaScript |
| Desktop demonstration | Java Swing locker console |
| Persistence | In-memory domain data with flat-file records under `data/` |
| Verification | Java-based automated test harness; Node.js can be used for a JavaScript syntax check |
| Additional reference code | C++, C#, and Python implementations under `polyglot/` (where included) |

**Runtime:** Use **JDK 21 or newer**; JDK 25 is the version reported for the latest verification run. The project is designed to run without third-party Java JAR dependencies.

## Architecture at a glance

```text
Browser (HTML / CSS / JavaScript)
              |
              v
      Java SE HTTP API
              |
     +--------+---------+
     |        |         |
  Domain   Services   Authorization
  Models   & State    & Validation
     |        |         |
     +--------+---------+
              |
     v
 In-memory state + flat files in data/
              |
              v
       Audit/WAL records
```

The backend owns validation and state transitions. The frontend displays server responses; it should not be treated as the authority for permissions, OTPs, asset ownership, or financial changes.

## Object-oriented concepts demonstrated

| Concept / pattern | How RentSphere uses it |
|---|---|
| Encapsulation and abstraction | Domain entities expose controlled operations over asset, lease, and account state. |
| Inheritance and polymorphism | A multi-level rentable-asset hierarchy supports category-specific behaviour and tariff/collateral rules. |
| State Pattern | Rental assets move through states such as `AVAILABLE`, `RESERVED_ESCROW`, `ACTIVE_LEASE`, `INSPECTION_QUARANTINE`, and `DISPUTED`. |
| Fail-fast validation pipeline | Reservation checks run through identity/KYC, state legality, escrow solvency, and locker-space validation. |
| Prototype cloning | Listings can be cloned while deep cloning isolates mutable reference data. |
| Runtime reflection (RTTI) | `ReflectionInspector` examines runtime class hierarchy and members. |
| Generics and repositories | Generic repository abstractions demonstrate reusable typed data access. |
| Trust scoring | `TrustScoreEngine` combines multiple trust-related inputs into a composite score. |
| Persistence and audit trail | Flat-file persistence and write-ahead-log-style records support data reload and audit demonstrations. |

For deeper design notes, see the project documentation linked below.

## Security and integrity highlights

The project includes demonstrations of:

- Password hashing using PBKDF2-HMAC-SHA256.
- Session-based authentication and server-side role-based access control.
- Server-side checks for asset ownership and administrative operations.
- OTP masking for unauthorized callers, with attempt lockout and expiry rules.
- State guards and duplicate-settlement protection for inspection, dispute, and support workflows.
- Flat-file persistence and audit records for important operations.

These are project-level implementations and tests, **not a claim of production security certification**. Before any external deployment, conduct an independent security review, configure secrets and accounts safely, harden storage and transport, and remove or isolate academic demo-switching features. Never publish demo credentials; rotate any credential that has been committed to source history.

## Build and test

### Prerequisites

- JDK 21 or newer (`java -version` and `javac -version`)
- Node.js is optional and only needed for the JavaScript syntax check
- Windows PowerShell is used in the examples below; equivalent commands can be run in another shell

Run these commands from the repository root.

### 1. Create the source list if needed

If `sources.txt` already exists and contains the backend and test Java source files, you can skip this step.

```powershell
Get-ChildItem -Path "backend/src", "tests" -Filter "*.java" -Recurse |
  ForEach-Object { $_.FullName } |
  Set-Content -Encoding ascii sources.txt
```

### 2. Compile

```powershell
New-Item -ItemType Directory -Force -Path "bin" | Out-Null
javac -d bin -encoding UTF-8 @sources.txt
```

### 3. Run the automated verification suite

```powershell
java -cp bin tests.RentSphereVerificationTest
```

**Latest result documented in [`docs/05_TEST_RESULTS.md`](docs/05_TEST_RESULTS.md):** 163 tests passed across 17 suites. This count reflects the latest verification result currently documented on the repository's `main` branch; rerun the commands below to verify your checkout.

### 4. Start the web application

```powershell
java -cp bin rentsphere.api.MainServer 8080
```

Or double-click [`START_RENTSPHERE.bat`](START_RENTSPHERE.bat) to compile, start the server, and open the app. Use [http://localhost:8080/](http://localhost:8080/) while the server is running. **Do not open `frontend/index.html` directly**; the browser needs the Java server to load equipment, accounts, and bookings.

### 5. Optional JavaScript syntax check

```powershell
node -c frontend/js/app.js
```

## Local demo accounts

The project may include seeded local accounts for renter, host, and administrator demonstrations. Use credentials configured for your own local checkout; this README intentionally does not publish a shared password. **Do not use any repository-published shared demo password for a real account.**

- The **demo persona switcher**, if enabled, is for local academic walkthroughs only. It is not a substitute for authentication and must not be used as a production identity system.
- Do not expose the development server publicly with demo accounts enabled.
- Do not commit personal data, access tokens, passwords, session identifiers, OTPs, or production ledger contents to a public repository.
- If a demo credential has ever been committed, remove it from active configuration and rotate it; deleting it from the latest file alone does not remove it from Git history.

## What is simulated?

| Area | Current project scope |
|---|---|
| Locker doors and sensors | Software simulation using virtual device state and telemetry; no physical solenoids, RS-485 bus, GPIO, or ESP32 hardware is attached. |
| Payments | Sandbox payment/order and HMAC-signature flow; no production payment provider or commercial bank rail is connected. |
| Escrow and wallet | Internal application ledger for demonstrating accounting logic; it does not hold real funds and is not a bank-held escrow account. |
| Grievance workflow | Academic prototype with acknowledgement and resolution timing displays; it is not legal certification or a determination of compliance with applicable law. |
| Deployment | Local development/demo server; production hosting, TLS, operational monitoring, secret management, and hardening are outside the current prototype scope. |

## Documentation

Use these repository-relative links on GitHub (only files present in your checkout will resolve):

- [`IMPLEMENTATION_STATUS.md`](IMPLEMENTATION_STATUS.md) — implementation inventory and verification checkpoints.
- [`PROJECT_ARCHITECTURE.md`](PROJECT_ARCHITECTURE.md) — architecture and package overview.
- [`docs/UML_DIAGRAMS.md`](docs/UML_DIAGRAMS.md) — UML diagrams, if included in the checkout.
- [`docs/05_TEST_RESULTS.md`](docs/05_TEST_RESULTS.md) — test-suite results and verification notes.
- [`SECURITY_REVIEW.md`](SECURITY_REVIEW.md) — security findings and limitations.
- [`docs/01_PROJECT_HANDOVER.md`](docs/01_PROJECT_HANDOVER.md) — team handover and demo notes.
- [`docs/VIVA_PREPARATION.md`](docs/VIVA_PREPARATION.md) — short project-flow explanations and viva questions.

If a link does not exist in your repository, remove it or update it to the actual path before publishing.

## Known limitations and future work

- Connect the locker domain to actual hardware only if suitable devices and a safe integration environment are available.
- Integrate a payment provider in a separate, properly secured production implementation; the current payment flow is a sandbox.
- Review flat-file persistence, backup/recovery, and multi-process consistency before supporting real users or concurrent production instances.
- Complete independent security, accessibility, privacy, and legal reviews before any commercial launch.

## Academic context

**Project:** RentSphere — Peer-to-Peer Equipment Rental Marketplace & 8×8 Simulated Smart Pickup Hub  
**Programme:** Master of Computer Applications (MCA)  
**Subject:** Object-Oriented Technology (OOT) Lab Mini-Project  
**Institution:** Department of Computer Science & Technology, Goa University

---

*Built as an academic software prototype to demonstrate object-oriented design, workflow validation, simulated smart-locker operations, and ledger-based rental accounting.*
