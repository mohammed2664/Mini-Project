package rentsphere.support;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Customer Support and Grievance Redressal Service.
 * Fulfills statutory Indian E-Commerce Grievance Redressal requirements.
 */
public class SupportService {

    public static class SupportTicket {
        public String ticketId;
        public String userId;
        public String category; // "BOOKING", "PAYMENT", "HARDWARE_LOCKER", "DAMAGE_DISPUTE", "ACCOUNT"
        public String subject;
        public String description;
        public String status;   // "OPEN", "IN_PROGRESS", "RESOLVED"
        public String resolution;
        public long createdAt;
        public long resolvedAt;

        public SupportTicket(String ticketId, String userId, String category, String subject, String description) {
            this.ticketId = ticketId;
            this.userId = userId;
            this.category = category;
            this.subject = subject;
            this.description = description;
            this.status = "OPEN";
            this.resolution = "";
            this.createdAt = System.currentTimeMillis();
        }

        public String serializeToPipe() {
            return String.format("%s|%s|%s|%s|%s|%s|%s|%d|%d",
                    ticketId, userId, category, subject.replace("|", "/"), description.replace("|", "/"),
                    status, resolution.replace("|", "/"), createdAt, resolvedAt);
        }
    }

    private final Map<String, SupportTicket> tickets = new ConcurrentHashMap<>();
    private final Path ticketsFile;
    private final Object fileLock = new Object();

    public SupportService(String baseDataDir) {
        Path dataDir = Paths.get(baseDataDir != null ? baseDataDir : "data");
        this.ticketsFile = dataDir.resolve("support_tickets.txt");
        loadTickets();
    }

    public synchronized SupportTicket createTicket(String userId, String category, String subject, String description) {
        String ticketId = "TCK-" + (100 + tickets.size() + 1);
        SupportTicket ticket = new SupportTicket(ticketId, userId, category, subject, description);
        tickets.put(ticketId, ticket);
        saveTickets();
        return ticket;
    }

    public synchronized boolean resolveTicket(String ticketId, String adminId, String resolutionNotes) {
        SupportTicket t = tickets.get(ticketId);
        if (t == null) return false;
        t.status = "RESOLVED";
        t.resolution = "Resolved by " + adminId + ": " + resolutionNotes;
        t.resolvedAt = System.currentTimeMillis();
        saveTickets();
        return true;
    }

    public SupportTicket getTicket(String ticketId) {
        return tickets.get(ticketId);
    }

    public List<SupportTicket> getAllTickets() {
        return new ArrayList<>(tickets.values());
    }

    public List<SupportTicket> getUserTickets(String userId) {
        List<SupportTicket> list = new ArrayList<>();
        for (SupportTicket t : tickets.values()) {
            if (t.userId.equals(userId)) {
                list.add(t);
            }
        }
        return list;
    }

    private void loadTickets() {
        synchronized (fileLock) {
            if (!Files.exists(ticketsFile)) return;
            try (BufferedReader reader = Files.newBufferedReader(ticketsFile, StandardCharsets.UTF_8)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) continue;

                    String[] p = line.split("\\|", -1);
                    if (p.length < 9) continue;

                    SupportTicket t = new SupportTicket(p[0], p[1], p[2], p[3], p[4]);
                    t.status = p[5];
                    t.resolution = p[6];
                    t.createdAt = parseLong(p[7], System.currentTimeMillis());
                    t.resolvedAt = parseLong(p[8], 0L);
                    tickets.put(t.ticketId, t);
                }
            } catch (IOException e) {
                System.err.println("[SUPPORT] Error loading support_tickets.txt: " + e.getMessage());
            }
        }
    }

    private void saveTickets() {
        synchronized (fileLock) {
            Path temp = ticketsFile.resolveSibling(ticketsFile.getFileName() + ".tmp");
            try {
                try (BufferedWriter writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                    writer.write("# RentSphere Support Tickets & Grievance Redressal Ledger");
                    writer.newLine();
                    for (SupportTicket t : tickets.values()) {
                        writer.write(t.serializeToPipe());
                        writer.newLine();
                    }
                }
                Files.move(temp, ticketsFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException e) {
                try {
                    Files.move(temp, ticketsFile, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException ex) {
                    System.err.println("[SUPPORT] Error saving support_tickets.txt: " + ex.getMessage());
                }
            }
        }
    }

    private long parseLong(String s, long d) {
        try { return Long.parseLong(s.trim()); } catch (Exception e) { return d; }
    }
}
