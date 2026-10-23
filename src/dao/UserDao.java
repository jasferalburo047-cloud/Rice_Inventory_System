package dao;

import model.Role;
import model.User;
import model.UserStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** All user-related database access. Every query is a parameterized PreparedStatement. */
public class UserDao {

    private static final String SQL_USERNAME_EXISTS = "SELECT 1 FROM users WHERE username = ?";
    private static final String SQL_REGISTER = "INSERT INTO users (username, password, full_name, role, status) VALUES (?, ?, ?, ?, 'PENDING')";
    private static final String SQL_LOGIN = "SELECT id, username, full_name, role, status FROM users WHERE username = ? AND password = ?";
    private static final String SQL_FIND_BY_ID = "SELECT id, username, full_name, role, status FROM users WHERE id = ?";
    private static final String SQL_GET_ALL = "SELECT id, username, full_name, role, status FROM users ORDER BY id";
    private static final String SQL_GET_PENDING = "SELECT id, username, full_name, role, status FROM users WHERE status = 'PENDING' ORDER BY id";
    private static final String SQL_APPROVE = "UPDATE users SET status = 'APPROVED' WHERE id = ?";
    private static final String SQL_UPDATE_STATUS = "UPDATE users SET status = ? WHERE id = ?";

    public boolean usernameExists(String username) {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(SQL_USERNAME_EXISTS)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not check username.", e);
        }
    }

    /** Registers a new account. Every new account starts as PENDING. */
    public void register(String username, int password, String fullName, Role role) {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(SQL_REGISTER)) {
            ps.setString(1, username);
            ps.setInt(2, password);
            ps.setString(3, fullName);
            ps.setString(4, role.name());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Could not register user.", e);
        }
    }

    /** Returns the matching user (any status) or null if the credentials are wrong. */
    public User login(String username, int password) {
        List<User> found = query(SQL_LOGIN, username, password);
        return found.isEmpty() ? null : found.get(0);
    }

    public User findById(int id) {
        List<User> found = query(SQL_FIND_BY_ID, id);
        return found.isEmpty() ? null : found.get(0);
    }

    /** All registered accounts, regardless of role or status. */
    public List<User> getAllUsers() {
        return query(SQL_GET_ALL);
    }

    /** All accounts (any role) that are still waiting for approval. */
    public List<User> getPendingUsers() {
        return query(SQL_GET_PENDING);
    }

    /** Changes a user's status from PENDING to APPROVED. */
    public boolean approveUser(int userId) {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(SQL_APPROVE)) {
            ps.setInt(1, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Could not approve user.", e);
        }
    }

    public boolean updateStatus(int userId, UserStatus status) {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(SQL_UPDATE_STATUS)) {
            ps.setString(1, status.name());
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Could not update user status.", e);
        }
    }

    private List<User> query(String sql, Object... params) {
        List<User> list = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load users.", e);
        }
        return list;
    }

    private User map(ResultSet rs) throws SQLException {
        return new User(
                rs.getInt("id"),
                rs.getString("username"),
                rs.getString("full_name"),
                Role.valueOf(rs.getString("role")),
                UserStatus.valueOf(rs.getString("status")));
    }
}