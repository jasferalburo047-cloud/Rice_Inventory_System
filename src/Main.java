package com.riceinventory;

import dao.*;
import ui.Authentication;

import java.util.Scanner;

/**
 * Single entry point.
 * Flow: Initialize Database -> Instantiate DAOs & Utilities -> Authentication.start()
 */
public class Main {

    public static void main(String[] args) {
        // 1. Initialize the database (tables are created automatically)
        try {
            new DatabaseInitializer().initialize();
        } catch (DataAccessException e) {
            System.out.println(e.getMessage());
            System.out.println("Make sure the sqlite-jdbc driver is on your classpath.");
            return;
        }

        // 2. Instantiate DAOs & utilities (ONE shared instance of each)
        UserDao userDao = new UserDao();
        ProductDao productDao = new ProductDao();
        SupplierDao supplierDao = new SupplierDao();
        StockTransactionDao transactionDao = new StockTransactionDao();
        ActivityLogDao logDao = new ActivityLogDao();
        Scanner scanner = new Scanner(System.in);

        // 3. Delegate everything else to Authentication
        new Authentication(scanner, userDao, productDao, supplierDao, transactionDao, logDao).start();

        scanner.close();
    }
}