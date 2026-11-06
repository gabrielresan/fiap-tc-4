package com.example.weeklyreport.repository;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Objects;

public class PasswordConnectionProvider implements ConnectionProvider {

    private final String jdbcUrl;
    private final String user;
    private final String password;

    public PasswordConnectionProvider(String jdbcUrl, String user, String password) {
        this.jdbcUrl = Objects.requireNonNull(jdbcUrl, "jdbcUrl must not be null");
        this.user = Objects.requireNonNull(user, "user must not be null");
        this.password = Objects.requireNonNull(password, "password must not be null");
    }

    @Override
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(jdbcUrl, user, password);
    }
}