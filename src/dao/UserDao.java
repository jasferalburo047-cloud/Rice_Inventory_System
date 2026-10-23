package dao;

import model.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDao {

    private static final String SELECT = "SELECT id, username, full_name, role, status FROM users";

    public boolean usernameExists(String username) {
        String sql = "SELECT 1 FROM users WHERE username = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
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
        String sql = "INSERT INTO users (username, password, full_name, role, status) "
                + "VALUES (?, ?, ?, ?, 'PENDING')";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setInt(2, password);
            ps.setString(3, fullName);
            ps.setString(4, role.name());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Could not register user.", e);
        }
    }

    /** Returns the matching user (any status) or null if credentials are wrong. */
    public User login(String username, int password) {
        String sql = SELECT + " WHERE username = ? AND password = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setInt(2, password);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not log in.", e);
        }
    }

    public User findById(int id) {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(SELECT + " WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not find user.", e);
        }
    }

    public List<User> findAll() {
        List<User> list = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(SELECT + " ORDER BY id")) {
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            throw new DataAccessException("Could not load users.", e);
        }
        return list;
    }

    public boolean updateStatus(int id, UserStatus status) {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("UPDATE users SET status = ? WHERE id = ?")) {
            ps.setString(1, status.name());
            ps.setInt(2, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Could not update user status.", e);
        }
    }

    private User map(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String username = rs.getString("username");
        String fullName = rs.getString("full_name");
        Role role = Role.valueOf(rs.getString("role"));
        UserStatus status = UserStatus.valueOf(rs.getString("status"));
        switch (role) {
            case MANAGER: return new Manager(id, username, fullName, status);
            case STAFF:   return new Staff(id, username, fullName, status);
            default:      return new Cashier(id, username, fullName, status);
        }
    }
}