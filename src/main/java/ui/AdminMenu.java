package ui;

import dao.ActivityLogDao;
import dao.ProductDao;
import dao.StockTransactionDao;
import dao.SupplierDao;
import dao.UserDao;
import model.ActivityLog;
import model.Product;
import model.Role;
import model.TransactionType;
import model.User;
import model.UserStatus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

/**
 * Menu for the Manager / Admin role.
 * NOTE: no SQL in this class - only DAO calls. The DAOs write the audit-log entries.
 */
public class AdminMenu extends RoleMenu {

    /** Column layout of the audit-log table: ID | User ID | Action Type | Description | Timestamp */
    private static final String LOG_ROW = "%-5s | %-8s | %-18s | %-35s | %-20s";
    private static final int LOG_DESCRIPTION_WIDTH = 35;

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
                new MenuItem("Update User Role & Status", this::updateUserRoleAndStatus),
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
                new MenuItem("View System Audit Logs", this::viewAuditLogs)
        );
    }

    // ================================================================
    //  AUDIT LOGS
    // ================================================================

    private void viewAuditLogs() {
        while (true) {
            System.out.println("\n== System Audit Logs ==");
            System.out.println(" 1. View all system logs");
            System.out.println(" 2. Filter logs by User ID");
            System.out.println(" 3. Clear all logs");
            System.out.println(" 0. Back");
            int choice = readInt("Choose: ");

            switch (choice) {
                case 1:
                    printLogTable("All System Logs", logDao.getAllLogs());
                    break;
                case 2:
                    filterLogsByUser();
                    break;
                case 3:
                    clearAllLogs();
                    break;
                case 0:
                    return;
                default:
                    System.out.println("  Invalid choice. Please pick 0 to 3.");
            }
        }
    }

    private void filterLogsByUser() {
        int targetId = readInt("Enter User ID to filter (0 to cancel): ");
        if (targetId == 0) {
            System.out.println("Cancelled.");
            return;
        }
        printLogTable("Logs for User ID " + targetId, logDao.getLogsByUserId(targetId));
    }

    private void clearAllLogs() {
        int total = logDao.getAllLogs().size();
        if (total == 0) {
            System.out.println("There are no logs to clear.");
            return;
        }
        System.out.println("WARNING: This permanently deletes " + total + (total == 1 ? " log entry." : " log entries."));
        String confirm = in.ask("Type CLEAR to confirm (anything else cancels): ");
        if (!confirm.equals("CLEAR")) {
            System.out.println("Cancelled. No logs were deleted.");
            return;
        }
        int deleted = logDao.clearLogs(user.id());
        System.out.println(deleted + (deleted == 1 ? " log entry" : " log entries") + " deleted. This clear action was recorded in the log.");
    }

    /** Aligned console table for any list of audit-log entries (long descriptions wrap onto extra lines). */
    private void printLogTable(String title, List<ActivityLog> logs) {
        System.out.println("\n== " + title + " ==");
        if (logs.isEmpty()) {
            System.out.println("No log entries found.");
            return;
        }

        String header = String.format(LOG_ROW, "ID", "User ID", "Action Type", "Description", "Timestamp");
        String divider = "-".repeat(header.length());

        System.out.println(divider);
        System.out.println(header);
        System.out.println(divider);
        for (ActivityLog log : logs) {
            List<String> lines = wrap(log.getDescription(), LOG_DESCRIPTION_WIDTH);
            System.out.println(String.format(LOG_ROW, log.getId(), log.getUserId(),
                    fit(log.getActionType(), 18), lines.get(0), log.getTimestamp()));
            for (int i = 1; i < lines.size(); i++) {
                System.out.println(String.format(LOG_ROW, "", "", "", lines.get(i), ""));
            }
        }
        System.out.println(divider);
        System.out.println("Total: " + logs.size() + (logs.size() == 1 ? " log entry" : " log entries"));
    }

    /** Splits text into lines of at most 'width' characters, breaking at spaces where possible. */
    private List<String> wrap(String text, int width) {
        List<String> lines = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String word : text.trim().split("\\s+")) {
            while (word.length() > width) {              // a single very long word
                if (current.length() > 0) {
                    lines.add(current.toString());
                    current.setLength(0);
                }
                lines.add(word.substring(0, width));
                word = word.substring(width);
            }
            if (current.length() == 0) {
                current.append(word);
            } else if (current.length() + 1 + word.length() <= width) {
                current.append(' ').append(word);
            } else {
                lines.add(current.toString());
                current.setLength(0);
                current.append(word);
            }
        }
        if (current.length() > 0 || lines.isEmpty()) {
            lines.add(current.toString());
        }
        return lines;
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
            int id = readInt("Enter User ID to approve (0 to cancel): ");
            if (id == 0) {
                System.out.println("Approval cancelled.");
                return;
            }

            User target = findInList(pending, id);
            if (target == null) {
                System.out.println("  User ID " + id + " is not in the pending list. Try again or enter 0 to cancel.");
                continue;
            }

            // UserDao writes the USER_APPROVED audit entry
            if (userDao.approveUser(user.id(), id)) {
                System.out.println("SUCCESS: " + target.username() + " (" + target.role()
                        + ") has been APPROVED and can now log in.");
            } else {
                System.out.println("Approval failed. Please try again.");
            }
            return;
        }
    }

    /**
     * Admin feature: change another user's role and status.
     * Enter 0 at any prompt to cancel. The admin cannot edit their own account.
     */
    private void updateUserRoleAndStatus() {
        List<User> users = userDao.getAllUsers();
        printUserTable("All System Users", users);

        User target = null;
        while (target == null) {
            int targetUserId = readInt("Enter User ID to update (0 to cancel): ");
            if (targetUserId == 0) {
                System.out.println("Update cancelled.");
                return;
            }
            if (targetUserId == user.id()) {   // user = the admin who is logged in
                System.out.println("WARNING: Modifying the currently active admin account is restricted.");
                continue;
            }
            target = findInList(users, targetUserId);
            if (target == null) {
                System.out.println("  User ID " + targetUserId + " does not exist. Try again or enter 0 to cancel.");
            }
        }

        System.out.println("Selected: " + target.username() + " (current role: " + target.role()
                + ", current status: " + target.status() + ")");

        String newRole = readValidRole();
        if (newRole == null) {
            System.out.println("Update cancelled.");
            return;
        }

        String newStatus = readValidStatus();
        if (newStatus == null) {
            System.out.println("Update cancelled.");
            return;
        }

        // UserDao writes the USER_UPDATED audit entry
        if (userDao.updateUserRoleAndStatus(user.id(), target.id(), newRole, newStatus)) {
            System.out.println("SUCCESS: " + target.username() + " is now " + newRole + " with status " + newStatus + ".");
            printUserTable("Updated User List", userDao.getAllUsers());
        } else {
            System.out.println("Update failed. Please try again.");
        }
    }

    // ---------------------------------------------------------------
    //  INPUT VALIDATION WRAPPERS (never crash on bad input)
    // ---------------------------------------------------------------

    /** Keeps asking until the user types a whole number (letters / symbols are rejected). */
    private int readInt(String prompt) {
        while (true) {
            String input = in.ask(prompt);
            try {
                return Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("  Invalid input. Please enter numbers only.");
            }
        }
    }

    /** Keeps asking until a valid role is typed. Returns null if the user enters 0 (cancel). */
    private String readValidRole() {
        while (true) {
            String input = in.ask("Enter new role (MANAGER / STAFF / CASHIER, 0 to cancel): ").toUpperCase();
            if (input.equals("0")) {
                return null;
            }
            for (Role r : Role.values()) {
                if (r.name().equals(input)) {
                    return r.name();
                }
            }
            System.out.println("  Invalid role. Please type MANAGER, STAFF or CASHIER.");
        }
    }

    /** Keeps asking until APPROVED or PENDING is typed. Returns null if the user enters 0 (cancel). */
    private String readValidStatus() {
        while (true) {
            String input = in.ask("Enter new status (APPROVED / PENDING, 0 to cancel): ").toUpperCase();
            if (input.equals("0")) {
                return null;
            }
            if (input.equals(UserStatus.APPROVED.name()) || input.equals(UserStatus.PENDING.name())) {
                return input;
            }
            System.out.println("  Invalid status. Please type APPROVED or PENDING.");
        }
    }

    private void toggleAccountStatus() {
        List<User> users = userDao.getAllUsers();
        printUserTable("All System Users", users);

        while (true) {
            int id = readInt("Enter User ID to deactivate / reactivate (0 to cancel): ");
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

            if (userDao.updateStatus(user.id(), id, newStatus)) {
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
    //  PRODUCTS / SUPPLIERS
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
        int id = productDao.insert(user.id(), new Product(0, name, type, price, qty, reorder, supplierId));
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
        productDao.update(user.id(), p);
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
        productDao.delete(user.id(), p.getId());
        System.out.println("Product removed.");
    }

    private void addSupplier() {
        System.out.println("\n== Add Supplier ==");
        String name = in.askRequired("Supplier name: ");
        String contact = in.askRequired("Contact number: ");
        String address = in.askRequired("Address: ");
        int id = supplierDao.insert(user.id(), name, contact, address);
        System.out.println("Supplier added with ID " + id);
    }
}