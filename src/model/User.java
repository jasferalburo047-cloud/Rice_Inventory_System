package model;

/** Abstract parent of all system users (Inheritance + Abstraction). */
public abstract class User {
    private final int id;
    private final String username;
    private final String fullName;
    private final UserStatus status;

    protected User(int id, String username, String fullName, UserStatus status) {
        this.id = id;
        this.username = username;
        this.fullName = fullName;
        this.status = status;
    }

    public int getId() { return id; }
    public String getUsername() { return username; }
    public String getFullName() { return fullName; }
    public UserStatus getStatus() { return status; }

    public abstract Role getRole();
}