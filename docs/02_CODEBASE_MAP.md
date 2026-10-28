# RentSphere Codebase and Connection Map

This document answers three questions:

1. What is inside each folder?
2. Which files work together?
3. What happens when a renter performs an action in the browser?

## 1. Top-level map

```text
RentSphere/
├── backend/src/rentsphere/   Java application code
├── frontend/                 Browser interface
├── data/                     Persistent demo records
├── tests/                    Automated verification
├── docs/                     Architecture and handover notes
├── config/                   Runtime settings
├── polyglot/                 Reference implementations for comparison
├── bin/                      Compiled .class output
└── target/                   Maven build output, if Maven is used
```

The normal runtime path is:

```text
Browser
  → frontend/index.html + frontend/js/app.js
  → HTTP request to MainServer.java
  → authentication and role checks
  → rental service / state / pipeline / ledger classes
  → data/*.txt persistence
  → JSON response back to the browser
```

## 2. Frontend folder

### `frontend/index.html`

The page structure and visible sections. It contains the marketplace, navigation, dialogs, rental flow views and role-specific screens.

### `frontend/js/app.js`

The browser controller. It:

- Calls backend API endpoints with `fetch()`.
- Stores the current session token in browser storage.
- Renders assets, prices and availability.
- Opens reservation and sign-in dialogs.
- Switches between marketplace, rentals, hub, host and admin views.
- Sends reservation, pickup, return and support actions to the backend.

### `frontend/css/styles.css`

All visual styling, responsive layout, marketplace cards, dialogs, navigation and mobile behavior.

The frontend does not permanently own the asset catalog. It displays data received from the Java API.

## 3. API and server folder

### `backend/src/rentsphere/api/MainServer.java`

The application entry point and HTTP controller. It:

- Starts the server on port 8080.
- Serves the `frontend/` files.
- Defines API routes.
- Reads the session token from the request.
- Enforces authentication and role permissions.
- Calls the domain services.
- Converts results into JSON responses.

This is the connection point between the browser and the Java domain model.

## 4. Domain model folder

### `backend/src/rentsphere/model/`

Contains the equipment classes and inheritance hierarchy:

```text
AbstractDomainEntity
  → RentableAsset
    → CategorizedRentalAsset
      → EventAndCreatorAsset / UtilityAndMobilityAsset
        → CameraAndOpticsGear
        → EventAudioAndLighting
        → PowerToolEquipment
        → EventAndMobilityVehicle
```

These classes represent the rentable objects. They hold identity, name, category, owner, price, deposit, specifications and current state.

## 5. Rental State Pattern

### `backend/src/rentsphere/state/`

Each state controls which operations are legal:

```text
LeaseState                 common state interface
├── AvailableState          can be reserved
├── ReservedEscrowState     payment/deposit held
├── ActiveLeaseState        equipment is with renter
├── InspectionQuarantineState returned, waiting for host inspection
└── DisputedState            damage or settlement under review
```

The asset delegates operations to its current state. This prevents invalid actions such as renting an item that is already leased or still under inspection.

## 6. Reservation validation pipeline

### `backend/src/rentsphere/pipeline/`

Before a reservation is accepted, `FailFastPipeline` runs the gates in order:

```text
KycIdentityGate
  → StateLegalityGate
  → EscrowSolvencyGate
  → LockerSpatialGate
```

- `KycIdentityGate`: checks that the renter is authenticated and eligible.
- `StateLegalityGate`: checks that the asset can legally transition.
- `EscrowSolvencyGate`: checks wallet balance and collateral.
- `LockerSpatialGate`: checks that the chosen hub bay can hold the asset.

The first failed gate rejects the reservation with a clear reason.

## 7. Escrow and payment connection

### `backend/src/rentsphere/escrow/`

- `EscrowVault.java`: wallet balances and locked collateral.
- `EscrowAccount.java`: one user's balance and locked amount.
- `EscrowTransaction.java`: a single debit, hold, refund or settlement record.

### `backend/src/rentsphere/payment/`

- `PaymentGateway.java`: gateway abstraction.
- `SandboxPaymentAdapter.java`: simulated payment implementation with HMAC verification and ledger persistence.
- `PaymentOrder.java`: order details such as rent, deposit and total.

Reservation connection:

```text
Reservation request
  → PaymentOrder
  → SandboxPaymentAdapter
  → EscrowVault hold
  → assets_ledger status update
  → audit_wal entry
```

