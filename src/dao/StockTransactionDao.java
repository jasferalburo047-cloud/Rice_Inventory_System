package dao;

import model.StockTransaction;
import model.TransactionType;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class StockTransactionDao {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private static final String SELECT =
            "SELECT t.id, t.product_id, p.name AS product_name, t.user_id, u.username, "
                    + "t.type, t.quantity, t.total_amount, t.transaction_date "
                    + "FROM stock_transactions t "
                    + "LEFT JOIN products p ON p.id = t.product_id "
                    + "JOIN users u ON u.id = t.user_id ";

    /**
     * Updates the product stock AND records the transaction in one database transaction.
     * @return false if there was not enough stock (nothing is changed in that case)
     */
    public boolean record(int productId, int userId, TransactionType type, double quantity, double totalAmount) {
        String updateSql = (type == TransactionType.STOCK_IN)
                ? "UPDATE products SET quantity = quantity + ? WHERE id = ?"
                : "UPDATE products SET quantity = quantity - ? WHERE id = ? AND quantity >= ?";
        String insertSql = "INSERT INTO stock_transactions "
                + "(product_id, user_id, type, quantity, total_amount, transaction_date) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection c = DatabaseConnection.getConnection()) {
            c.setAutoCommit(false);
            try (PreparedStatement up = c.prepareStatement(updateSql);
                 PreparedStatement ins = c.prepareStatement(insertSql)) {

                up.setDouble(1, quantity);
                up.setInt(2, productId);
                if (type != TransactionType.STOCK_IN) up.setDouble(3, quantity);
                if (up.executeUpdate() == 0) {
                    c.rollback();
                    return false;
                }

                ins.setInt(1, productId);
                ins.setInt(2, userId);
                ins.setString(3, type.name());
                ins.setDouble(4, quantity);
                ins.setDouble(5, totalAmount);
                ins.setString(6, LocalDateTime.now().format(FMT));
                ins.executeUpdate();

                c.commit();
                return true;
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not record transaction.", e);
        }
    }

    public List<StockTransaction> findAll() {
        return query(SELECT + "ORDER BY t.id DESC", null);
    }

    public List<StockTransaction> findByUser(int userId) {
        return query(SELECT + "WHERE t.user_id = ? ORDER BY t.id DESC", userId);
    }

    private List<StockTransaction> query(String sql, Integer userId) {
        List<StockTransaction> list = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            if (userId != null) ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new StockTransaction(
                            rs.getInt("id"), rs.getInt("product_id"), rs.getString("product_name"),
                            rs.getInt("user_id"), rs.getString("username"),
                            TransactionType.valueOf(rs.getString("type")),
                            rs.getDouble("quantity"), rs.getDouble("total_amount"),
                            rs.getString("transaction_date")));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load transactions.", e);
        }
        return list;
    }
}