# RentSphere Deployment, Operations & Recovery Guide

**Application:** RentSphere: Peer-to-Peer Equipment Rental Marketplace & 8×8 Smart Pickup Hub  
**Target Architecture:** Java SE 17+ / 25 (Standard Built-in APIs Only, Zero External JARs)  
**Persistence:** Deterministic Flat-File Ledger with Write-Ahead Logging (WAL)  
**Environment:** Local Campus Pilot / Staging / Production Architecture  

---

## 1. Quick Start Guide (Zero External JAR Dependencies)

RentSphere runs on **pure standard Java SE**. It does **NOT** require Maven, Gradle, Spring Boot, or external JAR downloads to compile or run. Everything is compiled directly with standard `javac` and run with `java`.

### Prerequisites
- **Java Development Kit (JDK):** JDK 17, 21, or 25 installed and available on `PATH`.
- **Git:** Standard Git client.
- **Operating System:** Windows, Linux, or macOS.

### Step 1: Clone & Verify Directory Structure
```bash
git clone <repository-url>
cd OOT-MINIPROJECT
```

### Step 2: Compile the Entire Codebase
On Windows (PowerShell):
```powershell
# Create output directory for class files
if (!(Test-Path "bin")) { New-Item -ItemType Directory -Path "bin" }

# Compile all backend classes and verification tests into bin/
Get-ChildItem -Path "backend/src", "tests" -Filter "*.java" -Recurse | ForEach-Object { $_.FullName } | Out-File -Encoding ASCII sources.txt
javac -d bin -sourcepath "backend/src;tests" @sources.txt
```

On Linux / macOS (Bash):
```bash
mkdir -p bin
find backend/src tests -name "*.java" > sources.txt
javac -d bin -sourcepath "backend/src:tests" @sources.txt
```

### Step 3: Run the Automated Verification Suite
Verify all 76 unit, integration, and security tests before launching the server:
```bash
java -cp bin tests.RentSphereVerificationTest
```
*Expected Output:* `VERIFICATION SUMMARY: 76 / 76 TESTS PASSED (100% SUCCESS)`.

### Step 4: Launch the RentSphere Application Server
```bash
# Starts Java HTTP server on port 8080 (serving both /api/* REST endpoints and frontend UI)
java -cp bin rentsphere.api.MainServer 8080
```

### Step 5: Access the Application
Open your web browser and navigate to:
```
http://localhost:8080/
```

---

## 2. Default Seed Accounts & Persona Credentials

The platform is pre-seeded with realistic Goa community members spanning all system roles. All seeded accounts use the secure default password:

**Default Password for all seeded accounts:** `RentSphere@2026`

| Persona / Name | User ID | Email Address | Role | Default Balance | Description |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Dr. A. Almeida** | `inspector_almeida` | `almeida@unigoa.ac.in` | `ADMIN` | ₹80,000 | Faculty In-Charge, Grievance Officer, Hub Administrator |
| **Faaris Shaikh** | `host_faaris` | `faaris@rentsphere.in` | `HOST` | ₹45,000 | Cine Equipment Host (Sony FX6, Sound Devices) |
| **Ketan Sinai** | `host_ketan` | `ketan@rentsphere.in` | `HOST` | ₹38,000 | Agritech & Mobility Host (Mahindra Thar, Rotary Tiller) |
| **Deepak Naik** | `renter_deepak` | `deepak@unigoa.ac.in` | `RENTER` | ₹25,000 | Verified Student / Renter |
| **Nikhil Borkar** | `renter_nikhil` | `nikhil@rentsphere.in` | `RENTER` | ₹18,000 | Verified Community Renter |
| **Priya Kamat** | `renter_priya` | `priya@rentsphere.in` | `RENTER` | ₹30,000 | Verified Wedding & Event Planner Renter |

*Note: Visitors can also register new accounts instantly via the "Log In / Register" modal on the homepage.*

---

## 3. Environment Modes & Configuration

RentSphere defines four operating tiers:

| Operating Tier | Payment Gateway | Locker Hardware | Identity / KYC | TLS / SSL |
| :--- | :--- | :--- | :--- | :--- |
| **Local Development** | In-memory sandbox | `SimulatedLockerDevice` | In-memory verified accounts | Plain HTTP (`localhost:8080`) |
| **Automated Testing** | Deterministic Mock | Test fixtures (`LockerCompartment`) | Pre-configured test users | In-memory unit test harness |
| **Staging Pilot** | Sandbox Gateway with HMAC | Simulated device with sensor logs | Self-certification & admin review | Plain HTTP or Self-signed TLS |
| **Commercial Production** | Razorpay / Cashfree API | IoT MQTT Solenoid Hub | DigiLocker / HyperVerge API | TLS 1.3 Reverse Proxy (Nginx) |

### Environment Variables (.env.example)
Create a `.env` file in the root directory for staging/production deployments:
```ini
# Server Port Configuration
RENTSPHERE_PORT=8080
RENTSPHERE_ENV=development # development | staging | production

# Payment Gateway (Staging / Production)
PAYMENT_GATEWAY_PROVIDER=sandbox # sandbox | razorpay | cashfree
PAYMENT_GATEWAY_KEY_ID=rzp_test_YourKeyIdHere
PAYMENT_GATEWAY_KEY_SECRET=YourSecretKeyHere
PAYMENT_WEBHOOK_SECRET=YourWebhookSecretHere

# Smart Pickup Hub IoT Controller (Production)
HUB_DEVICE_TYPE=simulated # simulated | mqtt_hardware
HUB_MQTT_BROKER_URL=ssl://iot.rentsphere.in:8883
HUB_MQTT_CLIENT_ID=goa_hub_alpha_01
HUB_MQTT_TOPIC_COMMANDS=rentsphere/hub/commands
HUB_MQTT_TOPIC_TELEMETRY=rentsphere/hub/telemetry

# Statutory Grievance Redressal
GRIEVANCE_OFFICER_NAME=Dr. A. Almeida
GRIEVANCE_OFFICER_EMAIL=grievance@rentsphere.in
GRIEVANCE_OFFICER_PHONE=+91-832-6519000
```

