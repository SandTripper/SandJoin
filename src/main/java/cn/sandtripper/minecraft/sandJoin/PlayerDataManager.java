package cn.sandtripper.minecraft.sandJoin;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class PlayerDataManager {

    private final SandJoin plugin;
    private final Path dbPath;
    private HikariDataSource dataSource;

    public PlayerDataManager(SandJoin plugin, Path dataDirectory) {
        this.plugin = plugin;
        this.dbPath = dataDirectory.resolve("players.db");

        ensureDatabaseFileExists();

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:sqlite:" + dbPath);
        config.setDriverClassName("org.sqlite.JDBC");
        config.setConnectionTestQuery("SELECT 1");
        config.setMaximumPoolSize(1);

        dataSource = new HikariDataSource(config);

        initializeDatabase();
    }

    private void ensureDatabaseFileExists() {
        File dbFile = dbPath.toFile();
        if (!dbFile.exists()) {
            try {
                Files.createDirectories(dbFile.getParentFile().toPath());
                Files.createFile(dbPath);
                plugin.getLogger().info("Created new database file: " + dbPath);
            } catch (IOException e) {
                plugin.getLogger().error("Error creating database file", e);
            }
        }
    }

    private void initializeDatabase() {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "CREATE TABLE IF NOT EXISTS players (uuid TEXT PRIMARY KEY)")) {
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().error("Error initializing database", e);
        }
    }

    public CompletableFuture<Boolean> isNewPlayer(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement("SELECT 1 FROM players WHERE uuid = ?")) {
                stmt.setString(1, uuid.toString());
                try (ResultSet rs = stmt.executeQuery()) {
                    return !rs.next();
                }
            } catch (SQLException e) {
                plugin.getLogger().error("Error checking if player is new", e);
                return false;
            }
        });
    }

    public void addPlayer(UUID uuid) {
        CompletableFuture.runAsync(() -> {
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement("INSERT OR IGNORE INTO players (uuid) VALUES (?)")) {
                stmt.setString(1, uuid.toString());
                stmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().error("Error adding player to database", e);
            }
        });
    }

    public void reloadDatabase() {
        // Close the existing data source
        if (dataSource != null) {
            dataSource.close();
        }

        // Ensure the database file exists
        ensureDatabaseFileExists();

        // Reinitialize the database connection
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:sqlite:" + dbPath);
        config.setConnectionTestQuery("SELECT 1");
        config.setMaximumPoolSize(1);

        dataSource = new HikariDataSource(config);

        initializeDatabase();
    }
}
