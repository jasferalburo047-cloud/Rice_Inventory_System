package ui;

import dao.*;
import model.*;

import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class StaffMenu extends RoleMenu {

    public StaffMenu(Scanner scanner, User user, UserDao userDao, ProductDao productDao,
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
                new MenuItem("Record stock-in", () -> recordStock(TransactionType.STOCK_IN)),
                new MenuItem("Record stock-out", () -> recordStock(TransactionType.STOCK_OUT)),
                new MenuItem("Update stock quantity", this::adjustQuantity),
                new MenuItem("View transaction history", this::viewTransactions),
                new MenuItem("Low-Stock Alert Report", this::lowStockReport),
                new MenuItem("View suppliers", this::viewSuppliers)
        );
    }
}