---

## 4. Flat-File Persistence Architecture & Write-Ahead Log (WAL)

All application records are deterministically stored in the `data/` directory using pipe-delimited (`|`) UTF-8 flat files:

| File Path | Description | Schema / Content |
| :--- | :--- | :--- |
| `data/assets_ledger.txt` | Master catalog of rentable equipment | `id|name|cat|tariff|collateral|owner|status|specs|location|ground` |
| `data/locker_grid_8x8.txt` | 8×8 smart locker compartment matrix | `bayId|row|col|sizeTier|occupied|assetId|deviceState|otpPin|temp|humidity|weight` |
| `data/escrow_vault.txt` | User wallet balances & collateral accounts | `userId|userRole|balance|lockedEscrow|kycStatus` |
| `data/users_auth.txt` | PBKDF2 hashed user credentials | `userId|email|fullName|role|saltHex|hashHex|status|createdAt` |
| `data/payments_ledger.txt` | Order records & HMAC payment transactions | `orderId|bookingId|renterId|tariff|deposit|total|provider|status|paymentId|createdAt` |
| `data/disputes_ledger.txt` | Damage dispute claims and arbitration | `disputeId|bookingId|assetId|renterId|hostId|reason|notes|collateral|status|resolution` |
| `data/policy_acceptance.txt`| Versioned consent audit log (DPDP Act) | `consentId|userId|policyId|version|acceptedAtUtc|ipAddress` |
| `data/support_tickets.txt` | Statutory grievance and support tickets | `ticketId|userId|category|subject|description|status|createdAt|updatedAt` |
| `data/audit_wal.txt` | Append-only Write-Ahead Log (WAL) | `[timestamp] [threadId] [category] TX_DETAILS` |

---

## 5. Crash Recovery & Backup Procedures

### Atomic Write Protection
Every file update in RentSphere follows an **atomic write-and-replace** protocol:
1. Target data is written to a temporary staging file: `data/<filename>.txt.tmp`.
2. Flushed and synced to physical disk.
3. Renamed to replace the live file via `Files.move(..., REPLACE_EXISTING, ATOMIC_MOVE)`.
This guarantees that an unexpected power failure or OS kill signal cannot leave a half-written, corrupted file on disk.

### Disaster Recovery Procedure
If a file becomes corrupted or unreadable:
1. Stop the application server:
   ```powershell
   Get-Process -Name java | Stop-Process -Force
   ```
2. Inspect the latest transactions in `data/audit_wal.txt`:
   ```powershell
   Get-Content data/audit_wal.txt -Tail 50
   ```
3. If an automated backup exists in `data/backups/`, restore the latest snapshot:
   ```powershell
   Copy-Item "data/backups/assets_ledger_*.txt" "data/assets_ledger.txt" -Force
   ```
4. Replay transactions from the Write-Ahead Log since the snapshot timestamp.
5. Restart the server and verify health via `http://localhost:8080/api/health`.

### Manual Backup Command
To take an immediate point-in-time snapshot of all ledgers:
```powershell
$timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
New-Item -ItemType Directory -Path "data/backups" -Force | Out-Null
Copy-Item "data/*.txt" "data/backups/" -Filter { $_.Name -notlike "*backups*" }
```

---

## 6. Production Reverse Proxy Deployment (Nginx Configuration)

For production internet deployment, place Nginx in front of RentSphere for SSL termination and static asset caching:

```nginx
server {
    listen 80;
    server_name rentsphere.in www.rentsphere.in;
    return 301 https://$host$request_uri;
}

server {
    listen 443 ssl http2;
    server_name rentsphere.in www.rentsphere.in;

    ssl_certificate /etc/letsencrypt/live/rentsphere.in/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/rentsphere.in/privkey.pem;
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;

    # Security Headers
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-Frame-Options "DENY" always;
    add_header X-XSS-Protection "1; mode=block" always;
    add_header Referrer-Policy "strict-origin-when-cross-origin" always;
    add_header Content-Security-Policy "default-src 'self' 'unsafe-inline' https://fonts.googleapis.com https://fonts.gstatic.com;" always;

    # Reverse proxy to Java Backend
    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_read_timeout 90;
    }
}
```

---

## 7. Operational Health Checks & Observability

- **API Health Endpoint:** `GET /api/health`
  - Returns `{"status":"UP","timestamp":"...","uptimeMillis":...}`
- **Active Threads & Memory Check:** Monitored via JVM standard metrics.
- **Log Monitoring:** Audit logs are continuously written to `data/audit_wal.txt`. To follow live logs in real time:
  ```powershell
  Get-Content -Path data/audit_wal.txt -Wait -Tail 20
  ```

*Prepared by:* **RentSphere DevOps & Quality Assurance Team**  
*Approved for:* **Goa University Pilot Execution**
