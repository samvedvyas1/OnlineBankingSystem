package com.bank.model;

import java.math.BigDecimal;

public class SavingsAccount extends Account {
    public SavingsAccount(int accountId, String accountNumber, BigDecimal balance) {
        super(accountId, accountNumber, balance, "Savings");
    }
}