This is a sandbox model, not a real bank transaction.

## 8. Locker and pickup connection

### `backend/src/rentsphere/locker/`

- `SmartLockerGrid8x8.java`: owns the 64-bay matrix.
- `LockerCompartment.java`: one bay's assignment and state.
- `CompartmentSize.java`: small, medium and large bay rules.
- `LockerDevice.java`: device abstraction.
- `SimulatedLockerDevice.java`: software simulation of sensors and door state.
- `SwingLockerConsole.java`: optional offline desktop visualizer.

Pickup connection:

```text
Reservation
  → bay assignment in locker_grid_8x8.txt
  → six-digit OTP generated
  → renter submits OTP
  → OTP and lockout checks
  → simulated door unlock
  → asset changes to ActiveLeaseState
```

Ground vehicles must use Row H bays H1–H8.

## 9. Authentication and roles

### `backend/src/rentsphere/auth/`

- `UserAccount.java`: user identity, email, role and status.
- `PasswordHasher.java`: PBKDF2 password hashing.
- `AuthService.java`: login, account loading and role checks.
- `UserSession.java`: session token and expiry.

Roles used by the application include visitor, renter, host and administrator. `MainServer.java` checks the session before protected operations.

## 10. Persistence folder and data flow

### `backend/src/rentsphere/persistence/FlatFilePersistenceEngine.java`

This class reads and writes the main asset, locker, escrow and audit files. Other subsystems own their specialized files, but use the same `data/` directory.

```text
Java objects loaded at startup
  ↔ data/assets_ledger.txt
  ↔ data/locker_grid_8x8.txt
  ↔ data/escrow_vault.txt
  ↔ data/audit_wal.txt
```

Writes use temporary staging and replacement so a partially written main file is less likely.

See `03_STORAGE_MAP.md` for the complete file table.

## 11. Specialized service folders

### `auth/`

Login, password hashing, sessions and RBAC.

### `dispute/`

Damage claims, renter evidence, host response and administrator arbitration.

### `policy/`

Policy catalogue and consent logging.

### `support/`

Grievance tickets, status tracking and notifications.

### `meta/`

Academic demonstrations of generics, reflection, prototype cloning and trust-score simulation. These tools support the OOP lab evaluation and are not required for the basic browse-to-rent path.

## 12. Tests

### `tests/RentSphereVerificationTest.java`

The end-to-end verification suite. It exercises OOP behavior, states, pipeline gates, persistence, authentication, payments, OTP, disputes, support, security and settlement behavior.

Test folders under `data/test_*` keep test writes separate from the live demo records.

## 13. Build output and files not to edit

- `bin/` contains compiled classes generated by `javac`.
- `target/` contains build output generated by Maven.
- `sources.txt` is a generated source list.

Edit Java source under `backend/src/`, not `.class` files under `bin/` or `target/`.

## 14. Safe change rules

When changing a feature:

1. Change the Java source, not compiled output.
2. Preserve the API response shape expected by `frontend/js/app.js`.
3. Update the relevant state or service class rather than putting all rules in the frontend.
4. Keep live data files backed up before demo changes.
5. Run compilation and all tests.
6. Start the server and test the affected browser flow.
7. Check `data/audit_wal.txt` if the change affects a rental or settlement.

## 15. Where to look when something breaks

| Symptom | First files to inspect |
|---|---|
| Cards or prices missing | `MainServer.java`, `frontend/js/app.js`, `data/assets_ledger.txt` |
| Login fails | `auth/AuthService.java`, `data/users_auth.txt` |
| Reservation rejected | `pipeline/`, `state/`, `data/escrow_vault.txt` |
| Deposit or wallet incorrect | `escrow/`, `payment/`, `data/escrow_vault.txt`, `data/payments_ledger.txt` |
| Pickup or OTP fails | `locker/`, `MainServer.java`, `data/locker_grid_8x8.txt` |
| Return cannot complete | `state/ActiveLeaseState.java`, `state/InspectionQuarantineState.java` |
| Audit history missing | `FlatFilePersistenceEngine.java`, `data/audit_wal.txt` |
| Page looks broken | `frontend/index.html`, `frontend/css/styles.css`, browser console |
| Tests contaminate demo data | Check that the test uses `data/test_*`, not live `data/` files |

