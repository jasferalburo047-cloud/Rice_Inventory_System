package dao;

import model.ActionType;
import model.Role;
import model.User;
import model.UserStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * All user-related database access. Every query is a parameterized PreparedStatement.
 * Account changes are written to the audit trail (activity_logs) automatically.
 */
public class UserDao {

    private static final String SQL_USERNAME_EXISTS = "SELECT 1 FROM users WHERE username = ?";
    private static final String SQL_REGISTER = "INSERT INTO users (username, password, full_name, role, status) VALUES (?, ?, ?, ?, 'PENDING')";
    private static final String SQL_LOGIN = "SELECT id, username, full_name, role, status FROM users WHERE username = ? AND password = ?";
    private static final String SQL_FIND_BY_ID = "SELECT id, username, full_name, role, status FROM users WHERE id = ?";
    private static final String SQL_GET_ALL = "SELECT id, username, full_name, role, status FROM users ORDER BY id";
    private static final String SQL_GET_PENDING = "SELECT id, username, full_name, role, status FROM users WHERE status = 'PENDING' ORDER BY id";
    private static final String SQL_APPROVE = "UPDATE users SET status = 'APPROVED' WHERE id = ?";
    private static final String SQL_UPDATE_STATUS = "UPDATE users SET status = ? WHERE id = ?";
    private static final String SQL_UPDATE_ROLE_STATUS = "UPDATE users SET role = ?, status = ? WHERE id = ?";

    private final ActivityLogDao activityLogDao;

    public UserDao(ActivityLogDao activityLogDao) {
        this.activityLogDao = activityLogDao;
    }

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

    /**
     * Registers a new account. Every new account starts as PENDING.
     * @return the id of the new account
     */
    public int register(String username, int password, String fullName, Role role) {
        int newId = -1;
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(SQL_REGISTER, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, username);
            ps.setInt(2, password);
            ps.setString(3, fullName);
            ps.setString(4, role.name());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                newId = keys.next() ? keys.getInt(1) : -1;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not register user.", e);
        }
        if (newId > 0) {
            activityLogDao.logAction(newId, ActionType.USER_REGISTERED,
                    "User " + username + " registered as " + role.name() + " (status PENDING).");
        }
        return newId;
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

    /** Changes a user's status from PENDING to APPROVED and logs it (USER_APPROVED). */
    public boolean approveUser(int adminId, int userId) {
        User target = findById(userId);
        boolean updated = false;
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(SQL_APPROVE)) {
            ps.setInt(1, userId);
            updated = ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Could not approve user.", e);
        }
        if (updated) {
            activityLogDao.logAction(adminId, ActionType.USER_APPROVED,
                    "Admin " + adminId + " approved User " + userId + nameOf(target) + ".");
        }
        return updated;
    }

    /** Changes only the status of a user and logs it (USER_UPDATED). */
    public boolean updateStatus(int adminId, int userId, UserStatus status) {
        User target = findById(userId);
        boolean updated = false;
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(SQL_UPDATE_STATUS)) {
            ps.setString(1, status.name());
            ps.setInt(2, userId);
            updated = ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Could not update user status.", e);
        }
        if (updated) {
            activityLogDao.logAction(adminId, ActionType.USER_UPDATED,
                    "Admin " + adminId + " set status of User " + userId + nameOf(target) + " to " + status.name() + ".");
        }
        return updated;
    }

    /**
     * Updates a user's role and status in one parameterized statement and logs it (USER_UPDATED).
     * Both values are converted to upper case before they are sent to the database.
     */
    public boolean updateUserRoleAndStatus(int adminId, int userId, String newRole, String newStatus) {
        String role = newRole.trim().toUpperCase();
        String status = newStatus.trim().toUpperCase();
        User target = findById(userId);
        boolean updated = false;
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(SQL_UPDATE_ROLE_STATUS)) {
            ps.setString(1, role);
            ps.setString(2, status);
            ps.setInt(3, userId);
            updated = ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Could not update user role and status.", e);
        }
        if (updated) {
            activityLogDao.logAction(adminId, ActionType.USER_UPDATED,
                    "Admin " + adminId + " changed role of User " + userId + nameOf(target)
                            + " to " + role + ", status to " + status + ".");
        }
        return updated;
    }

    private String nameOf(User u) {
        return u == null ? "" : " (" + u.username() + ")";
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