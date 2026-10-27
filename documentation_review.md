# RentSphere — Documentation Evidence & Consistency Review

**Review Date:** 2026-10-26  
**Reviewer:** Antigravity (automated static + runtime audit)  
**Scope:** All source files, flat-file data, and 12 documentation artefacts.  
**Build command executed:** `javac -d bin -sourcepath "backend/src;tests" @sources.txt`  
**Test command executed:** `java -cp bin tests.RentSphereVerificationTest`

---

## Part 1 — Build & Test Execution (Requirement 1 & 2)

| Item | Result |
|---|---|
| **Source files compiled** | 51 `.java` files (50 backend + 1 test harness) |
| **Compiler exit code** | **0** (zero errors, zero warnings) |
| **Test command** | `java -cp bin tests.RentSphereVerificationTest` |
| **Test exit code** | **0** |
| **Test count** | **105 tests across 15 suites** |
| **Failures** | **0** |
| **Warnings during tests** | None emitted to stdout/stderr |

> [!NOTE]
> The README badge `105 / 105 PASSED` is **independently verified** by this live run. The claim is accurate.

---

## Part 2 — Evidence Table

> Legend: ✅ VERIFIED | ⚠️ PARTIAL | ❓ UNVERIFIED | ❌ INCORRECT

### A. Build & Architecture Claims

| # | Claim | Doc Source | Evidence Location | Verification | Result | Required Correction |
|---|---|---|---|---|---|---|
| 1 | Compiles with zero external JARs on JDK 17+ | README §1, DEPLOYMENT §1 | `pom.xml` — only `junit-jupiter-api:test`; `MainServer.java` uses `com.sun.net.httpserver` | Compiled successfully with `javac` only; `pom.xml` has JUnit 5 test-scope dep (unused at runtime) | ✅ **VERIFIED** | None |
| 2 | "50 backend Java source files" | TEAM_HANDOVER §B | `Get-ChildItem backend/src -Filter *.java -Recurse` → 50 files | Counted manually: 50 files in `backend/src` | ✅ **VERIFIED** | None |
| 3 | 5-tier inheritance hierarchy with exactly 9 model classes | README §3, IMPLEMENTATION_STATUS | `model/` directory listing → 9 files present | All 9 listed classes found | ✅ **VERIFIED** | None |
| 4 | GoF State Pattern with 6 state classes | README §3, IMPLEMENTATION_STATUS | `state/` → 6 files | All 6 found | ✅ **VERIFIED** | None |
| 5 | 4-Gate Fail-Fast Pipeline with 8 classes | README §1, TEAM_HANDOVER | `pipeline/` → 8 files | All 8 found | ✅ **VERIFIED** | None |
| 6 | Polyglot: C++, C#, Python mirrors | README §1, TEAM_HANDOVER | `polyglot/cpp/`, `/csharp/`, `/python/` each contain 1 file | All 3 polyglot files present | ✅ **VERIFIED** | None |

### B. Authentication & Security Claims

