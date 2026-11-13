package dao;

import model.ActionType;
import model.Supplier;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class SupplierDao {

    private static final String SQL_INSERT = "INSERT INTO suppliers (name, contact_number, address) VALUES (?, ?, ?)";
    private static final String SQL_FIND_BY_ID = "SELECT id, name, contact_number, address FROM suppliers WHERE id = ?";
    private static final String SQL_FIND_ALL = "SELECT id, name, contact_number, address FROM suppliers ORDER BY id";

    private final ActivityLogDao activityLogDao;

    public SupplierDao(ActivityLogDao activityLogDao) {
        this.activityLogDao = activityLogDao;
    }

    /** Adds a supplier and logs it (SUPPLIER_ADDED). */
    public int insert(int userId, String name, String contact, String address) {
        int newId = -1;
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.setString(2, contact);
            ps.setString(3, address);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                newId = keys.next() ? keys.getInt(1) : -1;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not add supplier.", e);
        }
        activityLogDao.logAction(userId, ActionType.SUPPLIER_ADDED, "Added supplier #" + newId + " " + name + ".");
        return newId;
    }

    public Supplier findById(int id) {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(SQL_FIND_BY_ID)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not find supplier.", e);
        }
    }

    public List<Supplier> findAll() {
        List<Supplier> list = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(SQL_FIND_ALL);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(map(rs));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load suppliers.", e);
        }
        return list;
    }

    private Supplier map(ResultSet rs) throws SQLException {
        return new Supplier(rs.getInt("id"), rs.getString("name"),
                rs.getString("contact_number"), rs.getString("address"));
    }
}