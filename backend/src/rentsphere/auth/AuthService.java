package rentsphere.auth;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Authentication and Session Management Service.
 * Implements:
 *  - PBKDF2-HMAC-SHA256 password hashing with 32-byte salt
 *  - Secure 256-bit session token generation
 *  - Role-Based Access Control (RBAC): VISITOR, RENTER, HOST, ADMIN
 *  - Deterministic flat-file persistence in data/users_auth.txt
 *  - Atomic file writes to prevent credential file corruption
 */
public class AuthService {

    private final Map<String, UserAccount> usersById = new ConcurrentHashMap<>();
    private final Map<String, UserAccount> usersByEmail = new ConcurrentHashMap<>();
    private final Map<String, UserSession> activeSessions = new ConcurrentHashMap<>();
    private final Path usersFile;
    private final Object fileLock = new Object();
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final long SESSION_DURATION_MS = 24 * 60 * 60 * 1000L; // 24 Hours

    public AuthService(String baseDataDir) {
        Path dataDir = Paths.get(baseDataDir != null ? baseDataDir : "data");
        this.usersFile = dataDir.resolve("users_auth.txt");
        loadUsers();
        if (usersById.isEmpty()) {
            seedDefaultAccounts();
        }
    }

    // ========================================================================
    // REGISTRATION & AUTHENTICATION
    // ========================================================================

    public synchronized UserAccount registerUser(String email, String rawPassword, String fullName, 
                                                 String role, String phone) {
        if (email == null || email.isBlank() || !email.contains("@")) {
            throw new IllegalArgumentException("Invalid email address format.");
        }
        if (rawPassword == null || rawPassword.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters long.");
        }
        if (fullName == null || fullName.isBlank()) {
            throw new IllegalArgumentException("Full name is required.");
        }

        String normalizedEmail = email.trim().toLowerCase();
        if (usersByEmail.containsKey(normalizedEmail)) {
            throw new IllegalStateException("An account with this email already exists: " + email);
        }

        String roleClean = (role != null && role.equalsIgnoreCase("HOST")) ? "HOST" : "RENTER";
        String userId = "USR-" + (100 + usersById.size() + 1);

        String salt = PasswordHasher.generateSalt();
        String hash = PasswordHasher.hashPassword(rawPassword, salt);

        UserAccount user = new UserAccount(userId, normalizedEmail, fullName.trim(), hash, salt, 
                                           roleClean, phone != null ? phone.trim() : "", true, "ACTIVE", System.currentTimeMillis());

        usersById.put(user.getUserId(), user);
        usersByEmail.put(user.getEmail(), user);
        saveUsers();
        return user;
    }

    public synchronized UserSession login(String emailOrUserId, String rawPassword) {
        if (emailOrUserId == null || rawPassword == null) {
            return null;
        }

        String query = emailOrUserId.trim().toLowerCase();
        UserAccount user = usersByEmail.get(query);
        if (user == null) {
            user = usersById.get(emailOrUserId.trim());
        }

        if (user == null) {
            return null;
        }

        if ("SUSPENDED".equalsIgnoreCase(user.getAccountStatus())) {
            throw new IllegalStateException("Account is suspended. Please contact Goa University Hub Administration.");
        }

        if (!user.checkPassword(rawPassword)) {
            return null;
        }

        user.setLastLoginAt(System.currentTimeMillis());
        saveUsers();

        // Generate 32-byte secure session token
        byte[] tokenBytes = new byte[32];
        RANDOM.nextBytes(tokenBytes);
        String token = HexFormat.of().formatHex(tokenBytes);

        UserSession session = new UserSession(token, user.getUserId(), user.getRole(), SESSION_DURATION_MS);
        activeSessions.put(token, session);
        return session;
    }

    public void logout(String token) {
        if (token != null) {
            activeSessions.remove(token);
        }
    }

    public UserSession validateSession(String token) {
        if (token == null || token.isBlank()) return null;
        UserSession session = activeSessions.get(token);
        if (session == null) return null;
        if (session.isExpired()) {
            activeSessions.remove(token);
            return null;
        }
        session.touch();
        return session;
    }

    public UserAccount getUserById(String userId) {
        return usersById.get(userId);
    }

    public UserAccount getUserByEmail(String email) {
        if (email == null) return null;
        return usersByEmail.get(email.trim().toLowerCase());
    }

    public List<UserAccount> getAllUsers() {
        return new ArrayList<>(usersById.values());
    }

    public synchronized boolean updateKycStatus(String userId, boolean verified) {
        UserAccount user = usersById.get(userId);
        if (user != null) {
            user.setKycVerified(verified);
            saveUsers();
            return true;
        }
        return false;
    }

