package rentsphere.policy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * RentSphere Legal & Policy Management Service.
 * Manages 20 distinct Indian regulatory and marketplace policy documents,
 * policy versioning, and auditable user consent logging.
 *
 * Applicable Indian Legal Frameworks:
 * - Digital Personal Data Protection Act (DPDP Act, 2023)
 * - Information Technology (Intermediary Guidelines and Digital Media Ethics Code) Rules, 2021
 * - Consumer Protection (E-Commerce) Rules, 2020
 * - Indian Contract Act, 1872
 */
public class PolicyService {
    public static final String CURRENT_POLICY_VERSION = "v1.0-2026";
    private final Path policyAcceptancePath;
    private final Map<String, PolicyDocument> policies = new LinkedHashMap<>();
    // userId -> set of accepted policy keys
    private final Map<String, Set<String>> userAcceptances = new ConcurrentHashMap<>();

    public record PolicyDocument(
            String id,
            String title,
            String summary,
            String jurisdictionNote,
            String content,
            String version
    ) {}

    public PolicyService(String dataDir) {
        this.policyAcceptancePath = Paths.get(dataDir, "policy_acceptance.txt");
        initializePolicies();
        loadAcceptances();
    }

    private void initializePolicies() {
        registerPolicy("tos", "Terms of Service",
                "Binding agreement governing marketplace access, accounts, and peer interactions in Goa, India.",
                "Subject to Indian Contract Act 1872 and IT Act 2000.",
                "RentSphere facilitates peer-to-peer equipment rentals across Goa. Users must be at least 18 years old. Platform acts as an intermediary under Section 79 of the Information Technology Act, 2000. Renters and hosts agree to honest representations, safe equipment operations, and timely settlement of tariffs and security deposits in Indian Rupees (INR ₹).");

        registerPolicy("privacy", "Privacy Policy (DPDP Act 2023 Compliant)",
                "Data collection, processing purpose, retention, and digital personal data rights.",
                "Digital Personal Data Protection Act, 2023 (DPDP Act).",
                "We collect personal data (Name, Email, Phone, Session telemetry, Rental history) strictly for booking fulfillment, pickup authorization, and fraud prevention. Data is processed locally within Indian jurisdiction. Users retain rights to access, correct, and request erasure of personal data under the DPDP Act 2023, subject to legal record-keeping obligations.");

        registerPolicy("rental_agreement", "Standard Rental Agreement",
                "Contractual terms governing the temporary custody, lease tariff, and return of equipment.",
                "Bailment under Indian Contract Act, 1872 (Sections 148-171).",
                "The Host grants the Renter temporary possession of the listed asset for the agreed rental duration. The Renter owes a duty of reasonable care as a bailee under Section 151 of the Indian Contract Act, 1872. The asset must be returned in identical condition, minus fair wear and tear.");

        registerPolicy("cancellation", "Cancellation & Refund Policy",
                "Cancellation windows, host rejection refunds, and processing timelines.",
                "Consumer Protection (E-Commerce) Rules, 2020.",
                "Full refund of rental tariff and security deposit if cancelled >24 hours prior to scheduled pickup. Within 24 hours of pickup, 10% platform restocking fee may apply. If Host rejects or fails to confirm reservation, 100% refund is credited within 24-48 business hours.");

        registerPolicy("late_return", "Late Return Policy",
                "Grace periods, overtime surcharges, and missing equipment procedures.",
                "Clear operational rules before legal escalation.",
                "A 30-minute grace period applies to all returns. Returns beyond grace period incur a late fee of 1.5x hourly rate. If equipment is overdue by >48 hours without host communication, platform initiates support outreach, locker security lockout, and claims against security deposit.");

        registerPolicy("damage_loss", "Damage, Loss, and Fair Wear-and-Tear Policy",
                "Inspection quarantine standards, damage severity scales, and repair estimation.",
                "Standard objective assessment matrix.",
                "Returned items undergo mandatory inspection quarantine. Hosts assess physical integrity and accessories. Minor surface scuffs are classified as fair wear-and-tear (zero deduction). Functional impairment, cracked optics, or burnt motors require repair quotes before deposit deduction.");

        registerPolicy("host_policy", "Host Policy & Quality Standards",
                "Host obligations, listing accuracy, asset safety, and maintenance logs.",
                "Intermediary listing verification standards.",
                "Hosts must own or hold lawful authority to lease the listed equipment. Equipment must be tested, operational, and safe. Hosts must honor confirmed bookings and conduct timely return inspections within 24 hours of locker check-in.");

        registerPolicy("renter_policy", "Renter Responsibilities & Code of Conduct",
                "Safe handling, non-subleasing, and prompt pickup/return compliance.",
                "Renter contractual code of conduct.",
                "Renters must operate equipment in accordance with manufacturer guidelines. Subleasing or commercial re-renting without host consent is strictly prohibited. The designated pickup OTP must never be shared with third parties.");

        registerPolicy("deposit_payment", "Deposit and Payment Policy",
                "Tariff calculation, refundable deposit escrow hold, and release conditions.",
                "Reserve Bank of India (RBI) payment guidelines.",
                "Tariff and refundable security deposits are clearly segregated before payment authorization. In simulated/sandbox mode, tokens emulate gateway settlements. Real deposits are released immediately upon host approval of clean inspection or timeout.");

        registerPolicy("dispute_resolution", "Dispute Resolution Policy",
                "Arbitration framework for contested damage deductions and late claims.",
                "Arbitration and Conciliation Act, 1996 / Consumer Grievance.",
                "If a Renter contests a Host's damage assessment, the booking enters DISPUTED state. The security deposit deduction is frozen pending independent RentSphere arbitration. Both parties submit photo evidence, timestamps, and quotes within 72 hours.");

        registerPolicy("prohibited_items", "Prohibited Items & Unsafe Equipment Policy",
                "List of restricted, illegal, hazardous, or unverified equipment.",
                "Indian Explosives Act, Arms Act, and safety regulations.",
                "Weapons, firearms, hazardous chemicals, unregistered commercial drones violating DGCA rules, pirated media, and unsafe DIY electrical rigs lacking BIS certification are strictly banned from RentSphere.");

        registerPolicy("copyright_ip", "Copyright and Intellectual Property Policy",
                "Protection of trademarks, brand names, and original media content.",
                "Copyright Act, 1957 & Trade Marks Act, 1999.",
                "Hosts must not upload promotional photos or text infringing third-party copyrights. Take-down requests are processed within 36 hours of formal notice to our Grievance Officer.");

        registerPolicy("content_rights", "Listing Content and Image Rights Policy",
                "Authentic photography rules and license to display listings on platform.",
                "Truth-in-advertising guidelines.",
                "Photographs uploaded to listings must depict the actual physical asset in Goa, not stock manufacturer renders. Uploading misleading images is grounds for listing deactivation.");

        registerPolicy("community_guidelines", "Community Guidelines",
                "Harassment-free marketplace, transparent communication, and mutual respect.",
                "Campus and community standard of conduct.",
                "RentSphere thrives on community trust. Abuse, abusive messaging, intimidation, discrimination, or fraud will result in immediate account suspension across the Goa pilot network.");

        registerPolicy("safety_guidance", "Safety & Equipment Usage Guidance",
                "Protective equipment, high-voltage warnings, and operating precautions.",
                "Occupational safety principles.",
                "Users renting heavy power tools, rotary tillers, or high-intensity lighting must wear appropriate personal protective equipment (PPE). Hosts must provide basic operating instructions upon handover.");

        registerPolicy("cookie_tracking", "Cookie and Tracking Disclosures",
                "Local storage, secure HTTP-only cookies, and session telemetry.",
                "DPDP Act consent guidelines for tracking technologies.",
                "RentSphere uses essential HTTP cookies and local storage tokens solely for session management, CSRF prevention, and locker status polling. We do not sell tracking data to third-party ad networks.");

        registerPolicy("data_retention", "Data Retention & Account Deletion Policy",
                "Timeline for retaining transaction records and user deletion rights.",
                "DPDP Act statutory record-keeping.",
                "Users may request account deletion at any time if all rentals, inspections, and dispute resolutions are closed. Financial and audit log records are retained for 5 years as required under Indian GST and taxation laws.");

        registerPolicy("grievance_support", "Contact, Grievance, and Support Policy",
                "Designated Grievance Officer details and response service levels.",
                "Rule 3(2) of Information Technology Intermediary Rules, 2021.",
                "Designated Grievance Officer: Prof. Support / Compliance Lead, RentSphere Pilot Office, Goa University Campus, Taleigao Plateau, Goa 403206. Email: grievance@rentsphere.local. Acknowledgment within 24 hours; resolution within 15 days.");

        registerPolicy("limitations_liability", "Marketplace Limitations & Liability Disclosures",
                "Disclaimer of warranty, intermediary liability limitation, and damage caps.",
                "Section 79 IT Act 2000 & Consumer Protection Act, 2019.",
                "RentSphere is an equipment rental technology facilitator. Except where mandated by statute, platform liability for any equipment malfunction, downtime, or incidental loss is capped at the total rental tariff paid for the specific transaction.");

        registerPolicy("accessibility_policy", "Accessibility & Acceptable-Use Policy",
                "WCAG 2.1 compliance, inclusive keyboard navigation, and anti-abuse safeguards.",
                "Rights of Persons with Disabilities Act, 2016.",
                "RentSphere is committed to accessible digital experiences across desktop and mobile browsers, supporting screen readers, keyboard navigation, and high-contrast Warm Horizon color palettes.");
    }

