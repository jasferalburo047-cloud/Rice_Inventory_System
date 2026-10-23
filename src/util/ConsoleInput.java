package util;

import java.util.Scanner;

/** Input helpers built on top of the ONE shared Scanner. */
public class ConsoleInput {

    private final Scanner scanner;

    public ConsoleInput(Scanner scanner) {
        this.scanner = scanner;
    }

    public String ask(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }

    public String askRequired(String prompt) {
        String s;
        do {
            s = ask(prompt);
            if (s.isEmpty()) System.out.println("  This field is required.");
        } while (s.isEmpty());
        return s;
    }

    public int askInt(String prompt) {
        while (true) {
            try {
                return Integer.parseInt(ask(prompt));
            } catch (NumberFormatException e) {
                System.out.println("  Please enter a whole number.");
            }
        }
    }

    public double askPositive(String prompt, boolean allowZero) {
        while (true) {
            try {
                double d = Double.parseDouble(ask(prompt));
                if (d > 0 || (allowZero && d == 0)) return d;
                System.out.println("  Value must be " + (allowZero ? "0 or more." : "greater than 0."));
            } catch (NumberFormatException e) {
                System.out.println("  Please enter a valid number.");
            }
        }
    }

    /** Numeric password (4 to 9 digits) stored as an integer. */
    public int askPin(String prompt) {
        while (true) {
            String s = ask(prompt);
            if (s.matches("\\d{4,9}")) return Integer.parseInt(s);
            System.out.println("  Password must be 4 to 9 digits (numbers only).");
        }
    }

    public boolean askYes(String prompt) {
        return ask(prompt).equalsIgnoreCase("y");
    }
}