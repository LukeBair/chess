package dataaccess;

import models.AuthData;
import models.GameData;
import models.UserData;

import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.util.List;
import java.util.Properties;

public class SQLDataAccess implements DataAccess {

    private static final String DB_PROPERTIES = "db.properties";

    // -----------------------------------------------------------------
    // Load db.properties
    // -----------------------------------------------------------------
    private final String dbUrl;
    private final String dbUser;
    private final String dbPassword;

    public SQLDataAccess() throws DataAccessException {
        Properties props = loadProperties();
        this.dbUrl      = props.getProperty("db.url");
        this.dbUser     = props.getProperty("db.user");
        this.dbPassword = props.getProperty("db.password");

        if (dbUser == null || dbPassword == null) {
            throw new DataAccessException("Missing required properties in " + DB_PROPERTIES);
        }

        initTables();
    }

    private void initTables() throws DataAccessException {
        try {
            DatabaseManager.createDatabase();
        } catch (DataAccessException e) {
            throw new RuntimeException("Unable to create database: " + e.getMessage());
        }

        try (var conn = DatabaseManager.getConnection()) {
            String createUserTable = """
                CREATE TABLE IF NOT EXISTS users (
                    username VARCHAR(255) PRIMARY KEY,
                    password VARCHAR(255) NOT NULL,
                    email VARCHAR(255) NOT NULL
                )
                """;

            String createAuthTable = """
                CREATE TABLE IF NOT EXISTS auth (
                    authToken VARCHAR(255) PRIMARY KEY,
                    username VARCHAR(255) NOT NULL
                )
                """;

            String createGameTable = """
                CREATE TABLE IF NOT EXISTS games (
                    gameID INT AUTO_INCREMENT PRIMARY KEY,
                    whiteUsername VARCHAR(255),
                    blackUsername VARCHAR(255),
                    gameName VARCHAR(255) NOT NULL,
                    game TEXT NOT NULL
                )
                """;

            try (var stmt = conn.prepareStatement(createUserTable)) {
                stmt.executeUpdate();
            }
            try (var stmt = conn.prepareStatement(createAuthTable)) {
                stmt.executeUpdate();
            }
            try (var stmt = conn.prepareStatement(createGameTable)) {
                stmt.executeUpdate();
            }

        } catch (SQLException ex) {
            throw new DataAccessException("Unable to configure database: " + ex.getMessage());
        } catch (DataAccessException e) {
            throw new DataAccessException("Unable to access database: " + e.getMessage());
        }

    }

    private Properties loadProperties() throws DataAccessException {
        Properties props = new Properties();
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(DB_PROPERTIES)) {
            if (is == null) {
                throw new DataAccessException("Unable to locate " + DB_PROPERTIES + " on classpath");
            }
            props.load(is);
        } catch (IOException e) {
            throw new DataAccessException("Failed to read " + DB_PROPERTIES + ": " + e.getMessage());
        }
        return props;
    }

    // -----------------------------------------------------------------
    // CLEAR – delete every row from the three tables
    // -----------------------------------------------------------------
    @Override
    public void clear() throws DataAccessException {
        // Order matters when foreign-key constraints exist (auth → user, game → user)
        String[] tables = {"auths", "games", "users"};   // adjust if your table names differ

        String sql = String.join("; ",
                "DELETE FROM auths",
                "DELETE FROM games",
                "DELETE FROM users");

        try (Connection conn = DatabaseManager.getConnection()) {
             Statement stmt = conn.createStatement();

            stmt.executeUpdate(sql);

        } catch (SQLException e) {
            throw new DataAccessException("Error clearing database: " + e.getMessage(), e);
        }
    }

    // -----------------------------------------------------------------
    // Insert User - add a user to the users table
    // -----------------------------------------------------------------
    @Override public void insertUser(UserData user) throws DataAccessException {
        String sql = "INSERT INTO users (username, password, email) VALUES (?, ?, ?)";

        try (Connection conn = DatabaseManager.getConnection()) {
            PreparedStatement pstmt = conn.prepareStatement(sql);

            pstmt.setString(1, user.username());
            pstmt.setString(2, user.password());
            pstmt.setString(3, user.email());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Error inserting user: " + e.getMessage(), e);
        }
    }

    // -----------------------------------------------------------------
    // Get USER - retrieve a user from the users table by username
    // -----------------------------------------------------------------
    @Override
    public UserData getUser(String username) throws DataAccessException {
        String sql = "SELECT username, password, email FROM users WHERE username = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            return rs.next()
                    ? new UserData(rs.getString("username"),
                    rs.getString("password"),
                    rs.getString("email"))
                    : null;
        } catch (SQLException e) {
            throw new DataAccessException("Error retrieving user: " + username, e);
        }
    }

    @Override public void createGame(GameData game) throws DataAccessException { }
    @Override public GameData getGame(int gameID) throws DataAccessException { return null; }
    @Override public List<GameData> listGames() throws DataAccessException { return List.of(); }
    @Override public void updateGame(GameData game) throws DataAccessException { }
    @Override public void createAuth(AuthData auth) throws DataAccessException { }
    @Override public AuthData getAuth(String authToken) throws DataAccessException { return null; }
    @Override public void deleteAuth(String authToken) throws DataAccessException { }
}