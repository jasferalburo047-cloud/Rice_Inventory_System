package ui;

import dao.*;
import model.*;
import util.ConsoleInput;

import java.util.List;
import java.util.Scanner;

/**
 * Base class of the role-based menus (Abstraction + Polymorphism).
 * Each role decides its own menu items; shared features live here.
 * NOTE: no SQL in this layer - only DAO calls.
 */
public abstract class RoleMenu {

    protected static class MenuItem {
        final String label;
        final Runnable action;
        MenuItem(String label, Runnable action) {
            this.label = label;
            this.action = action;
        }
    }

    protected final User user;
    protected final UserDao userDao;
    protected final ProductDao productDao;
    protected final SupplierDao supplierDao;
    protected final StockTransactionDao transactionDao;
    protected final ActivityLogDao logDao;
    protected final ConsoleInput in;

    protected RoleMenu(Scanner scanner, User user, UserDao userDao, ProductDao productDao,
                       SupplierDao supplierDao, StockTransactionDao transactionDao, ActivityLogDao logDao) {
        this.in = new ConsoleInput(scanner);   // wraps the single shared Scanner
        this.user = user;
        this.userDao = userDao;
        this.productDao = productDao;
        this.supplierDao = supplierDao;
        this.transactionDao = transactionDao;
        this.logDao = logDao;
    }

    protected abstract List<MenuItem> buildMenu();

    protected boolean showLowStockAlertOnStart() { return false; }
    protected boolean ownTransactionsOnly() { return false; }

    // ------------------------------------------------------------------ loop
    public void start() {
        if (showLowStockAlertOnStart()) {
            try {
                int low = productDao.findLowStock().size();
                if (low > 0) System.out.println("\n! ALERT: " + low + " product(s) at or below minimum stock level.");
            } catch (DataAccessException e) {
                System.out.println("Database error: " + e.getMessage());
            }
        }

        List<MenuItem> items = buildMenu();
        while (true) {
            System.out.println("\n===== MAIN MENU - " + user.getFullName() + " (" + user.getRole() + ") =====");
            for (int i = 0; i < items.size(); i++) {
                System.out.printf("%2d. %s%n", i + 1, items.get(i).label);
            }
            System.out.println(" 0. Logout");

            int choice = in.askInt("Choose: ");
            if (choice == 0) {
                log("Logged out");
                return;
            }
            if (choice < 1 || choice > items.size()) {
                System.out.println("Invalid choice.");
                continue;
            }
            try {
                items.get(choice - 1).action.run();
            } catch (DataAccessException e) {
                System.out.println("Database error: " + e.getMessage());
            }
        }
    }

    protected void log(String action) {
        logDao.log(user.getId(), action);
    }

    protected void line() { System.out.println("-".repeat(78)); }

    // ------------------------------------------------------- shared features
    protected void viewProducts() {
        System.out.println("\n== Rice Products ==");
        List<Product> products = productDao.findAll();
        if (products.isEmpty()) { System.out.println("No products yet."); return; }
        printProducts(products);
    }

    protected void searchProduct() {
        String key = in.askRequired("\nSearch (name or rice type): ");
        List<Product> found = productDao.search(key);
        if (found.isEmpty()) System.out.println("No matching product found.");
        else printProducts(found);
    }

    private void printProducts(List<Product> products) {
        line();
        System.out.printf("%-4s %-20s %-12s %9s %10s %9s %s%n",
                "ID", "Name", "Type", "Price/kg", "Stock(kg)", "Reorder", "Status");
        line();
        for (Product p : products) {
            System.out.printf("%-4d %-20s %-12s %9.2f %10.2f %9.2f %s%n", p.getId(), p.getName(),
                    p.getRiceType(), p.getPrice(), p.getQuantity(), p.getReorderLevel(),
                    p.isLowStock() ? "LOW STOCK" : "OK");
        }
        line();
    }

    protected void viewSuppliers() {
        System.out.println("\n== Suppliers ==");
        List<Supplier> suppliers = supplierDao.findAll();
        if (suppliers.isEmpty()) { System.out.println("No suppliers yet."); return; }
        line();
        System.out.printf("%-4s %-26s %-16s %s%n", "ID", "Name", "Contact", "Address");
        line();
        for (Supplier s : suppliers) {
            System.out.printf("%-4d %-26s %-16s %s%n", s.getId(), s.getName(), s.getContactNumber(), s.getAddress());
        }
        line();
    }

    protected void viewTransactions() {
        System.out.println("\n== Transaction History ==");
        List<StockTransaction> list = ownTransactionsOnly()
                ? transactionDao.findByUser(user.getId())
                : transactionDao.findAll();
        if (list.isEmpty()) { System.out.println("No transactions recorded."); return; }
        line();
        System.out.printf("%-5s %-16s %-10s %-18s %9s %11s %s%n",
                "ID", "Date", "Type", "Product", "Qty(kg)", "Total", "By");
        line();
        for (StockTransaction t : list) {
            System.out.printf("%-5d %-16s %-10s %-18s %9.2f %11.2f %s%n", t.getId(), t.getDate(), t.getType(),
                    t.getProductName() == null ? "(removed)" : t.getProductName(),
                    t.getQuantity(), t.getTotalAmount(), t.getUsername());
        }
        line();
    }

    protected void lowStockReport() {
        System.out.println("\n== Low-Stock Alert Report ==");
        List<Product> low = productDao.findLowStock();
        if (low.isEmpty()) { System.out.println("All products are above their minimum stock level."); return; }
        line();
        System.out.printf("%-20s %-12s %10s %9s  %s%n", "Product", "Type", "Current", "Reorder", "Supplier");
        line();
        for (Product p : low) {
            Supplier s = supplierDao.findById(p.getSupplierId());
            System.out.printf("%-20s %-12s %10.2f %9.2f  %s%n", p.getName(), p.getRiceType(),
                    p.getQuantity(), p.getReorderLevel(),
                    s == null ? "-" : s.getName() + " (" + s.getContactNumber() + ")");
        }
        line();
    }

    protected void recordStock(TransactionType type) {
        System.out.println("\n== Record " + (type == TransactionType.STOCK_IN ? "Stock-In" : "Stock-Out") + " ==");
        viewProducts();
        Product p = productDao.findById(in.askInt("Product ID: "));
        if (p == null) { System.out.println("Product not found."); return; }
        double qty = in.askPositive("Quantity (kg): ", false);

        if (!transactionDao.record(p.getId(), user.getId(), type, qty, 0)) {
            System.out.println("Not enough stock. Available: " + p.getQuantity() + " kg");
            return;
        }
        log(type + " " + qty + " kg of " + p.getName());
        Product updated = productDao.findById(p.getId());
        System.out.println("Recorded. New stock: " + updated.getQuantity() + " kg");
        if (updated.isLowStock()) System.out.println("  ! Alert: low stock for " + updated.getName());
    }

    protected void adjustQuantity() {
        viewProducts();
        Product p = productDao.findById(in.askInt("Product ID: "));
        if (p == null) { System.out.println("Product not found."); return; }
        double newQty = in.askPositive("New actual quantity in stock (kg): ", true);
        productDao.setQuantity(p.getId(), newQty);
        log("Adjusted stock of " + p.getName() + " from " + p.getQuantity() + " to " + newQty + " kg");
        System.out.println("Stock quantity updated.");
        if (newQty <= p.getReorderLevel()) System.out.println("  ! Alert: this product is at or below its reorder level.");
    }
}