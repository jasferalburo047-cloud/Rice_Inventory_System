package ui;

import dao.ActivityLogDao;
import dao.DataAccessException;
import dao.ProductDao;
import dao.StockTransactionDao;
import dao.SupplierDao;
import dao.UserDao;
import model.Role;
import model.User;
import util.ConsoleInput;

import java.util.Scanner;

/**
 * Handles login, registration and role-based routing.
 * Receives the single shared Scanner and UserDao (plus the other DAOs for the role menus).
 * NOTE: no SQL in this class - only DAO calls.
 */
public class Authentication {

    private final Scanner scanner;
    private final ConsoleInput in;
    private final UserDao userDao;
    private final ProductDao productDao;
    private final SupplierDao supplierDao;
    private final StockTransactionDao transactionDao;
    private final ActivityLogDao logDao;

    public Authentication(Scanner scanner, UserDao userDao, ProductDao productDao,
                          SupplierDao supplierDao, StockTransactionDao transactionDao, ActivityLogDao logDao) {
        this.scanner = scanner;
        this.in = new ConsoleInput(scanner);
        this.userDao = userDao;
        this.productDao = productDao;
        this.supplierDao = supplierDao;
        this.transactionDao = transactionDao;
        this.logDao = logDao;
    }

    public void start() {
        while (true) {
            System.out.println("\n=========== RICE INVENTORY SYSTEM ===========");
            System.out.println("1. Login");
            System.out.println("2. Register");
            System.out.println("0. Exit");
            int choice = in.askInt("Choose: ");

            try {
                switch (choice) {
                    case 1:
                        login();
                        break;
                    case 2:
                        register();
                        break;
                    case 0:
                        System.out.println("Goodbye!");
                        return;
                    default:
                        System.out.println("Invalid choice.");
                }
            } catch (DataAccessException e) {
                System.out.println("Database error: " + e.getMessage());
            }
        }
    }

    // ---------------------------------------------------------------- login
    private void login() {
        System.out.println("\n== Login ==");
        for (int attempt = 1; attempt <= 3; attempt++) {
            String username = in.ask("Username: ");
            String pw = in.ask("Password: ");

            User user = null;
            if (pw.matches("\\d{1,9}")) {
                user = userDao.login(username, Integer.parseInt(pw));
            }

            if (user == null) {
                System.out.println("Invalid username or password. Attempts left: " + (3 - attempt));
                continue;
            }

            switch (user.status()) {
                case PENDING:
                    System.out.println("Your account is still PENDING approval by the admin.");
                    return;
                case INACTIVE:
                    System.out.println("Your account is deactivated. Contact the admin.");
                    return;
                default:
                    logDao.log(user.id(), "Logged in");
                    routeToMenu(user);
                    return;
            }
        }
    }

    /** Routes the logged-in user to the menu of their role. */
    private void routeToMenu(User user) {
        RoleMenu menu;
        switch (user.role()) {
            case MANAGER:
                menu = new AdminMenu(scanner, user, userDao, productDao, supplierDao, transactionDao, logDao);
                break;
            case STAFF:
                menu = new StaffMenu(scanner, user, userDao, productDao, supplierDao, transactionDao, logDao);
                break;
            default:
                menu = new CashierMenu(scanner, user, userDao, productDao, supplierDao, transactionDao, logDao);
                break;
        }
        menu.start();
    }

    // ------------------------------------------------------------- register
    private void register() {
        System.out.println("\n== Register New Account ==");
        String username;
        while (true) {
            username = in.askRequired("Username: ");
            if (userDao.usernameExists(username)) {
                System.out.println("  Username already exists.");
            } else {
                break;
            }
        }

        int password;
        while (true) {
            password = in.askPin("Password (4-9 digits, numbers only): ");
            int confirm = in.askPin("Confirm password: ");
            if (password == confirm) {
                break;
            }
            System.out.println("  Passwords do not match.");
        }

        String fullName = in.askRequired("Full name: ");

        System.out.println("Select role:");
        System.out.println("  1) Manager (Admin)");
        System.out.println("  2) Staff");
        System.out.println("  3) Cashier");
        int r;
        do {
            r = in.askInt("Role: ");
        } while (r < 1 || r > 3);
        Role role = (r == 1) ? Role.MANAGER : (r == 2) ? Role.STAFF : Role.CASHIER;

        userDao.register(username, password, fullName, role);
        System.out.println("Account created as " + role + " with status PENDING.");
        System.out.println("An admin must approve it before you can log in.");
    }
}