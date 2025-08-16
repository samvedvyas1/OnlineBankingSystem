package com.bank.dao;

import com.bank.config.Database;
import com.bank.exception.AccountException;
import com.bank.model.*;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BankDAO {

    // --- User and Account Creation ---
    public int createUser(String username, String password, String firstName, String lastName) throws SQLException {
        String sql = "INSERT INTO users (username, password, first_name, last_name) VALUES (?, ?, ?, ?)";
        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            pstmt.setString(3, firstName);
            pstmt.setString(4, lastName);
            pstmt.executeUpdate();

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                } else {
                    throw new SQLException("Creating user failed, no ID obtained.");
                }
            }
        }
    }

    public void createAccount(int userId, String accountType) throws SQLException {
        String accountNumber = UUID.randomUUID().toString().substring(0, 12).replace("-", "");
        String sql = "INSERT INTO accounts (user_id, account_number, account_type) VALUES (?, ?, ?)";
        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setString(2, accountNumber);
            pstmt.setString(3, accountType);
            pstmt.executeUpdate();
        }
    }

    // --- Core Banking Operations ---
    public void deposit(int accountId, BigDecimal amount) throws SQLException, AccountException {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new AccountException("Deposit amount must be positive.");
        }
        Connection conn = null;
        try {
            conn = Database.getConnection();
            conn.setAutoCommit(false); // Start transaction

            // Update balance
            String updateSql = "UPDATE accounts SET balance = balance + ? WHERE account_id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(updateSql)) {
                pstmt.setBigDecimal(1, amount);
                pstmt.setInt(2, accountId);
                pstmt.executeUpdate();
            }

            // Log transaction
            logTransaction(conn, accountId, "Deposit", amount, null);

            conn.commit(); // Commit transaction
        } catch (SQLException e) {
            if (conn != null) conn.rollback();
            throw e;
        } finally {
            if (conn != null) conn.close();
        }
    }

    public void withdraw(int accountId, BigDecimal amount) throws SQLException, AccountException {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new AccountException("Withdrawal amount must be positive.");
        }
        Connection conn = null;
        try {
            conn = Database.getConnection();
            conn.setAutoCommit(false);

            // Check for sufficient funds
            BigDecimal currentBalance = getAccountBalance(conn, accountId);
            if (currentBalance.compareTo(amount) < 0) {
                throw new AccountException("Insufficient funds for withdrawal.");
            }

            // Update balance
            String updateSql = "UPDATE accounts SET balance = balance - ? WHERE account_id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(updateSql)) {
                pstmt.setBigDecimal(1, amount);
                pstmt.setInt(2, accountId);
                pstmt.executeUpdate();
            }

            // Log transaction
            logTransaction(conn, accountId, "Withdrawal", amount, null);

            conn.commit();
        } catch (SQLException | AccountException e) {
            if (conn != null) conn.rollback();
            throw e;
        } finally {
            if (conn != null) conn.close();
        }
    }
    
    public void transfer(int fromAccountId, int toAccountId, BigDecimal amount) throws SQLException, AccountException {
        if (fromAccountId == toAccountId) {
            throw new AccountException("Cannot transfer to the same account.");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new AccountException("Transfer amount must be positive.");
        }
        
        Connection conn = null;
        try {
            conn = Database.getConnection();
            conn.setAutoCommit(false);

            // Check sender's balance
            BigDecimal fromBalance = getAccountBalance(conn, fromAccountId);
            if (fromBalance.compareTo(amount) < 0) {
                throw new AccountException("Insufficient funds for transfer.");
            }
            
            // Withdraw from sender
            String withdrawSql = "UPDATE accounts SET balance = balance - ? WHERE account_id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(withdrawSql)) {
                pstmt.setBigDecimal(1, amount);
                pstmt.setInt(2, fromAccountId);
                pstmt.executeUpdate();
            }

            // Deposit to receiver
            String depositSql = "UPDATE accounts SET balance = balance + ? WHERE account_id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(depositSql)) {
                pstmt.setBigDecimal(1, amount);
                pstmt.setInt(2, toAccountId);
                pstmt.executeUpdate();
            }
            
            // Log transactions for both accounts
            logTransaction(conn, fromAccountId, "Transfer", amount, toAccountId);
            logTransaction(conn, toAccountId, "Transfer", amount, fromAccountId);
            
            conn.commit();

        } catch (SQLException | AccountException e) {
            if (conn != null) conn.rollback();
            throw e;
        } finally {
            if (conn != null) conn.close();
        }
    }

    // --- Data Retrieval ---
    public User login(String username, String password) throws SQLException {
        String sql = "SELECT * FROM users WHERE username = ? AND password = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new User(rs.getInt("user_id"), rs.getString("username"), rs.getString("first_name"), rs.getString("last_name"));
                }
            }
        }
        return null; // Login failed
    }

    public List<Account> getAccountsByUserId(int userId) throws SQLException {
        List<Account> accounts = new ArrayList<>();
        String sql = "SELECT * FROM accounts WHERE user_id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    int accountId = rs.getInt("account_id");
                    String accountNumber = rs.getString("account_number");
                    BigDecimal balance = rs.getBigDecimal("balance");
                    String accountType = rs.getString("account_type");

                    if ("Savings".equalsIgnoreCase(accountType)) {
                        accounts.add(new SavingsAccount(accountId, accountNumber, balance));
                    } else {
                        accounts.add(new CurrentAccount(accountId, accountNumber, balance));
                    }
                }
            }
        }
        return accounts;
    }

    public List<Transaction> getTransactionHistory(int accountId) throws SQLException {
        List<Transaction> transactions = new ArrayList<>();
        String sql = "SELECT * FROM transactions WHERE account_id = ? ORDER BY transaction_date DESC";
        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, accountId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    transactions.add(new Transaction(
                        rs.getInt("transaction_id"),
                        rs.getString("transaction_type"),
                        rs.getBigDecimal("amount"),
                        rs.getTimestamp("transaction_date")
                    ));
                }
            }
        }
        return transactions;
    }
    
    // --- Helper Methods ---
    private BigDecimal getAccountBalance(Connection conn, int accountId) throws SQLException {
        String sql = "SELECT balance FROM accounts WHERE account_id = ? FOR UPDATE"; // Lock row for transaction
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, accountId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getBigDecimal("balance");
                }
                throw new SQLException("Account not found.");
            }
        }
    }

    private void logTransaction(Connection conn, int accountId, String type, BigDecimal amount, Integer relatedAccountId) throws SQLException {
        String sql = "INSERT INTO transactions (account_id, transaction_type, amount, related_account_id) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, accountId);
            pstmt.setString(2, type);
            pstmt.setBigDecimal(3, amount);
            if (relatedAccountId != null) {
                pstmt.setInt(4, relatedAccountId);
            } else {
                pstmt.setNull(4, Types.INTEGER);
            }
            pstmt.executeUpdate();
        }
    }
}