package model;

import java.util.Objects;

/**
 * Immutable user account. The password is never stored in this object.
 * Accessor names (id(), username(), ...) match a Java record, so this can be
 * swapped for "public record User(...)" on JDK 16+ without touching other files.
 */
public final class User {

    private final int id;
    private final String username;
    private final String fullName;
    private final Role role;
    private final UserStatus status;

    public User(int id, String username, String fullName, Role role, UserStatus status) {
        this.id = id;
        this.username = username;
        this.fullName = fullName;
        this.role = role;
        this.status = status;
    }

    public int id() { return id; }
    public String username() { return username; }
    public String fullName() { return fullName; }
    public Role role() { return role; }
    public UserStatus status() { return status; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User)) return false;
        User other = (User) o;
        return id == other.id
                && Objects.equals(username, other.username)
                && Objects.equals(fullName, other.fullName)
                && role == other.role
                && status == other.status;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, username, fullName, role, status);
    }

    @Override
    public String toString() {
        return "User[id=" + id + ", username=" + username + ", fullName=" + fullName
                + ", role=" + role + ", status=" + status + "]";
    }
}