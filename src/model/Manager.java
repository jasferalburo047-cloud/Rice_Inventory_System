package model;

public class Manager extends User {
    public Manager(int id, String username, String fullName, UserStatus status) {
        super(id, username, fullName, status);
    }
    @Override
    public Role getRole() { return Role.MANAGER; }
}