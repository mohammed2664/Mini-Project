# RentSphere 5-Minute Live Demo Walkthrough

Use this as a spoken script. Do not explain every feature. Show one clean end-to-end story and point out the OOP rule behind it.

## Before the examiner arrives

1. Run `START_RENTSPHERE.bat`.
2. Open `http://localhost:8080/`.
3. Keep the browser zoom at 100% and begin on the Marketplace page.
4. Keep this file open in another tab only if you need a prompt.

## 0:00–0:35 — Marketplace

**Click:** `Explore Marketplace`.

**Say:**

> “Good morning. Our project is RentSphere, a Goa-based peer-to-peer equipment rental platform. A host can list equipment, while a renter can browse, reserve, collect and return it through a local pickup hub. The main goal is to control the rental lifecycle safely using Java OOP.”

**Show:** Search, category chips and listing cards.

> “The renter starts here, exactly like a normal rental marketplace. The card shows the daily rent, refundable deposit, category and current availability.”

## 0:35–1:10 — Reservation and escrow

**Click:** An available equipment card, then `Rent this item` / reserve.

**Say:**

> “Before reserving, the system makes the cost transparent: rental amount, refundable security deposit and total. A guest can preview this first, but a signed-in renter is required to make the reservation.”

**If prompted:** Sign in through the existing demo persona selector or login screen.

> “When the renter confirms, the backend does not blindly reserve the item. It runs four checks: renter eligibility, state legality, wallet solvency and valid locker allocation. If any check fails, the process stops safely.”

**Point out:** Reservation confirmation or updated item state.

> “After success, the asset moves from Available to Reserved Escrow. The amount is held in our simulated escrow wallet, and the audit log records the action.”

## 1:10–1:50 — Smart pickup hub and State Pattern

**Click:** `Pickup Hub & Parking`.

**Select:** The assigned bay, then show the OTP area.

**Say:**

> “The reservation is linked to our simulated 8 by 8 smart pickup hub. The renter receives a six-digit OTP for the assigned bay. We also enforce asset size rules. For example, vehicles are restricted to the Row H ground bays.”

**Click:** Verify pickup OTP.

> “A correct OTP unlocks the simulated locker and changes the asset into Active Lease. This is the State Pattern in practice. Each state allows only valid actions.”

## 1:50–2:35 — Return and inspection quarantine

**Click:** Return equipment or open `Escrow Wallet & Returns`.

**Say:**

> “On return, the equipment cannot become available immediately. It must enter Inspection Quarantine. This prevents the next renter from receiving an item before the host checks its condition.”

**Show:** Quarantine card/inspection queue.

> “The host can approve a clean return, which refunds the renter’s deposit and settles the rental payment to the host. If there is damage, the system opens a dispute instead.”

## 2:35–3:15 — Host features and OOP extras

**Switch persona:** Host, then open `Host Tools & Diagnostics`.

**Say:**

> “The host can manage listings and inspect returns. For the OOT component, we also demonstrate Prototype cloning. A host can duplicate a listing, and our deep clone keeps the copied service history independent from the original.”

**Optional:** Show reflection or trust score only if it is already visible.

> “We use reflection to inspect the Java class hierarchy at runtime and a weighted trust score for a host or renter profile.”

## 3:15–4:00 — Security and accountability

**Click:** `Admin & Support` only if it opens cleanly. Otherwise point to the role controls and audit section.

**Say:**

> “The application uses role-based access. A renter cannot approve an inspection or use administrator actions. Passwords are stored as PBKDF2 hashes, OTP attempts are limited, and important actions are recorded in an append-only audit log.”

> “Payments and locker hardware are sandbox simulations for this academic prototype. The interfaces are separated so real payment and IoT services can replace them later.”

## 4:00–4:35 — Testing proof

**Show:** Terminal result or the test-result slide.

**Say:**

> “We verified the project with 163 automated tests across 17 suites. The tests cover the inheritance hierarchy, State Pattern, invalid transitions, wallet calculations, OTP lockout, security permissions, persistence and concurrent settlement protection.”

## 4:35–5:00 — Close

**Say:**

> “So RentSphere is not only a listing interface. It is a controlled rental system where each operation follows a valid lifecycle: browse, reserve, escrow, OTP pickup, active lease, return, inspection and settlement. Thank you.”

## Likely examiner questions

| Question | Short answer |
|---|---|
| Why State Pattern? | “It keeps each rental state’s legal operations separate and blocks invalid transitions cleanly.” |
| Why flat files? | “For a portable OOT lab prototype with no database setup. The service layer can later use PostgreSQL.” |
| Is escrow real? | “No, it is a simulated wallet and ledger. We model the hold, refund and settlement logic.” |
| Is the locker real hardware? | “The locker device is simulated through an interface. The 8×8 rules, OTP and telemetry logic are implemented.” |
| Why Java? | “Java makes the inheritance, interfaces, State Pattern, file persistence and concurrency controls clear for the OOT objective.” |

## If the current demo data is not in the expected state

- Pick another item marked Available.
- If a return is already in quarantine, use that to demonstrate the inspection rule.
- If an item is already active, use it to demonstrate the hub and active-lease view.
- Never claim a real bank transfer or real hardware operation. Call them sandbox and simulation.

