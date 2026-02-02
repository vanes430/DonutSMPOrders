package github.vanes430.orderplugin;

import java.io.File;
import java.util.logging.Logger;
import net.kyori.adventure.text.Component;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import github.vanes430.orderplugin.command.AdminCommand;
import github.vanes430.orderplugin.command.OrderCommand;
import github.vanes430.orderplugin.gui.GuiManager;
import github.vanes430.orderplugin.gui.MenuListener;
import github.vanes430.orderplugin.manager.DatabaseManager;
import github.vanes430.orderplugin.manager.OrderManager;
import github.vanes430.orderplugin.manager.PendingMessageManager;
import github.vanes430.orderplugin.utils.Utils;

public final class OrderPlugin extends JavaPlugin {
    private static final Logger logger = Logger.getLogger("Minecraft");
    private static OrderPlugin instance;
    private static Economy economy = null;
    private DatabaseManager databaseManager;
    private OrderManager orderManager;
    private GuiManager guiManager;
    private PendingMessageManager pendingMessageManager;
    private FileConfiguration messagesConfig;

    public void onEnable() {
        instance = this;
        this.saveDefaultConfig();
        if (!new File(this.getDataFolder(), "messages.yml").exists()) {
            this.saveResource("messages.yml", false);
        }

        this.loadMessagesConfig();
        if (!this.setupEconomy()) {
            logger.severe(String.format("[%s] - Disabled due to missing dependencies (Vault/Economy)!", this.getDescription().getName()));
            this.getServer().getPluginManager().disablePlugin(this);
        } else {
            this.databaseManager = new DatabaseManager(this);
            this.orderManager = new OrderManager(this);
            this.guiManager = new GuiManager(this);
            this.pendingMessageManager = new PendingMessageManager(this);
            
            this.orderManager.loadOrders();

            this.getCommand("order").setExecutor(new OrderCommand(this));
            AdminCommand adminCmd = new AdminCommand(this);
            this.getCommand("donutordersadmin").setExecutor(adminCmd);
            this.getCommand("donutordersadmin").setTabCompleter(adminCmd);

            this.getServer().getPluginManager().registerEvents(new MenuListener(this), this);
            this.getServer().getPluginManager().registerEvents(this.pendingMessageManager, this);

            logger.info(String.format("[%s] Activated Version %s (H2 Mode)", this.getDescription().getName(), this.getDescription().getVersion()));
        }
    }

    public void onDisable() {
        if (this.orderManager != null) {
            // Synchronous save on disable to ensure data integrity
            this.orderManager.saveOrdersSync();
        }
        if (this.databaseManager != null) {
            this.databaseManager.close();
        }

        logger.info(String.format("[%s] Disabled.", this.getDescription().getName()));
    }

    private boolean setupEconomy() {
        if (this.getServer().getPluginManager().getPlugin("Vault") == null) {
            logger.severe("Vault not found!");
            return false;
        } else {
            RegisteredServiceProvider<Economy> rsp = this.getServer().getServicesManager().getRegistration(Economy.class);
            if (rsp == null) {
                logger.severe("No economy plugin found (e.g., Essentials, CMI)!");
                return false;
            } else {
                economy = rsp.getProvider();
                return economy != null;
            }
        }
    }

    public static OrderPlugin getInstance() {
        return instance;
    }

    public static Economy getEconomy() {
        return economy;
    }

    public DatabaseManager getDatabaseManager() {
        return this.databaseManager;
    }

    public OrderManager getOrderManager() {
        return this.orderManager;
    }

    public GuiManager getGuiManager() {
        return this.guiManager;
    }

    public PendingMessageManager getPendingMessageManager() {
        return this.pendingMessageManager;
    }

    public FileConfiguration getMessagesConfig() {
        return this.messagesConfig;
    }

    public void loadMessagesConfig() {
        File file = new File(this.getDataFolder(), "messages.yml");
        if (!file.exists()) {
            logger.warning("messages.yml not found, creating default.");
            this.saveResource("messages.yml", false);
        }

        this.messagesConfig = YamlConfiguration.loadConfiguration(file);
        logger.info("Loaded messages.yml. Total keys: " + this.messagesConfig.getKeys(true).size());
    }

    public void reloadAllConfigs() {
        logger.info("Reloading all configs...");
        this.reloadConfig();
        this.loadMessagesConfig();
        logger.info("Configs reloaded.");
    }

    public Component getMessage(String key, String def) {
        if (this.messagesConfig == null) {
            this.loadMessagesConfig();
        }

        String msg = this.messagesConfig.getString(key);
        if (msg == null) {
            msg = def;
        }

        String prefix = this.getConfig().getString("prefix", "");
        msg = msg.replace("{prefix}", prefix);
        return Utils.color(msg);
    }

    public Component getMessage(String key, String def, String... placeholders) {
        if (this.messagesConfig == null) {
            this.loadMessagesConfig();
        }
        
        String msg = this.messagesConfig.getString(key);
        if (msg == null) {
            msg = def;
        }

        String prefix = this.getConfig().getString("prefix", "");
        msg = msg.replace("{prefix}", prefix);

        for (int i = 0; i < placeholders.length - 1; i += 2) {
            msg = msg.replace(placeholders[i], placeholders[i + 1]);
        }

        return Utils.color(msg);
    }
}