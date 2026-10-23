package model;

public class Cashier extends User {
    public Cashier(int id, String username, String fullName, UserStatus status) {
        super(id, username, fullName, status);
    }
    @Override
    public Role getRole() { return Role.CASHIER; }
}