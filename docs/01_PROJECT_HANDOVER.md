# RentSphere Complete Project Handover

This guide explains the whole project in simple terms so a teammate can run it, understand the flow, demonstrate it, and continue development.

## 1. What RentSphere is

RentSphere is a Goa-based peer-to-peer equipment rental prototype. A host lists equipment, a renter reserves it, the system holds the rental amount and refundable deposit in a simulated escrow wallet, the renter collects it from a simulated local pickup hub, returns it, and the host inspects it before settlement.

The project also demonstrates an 8×8 smart pickup hub and micro-storage concept. The hub, sensors, OTP door and payment gateway are software simulations for the OOT lab.

## 2. One complete rental flow

1. A visitor opens the marketplace and browses equipment.
2. The renter selects an item and chooses the rental duration.
3. The system shows rent, refundable deposit and total before reservation.
4. The renter signs in and reserves an available asset.
5. The fail-fast pipeline checks identity, asset state, wallet solvency and locker placement.
6. The system locks the amount in the simulated escrow vault and changes the asset to `ReservedEscrowState`.
7. The renter enters the six-digit pickup OTP at the assigned hub bay.
8. A successful pickup changes the asset to `ActiveLeaseState`.
9. The renter returns the equipment.
10. The system moves it to `InspectionQuarantineState`. It cannot be rented again before inspection.
11. The host approves a clean return, or an administrator handles a damage dispute.
12. The deposit is refunded or adjusted, the host receives the rental settlement, and the audit log records the action.

## 3. Main project folders

| Folder | Purpose |
|---|---|
| `backend/src/rentsphere/api/` | Java HTTP server and API endpoints |
| `backend/src/rentsphere/model/` | Asset and domain class hierarchy |
| `backend/src/rentsphere/state/` | Rental State Pattern classes |
| `backend/src/rentsphere/escrow/` | Wallet, collateral and escrow ledger |
| `backend/src/rentsphere/payment/` | Sandbox payment order and HMAC verification |
| `backend/src/rentsphere/locker/` | 8×8 hub, bays, OTP and simulated sensors |
| `backend/src/rentsphere/pipeline/` | Four validation gates before reservation |
| `backend/src/rentsphere/auth/` | PBKDF2 authentication, sessions and roles |
| `backend/src/rentsphere/persistence/` | Flat-file loading, saving and audit WAL |
| `backend/src/rentsphere/dispute/` | Damage disputes and arbitration |
| `backend/src/rentsphere/policy/` | Policy acceptance records |
| `backend/src/rentsphere/support/` | Support tickets and notifications |
| `backend/src/rentsphere/meta/` | Generics, reflection, cloning and trust-score demonstrations |
| `frontend/` | Marketplace HTML, CSS and JavaScript |
| `data/` | Live demo records and ledgers |
| `data/test_*` | Isolated automated-test records |
| `tests/` | Java verification suite |
| `docs/` | Architecture, viva and handover documentation |

## 4. OOP concepts demonstrated

- Encapsulation: domain fields and guarded operations.
- Inheritance: five-tier asset hierarchy.
- Abstraction: `LeaseState`, gateway and locker interfaces.
- Polymorphism: each rental state implements the same operations differently.
- State Pattern: available, reserved, active lease, inspection quarantine and dispute states.
- Prototype Pattern: shallow and deep listing cloning.
- Reflection: runtime class and field inspection.
- Generics: reusable repository and filtering logic.
- Exception handling: invalid rental operations fail safely.
- Concurrency and idempotency: synchronized settlement and duplicate-payment protection.

## 5. Important storage locations

Read [`03_STORAGE_MAP.md`](03_STORAGE_MAP.md) for the complete file-by-file table. The most important files are:

- `data/assets_ledger.txt`: equipment listings and current asset state.
- `data/escrow_vault.txt`: wallet balances and locked collateral.
- `data/locker_grid_8x8.txt`: hub bays, assignments and simulated telemetry.
- `data/users_auth.txt`: accounts, roles and password hashes.
- `data/payments_ledger.txt`: sandbox payment records.
- `data/audit_wal.txt`: append-only activity history.

Never delete the live files directly under `data/` without a backup.

## 6. How to run it

### Windows shortcut

Run `START_RENTSPHERE.bat`.

### Manual commands

```powershell
$files = (Get-ChildItem -Recurse -Filter *.java backend/src, tests | Select-Object -ExpandProperty FullName)
javac -d bin -encoding UTF-8 $files
java -cp bin tests.RentSphereVerificationTest
java -cp bin rentsphere.api.MainServer 8080
```

Then open `http://localhost:8080/`.

The expected verification result is `163 / 163 tests passed` across 17 suites.

## 7. Recommended demonstration order

1. Show the marketplace and explain that a renter starts with discovery.
2. Open one listing and show the rent, deposit and total.
3. Sign in as a renter and reserve the asset.
4. Show the escrow balance and reservation state.
5. Open the assigned locker bay and verify the OTP.
6. Show the state changing to active lease.
7. Return the asset and show mandatory inspection quarantine.
8. Switch to the host and approve a clean return.
9. Show the deposit refund and host settlement.
10. Open the audit ledger and show the recorded transaction.
11. If time permits, show one invalid operation being rejected and the test result.

## 8. What is real and what is simulated

The Java business rules, state transitions, validation, persistence, authentication and audit behavior are implemented. The following are academic simulations:

- Physical locker hardware and sensors.
- Real bank or Razorpay payment processing.
- Production-grade regulatory escrow.
- Large-scale cloud deployment.

The honest explanation is: “The project models these integrations behind Java interfaces so they can be replaced by real services later.”

## 9. Current limitations and future scope

- Replace flat files with PostgreSQL or another transactional database.
- Connect real payment and identity providers.
- Connect physical locker hardware through an IoT gateway.
- Add production deployment, monitoring and backups.
- Add richer search, availability calendars and host analytics.

## 10. Viva explanation in one minute

“RentSphere is a peer-to-peer equipment rental system designed for a Goa campus and local community. The renter browses an asset, reserves it through a four-gate validation pipeline, pays through a sandbox escrow model, collects it using a simulated smart locker OTP, and returns it for mandatory inspection. The Java backend uses a five-tier inheritance hierarchy, the State Pattern for rental lifecycle control, flat-file persistence, authentication, audit logging and automated verification. The prototype is intentionally portable and demonstrates the business rules without claiming to be a production banking or IoT system.”

