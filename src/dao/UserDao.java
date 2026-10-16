package dao;

import model.User;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

/** All SQL for the users table. Every query uses PreparedStatement. */
public class UserDao {

    private static final String INSERT =
            "INSERT INTO users (username, password, roles, status) VALUES (?, ?, ?, ?)";
    private static final String FIND_BY_USERNAME =
            "SELECT id, username, password, roles, status FROM users WHERE username = ?";
    private static final String FIND_BY_STATUS =
            "SELECT id, username, password, roles, status FROM users WHERE status = ? ORDER BY id";
    private static final String UPDATE_STATUS =
            "UPDATE users SET status = ? WHERE id = ?";

    private static final SecureRandom RANDOM = new SecureRandom();

    /** Saves a new user. The plain password is hashed here; status defaults to PENDING. */
    public boolean register(String username, String plainPassword, String role) {
        return registerWithStatus(username, plainPassword, role, User.STATUS_PENDING);
    }

    /** Same as register, but with an explicit status (used to seed the first manager). */
    public boolean registerWithStatus(String username, String plainPassword, String role, String status) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(INSERT)) {
            ps.setString(1, username);
            ps.setString(2, hashPassword(plainPassword));
            ps.setString(3, role);
            ps.setString(4, status);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new IllegalStateException("Register failed: " + e.getMessage(), e);
        }
    }

    public Optional<User> findByUsername(String username) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(FIND_BY_USERNAME)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Lookup failed: " + e.getMessage(), e);
        }
    }

    public boolean usernameExists(String username) {
        return findByUsername(username).isPresent();
    }

    /** Returns the user only if the username exists and the password matches. */
    public Optional<User> authenticate(String username, String plainPassword) {
        return findByUsername(username)
                .filter(user -> verifyPassword(plainPassword, user.password()));
    }

    public List<User> findByStatus(String status) {
        List<User> users = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(FIND_BY_STATUS)) {
            ps.setString(1, status);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    users.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Query failed: " + e.getMessage(), e);
        }
        return users;
    }

    public boolean updateStatus(int userId, String newStatus) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(UPDATE_STATUS)) {
            ps.setString(1, newStatus);
            ps.setInt(2, userId);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new IllegalStateException("Update failed: " + e.getMessage(), e);
        }
    }

    private User mapRow(ResultSet rs) throws SQLException {
        return new User(
                rs.getInt("id"),
                rs.getString("username"),
                rs.getString("password"),
                rs.getString("roles"),
                rs.getString("status"));
    }

    // ---------- Password hashing (salted SHA-256, stored as salt:hash) ----------

    private String hashPassword(String plainPassword) {
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt) + ":" + digest(salt, plainPassword);
    }

    private boolean verifyPassword(String plainPassword, String stored) {
        if (stored == null || !stored.contains(":")) {
            return false;
        }
        String[] parts = stored.split(":", 2);
        byte[] salt = Base64.getDecoder().decode(parts[0]);
        return digest(salt, plainPassword).equals(parts[1]);
    }

    private String digest(byte[] salt, String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt);
            byte[] hashed = md.digest(password.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}