package model;

/**
 * Java Record representing a row in the users table.
 * The password field holds the salted hash, never the plain text.
 */
public record User(int id, String username, String password, String roles, String status) {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_DEACTIVATED = "DEACTIVATED";

    public static final String ROLE_MANAGER = "MANAGER";
    public static final String ROLE_STAFF = "STAFF";
    public static final String ROLE_CASHIER = "CASHIER";

    public boolean isActive() {
        return STATUS_ACTIVE.equals(status);
    }

    public boolean isManager() {
        return ROLE_MANAGER.equals(roles);
    }
}