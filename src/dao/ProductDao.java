package dao;

import model.Product;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProductDao {

    private static final String SELECT =
            "SELECT id, name, rice_type, price, quantity, reorder_level, supplier_id FROM products";

    public int insert(Product p) {
        String sql = "INSERT INTO products (name, rice_type, price, quantity, reorder_level, supplier_id) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
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
        return query(SELECT + " ORDER BY id");
    }

    public List<Product> findLowStock() {
        return query(SELECT + " WHERE quantity <= reorder_level ORDER BY id");
    }

    public List<Product> search(String keyword) {
        String like = "%" + keyword.toLowerCase() + "%";
        return query(SELECT + " WHERE LOWER(name) LIKE ? OR LOWER(rice_type) LIKE ? ORDER BY id", like, like);
    }

    public Product findById(int id) {
        List<Product> list = query(SELECT + " WHERE id = ?", id);
        return list.isEmpty() ? null : list.get(0);
    }

    public boolean update(Product p) {
        String sql = "UPDATE products SET name = ?, rice_type = ?, price = ?, reorder_level = ?, supplier_id = ? WHERE id = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
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
             PreparedStatement ps = c.prepareStatement("UPDATE products SET quantity = ? WHERE id = ?")) {
            ps.setDouble(1, quantity);
            ps.setInt(2, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Could not update stock quantity.", e);
        }
    }

    public boolean delete(int id) {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM products WHERE id = ?")) {
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
            for (int i = 0; i < params.length; i++) ps.setObject(i + 1, params[i]);
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