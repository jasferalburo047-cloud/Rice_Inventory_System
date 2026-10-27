package dao;

import model.Product;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class ProductDao {

    private static final String SQL_INSERT = "INSERT INTO products (name, rice_type, price, quantity, reorder_level, supplier_id) VALUES (?, ?, ?, ?, ?, ?)";
    private static final String SQL_FIND_ALL = "SELECT id, name, rice_type, price, quantity, reorder_level, supplier_id FROM products ORDER BY id";
    private static final String SQL_FIND_LOW_STOCK = "SELECT id, name, rice_type, price, quantity, reorder_level, supplier_id FROM products WHERE quantity <= reorder_level ORDER BY id";
    private static final String SQL_SEARCH = "SELECT id, name, rice_type, price, quantity, reorder_level, supplier_id FROM products WHERE LOWER(name) LIKE ? OR LOWER(rice_type) LIKE ? ORDER BY id";
    private static final String SQL_FIND_BY_ID = "SELECT id, name, rice_type, price, quantity, reorder_level, supplier_id FROM products WHERE id = ?";
    private static final String SQL_UPDATE = "UPDATE products SET name = ?, rice_type = ?, price = ?, reorder_level = ?, supplier_id = ? WHERE id = ?";
    private static final String SQL_SET_QUANTITY = "UPDATE products SET quantity = ? WHERE id = ?";
    private static final String SQL_DELETE = "DELETE FROM products WHERE id = ?";

    public int insert(Product p) {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, p.getName());
            ps.setString(2, p.getRiceType());
            ps.setDouble(3, p.getPrice());
            ps.setDouble(4, p.getQuantity());
            ps.setDouble(5, p.getReorderLevel());
            ps.setInt(6, p.getSupplierId());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : -1;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not add product.", e);
        }
    }

    public List<Product> findAll() {
        return query(SQL_FIND_ALL);
    }

    public List<Product> findLowStock() {
        return query(SQL_FIND_LOW_STOCK);
    }

    public List<Product> search(String keyword) {
        String like = "%" + keyword.toLowerCase() + "%";
        return query(SQL_SEARCH, like, like);
    }

    public Product findById(int id) {
        List<Product> list = query(SQL_FIND_BY_ID, id);
        return list.isEmpty() ? null : list.get(0);
    }

    public boolean update(Product p) {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(SQL_UPDATE)) {
            ps.setString(1, p.getName());
            ps.setString(2, p.getRiceType());
            ps.setDouble(3, p.getPrice());
            ps.setDouble(4, p.getReorderLevel());
            ps.setInt(5, p.getSupplierId());
            ps.setInt(6, p.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Could not update product.", e);
        }
    }

    public boolean setQuantity(int id, double quantity) {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(SQL_SET_QUANTITY)) {
            ps.setDouble(1, quantity);
            ps.setInt(2, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Could not update stock quantity.", e);
        }
    }

    public boolean delete(int id) {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(SQL_DELETE)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Could not remove product.", e);
        }
    }

    private List<Product> query(String sql, Object... params) {
        List<Product> list = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Product(rs.getInt("id"), rs.getString("name"), rs.getString("rice_type"),
                            rs.getDouble("price"), rs.getDouble("quantity"),
                            rs.getDouble("reorder_level"), rs.getInt("supplier_id")));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load products.", e);
        }
        return list;
    }
}