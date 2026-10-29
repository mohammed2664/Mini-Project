# 💻 RentSphere: Source Code Architecture

**Project**: RentSphere — Peer-to-Peer (P2P) Asset Rental & 8×8 Smart Micro-Storage Management System  
**Institution**: Department of Computer Science & Technology, Goa University  
**Program**: Master of Computer Applications (MCA) — Object-Oriented Technology (OOT) Lab Mini-Project (2026–2027)  
**Runtime**: Pure Java SE (JDK 17+/25) • Zero External JAR Dependencies • Zero Database (Flat-File Engine)

---

## 📂 Code Organization

```text
code/
├── backend/                       # Core Java SE 17+ backend services
│   └── src/rentsphere/
│       ├── api/                   # Built-in JDK HttpServer (port 8080) & REST endpoints
│       ├── auth/                  # PBKDF2-HMAC-SHA256 crypto, sessions & RBAC
│       ├── dispute/               # Rule 3(2) IT Rules 2021 dispute arbitration
│       ├── escrow/                # Double-entry escrow ledger & financial vault (₹ INR)
│       ├── locker/                # 8×8 (64-bay) Smart Locker grid & IoT sensor telemetry
│       ├── meta/                  # RTTI live reflection & deep/shallow prototype cloner
│       ├── model/                 # 5-tier domain inheritance hierarchy & polymorphic rates
│       ├── payment/               # Indian payment gateway sandbox & HMAC signatures
│       ├── persistence/           # Atomic .tmp flat-file engine & append-only WAL
│       ├── pipeline/              # 4-Gate fail-fast validation pipeline
│       ├── policy/                # 20 statutory Indian legal policies & consent store
│       ├── state/                 # GoF State Pattern (Available, Escrow, Lease, Quarantine)
│       └── support/               # Grievance redressal ticketing service
├── frontend/                      # Modern responsive single-page web interface
│   ├── css/                       # Design system, themes & accessibility styles
│   ├── js/                        # Event-driven browser controller (Vanilla JS ES6+)
│   ├── images/                    # Product imagery & local visual assets
│   ├── index.html                 # Main portal (Marketplace, Hub, Quarantine, Admin)
│   └── product.html               # Dynamic product detail & specification view
├── tests/                         # Comprehensive automated verification test harness
│   └── RentSphereVerificationTest.java # 17 test suites, 163 automated unit/integration tests
├── data/                          # Human-auditable pipe-delimited flat-file ledgers & WAL
│   ├── assets_ledger.txt          # Active equipment catalog & state tracking
│   ├── audit_wal.txt              # Append-only Write-Ahead Log
│   ├── escrow_vault.txt           # Double-entry balances, escrow & earnings
│   ├── locker_grid_8x8.txt        # 64-bay locker matrix physical coordinates & telemetry
│   ├── users_auth.txt             # PBKDF2 hashed credentials & RBAC personas
│   └── policy_acceptance.txt      # Statutory consent records
├── polyglot/                      # Educational polyglot language mirrors
│   ├── cpp/                       # C++20 standard library core engine
│   ├── csharp/                    # C# .NET 8 modern implementation
│   └── python/                    # Python 3.12 dataclasses & pattern matching
├── sources.txt                    # Portable javac compilation source file list
└── START_RENTSPHERE.bat           # 1-click Windows build and startup script
```

---

## 🚀 How to Build & Run

### 1. Requirements
* Java Development Kit (JDK 17 or higher, tested on JDK 17, 21, and 25).
* No external Maven/Gradle downloads or database installations needed.

### 2. Compilation
From this `code/` folder:
```cmd
javac -d bin -encoding UTF-8 @sources.txt
```

### 3. Run Automated Test Suite (163/163 Tests Passed)
```cmd
java -cp bin tests.RentSphereVerificationTest
```

### 4. Start Live Web Application
```cmd
java -cp bin rentsphere.api.MainServer 8080
```
Open your web browser at: **`http://localhost:8080/`**