    public synchronized boolean setAccountStatus(String userId, String status) {
        UserAccount user = usersById.get(userId);
        if (user != null) {
            user.setAccountStatus(status);
            if ("SUSPENDED".equalsIgnoreCase(status)) {
                // Invalidate all active sessions for suspended user
                activeSessions.entrySet().removeIf(entry -> entry.getValue().getUserId().equals(userId));
            }
            saveUsers();
            return true;
        }
        return false;
    }

    // ========================================================================
    // FLAT-FILE PERSISTENCE (ATOMIC SAFE WRITING)
    // ========================================================================

    public void loadUsers() {
        synchronized (fileLock) {
            if (!Files.exists(usersFile)) return;

            try (BufferedReader reader = Files.newBufferedReader(usersFile, StandardCharsets.UTF_8)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) continue;

                    String[] parts = line.split("\\|", -1);
                    if (parts.length < 10) continue;

                    String userId = parts[0];
                    String email = parts[1];
                    String fullName = parts[2];
                    String passwordHash = parts[3];
                    String salt = parts[4];
                    String role = parts[5];
                    String phone = parts[6];
                    boolean kyc = Boolean.parseBoolean(parts[7]);
                    String status = parts[8];
                    long createdAt = parseLong(parts[9], System.currentTimeMillis());

                    UserAccount user = new UserAccount(userId, email, fullName, passwordHash, salt, 
                                                       role, phone, kyc, status, createdAt);
                    usersById.put(user.getUserId(), user);
                    usersByEmail.put(user.getEmail(), user);
                }
            } catch (IOException e) {
                System.err.println("[AUTH] Error loading users_auth.txt: " + e.getMessage());
            }
        }
    }

    public void saveUsers() {
        synchronized (fileLock) {
            Path tempFile = usersFile.resolveSibling(usersFile.getFileName() + ".tmp");
            try {
                try (BufferedWriter writer = Files.newBufferedWriter(tempFile, StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                    writer.write("# RentSphere User Accounts Ledger (PBKDF2 Hashed Credentials)");
                    writer.newLine();
                    for (UserAccount user : usersById.values()) {
                        writer.write(user.serializeToPipe());
                        writer.newLine();
                    }
                }
                // Atomic replace to guarantee zero corrupted file state on sudden crash
                Files.move(tempFile, usersFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException e) {
                // Fallback to non-atomic replace if filesystem does not support ATOMIC_MOVE
                try {
                    Files.move(tempFile, usersFile, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException ex) {
                    System.err.println("[AUTH] Critical error saving users_auth.txt: " + ex.getMessage());
                }
            }
        }
    }

    private void seedDefaultAccounts() {
        System.out.println("[AUTH] Pre-seeding baseline accounts with PBKDF2 password hashes (Default password: 'RentSphere@2026')...");
        String defaultPw = "RentSphere@2026";

        seedAccount("renter_deepak", "deepak@rentsphere.in", "Deepak Dev", defaultPw, "RENTER", "+91 98221 10001", true);
        seedAccount("host_faaris", "faaris@rentsphere.in", "Mohammed Faaris Khan", defaultPw, "HOST", "+91 98221 10002", true);
        seedAccount("host_ketan", "ketan@rentsphere.in", "Ketan Gurudas Naik", defaultPw, "HOST", "+91 98221 10003", true);
        seedAccount("renter_nikhil", "nikhil@rentsphere.in", "Nikhil Bhaiju", defaultPw, "RENTER", "+91 98221 10004", true);
        seedAccount("renter_priya", "priya@rentsphere.in", "Priya Sharma", defaultPw, "RENTER", "+91 98221 10005", true);
        seedAccount("inspector_almeida", "admin@rentsphere.in", "Prof. Almeida (Goa Univ Hub Admin)", defaultPw, "ADMIN", "+91 98221 10000", true);

        saveUsers();
    }

    private void seedAccount(String userId, String email, String fullName, String rawPassword, 
                             String role, String phone, boolean kyc) {
        String salt = PasswordHasher.generateSalt();
        String hash = PasswordHasher.hashPassword(rawPassword, salt);
        UserAccount user = new UserAccount(userId, email, fullName, hash, salt, role, phone, kyc, "ACTIVE", System.currentTimeMillis());
        usersById.put(user.getUserId(), user);
        usersByEmail.put(user.getEmail(), user);
    }

    private long parseLong(String str, long defaultVal) {
        try {
            return Long.parseLong(str.trim());
        } catch (Exception e) {
            return defaultVal;
        }
    }
}