| # | Claim | Doc Source | Evidence Location | Verification | Result | Required Correction |
|---|---|---|---|---|---|---|
| 7 | PBKDF2-HMAC-SHA256 with 100,000 iterations, 32-byte salt | README §5, SECURITY_REVIEW §2.1 | `PasswordHasher.java` L19-22: `ITERATIONS=100_000`, `SALT_BYTES=32`, algorithm `PBKDF2WithHmacSHA256` | Code matches exactly | ✅ **VERIFIED** | None |
| 8 | Constant-time comparison uses `MessageDigest.isEqual` | SECURITY_REVIEW §2.1 | `PasswordHasher.java` L55-62: custom XOR loop named `MessageDigestConstantTimeEquals` | **Does NOT use `MessageDigest.isEqual`**. Uses a hand-rolled char XOR loop. The loop is functionally constant-time for equal-length strings, but the doc attribution is inaccurate. | ❌ **INCORRECT** | SECURITY_REVIEW §2.1 must be corrected: "Constant-time custom XOR comparison (not `MessageDigest.isEqual`)" |
| 9 | Session tokens are 256-bit (32-byte) cryptographically secure | README §5, SECURITY_REVIEW | `AuthService.java` L101-103: `new byte[32]`; `RANDOM.nextBytes`; `HexFormat.of().formatHex` | 32 bytes × 8 = 256 bits. ✓ | ✅ **VERIFIED** | None |
| 10 | RBAC enforced — unauthenticated callers get 403 (F-02, F-03) | SECURITY_REVIEW §4, IMPLEMENTATION_STATUS | `MainServer.java` L1380, L1405: `caller == null \|\| !isUserAdmin(caller)` → 403 | Both admin user-status and device-state handlers correctly block null callers | ✅ **VERIFIED** | None |
| 11 | CWE-200 fix: OTP masked to `""` for unauthenticated requests | README §5, SECURITY_REVIEW §2.2 | `MainServer.java` L787-798: `maskedOtp=""` by default; only set for admin or matching renter | Correctly implemented | ✅ **VERIFIED** | None |
| 12 | OTP leakage via `/api/assets/reserve` response | (Not documented as a risk) | `MainServer.java` L363-365: `"otpPin":"%s"` in the 200 OK reserve response | **The `handleReserve` endpoint returns the cleartext OTP to the caller.** While the caller is the renter making the reservation (expected), this bypasses the locker-grid OTP masking and is not documented. For a sandbox this is arguably acceptable, but SECURITY_REVIEW does not note it. | ⚠️ **PARTIAL** | SECURITY_REVIEW should note that the OTP is also transmitted in the `/api/assets/reserve` response body (not just via the Locker API). Document as "by design" or add a note. |
| 13 | `data/users_auth.txt` storage schema described as `userId\|email\|fullName\|role\|saltHex\|hashHex\|status\|createdAt` | SECURITY_REVIEW §2.1, DEPLOYMENT §4 | Actual file line: `host_faaris\|faaris@rentsphere.in\|Mohammed Faaris Khan\|<hash>\|<salt>\|HOST\|+91...\|true\|ACTIVE\|<ts>` | **Schema in docs has wrong column order.** Actual order is: `userId\|email\|fullName\|HASH\|SALT\|role\|phone\|kycBool\|status\|createdAt` (10 cols). Docs omit `phone` and `kycBool` columns entirely and put role before salt. | ❌ **INCORRECT** | SECURITY_REVIEW §2.1 and DEPLOYMENT §4 schema table must be corrected to the 10-column actual schema. |

### C. Payment & Reservation Claims

| # | Claim | Doc Source | Evidence Location | Verification | Result | Required Correction |
|---|---|---|---|---|---|---|
| 14 | Payment verification triggers reservation atomically | IMPLEMENTATION_STATUS row "Live Payment Order to Reservation Auto-Link" | `MainServer.java` L999-1020 `handlePaymentVerify` | **`handlePaymentVerify` only marks the order as CAPTURED. It does NOT call `handleReserve` or lock escrow.** The reservation must be made separately by the frontend JavaScript after payment succeeds. | ⚠️ **PARTIAL** | IMPLEMENTATION_STATUS already flags this as "PARTIALLY IMPLEMENTED". README §4 "Checkout & Payment Sandbox Modal" does not explicitly warn users of this two-step dependency. Add a note to README §4. |
| 15 | Payment can be CAPTURED without a corresponding reservation | (Not documented) | `handlePaymentVerify` L1011: `paymentGateway.verifyPaymentSignature(orderId, paymentId, signature)` sets status=CAPTURED. `handleReserve` is a separate call. | **YES — a user can call `/api/payments/verify` after creating a payment order, mark it CAPTURED, then never call `/api/assets/reserve`.** The payment is logged as captured in `payments_ledger.txt` but no escrow is locked, no reservation is created, and no asset state changes. This is an orphaned capture risk in the sandbox. | ❌ **INCORRECT** | IMPLEMENTATION_STATUS must explicitly flag this as a defect: "Orphaned payment capture possible if `/api/assets/reserve` is not called after `/api/payments/verify`." SECURITY_REVIEW should note it as a residual sandbox limitation. |
| 16 | Sandbox secret hardcoded as `rzp_test_secret_rentsphere_goa_2026` | SECURITY_REVIEW §3 note, DEPLOYMENT §3 | `SandboxPaymentAdapter.java` L25 | Secret is hardcoded as an instance field. **SECURITY_REVIEW §3 references a *different* secret string (`rentsphere_sandbox_secret_2026`) than the actual code value.** | ❌ **INCORRECT** | SECURITY_REVIEW §3 should either match the actual code string or avoid quoting the secret value altogether (preferred for a public repo). |
| 17 | Webhook handler is idempotent | SECURITY_REVIEW §2.3 | `SandboxPaymentAdapter.java` L77-90: `processedWebhooks` set | Idempotency correctly implemented | ✅ **VERIFIED** | None |
| 18 | Webhook handler ignores actual payload; uses hardcoded `order_test/pay_test` | (Not documented) | `MainServer.java` L1033: `paymentGateway.processWebhook("payment.captured", "order_test", "pay_test", "sandbox_valid_sig")` | The webhook body is read but discarded. Actual `orderId`/`paymentId` from the payload are never parsed. The handler always tests a hardcoded dummy order. This is a sandbox approximation but is not disclosed. | ⚠️ **PARTIAL** | Add a comment in IMPLEMENTATION_STATUS noting the webhook handler is a stub that does not parse the real payload. |

