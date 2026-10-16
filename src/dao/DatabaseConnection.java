package dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/** Handles the SQLite connection and creates the tables on first run. */
public final class DatabaseConnection {

    private static final String URL = "jdbc:sqlite:rice_inventory.db";

    private static final String CREATE_USERS_TABLE = """
            CREATE TABLE IF NOT EXISTS users (
                id       INTEGER PRIMARY KEY AUTOINCREMENT,
                username TEXT NOT NULL UNIQUE,
                password TEXT NOT NULL,
                roles    TEXT NOT NULL,
                status   TEXT NOT NULL DEFAULT 'PENDING'
            )
            """;

    private DatabaseConnection() {}

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL);
    }

    public static void initialize() {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(CREATE_USERS_TABLE);
        } catch (SQLException e) {
            throw new IllegalStateException("Could not initialize database: " + e.getMessage(), e);
        }
    }
}