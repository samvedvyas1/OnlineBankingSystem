package com.bank.model;

import java.math.BigDecimal;

public abstract class Account {
    private int accountId;
    private String accountNumber;
    private BigDecimal balance;
    private String accountType;

    public Account(int accountId, String accountNumber, BigDecimal balance, String accountType) {
        this.accountId = accountId;
        this.accountNumber = accountNumber;
        this.balance = balance;
        this.accountType = accountType;
    }

    // Getters and Setters
    public int getAccountId() { return accountId; }
    public String getAccountNumber() { return accountNumber; }
    public BigDecimal getBalance() { return balance; }
    public String getAccountType() { return accountType; }

    @Override
    public String toString() {
        return String.format("Account Number: %s | Type: %s | Balance: $%.2f",
                accountNumber, accountType, balance);
    }
}