### D. Credential & Default Account Claims

| # | Claim | Doc Source | Evidence Location | Verification | Result | Required Correction |
|---|---|---|---|---|---|---|
| 19 | Default password for all seeded accounts is `RentSphere@2026` | README §4, DEPLOYMENT §2, TEAM_HANDOVER §2 | `AuthService.java` L232: `String defaultPw = "RentSphere@2026"` | Matches | ✅ **VERIFIED** | None — appropriate for student prototype; password is clearly labelled as a default seed credential for demo use. |
| 20 | Admin email listed as `almeida@unigoa.ac.in` in DEPLOYMENT/TEAM_HANDOVER | DEPLOYMENT §2, TEAM_HANDOVER §2 | `users_auth.txt` row 4: admin email = `admin@rentsphere.in`; `AuthService.java` L239: `seedAccount(…, "admin@rentsphere.in", …)` | **INCORRECT in docs.** The seeded admin user's email is `admin@rentsphere.in`, but DEPLOYMENT §2 and TEAM_HANDOVER §2 tables list it as `almeida@unigoa.ac.in`. README §4 also contradicts itself: the "How to Build" section says `almeida@unigoa.ac.in` but seed code uses `admin@rentsphere.in`. | ❌ **INCORRECT** | README §4, DEPLOYMENT §2, and TEAM_HANDOVER §2 must all be corrected to reflect the actual seeded email `admin@rentsphere.in`. |
| 21 | Default accounts are appropriate for a student prototype | DEPLOYMENT §2 | Seed credentials are test-only `@rentsphere.in` domain; password is clearly labelled "RentSphere@2026"; seeding only occurs when data file is absent | Credentials are reasonable for academic prototype; no real secrets present | ✅ **VERIFIED** | None. |
| 22 | Test pilot accounts in `users_auth.txt` (USR-107 … USR-118) | (Not documented) | `users_auth.txt` lines 5-19: 12 test pilot accounts with emails `test_pilot_*@unigoa.ac.in` and same phone number | These accounts are test artefacts from live test runs left in the data file. They are not harmful but inflate the user list. Not mentioned anywhere in documentation. | ⚠️ **PARTIAL** | IMPLEMENTATION_STATUS or README should note that live test runs create persistent accounts. A startup cleanup or reset procedure should be documented. |

### E. Locker Hub Claims

