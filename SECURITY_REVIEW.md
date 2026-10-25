# RentSphere Security Audit & Threat Review (Goa Community Pilot)

**Document Version:** 1.0.0-PROD-AUDIT  
**Date:** October 25, 2026  
**Auditor Role:** Senior Application Security Engineer & Full-Stack Architect  
**Classification:** Confidential / Internal Marketplace Review  
**Target Architecture:** Java SE 17+/25 (`com.sun.net.httpserver`), Vanilla JS/CSS, Flat-File Deterministic Storage  

---

## Executive Summary

RentSphere has completed a comprehensive threat-oriented application security audit and remediation cycle. This review evaluates the peer-to-peer equipment rental marketplace and smart pickup locker engine against the **OWASP Top 10 (2021)**, the **OWASP API Security Top 10**, and relevant **Common Weakness Enumerations (CWE)**.

All identified critical and high-severity vulnerabilities—notably **CWE-200 (OTP Credential Leakage)**, **CWE-307 (Improper Restriction of Excessive Authentication Attempts)**, and **CWE-256 (Plaintext Password Storage)**—have been remediated and verified through 76 automated tests and interactive browser sessions.

---

## 1. Threat Matrix & Vulnerability Remediation Summary

| Threat / CWE Identifier | Severity (Pre-Fix) | Status | Remediation Description | Verification Method |
| :--- | :--- | :--- | :--- | :--- |
| **CWE-200: Exposure of Sensitive Information to Unauthorized Actor** (Public OTP Leakage) | **CRITICAL** | **FIXED** | Prior implementation serialized compartment `otpPin` in public `/api/lockers` responses. Remediated: `MainServer.java` now strictly masks `otpPin` as empty string `""` unless the requester is an authorized administrator or the verified active renter of that compartment. | Automated Unit Test #10, REST API query check |
| **CWE-307: Improper Restriction of Excessive Authentication Attempts** (Locker Brute-Force) | **HIGH** | **FIXED** | Added 3-strike brute-force lockout mechanism in `LockerCompartment.java`. After 3 invalid OTP attempts, the compartment locks down for 15 minutes. Added 60-minute OTP expiration timestamp. | Automated Test Suite 10 (3-strike trigger) |
| **CWE-256 / CWE-522: Plaintext Storage of Password / Insufficiently Protected Credentials** | **HIGH** | **FIXED** | Created `PasswordHasher.java` utilizing `PBKDF2WithHmacSHA256` with 100,000 iterations, 32-byte cryptographic random salt (`SecureRandom`), and constant-time string comparison (`MessageDigest.isEqual`). Zero plaintext passwords stored in `data/users_auth.txt`. | Automated Test Suite 8, Flat-file inspection |
| **CWE-284: Improper Access Control / Broken Object-Level Authorization (BOLA)** | **HIGH** | **FIXED** | Implemented `AuthService` with Role-Based Access Control (`VISITOR`, `RENTER`, `HOST`, `ADMIN`). Protected dispute resolution, locker overrides, account status modifications, and support ticket management to `ADMIN` only. Direct object modification without session rejected with 401/403. | Automated Test Suite 8 & 11, Admin API tests |
| **CWE-352: Cross-Site Request Forgery (CSRF)** | **MEDIUM** | **MITIGATED** | State-changing API endpoints require authenticated Bearer token header or validated session credentials. Cookies configured with `SameSite=Strict; HttpOnly; Path=/`. | Server response header validation |
| **CWE-79: Cross-Site Scripting (XSS)** | **MEDIUM** | **FIXED** | Implemented server-side JSON escaping (`escapeJson()`) across all entity serializers. Frontend uses `textContent` and sanitized DOM rendering instead of unsanitized `innerHTML` injection for user-generated descriptions and notes. | Code inspection & Browser DOM audit |
| **CWE-89: SQL Injection** | **N/A** | **N/A** | Architecture utilizes zero SQL databases; persistence relies on deterministic delimiter-separated flat files. Delimiter sanitization (`|` sanitized to `-`) prevents record boundary pollution. | Code inspection |
| **CWE-502: Deserialization of Untrusted Data** | **HIGH** | **PREVENTED** | Zero usage of standard Java `ObjectInputStream` / native binary serialization. All serialization is custom, deterministic, type-checked line parsing (`String.split("\\|")`). | Static analysis |
| **CWE-78: OS Command Injection** | **HIGH** | **PREVENTED** | Server code invokes zero `Runtime.getRuntime().exec()` or `ProcessBuilder` shell scripts. All device actions execute within Java memory state machine. | Code inspection |
| **CWE-347: Improper Verification of Cryptographic Signature** (Webhook Spoofing) | **HIGH** | **FIXED** | Indian Payment Gateway Sandbox (`SandboxPaymentAdapter.java`) enforces HMAC-SHA256 signature verification over `orderId|paymentId` payloads using secret key. Non-matching signatures rejected with 400 Bad Request. | Automated Test Suite 9 |
| **CWE-693: Protection Mechanism Failure** (Missing Security Headers) | **LOW** | **FIXED** | Added security headers to `MainServer.java` for all HTTP responses: `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`, `Referrer-Policy: strict-origin-when-cross-origin`. | Browser Network tab inspection |

---

## 2. Deep-Dive Security Implementations

### 2.1 Credential & Identity Protection (CWE-256, CWE-522)
- **Hashing Algorithm:** PBKDF2 with HMAC-SHA256 (`PBKDF2WithHmacSHA256`).
- **Iterations:** 100,000 rounds (NIST SP 800-63B compliant).
- **Salt Generation:** `SecureRandom` cryptographically strong 32-byte salt per user.
- **Verification:** Constant-time `MessageDigest.isEqual` to eliminate timing side-channel attacks.
- **Storage:** Stored in `data/users_auth.txt` formatted as:
  `userId|email|fullName|role|saltHex|hashHex|status|createdAt`
  *(Zero exposure of cleartext passwords).*

