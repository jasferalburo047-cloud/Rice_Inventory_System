package ui;

import dao.*;
import model.*;

import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class CashierMenu extends RoleMenu {

    public CashierMenu(Scanner scanner, User user, UserDao userDao, ProductDao productDao,
                       SupplierDao supplierDao, StockTransactionDao transactionDao, ActivityLogDao logDao) {
        super(scanner, user, userDao, productDao, supplierDao, transactionDao, logDao);
    }

    @Override
    protected boolean ownTransactionsOnly() { return true; }

    @Override
    protected List<MenuItem> buildMenu() {
        return Arrays.asList(
                new MenuItem("View rice products & prices", this::viewProducts),
                new MenuItem("Search product (name / type)", this::searchProduct),
                new MenuItem("Process sale", this::processSale),
                new MenuItem("View my transactions", this::viewTransactions)
        );
    }

    private void processSale() {
        System.out.println("\n== Process Sale ==");
        viewProducts();
        Product p = productDao.findById(in.askInt("Product ID: "));
        if (p == null) { System.out.println("Product not found."); return; }
        double qty = in.askPositive("Quantity (kg): ", false);
        if (qty > p.getQuantity()) {
            System.out.println("Not enough stock. Available: " + p.getQuantity() + " kg");
            return;
        }

        double total = qty * p.getPrice();
        System.out.printf("Total: %.2f  (%.2f kg x %.2f)%n", total, qty, p.getPrice());
        if (!in.askYes("Confirm sale? (y/n): ")) { System.out.println("Sale cancelled."); return; }

        double pay;
        while (true) {
            pay = in.askPositive("Amount received: ", false);
            if (pay >= total) break;
            System.out.println("  Amount is less than the total.");
        }

        if (!transactionDao.record(p.getId(), user.id(), TransactionType.SALE, qty, total)) {
            System.out.println("Sale failed: not enough stock anymore.");
            return;
        }
        log(String.format("Sale: %.2f kg of %s = %.2f", qty, p.getName(), total));
        System.out.printf("Sale complete. Change: %.2f%n", pay - total);
    }
}