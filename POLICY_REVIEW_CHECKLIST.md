# RentSphere Legal & Regulatory Policy Review Checklist (India Jurisdiction)

**Document Version:** 1.0.0-LEGAL-PILOT  
**Jurisdiction:** Republic of India / State of Goa  
**Target Architecture:** P2P Asset Rental Marketplace & Smart Pickup Locker Engine  
**Review Status:** Pre-Launch Pilot Drafts — *Requires Formal Review by Qualified Indian Legal Counsel Prior to Open Commercial Launch*  

---

## Executive Legal Notice

> [!WARNING]
> **DISCLAIMER: NOT FORMAL LEGAL ADVICE**  
> The 20 policy documents included within RentSphere and managed by `PolicyService.java` have been drafted to adhere to current Indian digital commerce laws, including the **Information Technology Act, 2000**, the **Digital Personal Data Protection (DPDP) Act, 2023**, the **Consumer Protection (E-Commerce) Rules, 2020**, and the **Indian Contract Act, 1872**.  
> However, these policies are implemented for an academic demonstration and a controlled campus/community pilot. They **MUST** be formally reviewed, customized, and approved by a qualified Indian advocate prior to open public commercial launch or collecting real financial consideration from third parties.

---

## 1. Statutory Compliance Matrix (India)

