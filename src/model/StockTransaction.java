package model;

public class StockTransaction {
    private final int id;
    private final int productId;
    private final String productName; // null if the product was removed
    private final int userId;
    private final String username;
    private final TransactionType type;
    private final double quantity;
    private final double totalAmount;
    private final String date;

    public StockTransaction(int id, int productId, String productName, int userId, String username,
                            TransactionType type, double quantity, double totalAmount, String date) {
        this.id = id;
        this.productId = productId;
        this.productName = productName;
        this.userId = userId;
        this.username = username;
        this.type = type;
        this.quantity = quantity;
        this.totalAmount = totalAmount;
        this.date = date;
    }

    public int getId() { return id; }
    public int getProductId() { return productId; }
    public String getProductName() { return productName; }
    public int getUserId() { return userId; }
    public String getUsername() { return username; }
    public TransactionType getType() { return type; }
    public double getQuantity() { return quantity; }
    public double getTotalAmount() { return totalAmount; }
    public String getDate() { return date; }
}