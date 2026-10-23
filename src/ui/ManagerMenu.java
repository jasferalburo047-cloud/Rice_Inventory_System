package ui;

import dao.*;
import model.*;

import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class ManagerMenu extends RoleMenu {

    public ManagerMenu(Scanner scanner, User user, UserDao userDao, ProductDao productDao,
                       SupplierDao supplierDao, StockTransactionDao transactionDao, ActivityLogDao logDao) {
        super(scanner, user, userDao, productDao, supplierDao, transactionDao, logDao);
    }

    @Override
    protected boolean showLowStockAlertOnStart() { return true; }

    @Override
    protected List<MenuItem> buildMenu() {
        return Arrays.asList(
                new MenuItem("View all rice products", this::viewProducts),
                new MenuItem("Search product (name / type)", this::searchProduct),
                new MenuItem("Add new rice product", this::addProduct),
                new MenuItem("Edit product details / price", this::updateProduct),
                new MenuItem("Remove discontinued product", this::deleteProduct),
                new MenuItem("Record stock-in", () -> recordStock(TransactionType.STOCK_IN)),
                new MenuItem("Record stock-out", () -> recordStock(TransactionType.STOCK_OUT)),
                new MenuItem("Update stock quantity", this::adjustQuantity),
                new MenuItem("View transaction history", this::viewTransactions),
                new MenuItem("Low-Stock Alert Report", this::lowStockReport),
                new MenuItem("Add supplier", this::addSupplier),
                new MenuItem("View suppliers", this::viewSuppliers),
                new MenuItem("Manage user accounts (approve / deactivate)", this::manageAccounts),
                new MenuItem("View Activity (Audit) Log", this::viewLogs)
        );
    }

    private void addProduct() {
        System.out.println("\n== Add New Rice Product ==");
        String name = in.askRequired("Product name: ");
        String type = in.askRequired("Rice type (e.g., Jasmine, Dinorado): ");
        double price = in.askPositive("Price per kilo: ", false);
        double qty = in.askPositive("Initial quantity (kg): ", true);
        double reorder = in.askPositive("Reorder (minimum) level (kg): ", true);
        viewSuppliers();
        int supplierId = in.askInt("Supplier ID: ");
        if (supplierDao.findById(supplierId) == null) {
            System.out.println("Supplier not found. Product not saved.");
            return;
        }
        int id = productDao.insert(new Product(0, name, type, price, qty, reorder, supplierId));
        log("Added product #" + id + " (" + name + ")");
        System.out.println("Product added with ID " + id);
    }

    private void updateProduct() {
        viewProducts();
        Product p = productDao.findById(in.askInt("Product ID to edit: "));
        if (p == null) { System.out.println("Product not found."); return; }

        System.out.println("Press Enter to keep the current value.");
        String s = in.ask("Name [" + p.getName() + "]: ");
        if (!s.isEmpty()) p.setName(s);
        s = in.ask("Rice type [" + p.getRiceType() + "]: ");
        if (!s.isEmpty()) p.setRiceType(s);
        s = in.ask("Price per kilo [" + p.getPrice() + "]: ");
        if (!s.isEmpty()) {
            try {
                double d = Double.parseDouble(s);
                if (d > 0) p.setPrice(d); else System.out.println("  Invalid price, kept old.");
            } catch (NumberFormatException e) { System.out.println("  Invalid price, kept old."); }
        }
        s = in.ask("Reorder level [" + p.getReorderLevel() + "]: ");
        if (!s.isEmpty()) {
            try {
                double d = Double.parseDouble(s);
                if (d >= 0) p.setReorderLevel(d); else System.out.println("  Invalid, kept old.");
            } catch (NumberFormatException e) { System.out.println("  Invalid, kept old."); }
        }
        productDao.update(p);
        log("Updated product #" + p.getId());
        System.out.println("Product updated.");
    }

    private void deleteProduct() {
        viewProducts();
        Product p = productDao.findById(in.askInt("Product ID to remove: "));
        if (p == null) { System.out.println("Product not found."); return; }
        if (!in.askYes("Remove \"" + p.getName() + "\"? (y/n): ")) return;
        productDao.delete(p.getId());
        log("Removed product #" + p.getId() + " (" + p.getName() + ")");
        System.out.println("Product removed.");
    }

    private void addSupplier() {
        System.out.println("\n== Add Supplier ==");
        String name = in.askRequired("Supplier name: ");
        String contact = in.askRequired("Contact number: ");
        String address = in.askRequired("Address: ");
        int id = supplierDao.insert(name, contact, address);
        log("Added supplier #" + id + " (" + name + ")");
        System.out.println("Supplier added with ID " + id);
    }

    private void manageAccounts() {
        System.out.println("\n== User Accounts ==");
        List<User> users = userDao.findAll();
        line();
        System.out.printf("%-4s %-14s %-24s %-10s %s%n", "ID", "Username", "Full Name", "Role", "Status");
        line();
        for (User u : users) {
            System.out.printf("%-4d %-14s %-24s %-10s %s%n", u.getId(), u.getUsername(),
                    u.getFullName(), u.getRole(), u.getStatus());
        }
        line();

        int id = in.askInt("User ID to change (0 to cancel): ");
        if (id == 0) return;
        if (id == user.getId()) { System.out.println("You cannot change your own account."); return; }
        User target = userDao.findById(id);
        if (target == null) { System.out.println("User not found."); return; }

        System.out.println("1) Approve / Activate   2) Deactivate");
        int choice = in.askInt("Choose: ");
        UserStatus newStatus;
        if (choice == 1) newStatus = UserStatus.ACTIVE;
        else if (choice == 2) newStatus = UserStatus.INACTIVE;
        else { System.out.println("Cancelled."); return; }

        userDao.updateStatus(id, newStatus);
        log("Set user " + target.getUsername() + " to " + newStatus);
        System.out.println(target.getUsername() + " is now " + newStatus + ".");
    }

    private void viewLogs() {
        System.out.println("\n== Activity (Audit) Log ==");
        List<ActivityLog> logs = logDao.findAll();
        line();
        System.out.printf("%-5s %-16s %-12s %s%n", "ID", "Date", "User", "Action");
        line();
        for (ActivityLog l : logs) {
            System.out.printf("%-5d %-16s %-12s %s%n", l.getId(), l.getDate(), l.getUsername(), l.getAction());
        }
        line();
    }
}