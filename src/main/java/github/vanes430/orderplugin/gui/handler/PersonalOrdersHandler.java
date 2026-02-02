package github.vanes430.orderplugin.gui.handler;

import github.vanes430.orderplugin.OrderPlugin;
import github.vanes430.orderplugin.gui.GuiManager;
import github.vanes430.orderplugin.model.Order;
import github.vanes430.orderplugin.utils.Utils;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class PersonalOrdersHandler implements MenuHandler {
    private final OrderPlugin plugin;
    private final GuiManager guiManager;

    public PersonalOrdersHandler(OrderPlugin plugin) {
        this.plugin = plugin;
        this.guiManager = plugin.getGuiManager();
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        final Player player = (Player)event.getWhoClicked();
        final UUID uuid = player.getUniqueId();
        String titleStr = LegacyComponentSerializer.legacySection().serialize(event.getView().title());
        ItemStack currentItem = event.getCurrentItem();

        String yourOrdersTitle = this.plugin.getConfig().getString("gui-titles.your-orders", "ORDERS -> Your Orders");
        String editOrderTitle = this.plugin.getConfig().getString("gui-titles.edit-order", "ORDERS -> Edit Order");

        if (titleStr.startsWith(yourOrdersTitle)) {
            event.setCancelled(true);
            if (currentItem != null && currentItem.getType() != Material.AIR) {
                List<Order> owned = this.plugin.getOrderManager().getOrdersByOwner(uuid).stream()
                        .filter(o -> !o.isRemovedByAdmin() || o.hasItemsToCollect())
                        .collect(Collectors.toList());
                int slot = event.getSlot();
                int page = 1;
                if (titleStr.contains("(") && titleStr.contains("/")) {
                    try {
                        String pagePart = titleStr.substring(titleStr.lastIndexOf("(") + 1, titleStr.lastIndexOf("/"));
                        page = Integer.parseInt(pagePart);
                    } catch (Exception e) {}
                }

                if (slot >= 0 && slot < 45) {
                    int index = ((page - 1) * 45) + slot;
                    if (index < owned.size()) {
                        Order order = owned.get(index);
                        this.guiManager.activeEditOrder.put(uuid, order);
                        this.guiManager.openEditOrderMenu(player, order);
                    }
                } else if (slot == 48) { // Previous Page
                    if (page > 1) {
                        this.guiManager.openYourOrders(player, page - 1);
                    }
                } else if (slot == 49) { // Go Back
                    this.guiManager.openMainMenu(player);
                } else if (slot == 50) { // New Order
                    this.guiManager.selectedMaterial.remove(uuid);
                    this.guiManager.selectedAmount.put(uuid, 1);
                    this.guiManager.selectedPrice.put(uuid, 1.0);
                    this.guiManager.openNewOrderEditor(player);
                } else if (slot == 51) { // Next Page
                    int maxPages = (int) Math.ceil((double) owned.size() / 45);
                    if (page < maxPages) {
                        this.guiManager.openYourOrders(player, page + 1);
                    }
                }
            }
        } else if (titleStr.equals(editOrderTitle)) {
            event.setCancelled(true);
            if (currentItem != null && currentItem.getType() != Material.AIR) {
                int slot = event.getSlot();
                final Order order = this.guiManager.activeEditOrder.get(uuid);
                if (order == null) {
                    player.closeInventory();
                    return;
                }

                if (slot == 10) { // Delete Order
                    if (order.hasItemsToCollect()) {
                        player.sendMessage(Utils.color("&cYou must collect all items first!"));
                        return;
                    }

                    if (order.isCompleted()) {
                        this.plugin.getOrderManager().removeOrder(order);
                        player.sendMessage(Utils.color("&aOrder deleted."));
                        player.closeInventory();
                        this.guiManager.openYourOrders(player);
                    } else {
                        this.plugin.getOrderManager().cancelOrder(player, order);
                        player.closeInventory();
                    }
                } else if (slot == 12) { // View Storage
                    player.closeInventory();
                    this.guiManager.activeCollectOrder.put(uuid, order);
                    this.guiManager.collectionPage.put(uuid, 1);
                    player.getScheduler().runDelayed(this.plugin, (task) -> {
                        this.guiManager.openCollectItemsMenu(player, order, 1);
                    }, null, 2L);
                } else if (slot == 14) { // Drop All
                    List<ItemStack> allItems = order.getAllCollectedItems();
                    if (allItems.isEmpty()) {
                        player.sendMessage(Utils.color("&cNo items to drop!"));
                    } else {
                        for (ItemStack item : allItems) {
                            Map<Integer, ItemStack> left = player.getInventory().addItem(item);
                            if (!left.isEmpty()) {
                                for (ItemStack leftItem : left.values()) {
                                    player.getWorld().dropItemNaturally(player.getLocation(), leftItem);
                                }
                            }
                        }
                        order.clearInventory();
                        this.plugin.getOrderManager().saveOrders();
                        player.sendMessage(this.plugin.getMessage("success.items-dropped", "{prefix}&aDropped all items to your inventory/ground!"));

                        if (order.isCompleted() && !order.hasItemsToCollect()) {
                            this.plugin.getOrderManager().removeOrder(order);
                            this.guiManager.activeCollectOrder.remove(uuid);
                            this.guiManager.activeEditOrder.remove(uuid);
                            this.guiManager.openYourOrders(player);
                        } else {
                            this.guiManager.openEditOrderMenu(player, order);
                        }
                    }
                } else if (slot == 16) { // Sell All
                    player.sendMessage(this.plugin.getMessage("gui.sell-all-coming-soon", "{prefix}&eSell All is coming soon!"));
                } else if (slot == 22) { // Back
                    this.guiManager.openYourOrders(player);
                }
            }
        }
    }

    @Override
    public void onChat(AsyncPlayerChatEvent event) {}

    @Override
    public void onDrag(InventoryDragEvent event) {}

    @Override
    public void onClose(InventoryCloseEvent event) {}
}