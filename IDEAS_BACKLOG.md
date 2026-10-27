# RentSphere: Product & Technical Ideas Backlog

**Project Title**: RentSphere: Peer-to-Peer (P2P) Equipment Rental Marketplace & 8×8 Smart Pickup Hub  
**Institution**: Goa University — Department of Computer Science & Technology  
**Evaluation**: MCA Object-Oriented Technology (OOT) Lab Mini-Project  
**Standard**: Comprehensive tracking of proposed, accepted, deferred, and rejected concepts across codebase history  

---

## 1. Classification Definitions

- **ALREADY IMPLEMENTED**: Formally coded, verified, and operational in the current release.
- **ACCEPTED**: Agreed architectural requirement; scheduled for subsequent development phase.
- **PROPOSED**: Community or architectural concept documented for team review; awaiting formal decision.
- **DEFERRED**: Concept acknowledged as valuable but postponed due to external hardware, commercial, or academic scope constraints.
- **REJECTED**: Formally dismissed because it conflicts with project constraints, academic rules, or architectural philosophy.
- **NEEDS TEAM DECISION**: Requires consensus among student teammates, faculty guide, or legal counsel.

---

## 2. Master Ideas Register

| ID | Concept / Idea | Origin / Evidence | Current Status | Rationale / Decision Basis | Dependencies | Suggested Next Action |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **IDEA-01** | **Physical IoT Microcontroller & RS485 Solenoid Controller** | `PROJECT_AUDIT.md` §3 & `DEVELOPMENT_LOG.md` | **DEFERRED** | Physical microcontrollers (ESP32/RPi) and physical 12V electromagnetic solenoids were not available on workstation. Cleanly abstracted via `SimulatedLockerDevice`. | Physical IoT hardware & relay boards | Wire physical MQTT/GPIO client when deployment hardware is installed at Goa University campus. |
| **IDEA-02** | **Live Production Payment Rails (Razorpay / Cashfree Merchant API)** | `PROJECT_AUDIT.md` §1 & `DEPLOYMENT.md` §3 | **DEFERRED** | Live Indian banking rails require commercial merchant account, GSTIN registration, and corporate bank KYC. High-fidelity sandbox with HMAC verification built instead. | Commercial merchant credentials | Swap sandbox keys for live merchant webhook secrets upon formal pilot launch. |
| **IDEA-03** | **SQL Database / ORM Replacement (SQLite / H2 / MySQL)** | `DEVELOPMENT_LOG.md` Log 001 | **REJECTED** | Explicitly violates the core academic and architectural charter: **Zero-Database Rule** using pure Java OOP and human-readable pipe-delimited flat files in `data/`. | None | Retain flat-file persistence engine with atomic `.tmp` staging and WAL. |
| **IDEA-04** | **Third-Party JWT Library (jjwt / Auth0 / Nimbus)** | `PROJECT_AUDIT.md` §4 | **REJECTED** | Violates the **Zero External JAR Dependencies** constraint. Implemented custom 256-bit cryptographically secure session manager via standard `java.security.SecureRandom`. | None | Maintain built-in Java crypto session manager. |
| **IDEA-05** | **Automated 24-Hour Quarantine Auto-Approval Timer** | `PROJECT_AUDIT.md` §3 (Finding #15) | **PROPOSED** | If a host fails to inspect gear within 24 hours of return, the deposit should automatically be released to the renter to prevent indefinite capital lockup. | `ScheduledExecutorService` daemon in `MainServer` | Implement background timer in `MainServer.java` checking quarantine timestamp $> 24\text{ hours}$. |
| **IDEA-06** | **Server-Side Payment-to-Reservation Atomic Saga** | `GAP_ANALYSIS.md` §3.4 (Gap G-5) | **PROPOSED** | Currently frontend calls `/api/payments/verify` then calls `/api/assets/reserve`. If browser closes between calls, payment is captured without reservation. Server-side saga would link payment order directly to reservation. | `SandboxPaymentAdapter` & `MainServer.handlePaymentVerify` | Add `assetId` and `renterId` to `PaymentOrder` and trigger `executeReservation()` automatically within payment verification handler. |
| **IDEA-07** | **DigiLocker / Aadhaar XML Automated Identity Verification** | `POLICY_REVIEW_CHECKLIST.md` §1 | **NEEDS TEAM DECISION** | Automated Indian KYC verification via DigiLocker API or offline Aadhaar XML. Requires institutional developer account and DPDP Act consent compliance. | Government API credentials & legal sign-off | Discuss with faculty guide whether self-certified student ID uploads are sufficient for campus pilot. |
| **IDEA-08** | **Commercial Rent-a-Cab Licensing for P2P Vehicles** | `POLICY_REVIEW_CHECKLIST.md` §5 | **NEEDS TEAM DECISION** | P2P leasing of vehicles (Row H ground bays) may require commercial permits under the Motor Vehicles Act 1988 unless operated strictly as a private campus carpool demonstration. | Indian legal counsel review | Consult qualified advocate or restrict mobility category to campus demonstration electric scooters. |
| **IDEA-09** | **Multi-Hub Cluster Network Across Multiple Goa Locations** | `README.md` §1 | **PROPOSED** | Expanding beyond the primary Goa University 8×8 hub to additional smart hubs in Margao, Panaji, and Mapusa, allowing cross-hub returns. | Distributed hub state synchronization | Model multi-hub identifiers (`HUB-GU-01`, `HUB-PAN-02`) in `SmartLockerGrid8x8`. |
| **IDEA-10** | **Native Mobile Application (Android / iOS via Capacitor)** | `PROJECT_AUDIT.md` §5 | **DEFERRED** | Responsive web frontend with mobile media queries (`max-width: 480px`) already provides touch-friendly mobile access. Native packaging deferred until backend API stabilizes. | Node.js / Capacitor build toolchain | Package existing `frontend/` directory into an Android APK using WebView/Capacitor after lab evaluation. |
| **IDEA-11** | **Multi-Day Rental Duration Tracking (Finding F-01)** | Codebase Audit 2026-10-25 | **ALREADY IMPLEMENTED** | Multi-day tariff was locked at booking, but 1-day tariff was charged upon return. Remediated: Added `rentalDays` to `RentableAsset` and charged full duration tariff upon return approval. | Core domain model & persistence | Completed in audit remediation; verified by TEST 15. |
| **IDEA-12** | **Zero OTP Leakage to Public Endpoints (CWE-200)** | `PROJECT_AUDIT.md` CWE-200 | **ALREADY IMPLEMENTED** | OTPs were publicly readable in `/api/lockers`. Remediated: Masked OTP as `""` for unauthenticated visitors and non-renters. | `MainServer.java` & `app.js` | Completed and verified by TEST 10. |
| **IDEA-13** | **OTP Brute-Force 3-Strike Lockout (CWE-307)** | `PROJECT_AUDIT.md` CWE-307 | **ALREADY IMPLEMENTED** | Lockout bay for 15 minutes after 3 invalid attempts; expire OTP after 60 minutes. | `LockerCompartment.java` | Completed and verified by TEST 10. |
| **IDEA-14** | **PBKDF2 Password Hashing with Salt (CWE-256)** | `PROJECT_AUDIT.md` CWE-256 | **ALREADY IMPLEMENTED** | Upgraded plain persona switching to PBKDF2-HMAC-SHA256 with 32-byte salt and constant-time compare. | `PasswordHasher.java` | Completed and verified by TEST 8. |
| **IDEA-15** | **Damage Dispute Filing & Administrative Arbitration** | `PROJECT_AUDIT.md` §1 | **ALREADY IMPLEMENTED** | Introduced `DisputedState`, `DisputeRecord`, `DisputeService`, and Admin Arbitration Queue. | Domain model & web UI | Completed and verified by TEST 11. |
| **IDEA-16** | **20 Indian Statutory Policies & Versioned Consent** | `POLICY_REVIEW_CHECKLIST.md` | **ALREADY IMPLEMENTED** | Drafted 20 comprehensive policies and built consent audit logger persisting to `data/policy_acceptance.txt`. | `PolicyService.java` | Completed and verified by TEST 12. |
| **IDEA-17** | **Customer Grievance Redressal (Rule 3(2) IT Rules 2021)** | `POLICY_REVIEW_CHECKLIST.md` §4 | **ALREADY IMPLEMENTED** | Statutory grievance ticketing with 24h SLA compliance clock and admin resolution logging. | `SupportService.java` | Completed and verified by TEST 13. |
| **IDEA-18** | **In-App Notification Center with Flat-File Persistence** | `GAP_ANALYSIS.md` Gap G-3 | **ALREADY IMPLEMENTED** | Real-time notification queue persisting to `data/notifications_log.txt` with cross-restart recovery. | `NotificationService.java` | Completed and verified by TEST 14. |