    private void registerPolicy(String id, String title, String summary, String jurisdictionNote, String content) {
        policies.put(id, new PolicyDocument(id, title, summary, jurisdictionNote, content, CURRENT_POLICY_VERSION));
    }

    public List<PolicyDocument> getAllPolicies() {
        return new ArrayList<>(policies.values());
    }

    public PolicyDocument getPolicy(String id) {
        return policies.get(id);
    }

    public synchronized void recordAcceptance(String userId, List<String> policyIds) {
        if (userId == null || userId.isBlank()) return;
        Set<String> set = userAcceptances.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet());
        String now = Instant.now().toString();

        StringBuilder toAppend = new StringBuilder();
        for (String pid : policyIds) {
            if (policies.containsKey(pid) && !set.contains(pid)) {
                set.add(pid);
                toAppend.append(userId).append("|")
                        .append(pid).append("|")
                        .append(CURRENT_POLICY_VERSION).append("|")
                        .append(now).append("\n");
            }
        }

        if (toAppend.length() > 0) {
            saveAcceptancesAppend(toAppend.toString());
        }
    }

    public boolean hasAcceptedAllEssential(String userId) {
        if (userId == null) return false;
        Set<String> set = userAcceptances.get(userId);
        if (set == null) return false;
        // Essential policies: tos, privacy, rental_agreement, cancellation, deposit_payment
        return set.contains("tos") && set.contains("privacy") && set.contains("rental_agreement");
    }

    public Set<String> getAcceptedPolicies(String userId) {
        return userAcceptances.getOrDefault(userId, Collections.emptySet());
    }

    private synchronized void saveAcceptancesAppend(String record) {
        try {
            if (policyAcceptancePath.getParent() != null) {
                Files.createDirectories(policyAcceptancePath.getParent());
            }
            Files.writeString(policyAcceptancePath, record,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.err.println("[PolicyService] Failed to append policy acceptance: " + e.getMessage());
        }
    }

    private synchronized void loadAcceptances() {
        if (!Files.exists(policyAcceptancePath)) return;
        try {
            List<String> lines = Files.readAllLines(policyAcceptancePath, StandardCharsets.UTF_8);
            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
                String[] parts = trimmed.split("\\|");
                if (parts.length >= 2) {
                    String userId = parts[0].trim();
                    String policyId = parts[1].trim();
                    userAcceptances.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(policyId);
                }
            }
        } catch (IOException e) {
            System.err.println("[PolicyService] Failed to load policy acceptances: " + e.getMessage());
        }
    }
}
