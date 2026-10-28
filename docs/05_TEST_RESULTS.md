# RentSphere Test Results: What, Why and How

## 1. What was tested?

The test suite checks the complete rental system, not only whether Java classes compile. It checks:

- OOP inheritance and polymorphism
- Rental State Pattern transitions
- Invalid-operation rejection
- Four-gate reservation validation
- Escrow and wallet calculations
- Payment sandbox signatures and duplicate webhook protection
- 8×8 locker allocation and Row H vehicle rules
- OTP expiry and three-attempt lockout
- Authentication, sessions and role permissions
- Flat-file persistence and audit logging
- Damage disputes and administrator arbitration
- Policies, support tickets and notifications
- Endpoint authorization and impersonation protection
- Browser-level marketplace and rental flows

## 2. Current result

The latest verified run produced:

```text
163 / 163 tests passed
17 verification suites
100% success
Java compilation passed
JavaScript syntax check passed
API health check returned UP
```

The tests are executed by:

```text
tests/RentSphereVerificationTest.java
```

## 3. Why test these things?

### State transitions

Rental is a stateful process. Testing states proves that an asset cannot be reserved while already leased, and that returned equipment must pass inspection before becoming available again.

### Invalid operations

A rental system must reject unsafe actions. Tests verify that unauthenticated users, wrong owners, suspended users and renters attempting admin actions are blocked.

### Escrow and money calculations

The most dangerous business errors are incorrect rent, deposits, refunds or duplicate settlements. Tests verify the exact totals and confirm that a clean return refunds collateral correctly.

### Locker and OTP rules

The hub has physical constraints. Tests prove that vehicles use Row H, bays are assigned correctly, OTP guessing triggers lockout and the simulated door unlocks only after valid verification.

### Persistence

The server must preserve important records after restart. Tests write and reload assets, wallet values, notifications, multi-day rental duration and audit entries.

### Security

Tests prove that passwords are hashed, sessions are created, role checks work, OTP values are protected and one user cannot act as another user.

## 4. How to run the tests

From the project root in PowerShell:

```powershell
$files = (Get-ChildItem -Recurse -Filter *.java backend/src, tests | Select-Object -ExpandProperty FullName)
javac -d bin -encoding UTF-8 $files
java -cp bin tests.RentSphereVerificationTest
```

The command should finish with all tests passed and exit successfully.

Then run the web application separately:

```powershell
java -cp bin rentsphere.api.MainServer 8080
```

Open `http://localhost:8080/` and manually verify browse, reservation, pickup, return, inspection and settlement.

## 5. How to read a failed test

1. Read the suite name and test description.
2. Identify whether the failure is in the model, state, service, persistence or API layer.
3. Check the corresponding file using `02_CODEBASE_MAP.md`.
4. Check whether the failure changed a live file under `data/`.
5. Re-run the failing test after the smallest source change.
6. Run the full suite before committing.

## 6. Test data safety

Automated tests should use isolated folders such as `data/test_auth/` and `data/test_payments/`. They should not overwrite the live demo ledgers directly under `data/`.

Before a live demonstration, back up the production files under `data/` so a test or demo action can be reversed.

## 7. What the result proves

The result proves that the implemented academic prototype behaves consistently under the tested scenarios. It supports claims about Java OOP behavior, rental-state enforcement, validation, persistence, security controls and simulated integrations.

## 8. What the result does not prove

The result does not prove that:

- Physical locker hardware works in the real world.
- A real bank payment has been processed.
- The system is ready for internet-scale traffic.
- The policies are formal legal advice.
- Every possible browser, operating system or network failure has been tested.

The honest viva wording is:

> “Our automated suite verifies the implemented Java prototype and its simulated integrations. Production hardware, live payment credentials and large-scale deployment remain future integration work.”

## 9. How to present the test slide

Use a table with three columns:

| Test case | Expected result | Actual result |
|---|---|---|
| Reserve an available asset | State becomes `ReservedEscrowState` | Passed |
| Reserve an already reserved asset | `IllegalRentalStateException` | Passed |
| Activate a valid reservation | State becomes `ActiveLeaseState` | Passed |
| Return an active lease | State becomes `InspectionQuarantineState` | Passed |
| Rent an asset awaiting inspection | Operation rejected | Passed |
| Complete inspection | State becomes `AvailableState` | Passed |

Add this summary to the slide:

```text
163 / 163 automated tests passed across 17 verification suites
```

## 10. Why automated and manual testing are both needed

Automated tests are repeatable and catch business-rule regressions. Manual browser testing confirms that the user can actually understand and complete the flow. Both are needed because a backend can be correct while the interface is confusing, or a beautiful interface can call the wrong backend operation.

