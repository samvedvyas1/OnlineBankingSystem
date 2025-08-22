# Console-Based Online Banking System

A robust and secure command-line banking application built with Java and MySQL. This project simulates the core functionalities of an online banking system, demonstrating strong OOP principles, transactional database management with JDBC, and a clear separation of concerns in its architecture.

---

## ✨ Features

* **User Authentication**: Secure user registration and login system.
* **Account Management**:
    * Create multiple accounts (**Savings** or **Current**) under a single user.
    * View a detailed list of all personal accounts and their balances.
* **Core Banking Operations**:
    * **Deposit**: Add funds to any account.
    * **Withdraw**: Withdraw funds with checks for insufficient balance.
    * **Transfer**: Securely transfer funds between a user's own accounts.
* **Transaction History**: View a complete, chronologically ordered history of all transactions for any selected account.
* **Database Persistence**: All user data, account details, and transactions are securely stored and managed in a MySQL database.
* **Transactional Integrity**: Uses SQL transactions to ensure that financial operations (like transfers) are atomic, preventing data corruption.

---

## 🛠️ Technologies Used

* **Backend**: Java 11
* **Database**: MySQL 8.0
* **Build & Dependency Management**: Apache Maven
* **Database Connectivity**: Java Database Connectivity (JDBC) API
* **Development Environment**: WSL2 (Windows Subsystem for Linux)
* **Version Control**: Git & GitHub