| Indian Statute / Regulation | Applicability to RentSphere | Implemented Technical Safeguards | Status |
| :--- | :--- | :--- | :--- |
| **Information Technology Act, 2000 & IT (Intermediary Guidelines and Digital Media Ethics Code) Rules, 2021** | Intermediary safe harbor protection (Section 79); Grievance Officer appointment (Rule 3(2)); takedown notices. | `PolicyService.java` (Policy #18), `SupportService.java` grievance ticketing system with 24-hour acknowledgement and 15-day resolution SLA. | **DRAFTED & INTEGRATED** |
| **Digital Personal Data Protection (DPDP) Act, 2023** | Notice and consent for data processing (Section 6); user rights (access, correction, erasure); data minimization. | `PolicyService.java` (Policy #2 & #17), versioned consent audit log (`data/policy_acceptance.txt`), right to account deletion workflow in admin interface. | **DRAFTED & INTEGRATED** |
| **Consumer Protection Act, 2019 & Consumer Protection (E-Commerce) Rules, 2020** | Unfair trade practice prohibition; transparent pricing; mandatory refund policies; seller details disclosure. | Authoritative server-side pricing breakdown in ₹ INR, explicit distinction between rental fee and refundable deposit, transparent dispute arbitration. | **DRAFTED & INTEGRATED** |
| **Indian Contract Act, 1872 (Bailment Sections 148–171)** | Equipment rentals constitute statutory bailment. Bailor (host) and Bailee (renter) rights, standard of care (Section 151), return of bailed goods. | Standard Rental Agreement (Policy #3), GoF state transitions locking bailment duration, condition verification at pickup and return. | **DRAFTED & INTEGRATED** |
| **Motor Vehicles Act, 1988 & Rent a Cab Scheme, 1989** | Commercial leasing of vehicles, driving licenses, commercial permits, passenger insurance. | Vehicle category isolation in Row H ground bays; explicit disclaimer that pilot mobility assets are campus/private property demonstration vehicles. | **ACTION REQUIRED FOR PUBLIC LAUNCH** |
| **Arbitration and Conciliation Act, 1996** | Alternative dispute resolution for marketplace damages and deposit disputes. | Tiered dispute resolution: Step 1 (Platform Arbitration), Step 2 (Mediation in Panaji/Margao, Goa), Step 3 (Sole Arbitrator under 1996 Act). | **DRAFTED & INTEGRATED** |
| **Central Goods and Services Tax (CGST) Act, 2017 (TCS u/s 52)** | Tax Collected at Source (TCS) obligations for electronic commerce operators (ECO). | Financial ledger schema records tax components and host payouts in minor units (paise/rupees) ready for GSTIN reporting adapter. | **ADAPTER READY** |

---

## 2. Inventory of 20 Marketplace Policies & Implementation Status

| # | Policy Identifier | Document Title | System Integration | Legal Review Status |
| :-: | :--- | :--- | :--- | :--- |
| **1** | `terms-of-service` | Terms of Service (Platform Master Agreement) | Modal view, mandatory registration consent | Drafted for Pilot |
| **2** | `privacy-policy` | Privacy Policy (DPDP Act 2023 Compliant) | Cookie banner, registration checkbox, footer | Drafted for Pilot |
| **3** | `rental-agreement` | Standard Equipment Bailment & Rental Contract | Checkout modal consent gate, booking ledger | Drafted for Pilot |
| **4** | `cancellation-refund` | Cancellation, Rescheduling & Refund Policy | Displayed on asset detail & checkout modal | Drafted for Pilot |
| **5** | `late-returns` | Late Return, Overstay & Demurrage Policy | Smart hub return workflow, penalty calculation | Drafted for Pilot |
| **6** | `damage-wear-tear` | Damage, Loss & Fair Wear-and-Tear Policy | Quarantine inspection screen & dispute modal | Drafted for Pilot |
| **7** | `host-policy` | Host Terms, Earnings & Equipment Standards | Host dashboard & listing creator | Drafted for Pilot |
| **8** | `renter-policy` | Renter Code of Conduct & Fiduciary Obligations | Renter profile & booking summary | Drafted for Pilot |
| **9** | `deposit-escrow` | Security Deposit, Escrow & Collateral Policy | Payment gateway sandbox & wallet engine | Drafted for Pilot |
| **10** | `dispute-resolution` | Dispute Resolution, Arbitration & Claims Procedure | Renter dispute modal & admin arbitration hub | Drafted for Pilot |
| **11** | `prohibited-items` | Prohibited, Hazardous & Restricted Equipment | Listing validation gate & search filters | Drafted for Pilot |
| **12** | `intellectual-property`| Copyright, Trademark & IP Infringement Policy | Grievance ticket category & takedown process | Drafted for Pilot |
| **13** | `content-image-rights`| Listing Content, Photographs & Media Rights | Image upload validator & asset serializer | Drafted for Pilot |
| **14** | `community-guidelines`| Community Guidelines & Non-Discrimination Standard| Registration modal & public footer | Drafted for Pilot |
| **15** | `safety-equipment-usage`| Equipment Safety, Pre-Flight & Operational Guidance| Asset details spec sheet & pickup confirmation| Drafted for Pilot |
| **16** | `cookie-tracking` | Cookie, LocalStorage & Device Telemetry Policy | Cookie consent banner & footer legal link | Drafted for Pilot |
| **17** | `data-retention` | Data Retention, Archival & Account Erasure Policy | Admin user suspension/deletion workflow | Drafted for Pilot |
| **18** | `grievance-redressal`| Grievance Redressal Mechanism (Rule 3(2) IT Rules)| Dedicated Admin Grievance Hub & public footer| Drafted for Pilot |
| **19** | `liability-limitations`| Marketplace Disclaimer & Limitation of Liability | Checkout agreement & footer disclaimer | Drafted for Pilot |
| **20** | `accessibility-policy` | Digital Accessibility & Inclusive Access Statement | WCAG compliant UI & mobile responsive layout | Drafted for Pilot |

---

## 3. Policy Acceptance & Audit Logging Engine

RentSphere does not treat legal policies as static passive text. Acceptance is cryptographically and deterministically logged:

1. **Storage Mechanism:** `data/policy_acceptance.txt`
2. **Schema:** `consentId|userId|policyId|version|acceptedAtUtc|ipAddress`
3. **Trigger Points:**
   - User Registration: Terms of Service, Privacy Policy, Community Guidelines.
   - Equipment Reservation / Checkout: Standard Rental Bailment Agreement, Cancellation & Refund Policy, Deposit & Escrow Policy.
   - Listing Creation: Host Terms, Prohibited Items Policy.
   - Damage Dispute Filing: Damage & Fair Wear-and-Tear Policy, Dispute Resolution Policy.
4. **Consent Retrieval:** Verified via `GET /api/policies/acceptance?userId={id}`.

---

## 4. Statutory Grievance Redressal Office (Rule 3(2) IT Rules, 2021)

To fulfill the requirements of an electronic marketplace intermediary under Indian law, RentSphere implements a statutory grievance mechanism:

- **Designated Grievance Officer:** Dr. A. Almeida, Faculty In-Charge / Grievance Officer
- **Contact Email:** `grievance@rentsphere.in` / `support@rentsphere.in`
- **Physical Address:** Department of Computer Science & Technology, Goa University, Taleigao Plateau, Goa 403206, India.
- **SLA Requirements:**
  - Acknowledgement of receipt: Within **24 hours**.
  - Redressal of grievance: Within **15 days** from date of receipt.
- **Ticket Tracking:** Fully functional in `SupportService.java` and accessible in the **Admin & Grievance Hub** tab (`GET /api/admin/tickets`, `POST /api/support/ticket`).

---

## 5. Formal Legal Review Checklist for Retained Counsel

Before transitioning from pilot to commercial marketplace operation, legal counsel must review and confirm the following items:

- [ ] **Vehicle Rental Compliance:** Verify whether P2P vehicle listings (Row H ground bays) require a commercial "Rent a Cab" / "Rent a Motor Cycle" permit under Section 75 of the Motor Vehicles Act, 1988, or if private vehicle sharing requires specific municipal licensing in Goa.
- [ ] **Escrow & Nodal Account Regulations:** Confirm whether platform-held security deposits require a Reserve Bank of India (RBI) authorized Nodal Account / Escrow Account under the RBI Guidelines for Payment Aggregators and Payment Gateways (DPSS.CO.PD.No.1810/02.14.008/2019-20).
- [ ] **Goods and Services Tax (GST):** Confirm whether RentSphere qualifies as an Electronic Commerce Operator (ECO) liable to collect TCS @ 1% under Section 52 of the CGST Act, 2017.
- [ ] **Limitation of Liability Enforceability:** Review Section 19 clauses limiting intermediary liability under Section 73 of the Indian Contract Act for consequential damages arising from defective rented equipment.
- [ ] **Minor / Student Verification:** Ensure terms expressly require contractual capacity (18+ years of age under Section 11 of the Indian Contract Act) for campus student rentals.
- [ ] **Equipment Insurance:** Evaluate third-party commercial property and liability insurance options for high-value assets (drones, cinema cameras, power tools) exceeding deposit caps.

---

*Prepared by:* **RentSphere Full-Stack Engineering Team**  
*For Review By:* **Appointed Legal Counsel & Goa University Academic Review Committee**