| # | Claim | Doc Source | Evidence Location | Verification | Result | Required Correction |
|---|---|---|---|---|---|---|
| 23 | OTP brute-force lockout: 3 strikes → 15-minute lockout; OTP expires in 60 minutes | README §5, SECURITY_REVIEW §2.2 | `LockerCompartment.java` L119-121: `MAX_FAILED_ATTEMPTS=3`, `LOCKOUT_DURATION_MS=15*60*1000`, `OTP_VALIDITY_MS=60*60*1000` | Matches exactly | ✅ **VERIFIED** | None |
| 24 | 8×8 grid = exactly 64 bays | README §4, IMPLEMENTATION_STATUS | `SmartLockerGrid8x8.java` (not read directly); test assertion "64 physical bays" PASS | Verified via passing test | ✅ **VERIFIED** | None |
| 25 | `locker_grid_8x8.txt` schema described as `bayId\|row\|col\|sizeTier\|occupied\|assetId\|deviceState\|otpPin\|temp\|humidity\|weight` | DEPLOYMENT §4 | `LockerCompartment.java` L164-168: `serializeToPipe` outputs 11 fields | Schema matches DEPLOYMENT §4 description — correct | ✅ **VERIFIED** | None |

### F. Persistence Claims

| # | Claim | Doc Source | Evidence Location | Verification | Result | Required Correction |
|---|---|---|---|---|---|---|
| 26 | Atomic write using `.tmp` + `Files.move(ATOMIC_MOVE, REPLACE_EXISTING)` | SECURITY_REVIEW §2.4, DEPLOYMENT §5 | `AuthService.java` L206-226, `SandboxPaymentAdapter.java` L152-173, `FlatFilePersistenceEngine.java` (not read in full but tested) | Both individually read files implement atomic write; test suite confirms staging file pattern | ✅ **VERIFIED** | None |
| 27 | `data/backups/` automated backup directory | DEPLOYMENT §5 | Directory does not exist; no backup scheduler or automation code found in source | Backup is a **manual** procedure only (shell commands documented). "Automated backup" phrase in DEPLOYMENT §5 is misleading. | ❌ **INCORRECT** | DEPLOYMENT §5 heading "If an automated backup exists in `data/backups/`" should be changed to "If a manual backup snapshot exists…". No automated backup mechanism is implemented. |

### G. "Production Ready" & Legal Compliance Claims

| # | Claim | Doc Source | Evidence Location | Verification | Result | Required Correction |
|---|---|---|---|---|---|---|
| 28 | "APPROVED FOR CONTROLLED PILOT DEPLOYMENT" | SECURITY_REVIEW §5 | Full review performed | Appropriate for a **campus-scale academic prototype** with the residual risks clearly listed in §3 (no TLS, sandbox payment, simulated IoT, in-memory KYC). The phrase is acceptable given those caveats are present in the same document. | ⚠️ **PARTIAL** | Add a disclaimer that "Controlled Pilot" refers to supervised lab/campus use only, not internet-facing commercial operation. |
| 29 | "DPDP Act 2023 & IT Rules 2021 legal compliance" | README badges, POLICY_REVIEW_CHECKLIST | `PolicyService.java` → 20 policies in memory; `data/policy_acceptance.txt` logs consent | Policies exist as **in-memory text strings only**. They have not been reviewed by a licensed Indian legal professional. README badge says "Compliance" which overstates. | ⚠️ **PARTIAL** | README badge should read "Regulatory Policy Framework" instead of "Compliance". POLICY_REVIEW_CHECKLIST already contains a legal-counsel sign-off section — it should be the canonical caveat location. |
| 30 | "Residual Critical Vulnerabilities: 0" | SECURITY_REVIEW §5 | See finding in row #15 above (orphaned payment capture) | This statement is not fully supportable given the orphaned payment capture risk (row #15) and the webhook stub (row #18), even in sandbox context. | ⚠️ **PARTIAL** | Add finding F-11: "Orphaned payment capture in sandbox (payment CAPTURED without corresponding reservation)" to SECURITY_REVIEW §4. |

### H. Document & Class Existence Claims

| # | Claim | Doc Source | Evidence Location | Verification | Result | Required Correction |
|---|---|---|---|---|---|---|
| 31 | `PrototypeCloner.java` and `ReflectionInspector.java` exist | IMPLEMENTATION_STATUS | `meta/` directory listing | Both files present | ✅ **VERIFIED** | None |
| 32 | `TrustScoreEngine.java` and `GenericRepository.java` exist | IMPLEMENTATION_STATUS | `meta/` directory listing | Both files present | ✅ **VERIFIED** | None |
| 33 | `docs/UML_DIAGRAMS.md` contains Mermaid UML diagrams | README §8 | `docs/UML_DIAGRAMS.md` exists (10,942 bytes) | File exists and is non-trivial in size | ✅ **VERIFIED** | None |
| 34 | `config/rentsphere.properties` sets `app.environment=production` | (Not claimed in docs) | `config/rentsphere.properties` L6: `app.environment=production` | The config says "production" but the application is a campus sandbox. This property appears to be unused by the code (MainServer does not read it). | ⚠️ **PARTIAL** | Change `app.environment` to `development` or `pilot` to avoid misleading claim, or remove the property if it is unused. |

