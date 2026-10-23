package dao;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/** Creates all required tables (if missing) before the CLI starts. */
public class DatabaseInitializer {

    public void initialize() {
        try (Connection conn = DatabaseConnection.getConnection();
             Statement st = conn.createStatement()) {

            st.execute("CREATE TABLE IF NOT EXISTS users ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "username TEXT NOT NULL UNIQUE COLLATE NOCASE, "
                    + "password INTEGER NOT NULL, "
                    + "full_name TEXT NOT NULL, "
                    + "role TEXT NOT NULL CHECK (role IN ('MANAGER','STAFF','CASHIER')), "
                    + "status TEXT NOT NULL DEFAULT 'PENDING' "
                    + "CHECK (status IN ('PENDING','ACTIVE','INACTIVE')))");

            st.execute("CREATE TABLE IF NOT EXISTS suppliers ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "name TEXT NOT NULL, "
                    + "contact_number TEXT NOT NULL, "
                    + "address TEXT NOT NULL)");

            st.execute("CREATE TABLE IF NOT EXISTS products ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "name TEXT NOT NULL, "
                    + "rice_type TEXT NOT NULL, "
                    + "price REAL NOT NULL CHECK (price > 0), "
                    + "quantity REAL NOT NULL DEFAULT 0 CHECK (quantity >= 0), "
                    + "reorder_level REAL NOT NULL DEFAULT 0, "
                    + "supplier_id INTEGER NOT NULL, "
                    + "FOREIGN KEY (supplier_id) REFERENCES suppliers(id))");

            st.execute("CREATE TABLE IF NOT EXISTS stock_transactions ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "product_id INTEGER, "
                    + "user_id INTEGER NOT NULL, "
                    + "type TEXT NOT NULL CHECK (type IN ('STOCK_IN','STOCK_OUT','SALE')), "
                    + "quantity REAL NOT NULL, "
                    + "total_amount REAL NOT NULL DEFAULT 0, "
                    + "transaction_date TEXT NOT NULL, "
                    + "FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE SET NULL, "
                    + "FOREIGN KEY (user_id) REFERENCES users(id))");

            st.execute("CREATE TABLE IF NOT EXISTS activity_logs ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "user_id INTEGER NOT NULL, "
                    + "action TEXT NOT NULL, "
                    + "log_date TEXT NOT NULL, "
                    + "FOREIGN KEY (user_id) REFERENCES users(id))");

            seedDefaults(st);

        } catch (SQLException e) {
            throw new DataAccessException("Failed to initialize the database: " + e.getMessage(), e);
        }
    }

    /** First run only: a default (already ACTIVE) manager and a little sample data. */
    private void seedDefaults(Statement st) throws SQLException {
        if (count(st, "users") == 0) {
            st.executeUpdate("INSERT INTO users (username, password, full_name, role, status) "
                    + "VALUES ('admin', 1234, 'System Manager', 'MANAGER', 'ACTIVE')");
        }
        if (count(st, "suppliers") == 0 && count(st, "products") == 0) {
            st.executeUpdate("INSERT INTO suppliers (name, contact_number, address) VALUES "
                    + "('Golden Grains Trading', '09171234567', 'Cebu City'), "
                    + "('Visayas Rice Co.', '09281234567', 'Talisay City')");
            st.executeUpdate("INSERT INTO products (name, rice_type, price, quantity, reorder_level, supplier_id) VALUES "
                    + "('Jasmine Premium', 'Jasmine', 58.0, 120, 30, 1), "
                    + "('Sinandomeng', 'Sinandomeng', 52.0, 25, 40, 2), "
                    + "('Dinorado Special', 'Dinorado', 65.0, 80, 25, 1)");
        }
    }

    private int count(Statement st, String table) throws SQLException {
        try (ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM " + table)) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }
}