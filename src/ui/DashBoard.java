package ui;

import dao.UserDao;
import model.User;

import java.util.List;
import java.util.Scanner;

/** Screen shown after a successful login. Logout ends the session. */
public class DashBoard {

    private final User currentUser;
    private final UserDao userDao;
    private final Scanner scanner;

    public DashBoard(User currentUser, UserDao userDao, Scanner scanner) {
        this.currentUser = currentUser;
        this.userDao = userDao;
        this.scanner = scanner;
    }

    public void run() {
        System.out.println("\nWelcome, " + currentUser.username() + " (" + currentUser.roles() + ")!");

        boolean loggedIn = true;
        while (loggedIn) {
            System.out.println("\n===== DASHBOARD =====");
            if (currentUser.isManager()) {
                System.out.println("1. Review pending accounts");
            }
            System.out.println("0. Logout");
            System.out.print("Choose: ");
            String choice = scanner.nextLine().trim();

            if (choice.equals("0")) {
                System.out.println("Logged out. Returning to main menu.");
                loggedIn = false;
            } else if (choice.equals("1") && currentUser.isManager()) {
                reviewPendingAccounts();
            } else {
                System.out.println("Invalid choice.");
            }
        }
    }

    private void reviewPendingAccounts() {
        List<User> pending = userDao.findByStatus(User.STATUS_PENDING);
        if (pending.isEmpty()) {
            System.out.println("No pending accounts.");
            return;
        }

        System.out.println("\n--- Pending Accounts ---");
        for (User u : pending) {
            System.out.printf("ID %d | %s | %s%n", u.id(), u.username(), u.roles());
        }

        System.out.print("Enter the ID to review (or press Enter to go back): ");
        String input = scanner.nextLine().trim();
        if (input.isEmpty()) {
            return;
        }

        int id;
        try {
            id = Integer.parseInt(input);
        } catch (NumberFormatException e) {
            System.out.println("Invalid ID.");
            return;
        }
        if (pending.stream().noneMatch(u -> u.id() == id)) {
            System.out.println("That ID is not in the pending list.");
            return;
        }

        System.out.print("Approve (A) or Reject (R)? ");
        String action = scanner.nextLine().trim().toUpperCase();
        if (action.equals("A")) {
            userDao.updateStatus(id, User.STATUS_ACTIVE);
            System.out.println("Account approved.");
        } else if (action.equals("R")) {
            userDao.updateStatus(id, User.STATUS_DEACTIVATED);
            System.out.println("Account rejected.");
        } else {
            System.out.println("No changes made.");
        }
    }
}