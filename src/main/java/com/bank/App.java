package com.bank;

import com.bank.dao.BankDAO;
import com.bank.exception.AccountException;
import com.bank.model.Account;
import com.bank.model.Transaction;
import com.bank.model.User;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.Scanner;

public class App {
    private static final Scanner scanner = new Scanner(System.in);
    private static final BankDAO bankDAO = new BankDAO();
    private static User currentUser = null;

    public static void main(String[] args) {
        System.out.println("Welcome to the Online Banking System! 🏦");
        while (true) {
            if (currentUser == null) {
                showAuthMenu();
            } else {
                showMainMenu();
            }
        }
    }

    private static void showAuthMenu() {
        System.out.println("\n1. Login");
        System.out.println("2. Register");
        System.out.println("3. Exit");
        System.out.print("Choose an option: ");
        int choice = scanner.nextInt();
        scanner.nextLine();

        switch (choice) {
            case 1: login(); break;
            case 2: register(); break;
            case 3: System.exit(0);
            default: System.out.println("Invalid option. Please try again.");
        }
    }

    private static void showMainMenu() {
        System.out.printf("\nWelcome, %s!%n", currentUser.getFirstName());
        System.out.println("1. View Accounts");
        System.out.println("2. Create New Account");
        System.out.println("3. Deposit");
        System.out.println("4. Withdraw");
        System.out.println("5. Transfer Funds");
        System.out.println("6. View Transaction History");
        System.out.println("7. Logout");
        System.out.print("Choose an option: ");
        int choice = scanner.nextInt();
        scanner.nextLine();

        try {
            switch (choice) {
                case 1: viewAccounts(); break;
                case 2: createAccount(); break;
                case 3: performDeposit(); break;
                case 4: performWithdrawal(); break;
                case 5: performTransfer(); break;
                case 6: viewTransactionHistory(); break;
                case 7: currentUser = null; break;
                default: System.out.println("Invalid option.");
            }
        } catch (SQLException | AccountException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    // --- Authentication ---
    private static void login() {
        try {
            System.out.print("Enter username: ");
            String username = scanner.nextLine();
            System.out.print("Enter password: ");
            String password = scanner.nextLine();
            currentUser = bankDAO.login(username, password);
            if (currentUser == null) {
                System.out.println("Login failed. Invalid credentials.");
            }
        } catch (SQLException e) {
            System.err.println("Database error during login.");
        }
    }
    
    private static void register() {
        try {
            System.out.print("Enter new username: ");
            String username = scanner.nextLine();
            System.out.print("Enter password: ");
            String password = scanner.nextLine();
            System.out.print("Enter first name: ");
            String firstName = scanner.nextLine();
            System.out.print("Enter last name: ");
            String lastName = scanner.nextLine();
            
            int newUserId = bankDAO.createUser(username, password, firstName, lastName);
            System.out.printf("User %s created successfully with ID %d! Please login.%n", username, newUserId);
        } catch (SQLException e) {
            System.err.println("Registration failed. Username might already exist.");
        }
    }

    // --- Account Management ---
    private static void viewAccounts() throws SQLException {
        List<Account> accounts = bankDAO.getAccountsByUserId(currentUser.getUserId());
        if (accounts.isEmpty()) {
            System.out.println("You have no accounts yet.");
            return;
        }
        System.out.println("\n--- Your Accounts ---");
        accounts.forEach(System.out::println);
    }
    
    private static void createAccount() throws SQLException {
        System.out.println("Select account type:");
        System.out.println("1. Savings");
        System.out.println("2. Current");
        System.out.print("Choice: ");
        int typeChoice = scanner.nextInt();
        scanner.nextLine();
        
        String accountType = (typeChoice == 1) ? "Savings" : "Current";
        bankDAO.createAccount(currentUser.getUserId(), accountType);
        System.out.println(accountType + " account created successfully!");
    }
    
    // --- Transactions ---
    private static Account selectAccount() throws SQLException {
        List<Account> accounts = bankDAO.getAccountsByUserId(currentUser.getUserId());
        if (accounts.isEmpty()) {
            System.out.println("No accounts found.");
            return null;
        }
        System.out.println("Select an account:");
        for (int i = 0; i < accounts.size(); i++) {
            System.out.printf("%d. %s%n", i + 1, accounts.get(i));
        }
        System.out.print("Choice: ");
        int choice = scanner.nextInt();
        scanner.nextLine();
        return accounts.get(choice - 1);
    }

    private static void performDeposit() throws SQLException, AccountException {
        Account acc = selectAccount();
        if (acc == null) return;
        
        System.out.print("Enter amount to deposit: ");
        BigDecimal amount = scanner.nextBigDecimal();
        scanner.nextLine();
        
        bankDAO.deposit(acc.getAccountId(), amount);
        System.out.printf("Successfully deposited $%.2f into account %s.%n", amount, acc.getAccountNumber());
    }

    private static void performWithdrawal() throws SQLException, AccountException {
        Account acc = selectAccount();
        if (acc == null) return;

        System.out.print("Enter amount to withdraw: ");
        BigDecimal amount = scanner.nextBigDecimal();
        scanner.nextLine();

        bankDAO.withdraw(acc.getAccountId(), amount);
        System.out.printf("Successfully withdrew $%.2f from account %s.%n", amount, acc.getAccountNumber());
    }

    private static void performTransfer() throws SQLException, AccountException {
        System.out.println("Select account to transfer FROM:");
        Account fromAcc = selectAccount();
        if (fromAcc == null) return;

        System.out.println("Select account to transfer TO:");
        
        Account toAcc = selectAccount();
        if (toAcc == null) return;

        System.out.print("Enter amount to transfer: ");
        BigDecimal amount = scanner.nextBigDecimal();
        scanner.nextLine();

        bankDAO.transfer(fromAcc.getAccountId(), toAcc.getAccountId(), amount);
        System.out.printf("Successfully transferred $%.2f from %s to %s.%n", amount, fromAcc.getAccountNumber(), toAcc.getAccountNumber());
    }
    
    private static void viewTransactionHistory() throws SQLException {
        Account acc = selectAccount();
        if (acc == null) return;
        
        List<Transaction> transactions = bankDAO.getTransactionHistory(acc.getAccountId());
        if (transactions.isEmpty()) {
            System.out.println("No transaction history for this account.");
            return;
        }
        
        System.out.println("\n--- Transaction History for " + acc.getAccountNumber() + " ---");
        transactions.forEach(System.out::println);
    }
}