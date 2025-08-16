package com.bank.model;

import java.math.BigDecimal;

public class CurrentAccount extends Account {
    public CurrentAccount(int accountId, String accountNumber, BigDecimal balance) {
        super(accountId, accountNumber, balance, "Current");
    }
}