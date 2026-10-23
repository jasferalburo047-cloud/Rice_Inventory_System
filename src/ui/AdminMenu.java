package ui;

import dao.ActivityLogDao;
import dao.ProductDao;
import dao.StockTransactionDao;
import dao.SupplierDao;
import dao.UserDao;
import model.ActivityLog;
import model.Product;
import model.TransactionType;
import model.User;
import model.UserStatus;

import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

/**
 * Menu for the Manager / Admin role.
 * NOTE: no SQL in this class - only DAO calls.
 */
public class AdminMenu extends RoleMenu {

    public AdminMenu(Scanner scanner, User user, UserDao userDao, ProductDao productDao,
                     SupplierDao supplierDao, StockTransactionDao transactionDao, ActivityLogDao logDao) {
        super(scanner, user, userDao, productDao, supplierDao, transactionDao, logDao);
    }

    @Override
    protected boolean showLowStockAlertOnStart() { return true; }

    @Override
    protected List<MenuItem> buildMenu() {
        return Arrays.asList(
                new MenuItem("View All System Users", this::viewAllUsers),
                new MenuItem("View Pending Registrations", this::viewPendingUsers),
                new MenuItem("Approve User Account", this::approveUserAccount),
                new MenuItem("Deactivate / Reactivate User Account", this::toggleAccountStatus),
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
                new MenuItem("View Activity (Audit) Log", this::viewLogs)
        );
    }

    // ================================================================
    //  USER MANAGEMENT
    // ================================================================

    private void viewAllUsers() {
        printUserTable("All System Users", userDao.getAllUsers());
    }

    private void viewPendingUsers() {
        printUserTable("Pending Registrations", userDao.getPendingUsers());
    }

    private void approveUserAccount() {
        List<User> pending = userDao.getPendingUsers();
        printUserTable("Pending Registrations", pending);
        if (pending.isEmpty()) {
            return;
        }

        while (true) {
            int id = in.askInt("Enter User ID to approve (0 to cancel): ");
            if (id == 0) {
                System.out.println("Approval cancelled.");
                return;
            }

            User target = findInList(pending, id);
            if (target == null) {
                System.out.println("  User ID " + id + " is not in the pending list. Try again or enter 0 to cancel.");
                continue;
            }

            if (userDao.approveUser(id)) {
                log("Approved user " + target.username() + " (ID " + id + ")");
                System.out.println("SUCCESS: " + target.username() + " (" + target.role()
                        + ") has been APPROVED and can now log in.");
            } else {
                System.out.println("Approval failed. Please try again.");
            }
            return;
        }
    }

    private void toggleAccountStatus() {
        List<User> users = userDao.getAllUsers();
        printUserTable("All System Users", users);

        while (true) {
            int id = in.askInt("Enter User ID to deactivate / reactivate (0 to cancel): ");
            if (id == 0) {
                System.out.println("Action cancelled.");
                return;
            }

            User target = findInList(users, id);
            if (target == null) {
                System.out.println("  User ID " + id + " does not exist. Try again or enter 0 to cancel.");
                continue;
            }
            if (target.id() == user.id()) {
                System.out.println("  You cannot change your own account.");
                continue;
            }
            if (target.status() == UserStatus.PENDING) {
                System.out.println("  That account is still pending. Use \"Approve User Account\" instead.");
                continue;
            }

            UserStatus newStatus = (target.status() == UserStatus.APPROVED)
                    ? UserStatus.INACTIVE
                    : UserStatus.APPROVED;

            if (userDao.updateStatus(id, newStatus)) {
                log("Set user " + target.username() + " to " + newStatus);
                System.out.println(target.username() + " is now " + newStatus + ".");
            } else {
                System.out.println("Update failed. Please try again.");
            }
            return;
        }
    }

    private User findInList(List<User> users, int id) {
        for (User u : users) {
            if (u.id() == id) {
                return u;
            }
        }
        return null;
    }

    /** Reusable table renderer for any list of users. */
    private void printUserTable(String title, List<User> users) {
        System.out.println("\n== " + title + " ==");
        if (users.isEmpty()) {
            System.out.println("No accounts to display.");
            return;
        }

        String border = "+" + "-".repeat(7) + "+" + "-".repeat(17) + "+"
                + "-".repeat(14) + "+" + "-".repeat(12) + "+";

        System.out.println(border);
        System.out.println(String.format("| %-5s | %-15s | %-12s | %-10s |", "ID", "Username", "Role", "Status"));
        System.out.println(border);
        for (User u : users) {
            System.out.println(String.format("| %-5s | %-15s | %-12s | %-10s |",
                    u.id(), fit(u.username(), 15), u.role(), u.status()));
        }
        System.out.println(border);
        System.out.println("Total: " + users.size());
    }

    private String fit(String text, int max) {
        return text.length() <= max ? text : text.substring(0, max - 1) + "~";
    }

    // ================================================================
    //  PRODUCTS / SUPPLIERS / LOGS
    // ================================================================

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
        if (p == null) {
            System.out.println("Product not found.");
            return;
        }

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
            } catch (NumberFormatException e) {
                System.out.println("  Invalid price, kept old.");
            }
        }
        s = in.ask("Reorder level [" + p.getReorderLevel() + "]: ");
        if (!s.isEmpty()) {
            try {
                double d = Double.parseDouble(s);
                if (d >= 0) p.setReorderLevel(d); else System.out.println("  Invalid, kept old.");
            } catch (NumberFormatException e) {
                System.out.println("  Invalid, kept old.");
            }
        }
        productDao.update(p);
        log("Updated product #" + p.getId());
        System.out.println("Product updated.");
    }

    private void deleteProduct() {
        viewProducts();
        Product p = productDao.findById(in.askInt("Product ID to remove: "));
        if (p == null) {
            System.out.println("Product not found.");
            return;
        }
        if (!in.askYes("Remove \"" + p.getName() + "\"? (y/n): ")) {
            return;
        }
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