---

## Part 3 — Top-5 Highest-Priority Corrections

### P-1 🔴 Admin Email Inconsistency (All Credential Tables)
**Finding:** README §4, DEPLOYMENT §2, TEAM_HANDOVER §2 list the admin login email as `almeida@unigoa.ac.in`, but the actual seeded account in both `AuthService.java` and `data/users_auth.txt` uses `admin@rentsphere.in`.

**Impact:** Anyone following the documentation to log in as admin will fail authentication. This is the most operationally critical documentation bug.

**Fix:** Replace `almeida@unigoa.ac.in` with `admin@rentsphere.in` in README §4, DEPLOYMENT §2, and TEAM_HANDOVER §2.

---

### P-2 🔴 Orphaned Payment Capture (SECURITY_REVIEW & IMPLEMENTATION_STATUS)
**Finding:** `POST /api/payments/verify` marks an order `CAPTURED` and returns 200 OK without triggering a reservation. A user can call this endpoint without subsequently calling `/api/assets/reserve`, leaving a payment recorded in `payments_ledger.txt` with no corresponding asset state change or escrow lock.

**Impact:** In a real deployment this would result in a payment collected with no service delivered. In the sandbox, it can cause confusion and the `SECURITY_REVIEW §5` claim of "0 residual critical vulnerabilities" is inaccurate.

**Fix:** Add finding F-11 to SECURITY_REVIEW §4. Update IMPLEMENTATION_STATUS to explicitly flag this as a known sandbox defect. Optionally (code change): have `handlePaymentVerify` also invoke `handleReserve` logic when verification succeeds, making payment→reservation atomic on the server side.

---

### P-3 🔴 `users_auth.txt` Schema Wrong in Docs
**Finding:** SECURITY_REVIEW §2.1 and DEPLOYMENT §4 both publish a schema of 8 columns (`userId|email|fullName|role|saltHex|hashHex|status|createdAt`). The actual file has **10 columns** in a **different order**: `userId|email|fullName|HASH|SALT|role|phone|kycBool|status|createdAt`.

**Impact:** Anyone trying to manually read or repair the credential file using the documented schema will misinterpret the hash/salt fields and the column order.

**Fix:** Correct both schema tables to reflect the actual 10-column format.

---

### P-4 🟡 Constant-Time Comparison Attribution (SECURITY_REVIEW §2.1)
**Finding:** SECURITY_REVIEW §2.1 states verification uses `MessageDigest.isEqual`. The actual implementation (`PasswordHasher.java` L55-62) uses a custom XOR-loop named `MessageDigestConstantTimeEquals`. The loop is functionally correct and constant-time for equal-length hex strings, but the attribution to a standard library method is false and could undermine trust during a security audit.

**Fix:** Correct SECURITY_REVIEW §2.1 to say "constant-time custom XOR comparison (equivalent to MessageDigest.isEqual semantics)" or switch the implementation to use `java.security.MessageDigest.isEqual(a.getBytes(), b.getBytes())` (one-liner, stdlib-audited).

---

### P-5 🟡 Sandbox Secret Value Quoted Differently Across Docs
**Finding:** SECURITY_REVIEW §3 references secret `rentsphere_sandbox_secret_2026`; the actual code in `SandboxPaymentAdapter.java` L25 uses `rzp_test_secret_rentsphere_goa_2026`. The value is hardcoded in a public repo.

**Impact:** The documentation creates confusion; the discrepancy also highlights that the secret is tracked in Git history.

**Fix (two actions):**
1. Correct SECURITY_REVIEW §3 to match the actual string OR (preferred) remove the quoted secret value from the documentation entirely.
2. Add a note that in any real deployment this value must be externalized as an environment variable and the repo history sanitised with `git filter-repo`.

