package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/** Creates all required tables (if missing) before the CLI starts. */
public class DatabaseInitializer {

    private static final String CREATE_USERS =
            "CREATE TABLE IF NOT EXISTS users ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "username TEXT NOT NULL UNIQUE COLLATE NOCASE, "
                    + "password INTEGER NOT NULL, "
                    + "full_name TEXT NOT NULL, "
                    + "role TEXT NOT NULL CHECK (role IN ('MANAGER','STAFF','CASHIER')), "
                    + "status TEXT NOT NULL DEFAULT 'PENDING' "
                    + "CHECK (status IN ('PENDING','APPROVED','INACTIVE')))";

    private static final String CREATE_SUPPLIERS =
            "CREATE TABLE IF NOT EXISTS suppliers ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "name TEXT NOT NULL, "
                    + "contact_number TEXT NOT NULL, "
                    + "address TEXT NOT NULL)";

    private static final String CREATE_PRODUCTS =
            "CREATE TABLE IF NOT EXISTS products ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "name TEXT NOT NULL, "
                    + "rice_type TEXT NOT NULL, "
                    + "price REAL NOT NULL CHECK (price > 0), "
                    + "quantity REAL NOT NULL DEFAULT 0 CHECK (quantity >= 0), "
                    + "reorder_level REAL NOT NULL DEFAULT 0, "
                    + "supplier_id INTEGER NOT NULL, "
                    + "FOREIGN KEY (supplier_id) REFERENCES suppliers(id))";

    private static final String CREATE_STOCK_TRANSACTIONS =
            "CREATE TABLE IF NOT EXISTS stock_transactions ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "product_id INTEGER, "
                    + "user_id INTEGER NOT NULL, "
                    + "type TEXT NOT NULL CHECK (type IN ('STOCK_IN','STOCK_OUT','SALE')), "
                    + "quantity REAL NOT NULL, "
                    + "total_amount REAL NOT NULL DEFAULT 0, "
                    + "transaction_date TEXT NOT NULL, "
                    + "FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE SET NULL, "
                    + "FOREIGN KEY (user_id) REFERENCES users(id))";

    private static final String CREATE_ACTIVITY_LOGS =
            "CREATE TABLE IF NOT EXISTS activity_logs ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "user_id INTEGER NOT NULL, "
                    + "action_type TEXT NOT NULL, "
                    + "description TEXT NOT NULL, "
                    + "timestamp TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                    + "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE)";

    private static final String LOG_TABLE_COLUMNS = "PRAGMA table_info(activity_logs)";
    private static final String DROP_ACTIVITY_LOGS = "DROP TABLE IF EXISTS activity_logs";

    private static final String COUNT_USERS = "SELECT COUNT(*) FROM users";
    private static final String COUNT_SUPPLIERS = "SELECT COUNT(*) FROM suppliers";
    private static final String COUNT_PRODUCTS = "SELECT COUNT(*) FROM products";

    private static final String INSERT_USER =
            "INSERT INTO users (username, password, full_name, role, status) VALUES (?, ?, ?, ?, ?)";

    private static final String INSERT_SUPPLIER =
            "INSERT INTO suppliers (name, contact_number, address) VALUES (?, ?, ?)";

    private static final String INSERT_PRODUCT =
            "INSERT INTO products (name, rice_type, price, quantity, reorder_level, supplier_id) "
                    + "VALUES (?, ?, ?, ?, ?, ?)";

    public void initialize() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            try (Statement st = conn.createStatement()) {
                st.execute(CREATE_USERS);
                st.execute(CREATE_SUPPLIERS);
                st.execute(CREATE_PRODUCTS);
                st.execute(CREATE_STOCK_TRANSACTIONS);
                st.execute(CREATE_ACTIVITY_LOGS);
            }
            upgradeOldActivityLogsTable(conn);
            seedDefaults(conn);
        } catch (SQLException e) {
            throw new DataAccessException("Failed to initialize the database: " + e.getMessage(), e);
        }
    }

    /**
     * If an older database still has the previous activity_logs layout (no action_type column),
     * rebuild the table with the new layout so logging works without deleting the .db file.
     */
    private void upgradeOldActivityLogsTable(Connection conn) throws SQLException {
        boolean hasActionType = false;
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(LOG_TABLE_COLUMNS)) {
            while (rs.next()) {
                if ("action_type".equalsIgnoreCase(rs.getString("name"))) {
                    hasActionType = true;
                }
            }
        }
        if (!hasActionType) {
            try (Statement st = conn.createStatement()) {
                st.execute(DROP_ACTIVITY_LOGS);
                st.execute(CREATE_ACTIVITY_LOGS);
            }
        }
    }

    /** First run only: a default admin (already APPROVED) and a little sample data. */
    private void seedDefaults(Connection conn) throws SQLException {
        if (count(conn, COUNT_USERS) == 0) {
            try (PreparedStatement ps = conn.prepareStatement(INSERT_USER)) {
                ps.setString(1, "admin");
                ps.setInt(2, 1234);
                ps.setString(3, "System Admin");
                ps.setString(4, "MANAGER");
                ps.setString(5, "APPROVED");
                ps.executeUpdate();
            }
        }
        if (count(conn, COUNT_SUPPLIERS) == 0 && count(conn, COUNT_PRODUCTS) == 0) {
            insertSupplier(conn, "Golden Grains Trading", "09171234567", "Cebu City");
            insertSupplier(conn, "Visayas Rice Co.", "09281234567", "Talisay City");
            insertProduct(conn, "Jasmine Premium", "Jasmine", 58.0, 120, 30, 1);
            insertProduct(conn, "Sinandomeng", "Sinandomeng", 52.0, 25, 40, 2);
            insertProduct(conn, "Dinorado Special", "Dinorado", 65.0, 80, 25, 1);
        }
    }

    private int count(Connection conn, String sql) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private void insertSupplier(Connection conn, String name, String contact, String address) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(INSERT_SUPPLIER)) {
            ps.setString(1, name);
            ps.setString(2, contact);
            ps.setString(3, address);
            ps.executeUpdate();
        }
    }

    private void insertProduct(Connection conn, String name, String type, double price,
                               double qty, double reorder, int supplierId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(INSERT_PRODUCT)) {
            ps.setString(1, name);
            ps.setString(2, type);
            ps.setDouble(3, price);
            ps.setDouble(4, qty);
            ps.setDouble(5, reorder);
            ps.setInt(6, supplierId);
            ps.executeUpdate();
        }
    }
}