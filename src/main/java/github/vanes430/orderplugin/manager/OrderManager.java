package github.vanes430.orderplugin.manager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;
import github.vanes430.orderplugin.OrderPlugin;
import github.vanes430.orderplugin.model.Order;
import github.vanes430.orderplugin.utils.Utils;

public class OrderManager {
    private final OrderPlugin plugin;
    private final List<Order> orders = new CopyOnWriteArrayList<>();

    public OrderManager(OrderPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadOrders() {
        Bukkit.getAsyncScheduler().runNow(this.plugin, (task) -> {
            List<Order> loadedOrders = new ArrayList<>();
            try (Connection conn = plugin.getDatabaseManager().getConnection();
                 PreparedStatement ps = conn.prepareStatement("SELECT * FROM orders")) {
                
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    UUID orderId = (UUID) rs.getObject("order_id");
                    UUID ownerId = (UUID) rs.getObject("owner_id");
                    Material material = Material.valueOf(rs.getString("material"));
                    int needed = rs.getInt("needed_amount");
                    int filled = rs.getInt("filled_amount");
                    double price = rs.getDouble("price_per_item");
                    long created = rs.getLong("created_at");
                    long expiry = rs.getLong("expires_at");
                    
                    Order order = new Order(orderId, ownerId, material, needed, filled, price, created, expiry);
                    order.setRemovedByAdmin(rs.getBoolean("removed_by_admin"));
                    order.setPotionType(rs.getString("potion_type"));
                    order.setEnchantmentType(rs.getString("enchantment_type"));
                    
                    String invData = rs.getString("inventory_data");
                    if (invData != null) {
                        order.deserializeInventory(invData);
                    }
                    
                    loadedOrders.add(order);
                }
                
                this.orders.clear();
                this.orders.addAll(loadedOrders);
                this.plugin.getLogger().info("Loaded " + loadedOrders.size() + " orders from database.");
            } catch (SQLException e) {
                this.plugin.getLogger().log(Level.SEVERE, "Could not load orders from database", e);
            }
        });
    }

    public void saveOrders() {
        if (this.orders.isEmpty()) return;
        List<Order> snapshot = new ArrayList<>(this.orders);
        Bukkit.getAsyncScheduler().runNow(this.plugin, (task) -> {
            this.executeSave(snapshot);
        });
    }

    public void saveOrdersSync() {
        if (this.orders.isEmpty()) return;
        this.executeSave(new ArrayList<>(this.orders));
    }

