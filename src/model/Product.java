package model;

public class Product {
    private final int id;
    private String name;
    private String riceType;
    private double price;        // per kilo
    private double quantity;     // kilos in stock
    private double reorderLevel; // minimum stock level
    private int supplierId;

    public Product(int id, String name, String riceType, double price,
                   double quantity, double reorderLevel, int supplierId) {
        this.id = id;
        this.name = name;
        this.riceType = riceType;
        this.price = price;
        this.quantity = quantity;
        this.reorderLevel = reorderLevel;
        this.supplierId = supplierId;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getRiceType() { return riceType; }
    public void setRiceType(String riceType) { this.riceType = riceType; }
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
    public double getQuantity() { return quantity; }
    public void setQuantity(double quantity) { this.quantity = quantity; }
    public double getReorderLevel() { return reorderLevel; }
    public void setReorderLevel(double reorderLevel) { this.reorderLevel = reorderLevel; }
    public int getSupplierId() { return supplierId; }
    public void setSupplierId(int supplierId) { this.supplierId = supplierId; }

    public boolean isLowStock() { return quantity <= reorderLevel; }
}