---

## Part 4 — Consistent Feature Status List

| Feature | Status |
|---|---|
| 5-Tier Java Inheritance Hierarchy (9 model classes) | **IMPLEMENTED & VERIFIED** |
| GoF State Pattern — 5 states + mandatory quarantine | **IMPLEMENTED & VERIFIED** |
| 4-Gate Fail-Fast Pipeline | **IMPLEMENTED & VERIFIED** |
| 8×8 Smart Locker Grid (64 bays, Row H spatial invariant) | **IMPLEMENTED & VERIFIED** |
| PBKDF2-HMAC-SHA256 Password Hashing (100k iterations) | **IMPLEMENTED & VERIFIED** |
| 256-bit Secure Session Tokens | **IMPLEMENTED & VERIFIED** |
| Role-Based Access Control (VISITOR/RENTER/HOST/ADMIN) | **IMPLEMENTED & VERIFIED** |
| OTP Brute-Force Lockout (3 strikes / 15 min / 60 min expiry) | **IMPLEMENTED & VERIFIED** |
| Zero OTP Leakage on Public Locker API | **IMPLEMENTED & VERIFIED** |
| Double-Entry EscrowVault (lock/release/slash) | **IMPLEMENTED & VERIFIED** |
| Indian Payment Gateway Sandbox (HMAC-SHA256, idempotent webhook) | **IMPLEMENTED & VERIFIED** |
| Damage Dispute Filing & Admin Arbitration | **IMPLEMENTED & VERIFIED** |
| 20 Indian Statutory Policies & Versioned Consent Logging | **IMPLEMENTED & VERIFIED** |
| IT Rules 2021 Grievance Ticket Lifecycle (24h SLA) | **IMPLEMENTED & VERIFIED** |
| In-App Notification Center (flat-file persistence) | **IMPLEMENTED & VERIFIED** |
| Prototype Deep Cloning (isolated `serviceLogs`) | **IMPLEMENTED & VERIFIED** |
| Runtime Reflection Inspector (`java.lang.reflect`) | **IMPLEMENTED & VERIFIED** |
| RentSphere Trust Score Calculator (weighted, What-If deep clone) | **IMPLEMENTED & VERIFIED** |
| Deterministic Flat-File Persistence (atomic `.tmp` write) | **IMPLEMENTED & VERIFIED** |
| Write-Ahead Log (append-only audit) | **IMPLEMENTED & VERIFIED** |
| Atomic reservation + escrow lock (single-step, server-side) | **IMPLEMENTED & VERIFIED** |
| Responsive Web Frontend — 5 product views | **IMPLEMENTED & VERIFIED** |
| Polyglot Mirrors (C++20, C#, Python 3.12) | **IMPLEMENTED** (no automated tests for polyglot files) |
| Atomic payment+reservation saga (payment verify → auto-reserve) | **SIMULATED** — two separate client-driven calls; orphaned capture possible |
| Physical IoT locker hardware (ESP32/MQTT solenoids) | **SIMULATED BY DESIGN** — `SimulatedLockerDevice` only |
| Real banking payment rails (Razorpay/Cashfree production keys) | **SIMULATED BY DESIGN** — sandbox HMAC adapter only |
| DigiLocker / HyperVerge KYC integration | **SIMULATED BY DESIGN** — in-memory flag only |
| Automated data backup scheduler | **PLANNED** — only a manual shell-command procedure is documented |
| Webhook payload parsing (real `orderId` from body) | **STUB** — hardcoded test values; real payload discarded |
| TLS / HTTPS termination | **PLANNED** — Nginx config documented but no server-side TLS |

---

## Part 5 — Document Consolidation Recommendations

| Document | Recommendation | Rationale |
|---|---|---|
| **README.md** | **Retain & Patch** | Primary entry point; fix admin email (P-1) and add orphaned-payment caveat |
| **IMPLEMENTATION_STATUS.md** | **Retain & Patch** | Best-quality status document; add F-11 orphaned capture row; already honest about simulations |
| **SECURITY_REVIEW.md** | **Retain & Patch** | Good structure; fix constant-time attribution (P-4), schema (P-3), secret mismatch (P-5), add F-11 |
| **DEPLOYMENT.md** | **Retain & Patch** | Fix admin email, fix `data/backups` "automated" claim, fix schema table |
| **TEAM_HANDOVER.md** | **Retain & Patch** | Fix admin email; otherwise accurate and useful for viva-voce |
| **PROJECT_ARCHITECTURE.md** | **Retain** | Comprehensive; no critical errors found in this pass |
| **DECISION_LOG.md** | **Retain** | ADR format is valuable; no critical errors found |
| **GAP_ANALYSIS.md** | **Merge or Shorten** | Overlaps significantly with IMPLEMENTATION_STATUS; consider merging into one document |
| **TEST_REPORT.md** | **Retain & Update** | Should reference the live test run in this audit as an independent verification |
| **IMPLEMENTATION_PLAN.md** | **Archive / Shorten** | Development plan; now largely historical. Reduce to a brief §1 in DEVELOPMENT_LOG |
| **DEVELOPMENT_LOG.md** | **Retain as Archive** | Useful engineering history; no action required |
| **POLICY_REVIEW_CHECKLIST.md** | **Retain** | Correct caveat about needing legal counsel |
| **IDEAS_BACKLOG.md** | **Retain** | Useful for future work; no inconsistencies found |
| **docs/UML_DIAGRAMS.md** | **Retain** | Diagrams exist; no structural issues found |

---

## Part 6 — Proposed Code Changes (Awaiting Approval)

> **No code changes have been made.** The following changes are proposed. Each requires explicit approval before implementation.

| ID | Priority | File | Proposed Change | Rationale |
|---|---|---|---|---|
| C-1 | 🔴 High | `backend/src/rentsphere/auth/PasswordHasher.java` | Replace custom XOR loop with `java.security.MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8))` | Use stdlib constant-time comparison that is independently audited and matches the documentation claim |
| C-2 | 🔴 High | `backend/src/rentsphere/auth/AuthService.java` | Change seeded admin email from `admin@rentsphere.in` to `almeida@unigoa.ac.in` (or vice versa) — whichever is the intended canonical value | Resolve email discrepancy between code and all documentation tables |
| C-3 | 🟡 Medium | `backend/src/rentsphere/api/MainServer.java` `handlePaymentVerify()` | After successful signature verification, check if the associated `PaymentOrder` bookingId maps to an existing reservation. If not, either: (a) automatically invoke reservation logic, or (b) return an additional warning field in the JSON response reminding the client to call `/api/assets/reserve` | Prevent silent orphaned payment captures |
| C-4 | 🟡 Medium | `backend/src/rentsphere/api/MainServer.java` `handlePaymentWebhook()` | Parse `orderId` and `paymentId` from the incoming JSON body rather than using hardcoded `order_test`/`pay_test` | Make the webhook handler functional rather than a stub |
| C-5 | 🟢 Low | `config/rentsphere.properties` | Change `app.environment=production` → `app.environment=pilot` | Avoid misleading environment label; the value currently appears unused by the application code |

---

## Part 7 — Secrets & Sensitive Credential Assessment

| Item | Location | Assessment |
|---|---|---|
| Default seed password `RentSphere@2026` | `AuthService.java` L232, README, DEPLOYMENT, TEAM_HANDOVER | **Acceptable.** Clearly labelled as a demo default; PBKDF2-hashed at rest. No real user data at risk. |
| Sandbox HMAC secret `rzp_test_secret_rentsphere_goa_2026` | `SandboxPaymentAdapter.java` L25 | **Low risk in current state** (sandbox test secret, not a live production key). However, the value is committed to Git history. Recommend externalising to an env variable before any public hosting. |
| Seeded personal phone numbers (`+91 98221 1000x`) | `AuthService.java` L234-239 | **Low risk** — clearly fictional test numbers. Not real personal data. |
| Test pilot accounts in `data/users_auth.txt` | `data/users_auth.txt` lines 5-19 | Hashed credentials only; low risk. Should be cleared from the data file before submission if a clean state is desired. |

---

*Review completed. No application code, git history, or documentation files were modified during this audit.*
