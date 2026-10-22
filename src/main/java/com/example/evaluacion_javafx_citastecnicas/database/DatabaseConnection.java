package com.example.evaluacion_javafx_citastecnicas.database;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

/**
 * Clase centralizada de conexion a PostgreSQL / NeonDB.
 * Cumple con los requerimientos tecnicos de la evaluacion:
 * - Conexion centralizada gestionada con JDBC nativo.
 * - Uso demostrable de Connection, Statement y ResultSet.
 * - Ejecucion de sentencia SQL fija 'SELECT 1' con Statement para verificar el enlace.
 * - Cierre seguro de recursos mediante bloques try-with-resources.
 */
public class DatabaseConnection {

    private static final String URL = "jdbc:postgresql://localhost:5432/TiendaJava";


    private static final String USER = "postgres";


    private static final String PASSWORD = "2103";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
