package github.vanes430.orderplugin.manager;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.configuration.ConfigurationSection;
import github.vanes430.orderplugin.OrderPlugin;

import java.io.File;
import java.sql.Connection;
import java.util.logging.Level;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {
    private final OrderPlugin plugin;
    private HikariDataSource dataSource;

    public DatabaseManager(OrderPlugin plugin) {
        this.plugin = plugin;
        this.init();
    }

    private void init() {
        ConfigurationSection storageSection = plugin.getConfig().getConfigurationSection("storage");
        String type = storageSection != null ? storageSection.getString("type", "H2").toUpperCase() : "H2";
        
        HikariConfig config = new HikariConfig();
        
        if (type.equals("MYSQL")) {
            ConfigurationSection mysql = storageSection.getConfigurationSection("mysql");
            if (mysql == null) {
                plugin.getLogger().severe("MySQL configuration is missing! Falling back to H2.");
                setupH2(config);
            } else {
                String host = mysql.getString("host");
                int port = mysql.getInt("port");
                String database = mysql.getString("database");
                String username = mysql.getString("username");
                String password = mysql.getString("password");
                boolean ssl = mysql.getBoolean("ssl");

                config.setJdbcUrl(String.format("jdbc:mysql://%s:%d/%s?useSSL=%b", host, port, database, ssl));
                config.setUsername(username);
                config.setPassword(password);
                config.setDriverClassName("com.mysql.cj.jdbc.Driver");
            }
        } else {
            setupH2(config);
        }

        config.setMaximumPoolSize(10);
        config.setConnectionTimeout(30000);
        config.setPoolName("DonutOrdersPool");
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        this.dataSource = new HikariDataSource(config);
        this.createTables();
    }

    private void setupH2(HikariConfig config) {
        File databaseFile = new File(plugin.getDataFolder(), "database");
        config.setJdbcUrl("jdbc:h2:" + databaseFile.getAbsolutePath() + ";MODE=MySQL");
        config.setDriverClassName("org.h2.Driver");
    }

    private void createTables() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            
            // Orders Table
            stmt.execute("CREATE TABLE IF NOT EXISTS orders (" +
                    "order_id UUID PRIMARY KEY, " +
                    "owner_id UUID NOT NULL, " +
                    "material VARCHAR(100) NOT NULL, " +
                    "needed_amount INT NOT NULL, " +
                    "filled_amount INT NOT NULL, " +
                    "price_per_item DOUBLE NOT NULL, " +
                    "created_at BIGINT NOT NULL, " +
                    "expires_at BIGINT NOT NULL, " +
                    "removed_by_admin BOOLEAN DEFAULT FALSE, " +
                    "potion_type VARCHAR(100), " +
                    "enchantment_type TEXT, " +
                    "inventory_data LONGTEXT" + // Changed from BLOB to LONGTEXT for easier Base64 handling across DBs
                    ")");

            // Pending Messages Table
            stmt.execute("CREATE TABLE IF NOT EXISTS pending_messages (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "player_id UUID NOT NULL, " +
                    "message TEXT NOT NULL" +
                    ")");

        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not create database tables", e);
        }
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }
}