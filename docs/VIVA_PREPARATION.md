# RentSphere Viva Preparation

This is a speaking guide, not a script to memorise word for word. Explain the idea first, then mention the class or test that supports it.

## 30-second introduction

> RentSphere is a Java-based peer-to-peer equipment rental system. A renter can browse equipment, reserve it, collect it from a simulated smart locker, return it for inspection, and receive the correct escrow settlement. Hosts manage equipment and inspections, while administrators handle users, disputes, support, and diagnostics. The project demonstrates object-oriented design through inheritance, polymorphism, the State Pattern, generic repositories, cloning, reflection, persistence, and concurrency control.

## Main application flow

1. The browser loads the HTML, CSS, and JavaScript frontend.
2. The frontend calls the Java HTTP API.
3. The server authenticates the user and checks the user role.
4. The reservation pipeline checks identity, asset state, escrow balance, and locker placement.
5. The server locks the tariff and collateral in the software escrow ledger.
6. The asset moves through reservation, active rental, return quarantine, and inspection.
7. A clean inspection refunds the deposit; a damage decision distributes the money according to the dispute result.
8. Important changes are saved to flat files and recorded in the audit WAL.

## OOP questions

### Where is inheritance used?

`RentableAsset` is part of a hierarchy. More specific classes such as camera, audio, power-tool, and mobility assets inherit common rental information and add category-specific behaviour.

### Where is polymorphism used?

The server can work with a `RentableAsset` reference while the actual subclass calculates its own tariff or collateral rules. The same operation therefore behaves differently for different asset types.

### Where is encapsulation used?

Domain classes keep their data and operations together. State changes and escrow changes happen through controlled methods instead of allowing the frontend to edit balances directly.

### Where is abstraction used?

Abstract domain classes and interfaces describe common behaviour without forcing every asset or state to implement it in the same way.

### Why use the State Pattern?

An asset behaves differently when it is available, reserved, rented, quarantined, or disputed. Separate state classes keep those rules clear and prevent invalid actions such as reserving an item that is still under inspection.

### Where is the Prototype Pattern used?

`PrototypeCloner` compares shallow and deep copies of an asset. Deep cloning keeps mutable service logs separate, which is useful for safe what-if simulations.

### Why use generics?

`GenericRepository<T>` provides reusable storage and filtering for domain entities while preserving compile-time type safety.

### Why use reflection?

The reflection inspector demonstrates runtime examination of an object's class, fields, methods, and inheritance hierarchy. It is an OOT demonstration rather than a core security feature.

## Security and accounting questions

### Can the frontend change a user's role?

No. The frontend can display a demo persona, but the server checks the authenticated session and role before allowing protected operations.

### How is duplicate settlement prevented?

The server checks the authoritative asset, dispute, or ticket state inside a synchronized critical section. One request performs the settlement; a competing request sees the updated state and receives a conflict response.

### What is the WAL?

The write-ahead log is an append-only audit record of important operations such as reservations and settlements. It helps show what happened and confirms that rejected duplicate requests did not create extra financial entries.

### Is the escrow real money?

No. It is an internal software ledger for demonstrating accounting logic. The project does not connect to a bank or hold real customer funds.

## Honest limitations

- The locker hardware and sensors are simulated in Java.
- Payments use a sandbox adapter and test signatures.
- Flat files are used instead of a production database.
- The application is intended for local academic demonstration, not public deployment.
- Legal and grievance features are educational prototypes, not legal certification.

## Useful test evidence

- Fresh Java compilation: passed.
- JavaScript syntax check: passed.
- Automated verification: **163/163 tests passed across 17 suites**.
- Concurrent dispute, inspection, slashing, and support tests: one success and one conflict response.
- Responsive checks: 1440px, 1280px, 768px, 375px, and 320px.
- Accessibility checks: labelled dialogs, focus return, Escape handling, live status region, visible focus styling, and reduced-motion support.

## If you do not know an answer

Use a truthful bridge instead of guessing:

> I did not make that part production-ready because this is an academic prototype. In the current implementation, the equivalent behaviour is demonstrated through ___.

or:

> I would need to check that implementation detail, but the design decision was ___ and the reason was ___.

## Final reminder

The project is not being judged as a commercial rental company. It is being judged as a Java OOT project. Explain the design decisions, show the flow, and be honest about what is simulated.
