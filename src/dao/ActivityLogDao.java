package dao;

import model.ActivityLog;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ActivityLogDao {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public void log(int userId, String action) {
        String sql = "INSERT INTO activity_logs (user_id, action, log_date) VALUES (?, ?, ?)";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, action);
            ps.setString(3, LocalDateTime.now().format(FMT));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Could not write activity log.", e);
        }
    }

    public List<ActivityLog> findAll() {
        String sql = "SELECT l.id, l.user_id, u.username, l.action, l.log_date "
                + "FROM activity_logs l JOIN users u ON u.id = l.user_id ORDER BY l.id DESC";
        List<ActivityLog> list = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
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