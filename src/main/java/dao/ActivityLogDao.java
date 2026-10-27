package dao;

import model.ActivityLog;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ActivityLogDao {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private static final String SQL_INSERT = "INSERT INTO activity_logs (user_id, action, log_date) VALUES (?, ?, ?)";
    private static final String SQL_FIND_ALL = "SELECT l.id, l.user_id, u.username, l.action, l.log_date FROM activity_logs l JOIN users u ON u.id = l.user_id ORDER BY l.id DESC";

    public void log(int userId, String action) {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(SQL_INSERT)) {
            ps.setInt(1, userId);
            ps.setString(2, action);
            ps.setString(3, LocalDateTime.now().format(FMT));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Could not write activity log.", e);
        }
    }

    public List<ActivityLog> findAll() {
        List<ActivityLog> list = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(SQL_FIND_ALL);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new ActivityLog(rs.getInt("id"), rs.getInt("user_id"),
                        rs.getString("username"), rs.getString("action"), rs.getString("log_date")));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load activity logs.", e);
        }
        return list;
    }
}