package model;

public class Supplier {
    private final int id;
    private final String name;
    private final String contactNumber;
    private final String address;

    public Supplier(int id, String name, String contactNumber, String address) {
        this.id = id;
        this.name = name;
        this.contactNumber = contactNumber;
        this.address = address;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getContactNumber() { return contactNumber; }
    public String getAddress() { return address; }
}