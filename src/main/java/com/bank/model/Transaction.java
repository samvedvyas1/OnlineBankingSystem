package com.bank.model;

import java.math.BigDecimal;
import java.sql.Timestamp;

public class Transaction {
    private int transactionId;
    private String transactionType;
    private BigDecimal amount;
    private Timestamp transactionDate;

    public Transaction(int transactionId, String transactionType, BigDecimal amount, Timestamp transactionDate) {
        this.transactionId = transactionId;
        this.transactionType = transactionType;
        this.amount = amount;
        this.transactionDate = transactionDate;
    }

    @Override
    public String toString() {
        return String.format("Date: %s | Type: %-10s | Amount: $%.2f",
                transactionDate, transactionType, amount);
    }
}