    private void executeSave(List<Order> snapshot) {
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            // Clear and re-insert for simplicity in this version, or use UPSERT
            // For H2 we can use MERGE INTO
            String sql = "MERGE INTO orders (order_id, owner_id, material, needed_amount, filled_amount, " +
                    "price_per_item, created_at, expires_at, removed_by_admin, potion_type, enchantment_type, inventory_data) " +
                    "KEY(order_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (Order order : snapshot) {
                    ps.setObject(1, order.getOrderId());
                    ps.setObject(2, order.getOwnerId());
                    ps.setString(3, order.getMaterial().name());
                    ps.setInt(4, order.getNeededAmount());
                    ps.setInt(5, order.getFilledAmount());
                    ps.setDouble(6, order.getPricePerItem());
                    ps.setLong(7, order.getCreatedAt());
                    ps.setLong(8, order.getExpiresAt());
                    ps.setBoolean(9, order.isRemovedByAdmin());
                    ps.setString(10, order.getPotionType());
                    ps.setString(11, order.getEnchantmentType());
                    ps.setString(12, order.serializeInventory());
                    ps.addBatch();
                }
                ps.executeBatch();
            }
        } catch (SQLException e) {
            this.plugin.getLogger().log(Level.SEVERE, "Could not save orders to database", e);
        }
    }

    public boolean createOrder(Player player, Material material, int amount, double price, String potionType, String enchantmentType) {
        int maxActive = this.getPlayerMaxOrders(player);
        long currentActive = this.orders.stream()
                .filter(o -> o.getOwnerId().equals(player.getUniqueId()) && !o.isCompleted())
                .count();
        
        if (currentActive >= maxActive) {
            player.sendMessage(this.plugin.getMessage("errors.max-orders-reached", "{prefix}&cYou have reached the maximum number of active orders (&e%limit%&c).", "%limit%", String.valueOf(maxActive)));
            return false;
        }

        double totalCost = amount * price;
        double taxPercent = this.plugin.getConfig().getDouble("orders.creation-tax-percent", 0.0);
        double tax = totalCost * taxPercent;
        double finalCost = totalCost + tax;

        if (OrderPlugin.getEconomy().getBalance(player) < finalCost) {
            player.sendMessage(
                    this.plugin.getMessage("errors.not-enough-money", "{prefix}&cNot enough money! Need &e%total%&c.", "%total%", String.format("%,.2f", finalCost))
            );
            return false;
        } else {
            OrderPlugin.getEconomy().withdrawPlayer(player, finalCost);
            Order order = new Order(player.getUniqueId(), material, amount, price, potionType, enchantmentType);
            this.orders.add(order);
            this.saveOrders();
            
            String itemName = Utils.formatMaterialName(material.name());
            if (potionType != null) {
                itemName = Utils.formatPotionName(potionType) + " Potion";
            }

            String amountStr = Utils.formatNumber((double) amount);
            Component message = this.plugin.getMessage("success.order-created", "&fYou ordered &#00d271%amount% %item%", "%amount%", amountStr, "%item%", itemName);
            player.sendMessage(message);
            player.sendActionBar(message);
            
            if (tax > 0) {
                Component taxMsg = Utils.color("{prefix}&7(A creation tax of &e$" + String.format("%,.2f", tax) + " &7was applied)").replaceText(config -> config.matchLiteral("{prefix}").replacement(this.plugin.getMessage("prefix", "")));
                player.sendMessage(taxMsg);
            }
            
            return true;
        }
    }

    public boolean cancelOrder(Player player, Order order) {
        if (!order.getOwnerId().equals(player.getUniqueId())) {
            player.sendMessage(this.plugin.getMessage("errors.not-owner", "{prefix}&cNot your order."));
            return false;
        } else if (order.isCompleted()) {
            player.sendMessage(this.plugin.getMessage("errors.already-complete", "{prefix}&cCannot cancel completed order."));
            return false;
        } else {
            double refund = order.getRemainingAmount() * order.getPricePerItem();
            OrderPlugin.getEconomy().depositPlayer(player, refund);

            for (ItemStack item : order.getAllCollectedItems()) {
                if (item != null && item.getType() != Material.AIR) {
                    player.getInventory().addItem(new ItemStack[]{item});
                }
            }

            this.orders.remove(order);
            // Delete from DB
            Bukkit.getAsyncScheduler().runNow(this.plugin, (task) -> {
                try (Connection conn = plugin.getDatabaseManager().getConnection();
                     PreparedStatement ps = conn.prepareStatement("DELETE FROM orders WHERE order_id = ?")) {
                    ps.setObject(1, order.getOrderId());
                    ps.executeUpdate();
                } catch (SQLException e) {
                    this.plugin.getLogger().log(Level.SEVERE, "Could not delete order from H2", e);
                }
            });
            player.sendMessage(this.plugin.getMessage("success.order-cancelled", "{prefix}&aCancelled. Refunded &e%price%", "%price%", String.format("%,.2f", refund)));
            return true;
        }
    }

    public void fulfillOrder(Player seller, Order order, int amount, double payment, List<ItemStack> items) {
        String itemName = Utils.formatMaterialName(order.getMaterial().name());
        OrderPlugin.getEconomy().depositPlayer(seller, payment);
        order.setFilledAmount(order.getFilledAmount() + amount);
        
        if (items != null && !items.isEmpty()) {
            for (ItemStack item : items) {
                if (item != null) {
                    order.addItem(item.clone());
                }
            }
        } else {
            int remaining = amount;
            Material material = order.getMaterial();

            while (remaining > 0) {
                int stackSize = Math.min(remaining, material.getMaxStackSize());
                ItemStack stack = new ItemStack(material, stackSize);
                if (order.isPotion() && order.getPotionType() != null) {
                    try {
                        PotionMeta meta = (PotionMeta) stack.getItemMeta();
                        PotionType type = PotionType.valueOf(order.getPotionType());
                        meta.setBasePotionType(type);
                        stack.setItemMeta(meta);
                    } catch (Exception e) {
                    }
                }

                if (order.isEnchantedBook() && order.getEnchantmentType() != null) {
                    try {
                        EnchantmentStorageMeta meta = (EnchantmentStorageMeta) stack.getItemMeta();
                        String[] parts = order.getEnchantmentType().split("_");
                        if (parts.length >= 2) {
                            String enchName = parts[0];
                            for (int i = 1; i < parts.length - 1; i++) {
                                enchName = enchName + "_" + parts[i];
                            }
                            int level = Integer.parseInt(parts[parts.length - 1]);
                            Enchantment enchantment = Enchantment.getByName(enchName);
                            if (enchantment != null) {
                                meta.addStoredEnchant(enchantment, level, true);
                            }
                        }
                        stack.setItemMeta(meta);
                    } catch (Exception e) {
                    }
                } else if (order.getEnchantmentType() != null && !order.getEnchantmentType().isEmpty()) {
                    try {
                        ItemMeta meta = stack.getItemMeta();
                        String[] enchs = order.getEnchantmentType().split(";");
                        for (String enchStr : enchs) {
                            String[] parts = enchStr.split("_");
                            if (parts.length >= 2) {
                                String enchName = parts[0];
                                for (int i = 1; i < parts.length - 1; i++) {
                                    enchName = enchName + "_" + parts[i];
                                }
                                int level = Integer.parseInt(parts[parts.length - 1]);
                                Enchantment enchantment = (Enchantment) Registry.ENCHANTMENT.get(NamespacedKey.minecraft(enchName.toLowerCase()));
                                if (enchantment != null) {
                                    meta.addEnchant(enchantment, level, true);
                                }
                            }
                        }
                        stack.setItemMeta(meta);
                    } catch (Exception e) {
                    }
                }
                order.addItem(stack);
                remaining -= stackSize;
            }
        }

        this.saveOrders();
        
        Component actionMessage = this.plugin.getMessage("success.order-fulfilled", 
                "&fYou delivered &a%amount% %item% &fand received &a%price%",
                "%amount%", Utils.formatNumber((double) amount),
                "%item%", itemName,
                "%price%", Utils.formatNumber(payment));
        seller.sendActionBar(actionMessage);
        
        Player owner = Bukkit.getPlayer(order.getOwnerId());
        if (owner != null && owner.isOnline()) {
            owner.getScheduler().run(this.plugin, (task) -> {
                owner.sendMessage(
                        this.plugin.getMessage(
                                "success.order-received",
                                "&a%seller% &fdelivered you &a%amount% %item%!",
                                "%seller%", seller.getName(),
                                "%amount%", Utils.formatNumber((double) amount),
                                "%item%", itemName
                        )
                );
                if (order.isCompleted()) {
                    owner.sendMessage(this.plugin.getMessage("success.order-complete", "&fYour &a%item% &forder is complete!", "%item%", itemName));
                }
            }, null);
        }
    }

    public void removeOrder(Order order) {
        this.orders.remove(order);
        Bukkit.getAsyncScheduler().runNow(this.plugin, (task) -> {
            try (Connection conn = plugin.getDatabaseManager().getConnection();
                 PreparedStatement ps = conn.prepareStatement("DELETE FROM orders WHERE order_id = ?")) {
                ps.setObject(1, order.getOrderId());
                ps.executeUpdate();
            } catch (SQLException e) {
                this.plugin.getLogger().log(Level.SEVERE, "Could not delete order from H2", e);
            }
        });
    }

    public int clearAllOrders() {
        int count = this.orders.size();
        this.orders.clear();
        Bukkit.getAsyncScheduler().runNow(this.plugin, (task) -> {
            try (Connection conn = plugin.getDatabaseManager().getConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.execute("DELETE FROM orders");
            } catch (SQLException e) {
                this.plugin.getLogger().log(Level.SEVERE, "Could not clear orders from H2", e);
            }
        });
        return count;
    }

    public List<Order> getActiveOrders() {
        long now = System.currentTimeMillis();
        return this.orders.stream().filter(o -> o.getExpiresAt() > now).collect(Collectors.toList());
    }

    public List<Order> getOrdersByOwner(UUID ownerId) {
        return this.orders.stream().filter(o -> o.getOwnerId().equals(ownerId)).collect(Collectors.toList());
    }

    public List<Order> getOrdersByMaterial(Material material) {
        return this.orders.stream().filter(o -> o.getMaterial().equals(material)).collect(Collectors.toList());
    }

    public Order getOrderById(UUID orderId) {
        return this.orders.stream().filter(o -> o.getOrderId().equals(orderId)).findFirst().orElse(null);
    }

    private int getPlayerMaxOrders(Player player) {
        int limit = this.plugin.getConfig().getInt("orders.max-active-per-player", 10);
        
        // Check for donutorders.limit.<number> permissions
        for (org.bukkit.permissions.PermissionAttachmentInfo info : player.getEffectivePermissions()) {
            String perm = info.getPermission().toLowerCase();
            if (perm.startsWith("donutorders.limit.")) {
                try {
                    int pLimit = Integer.parseInt(perm.substring("donutorders.limit.".length()));
                    if (pLimit > limit) {
                        limit = pLimit;
                    }
                } catch (NumberFormatException ignored) {}
            }
        }
        
        return limit;
    }
}