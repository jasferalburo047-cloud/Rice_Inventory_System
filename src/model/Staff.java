package model;

public class Staff extends User {
    public Staff(int id, String username, String fullName, UserStatus status) {
        super(id, username, fullName, status);
    }
    @Override
    public Role getRole() { return Role.STAFF; }
}