# RentSphere What and Why Guide

Use this document when a teammate or examiner asks: “What does this do?”, “Why did you choose it?”, or “Why did you not use another technology?”

## 1. What is the project?

RentSphere is a peer-to-peer equipment rental management system for a Goa campus and local community. It connects equipment hosts with renters and controls the full rental lifecycle from listing to return and inspection.

## 2. Why this problem?

Equipment such as cameras, sound systems, tools and vehicles is expensive to buy and often remains unused. Renters need short-term access, while owners need a safer way to list, reserve, collect deposits and track returns. The project turns that real process into a clear OOP model.

## 3. Why equipment rental instead of a normal shopping website?

A shopping cart mainly ends after purchase. Rental has a longer lifecycle: availability, deposit, reservation, pickup, active use, return, inspection and settlement. That makes rental a better demonstration of state transitions, validation, exceptions and object collaboration.

## 4. Why is Goa part of the project?

Goa provides a believable local context with students, creators, event organisers, tourism activity and community pickup points. The location gives the project a specific user group instead of making it an abstract generic marketplace.

## 5. What does the State Pattern do?

The State Pattern places state-specific rules inside state classes. An available asset can be reserved, an active lease can be returned, and an item in inspection quarantine cannot be rented again.

### Why use it?

Without the State Pattern, one large method would contain many nested `if` statements. Separate state classes make the legal transitions visible, testable and easier to extend.

### Why not use only an enum?

An enum is useful for naming states, but it does not naturally hold different behavior for each state. The State Pattern combines the state value with the operation rules.

## 6. Why four separate validation gates?

Reservation requires several independent checks: identity, asset legality, wallet solvency and locker placement. The fail-fast pipeline makes the checks explicit and stops at the first failure.

### Why not put all checks in `MainServer.java`?

That would make the HTTP controller responsible for business rules and make it difficult to test outside the browser. The server should coordinate requests while domain classes enforce rules.

## 7. Why use pure Java instead of Spring Boot?

The project is for an Object-Oriented Technology lab. Pure Java SE makes inheritance, interfaces, state objects, concurrency, file handling and HTTP behavior visible to the examiner.

### Why not Spring Boot?

Spring Boot is excellent for production applications, but its dependency injection, controllers and persistence abstractions would hide some of the OOP behavior this project needs to demonstrate. It would also add setup and external dependencies.

## 8. Why use a built-in Java HTTP server?

The built-in server keeps the project portable. A teammate can compile and run it with a JDK without installing an application server.

### What is the trade-off?

It is intentionally smaller and less feature-rich than a production web framework. A production version could move the same domain services behind Spring Boot or another framework.

## 9. Why use flat files instead of MySQL?

The flat-file store satisfies the portability goal: clone the project, install a JDK, and run it. The records are human-readable and easy to inspect during a lab demonstration.

### Why not MySQL or PostgreSQL?

A database would require installation, credentials, schemas and migration setup for every teammate. That is appropriate for a larger deployment but unnecessary for this portable academic prototype.

### What would change in production?

The domain services and interfaces could remain, while the persistence implementation would be replaced by a transactional database repository.

## 10. Why store both in-memory objects and files?

The Java objects provide fast live behavior during a server session. The flat files preserve important records across restarts. This separates runtime behavior from persistence.

### Why not read the file for every operation?

Repeated disk reads would complicate transactions and make the application slower. The server loads records at startup and writes controlled updates.

## 11. Why use an audit WAL?

Rental actions affect more than one record. The append-only audit log records important transitions, settlement actions and failures so the team can explain what happened during a demo.

### Why not claim full distributed transactions?

The project does not use a distributed transaction coordinator. It uses synchronized operations, atomic temporary-file replacement and an audit log. The documentation must describe that honestly.

## 12. What is the escrow vault?

It is a simulated wallet and collateral ledger. It records the renter's balance, the amount held for a reservation, refunds and host settlement.

### Why call it simulated?

No real bank account is connected. Calling it a real escrow service would be inaccurate and legally unsafe.

## 13. Why use a sandbox payment adapter?

The adapter models order creation, amount calculation, HMAC verification and idempotent settlement without exposing real payment credentials.

### Why not connect Razorpay or another live gateway?

Live merchant credentials, legal approvals and network access are outside the lab scope. The interface is designed so a real provider can be added later.

## 14. Why use simulated lockers and sensors?

The 8×8 hub demonstrates compartment allocation, size rules, OTP pickup and sensor telemetry without requiring physical hardware.

### Why not build a real IoT device?

Hardware would change the project into an embedded-systems project and make it difficult for every teammate to reproduce. The `LockerDevice` abstraction keeps the software ready for a future hardware implementation.

## 15. Why use OTP lockout?

The OTP models a basic pickup security control. A three-strike lockout and expiry prevent unlimited guessing during a demonstration.

## 16. Why use PBKDF2 password hashing?

Passwords should never be stored as plain text. PBKDF2 with a random salt makes offline guessing more difficult and demonstrates a real security practice.

### Why not store passwords in the frontend?

The browser is not a trusted place for account verification. Authentication belongs to the backend.

## 17. Why have separate renter, host and admin roles?

The roles reflect different responsibilities:

- Renters reserve and collect equipment.
- Hosts list equipment and inspect returns.
- Administrators handle disputes, support and restricted operations.

Role checks prevent a renter from performing host or administrator actions.

## 18. Why include micro-storage and an 8×8 grid?

The grid gives the project a concrete spatial problem: assigning assets to bays with size restrictions. Row H demonstrates a rule for ground vehicles. It also provides a visible feature for the live demonstration.

## 19. Why separate frontend and backend?

The browser handles presentation and user interaction. Java handles validation, state changes, security and persistence. This separation prevents users from bypassing business rules by editing the page.

## 20. Why include prototype cloning and reflection?

They demonstrate additional OOP and Java capabilities required by the lab:

- Prototype cloning shows deep versus shallow copy behavior.
- Reflection shows runtime inspection of classes, fields and methods.

They are supporting academic demonstrations, not required steps for every rental.

## 21. Why automated tests?

Rental systems are vulnerable to invalid transitions and incorrect money movement. Automated tests prove that legal operations work and illegal operations are rejected consistently.

The current verified suite contains 163 tests across 17 suites.

## 22. Why not build every marketplace feature?

The project prioritizes the core rental lifecycle and OOP evaluation. Features such as live maps, chat, recommendation engines, production analytics and real payment processing are future scope because they would expand the project beyond the lab objective.

## 23. Why is the frontend simple compared with a commercial rental company?

The interface is designed to make the academic flow easy to demonstrate. Commercial products would need production hosting, image storage, messaging, logistics, customer support and much larger product data. RentSphere demonstrates the same basic customer journey with a manageable local prototype.

## 24. Why not claim that this is production-ready?

The project is a strong academic prototype. It still uses simulated hardware, sandbox payments, flat files and local deployment. Saying this clearly builds credibility rather than weakening the project.

## 25. One answer for difficult “why” questions

“We chose the simplest implementation that makes the OOP rule visible, reproducible for every teammate and testable during the lab. We kept production integrations behind interfaces so the academic prototype can later move to a database, real payment provider or physical locker without rewriting the core rental model.”