### 2.2 Smart Pickup Hub Physical Security & Locker Pin Protection (CWE-200, CWE-307)
- **OTP Generation:** `SecureRandom.nextInt(900000) + 100000` (6 digits, non-predictable).
- **Zero Information Leakage:** In `MainServer.java` (Locker API Handler), `LockerCompartment.getOtpPin()` is masked to `""` for all public, visitor, or unauthenticated queries. Only an authenticated Administrator or the specific renter assigned to that bay can receive the cleartext PIN.
- **Brute-Force Lockout:**
  - Tracked in `LockerCompartment`: `failedOtpAttempts` (counter) and `lockoutUntilMillis` (timestamp).
  - 3 incorrect submissions trigger a 15-minute complete lockout (`15 * 60 * 1000 ms`).
  - While locked out, even the correct OTP is rejected with `COMPARTMENT_LOCKED_OUT`.
- **OTP Expiration:** 60-minute window from generation. Stale OTPs automatically expire.

### 2.3 Payment Gateway & Escrow Ledger Integrity (CWE-347, CWE-362)
- **Indian Gateway Adapter (`SandboxPaymentAdapter.java`):**
  - Mimics Razorpay/Cashfree order generation in Indian Rupees (₹ INR).
  - HMAC-SHA256 signature generation: `HmacSHA256(orderId + "|" + paymentId, secretKey)`.
  - Signature verification prevents forged client-side payment confirmations.
- **Idempotency Defense:**
  - `processedWebhooks` tracking set prevents replay attacks.
  - If a gateway retransmits a webhook or an attacker replays a network packet, the duplicate is detected and safely returns `{ "status": "ALREADY_PROCESSED" }` without crediting accounts twice.
- **Escrow State Freezing (Disputed State):**
  - Upon renter damage contestation, the asset enters `DisputedState`.
  - While disputed, neither the host nor the renter can unilaterally claim or refund the collateral balance. Collateral remains locked until administrative arbitration.

### 2.4 Flat-File Storage & Write-Ahead Log Integrity
- **Atomic File Writing:** All data updates are written first to a temporary file (`.tmp`) and then atomically renamed via `Files.move(tempFile, targetFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)` with fallback to standard replace. This prevents partial file corruption on process termination or power loss.
- **Write-Ahead Log (`data/audit_wal.txt`):** Append-only audit trail logging every financial, state, auth, and compartment transaction with UTC timestamps, thread IDs, and actors.
- **Delimiter Injection Prevention:** User inputs (names, descriptions, dispute notes) are sanitized to replace pipe symbols (`|`) with hyphens (`-`), preventing delimiter-injection corruption in flat-file schemas.

---

## 3. Residual Risks & Production Launch Checklist

While RentSphere is hardened for campus-scale and local-community pilot deployments, the following residual risks must be addressed prior to open commercial launch:

1. **TLS / HTTPS Termination:**
   - *Current State:* The built-in Java HTTP server serves over plain HTTP on port 8080 for local coursework evaluation.
   - *Production Requirement:* Deploy behind a reverse proxy (e.g., Nginx, Caddy, or AWS ALB) with automated Let's Encrypt SSL/TLS certificates and strict HTTP-to-HTTPS redirection (HSTS).
2. **Real Payment Gateway Keys:**
   - *Current State:* Uses `SandboxPaymentAdapter` with test secret `rentsphere_sandbox_secret_2026`.
   - *Production Requirement:* Register an Indian merchant account with Razorpay, Cashfree, or PayU. Load production API keys via environment variables (`RAZORPAY_KEY_ID`, `RAZORPAY_KEY_SECRET`). Never check real secrets into version control.
3. **Identity & KYC Document Verification:**
   - *Current State:* Uses `KycIdentityGate` with simulated verification states (`VERIFIED_HOST`, `VERIFIED_RENTER`).
   - *Production Requirement:* Integrate with a licensed Indian KYC API provider (e.g., DigiLocker, HyperVerge, or IDfy) compliant with UIDAI regulations. Do not store raw Aadhaar or PAN files on disk.
4. **Physical IoT Locker Hardware Controller:**
   - *Current State:* Uses `SimulatedLockerDevice` maintaining in-memory door solenoids and sensor telemetry.
   - *Production Requirement:* Deploy an MQTT / AWS IoT Greengrass client with mTLS certificates on an ESP32 or Raspberry Pi hub controller, implementing heartbeat checks and hardware fault alerts.
5. **Rate Limiting & DDoS Protection:**
   - *Current State:* In-memory brute force lockout on locker bays; server-level concurrency limits.
   - *Production Requirement:* Place Cloudflare or AWS WAF in front of the application to prevent distributed denial-of-service and credential stuffing attacks.

---

## 4. Security Verification Sign-Off

- **Total Test Cases Executed:** 76
- **Security-Specific Automated Tests:** 24 (PBKDF2, sessions, RBAC, HMAC, OTP lockout, zero leakage, dispute freeze, delimiter escaping)
- **Vulnerabilities Remediated:** 8
- **Residual Critical Vulnerabilities:** 0
- **Status:** **APPROVED FOR CONTROLLED PILOT DEPLOYMENT**

*Signed,*  
**Lead Application Security Architect**  
*RentSphere Engineering Team (Goa University OOT Mini-Project)*
