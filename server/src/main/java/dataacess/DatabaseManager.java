package dataacess;

import model.UserData;

import java.sql.*;
import java.util.Properties;

public class DatabaseManager {
    private static String databaseName;
    private static String dbUsername;
    private static String dbPassword;
    private static String connectionUrl;

    /*
     * Load the database information for the db.properties file.
     */
    static {
        loadPropertiesFromResources();
        try {
            createDatabase();
        } catch (DataAccessException e) {
            System.err.println("Error in creating database.");
            e.printStackTrace();
        }

        createTables();
    }

    private static void createTables() {
        try (var conn = DatabaseManager.getConnection()) {
            try (var statement = conn.createStatement()) {
                final String userTable = "CREATE TABLE IF NOT EXISTS users (" +
                        "username VARCHAR(255) NOT NULL UNIQUE, " +
                        "password VARCHAR(255) NOT NULL, " +
                        "email VARCHAR(255) NOT NULL" +
                        ")";

                final String authDataTable = "CREATE TABLE IF NOT EXISTS auth_data (" +
                        "username VARCHAR(255) NOT NULL, " +
                        "auth_token VARCHAR(255) NOT NULL UNIQUE, " +
                        ")";

                final String gameDataTable = "CREATE TABLE IF NOT EXISTS game_data (" +
                        "game_id INT NOT NULL AUTO_INCREMENT, " +
                        "white_username VARCHAR(255), " +
                        "black_username VARCHAR(255), " +
                        "game_name VARCHAR(255) NOT NULL, " +
                        "game VARCHAR(255)" +
                        ")";

                statement.executeUpdate(userTable);
                statement.executeUpdate(authDataTable);
                statement.executeUpdate(gameDataTable);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to create users table: " + e.getMessage(), e);
        }
    }

    /**
     * Creates the database if it does not already exist.
     */
    static public void createDatabase() throws DataAccessException {
        var statement = "CREATE DATABASE IF NOT EXISTS " + databaseName;
        try (var conn = DriverManager.getConnection(connectionUrl, dbUsername, dbPassword);
             var preparedStatement = conn.prepareStatement(statement)) {
            preparedStatement.executeUpdate();
        } catch (SQLException ex) {
            throw new DataAccessException("failed to create database", ex);
        }
    }

    /**
     * Create a connection to the database and sets the catalog based upon the
     * properties specified in db.properties. Connections to the database should
     * be short-lived, and you must close the connection when you are done with it.
     * The easiest way to do that is with a try-with-resource block.
     * <br/>
     * <code>
     * try (var conn = DatabaseManager.getConnection()) {
     * // execute SQL statements.
     * }
     * </code>
     */
    static Connection getConnection() throws DataAccessException {
        try {
            //do not wrap the following line with a try-with-resources
            var conn = DriverManager.getConnection(connectionUrl, dbUsername, dbPassword);
            conn.setCatalog(databaseName);
            return conn;
        } catch (SQLException ex) {
            throw new DataAccessException("failed to get connection", ex);
        }
    }

    private static void loadPropertiesFromResources() {
        try (var propStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("db.properties")) {
            if (propStream == null) {
                throw new Exception("Unable to load db.properties");
            }
            Properties props = new Properties();
            props.load(propStream);
            loadProperties(props);
        } catch (Exception ex) {
            throw new RuntimeException("unable to process db.properties", ex);
        }
    }

    private static void loadProperties(Properties props) {
        databaseName = props.getProperty("db.name");
        dbUsername = props.getProperty("db.user");
        dbPassword = props.getProperty("db.password");

        var host = props.getProperty("db.host");
        var port = Integer.parseInt(props.getProperty("db.port"));
        connectionUrl = String.format("jdbc:mysql://%s:%d", host, port);
    }

    public static void addUser(UserData userData) {
        try {
            Connection conn = getConnection();


        } catch (DataAccessException e) {
            System.err.println("Error in creating database.");
            e.printStackTrace();
        }
    }
}
