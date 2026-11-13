package dao;

import model.ActionType;
import model.ActivityLog;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** All activity-log (audit trail) database access. Every query is a parameterized PreparedStatement. */
public class ActivityLogDao {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final String SQL_INSERT = "INSERT INTO activity_logs (user_id, action_type, description, timestamp) VALUES (?, ?, ?, ?)";
    private static final String SQL_FIND_ALL = "SELECT id, user_id, action_type, description, timestamp FROM activity_logs ORDER BY timestamp DESC, id DESC";
    private static final String SQL_FIND_BY_USER = "SELECT id, user_id, action_type, description, timestamp FROM activity_logs WHERE user_id = ? ORDER BY timestamp DESC, id DESC";
    private static final String SQL_DELETE_ALL = "DELETE FROM activity_logs";

    /** Records one audit entry. */
    public void logAction(int userId, String actionType, String description) {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(SQL_INSERT)) {
            ps.setInt(1, userId);
            ps.setString(2, actionType);
            ps.setString(3, description);
            ps.setString(4, LocalDateTime.now().format(FMT));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Could not write activity log.", e);
        }
    }

    /** Every log entry, newest first. */
    public List<ActivityLog> getAllLogs() {
        return query(SQL_FIND_ALL);
    }

    /** Log entries of one user, newest first. */
    public List<ActivityLog> getLogsByUserId(int userId) {
        return query(SQL_FIND_BY_USER, userId);
    }

    /**
     * Deletes every log entry, then records the clearing itself so the action stays traceable.
     * @return how many entries were deleted
     */
    public int clearLogs(int adminId) {
        int deleted = 0;
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(SQL_DELETE_ALL)) {
            deleted = ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Could not clear activity logs.", e);
        }
        logAction(adminId, ActionType.LOGS_CLEARED, "Admin " + adminId + " cleared " + deleted + " log entries.");
        return deleted;
    }

    private List<ActivityLog> query(String sql, Object... params) {
        List<ActivityLog> list = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new ActivityLog(rs.getInt("id"), rs.getInt("user_id"),
                            rs.getString("action_type"), rs.getString("description"),
                            rs.getString("timestamp")));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load activity logs.", e);
        }
        return list;
    }
}