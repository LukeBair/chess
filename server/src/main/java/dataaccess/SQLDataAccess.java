package dataaccess;

import chess.ChessGame;
import com.google.gson.Gson;
import models.AuthData;
import models.GameData;
import models.UserData;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;
import java.util.List;

public class SQLDataAccess implements DataAccess {

    private static final String DB_PROPERTIES = "db.properties";

    // -----------------------------------------------------------------
    // Load db.properties
    // -----------------------------------------------------------------

    public SQLDataAccess() throws DataAccessException {
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
                    gameID INT PRIMARY KEY,
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

    // -----------------------------------------------------------------
    // CLEAR – delete every row from the three tables
    // -----------------------------------------------------------------
    @Override
    public void clear() throws DataAccessException {
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement()) {

            // Use correct table names matching your CREATE TABLE statements
            stmt.executeUpdate("DELETE FROM auth");
            stmt.executeUpdate("DELETE FROM games");
            stmt.executeUpdate("DELETE FROM users");

        } catch (SQLException e) {
            throw new DataAccessException("Error clearing database: " + e.getMessage());
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

    @Override public int createGame(GameData game) throws DataAccessException {
        String sql = "INSERT INTO games (gameID, whiteUsername, blackUsername, gameName, game) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            Gson gson = new Gson();
            pstmt.setInt(1, game.gameID());
            pstmt.setString(2, game.whiteUsername());
            pstmt.setString(3, game.blackUsername());
            pstmt.setString(4, game.gameName());
            pstmt.setString(5, gson.toJson(game.game()));
            pstmt.executeUpdate();

            return game.gameID();

        } catch (SQLException e) {
            throw new DataAccessException("Error creating game: " + e.getMessage(), e);
        }
    }

    @Override public GameData getGame(int gameID) throws DataAccessException {
        String sql = "SELECT gameID, whiteUsername, blackUsername, gameName, game FROM games WHERE gameID = ?";

        try (Connection conn = DatabaseManager.getConnection()) {
             PreparedStatement pstmt = conn.prepareStatement(sql);

            pstmt.setInt(1, gameID);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                Gson gson = new Gson();
                return new GameData(
                        rs.getInt("gameID"),
                        rs.getString("whiteUsername"),
                        rs.getString("blackUsername"),
                        rs.getString("gameName"),
                        gson.fromJson(rs.getString("game"), ChessGame.class)
                );
            } else {
                return null;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error retrieving game: " + gameID, e);
        }
    }
    @Override public List<GameData> listGames() throws DataAccessException {
        String sql = "SELECT gameID, whiteUsername, blackUsername, gameName, game FROM games";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            Gson gson = new Gson();
            List<GameData> games = new java.util.ArrayList<>();
            while (rs.next()) {
                games.add(new GameData(
                        rs.getInt("gameID"),
                        rs.getString("whiteUsername"),
                        rs.getString("blackUsername"),
                        rs.getString("gameName"),
                        gson.fromJson(rs.getString("game"), ChessGame.class)
                ));
            }
            return games;
        } catch (SQLException e) {
            throw new DataAccessException("Error listing games: " + e.getMessage(), e);
        }
    }

    @Override public void updateGame(GameData game) throws DataAccessException {
        String sql = "UPDATE games SET whiteUsername = ?, blackUsername = ?, gameName = ?, game = ? WHERE gameID = ?";
        try (Connection conn = DatabaseManager.getConnection()) {
            Gson gson = new Gson();
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, game.whiteUsername());
            pstmt.setString(2, game.blackUsername());
            pstmt.setString(3, game.gameName());
            pstmt.setString(4, gson.toJson(game.game()));
            pstmt.setInt(5, game.gameID());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Error updating game: " + e.getMessage(), e);
        }
    }
    @Override public void createAuth(AuthData auth) throws DataAccessException {
        String sql = "INSERT INTO auth (authToken, username) VALUES (?, ?)";
        try (Connection conn = DatabaseManager.getConnection()) {
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, auth.authToken());
            pstmt.setString(2, auth.username());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Error creating auth: " + e.getMessage(), e);
        }
    }
    @Override public AuthData getAuth(String authToken) throws DataAccessException {
        String sql = "SELECT authToken, username FROM auth WHERE authToken = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, authToken);
            ResultSet rs = pstmt.executeQuery();
            return rs.next()
                    ? new AuthData(rs.getString("authToken"),
                    rs.getString("username"))
                    : null;
        } catch (SQLException e) {
            throw new DataAccessException("Error retrieving auth: " + authToken, e);
        }
    }
    @Override public void deleteAuth(String authToken) throws DataAccessException {
        String sql = "DELETE FROM auth WHERE authToken = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, authToken);
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new DataAccessException("Auth token not found: " + authToken);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error deleting auth: " + authToken, e);
        }
    }
}