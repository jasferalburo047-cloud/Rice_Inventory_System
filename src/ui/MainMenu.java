package ui;

import dao.UserDao;
import model.User;

import java.util.Optional;
import java.util.Scanner;

/** Main menu: Register, Login, Exit. */
public class MainMenu {

    private final Scanner scanner = new Scanner(System.in);
    private final UserDao userDao;

    public MainMenu(UserDao userDao) {
        this.userDao = userDao;
    }

    public void run() {
        boolean running = true;
        while (running) {
            System.out.println("\n===== RICE INVENTORY SYSTEM =====");
            System.out.println("1. Register Account");
            System.out.println("2. Login");
            System.out.println("3. Exit");
            System.out.print("Choose: ");

            switch (scanner.nextLine().trim()) {
                case "1" -> register();
                case "2" -> login();
                case "3" -> {
                    System.out.println("Goodbye!");
                    running = false;
                }
                default -> System.out.println("Invalid choice. Please enter 1, 2, or 3.");
            }
        }
    }

    private void register() {
        System.out.println("\n--- Register Account ---");
        System.out.print("Username: ");
        String username = scanner.nextLine().trim();
        if (username.isEmpty()) {
            System.out.println("Username cannot be empty.");
            return;
        }
        if (userDao.usernameExists(username)) {
            System.out.println("That username is already taken.");
            return;
        }

        System.out.print("Password (min 6 characters): ");
        String password = scanner.nextLine();
        if (password.length() < 6) {
            System.out.println("Password is too short.");
            return;
        }

        System.out.println("Select role:");
        System.out.println("1. STAFF");
        System.out.println("2. CASHIER");
        System.out.print("Choose: ");
        String role = switch (scanner.nextLine().trim()) {
            case "1" -> User.ROLE_STAFF;
            case "2" -> User.ROLE_CASHIER;
            default -> null;
        };
        if (role == null) {
            System.out.println("Invalid role. Registration cancelled.");
            return;
        }

        if (userDao.register(username, password, role)) {
            System.out.println("Account created with status PENDING. A manager must approve it before you can log in.");
        } else {
            System.out.println("Registration failed.");
        }
    }

    private void login() {
        System.out.println("\n--- Login ---");
        System.out.print("Username: ");
        String username = scanner.nextLine().trim();
        System.out.print("Password: ");
        String password = scanner.nextLine();

        Optional<User> result = userDao.authenticate(username, password);
        if (result.isEmpty()) {
            System.out.println("Invalid username or password.");
            return;
        }

        User user = result.get();
        if (!user.isActive()) {
            System.out.println("Your account is " + user.status() + ". You cannot log in yet.");
            return;
        }

        new DashBoard(user, userDao, scanner).run();
    }
}