package com.example.evaluacion_javafx_citastecnicas.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class DatabaseConnection {

    private static final String URL  = "jdbc:postgresql://localhost:5432/citasT";
    private static final String USER = "postgres";
    private static final String PASS = "1234";

    private DatabaseConnection() { }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASS);
    }
}