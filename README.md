# Bank Transfer System

COSC 32133 / BECS 32263 - Full-Stack Software Development

Assignment 01 - Bank Transactions with
@Transactional, Rollback and @Query.

## Technologies
- Java 21
- Spring Boot 4.0.9
- Spring Data JPA
- MySQL / MariaDB
- Thymeleaf
- HTML and CSS
- Maven

## Features
- Customer registration
- Customer account balances
- Money transfers
- Transaction commit and rollback
- Simulated system failures
- Failed transaction audit records
- Custom JPQL and native SQL queries
- Deposit and withdrawal
- Account transaction history
- Large transfer reports
- Optional 1% transfer fee

## Database
Database: bank_db
Local development port: 3307

## Run
1. Start XAMPP MySQL on port 3307.
2. Stop XAMPP Tomcat if it is using port 8080.
3. Run: mvn spring-boot:run
4. Open: http://localhost:8080

## Important
This is an educational project using test accounts.
It is not intended for real banking or production use.