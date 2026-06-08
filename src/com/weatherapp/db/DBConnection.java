package com.weatherapp.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * DBConnection — central helper that hands out JDBC connections to MySQL.
 *
 * WHY a single helper class?
 *   Every class that talks to the database (our DAO) needs a java.sql.Connection.
 *   Instead of repeating the URL / user / password and the driver-loading code
 *   everywhere, we centralise it here (DRY principle). If the database details
 *   change, we edit ONE place.
 *
 * KEY JDBC CONCEPTS (common viva questions):
 *   - JDBC = Java Database Connectivity: the standard Java API for SQL databases.
 *   - Driver = the MySQL-specific library (mysql-connector-j.jar) that implements
 *     the JDBC interfaces for MySQL. It lives in WEB-INF/lib so it is deployed
 *     with the web app and loaded by Tomcat at runtime.
 *   - Connection URL = tells the driver where the DB is and how to behave.
 */
public class DBConnection {

    // ===================================================================
    // CONNECTION SETTINGS — edit to match the local MySQL installation.
    // ===================================================================

    // JDBC URL format: jdbc:mysql://<host>:<port>/<database>?<options>
    //   localhost:3306 -> MySQL server on this machine, default port
    //   weatherdb      -> the database/schema our table lives in
    // Options after '?':
    //   rewriteBatchedStatements=true -> lets the driver send a whole batch of
    //       INSERTs as ONE network round-trip. THIS is what makes the 96k-row
    //       upload finish in seconds instead of minutes (see DAO.batchInsert).
    //   useSSL=false / allowPublicKeyRetrieval=true -> simplifies local auth.
    //   serverTimezone=UTC -> avoids "unknown timezone" errors on some setups.
    private static final String URL =
            "jdbc:mysql://localhost:3306/weatherdb?rewriteBatchedStatements=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final String USER = "root";
    private static final String PASSWORD = "WeatherApp123";
    // ===================================================================

    // Static initialiser block: runs ONCE, the first time the class is touched.
    // Class.forName loads the MySQL driver class so it registers itself with
    // DriverManager. (On JDBC 4.0+ the driver auto-registers via the jar, so
    // this is explicit/belt-and-braces and shows we understand driver loading.)
    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            // If this fires, the connector jar is missing from WEB-INF/lib.
            System.err.println("FATAL: MySQL JDBC driver not found on classpath: " + e.getMessage());
        }
    }

    /** Private constructor: utility class, never instantiated (everything is static). */
    private DBConnection() {
    }

    /**
     * Opens and returns a NEW database connection.
     *
     * Design note: we open a fresh Connection per DAO call and the caller closes
     * it with try-with-resources. Simple and correct. (A production system would
     * use a connection pool to reuse connections — out of scope for this module.)
     *
     * @return a live Connection to weatherdb
     * @throws SQLException if the DB is down or the credentials are wrong
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
