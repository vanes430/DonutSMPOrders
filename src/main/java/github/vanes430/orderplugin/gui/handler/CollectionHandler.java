package github.vanes430.orderplugin.gui.handler;

import github.vanes430.orderplugin.OrderPlugin;
import github.vanes430.orderplugin.gui.GuiManager;
import github.vanes430.orderplugin.model.Order;
import github.vanes430.orderplugin.utils.Utils;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class CollectionHandler implements MenuHandler {
    private final OrderPlugin plugin;
    private final GuiManager guiManager;

    public CollectionHandler(OrderPlugin plugin) {
        this.plugin = plugin;
        this.guiManager = plugin.getGuiManager();
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        final Player player = (Player)event.getWhoClicked();
        final UUID uuid = player.getUniqueId();
        String titleStr = LegacyComponentSerializer.legacySection().serialize(event.getView().title());
        ItemStack currentItem = event.getCurrentItem();
        
        String collectTitle = "ORDERS -> Collect Items";
        String confirmSellTitle = this.plugin.getConfig().getString("gui-titles.confirm-sell", "ORDERS -> Confirm Sell");

        if (titleStr.startsWith(collectTitle)) {
            Order order = this.guiManager.activeCollectOrder.get(uuid);
            if (order == null) {
                event.setCancelled(true);
            } else {
                int slot = event.getSlot();
                int page = 1;
                if (titleStr.contains("(") && titleStr.contains("/")) {
                    try {
                        String pagePart = titleStr.substring(titleStr.lastIndexOf("(") + 1, titleStr.lastIndexOf("/"));
                        page = Integer.parseInt(pagePart);
                    } catch (Exception e) {}
                }

                if (slot >= 45 && slot <= 53) {
                    event.setCancelled(true);
                    if (slot == 45 && page > 1) {
                        this.guiManager.openCollectItemsMenu(player, order, page - 1);
                    } else if (slot == 48) {
                        // Collect All Logic
                        List<ItemStack> allItems = order.getAllCollectedItems();
                        if (allItems.isEmpty()) {
                            player.sendMessage(Utils.color("&cNo items to collect!"));
                            return;
                        }

                        List<ItemStack> remainingInOrder = new ArrayList<>();
                        boolean full = false;
                        int collectedCount = 0;

                        for (ItemStack item : allItems) {
                            if (full) {
                                remainingInOrder.add(item);
                                continue;
                            }

                            Map<Integer, ItemStack> left = player.getInventory().addItem(item);
                            if (left.isEmpty()) {
                                collectedCount += item.getAmount();
                            } else {
                                full = true;
                                for (ItemStack leftItem : left.values()) {
                                    remainingInOrder.add(leftItem);
                                    collectedCount += (item.getAmount() - leftItem.getAmount());
                                }
                            }
                        }
                        
                        order.setItems(remainingInOrder);
                        this.plugin.getOrderManager().saveOrders();
                        
                        if (collectedCount > 0) {
                            player.sendMessage(Utils.color("&aCollected &e" + collectedCount + " &aitems."));
                        }
                        if (full) {
                            player.sendMessage(Utils.color("&cInventory full! Remaining items stayed in order."));
                        }
                        
                        if (order.isCompleted() && !order.hasItemsToCollect()) {
                            this.plugin.getOrderManager().removeOrder(order);
                            this.guiManager.activeCollectOrder.remove(uuid);
                            player.closeInventory();
                            this.guiManager.openYourOrders(player);
                        } else {
                            this.guiManager.openCollectItemsMenu(player, order, 1);
                        }
                    } else if (slot == 49) { // Back
                        this.guiManager.openEditOrderMenu(player, order);
                    } else if (slot == 50) { // Drop All
                        List<ItemStack> rawItems = order.getAllCollectedItems();
                        if (rawItems.isEmpty()) {
                            player.sendMessage(Utils.color("&cNo items to drop!"));
                            return;
                        }

                        // Flatten items into single list of max stack size
                        List<ItemStack> allDrops = new ArrayList<>();
                        List<ItemStack> remainingItems = new ArrayList<>();
                        int droppedStacks = 0;
                        int maxStacks = 27;

                        for (ItemStack item : rawItems) {
                            int amount = item.getAmount();
                            while (amount > 0) {
                                int stackSize = Math.min(amount, item.getMaxStackSize()); // Use item's max stack size (e.g. 16 for pearls)
                                ItemStack stack = item.clone();
                                stack.setAmount(stackSize);
                                
                                if (droppedStacks < maxStacks) {
                                    allDrops.add(stack);
                                    droppedStacks++;
                                } else {
                                    remainingItems.add(stack);
                                }
                                amount -= stackSize;
                            }
                        }

                        for (ItemStack drop : allDrops) {
                            Item itemEntity = player.getWorld().dropItem(player.getEyeLocation(), drop);
                            itemEntity.setVelocity(player.getLocation().getDirection().multiply(0.3));
                        }

                        order.setItems(remainingItems); // Save remaining items back to order
                        this.plugin.getOrderManager().saveOrders();
                        
                        if (remainingItems.isEmpty()) {
                            player.sendMessage(Utils.color("&aDropped all collected items to the ground!"));
                        } else {
                            player.sendMessage(Utils.color("&aDropped " + droppedStacks + " stacks (Chest Limit). Remaining items are still in storage."));
                        }
                        
                        if (order.isCompleted() && !order.hasItemsToCollect()) {
                            this.plugin.getOrderManager().removeOrder(order);
                            this.guiManager.activeCollectOrder.remove(uuid);
                            player.closeInventory();
                            this.guiManager.openYourOrders(player);
                        } else {
                            // Re-open collection menu to show remaining items or refresh
                            this.guiManager.openCollectItemsMenu(player, order, 1); 
                        }
                    } else if (slot == 53) {
                        List<ItemStack> rawItems = order.getAllCollectedItems();
                        int totalStacks = 0;
                        for (ItemStack item : rawItems) {
                            totalStacks += (int) Math.ceil(item.getAmount() / 64.0);
                        }
                        int maxPages = (int)Math.ceil(totalStacks / 45.0);
                        if (page < maxPages) {
                            this.guiManager.openCollectItemsMenu(player, order, page + 1);
                        }
                    }
                } else if (slot >= 0 && slot < 45) { // Manual collect
                    if (currentItem != null && currentItem.getType() != Material.AIR) {
                        List<ItemStack> rawItems = order.getAllCollectedItems();
                        List<ItemStack> displayItems = new ArrayList<>();
                        for (ItemStack item : rawItems) {
                            int amount = item.getAmount();
                            while (amount > 0) {
                                int stackSize = Math.min(amount, 64);
                                ItemStack stack = item.clone();
                                stack.setAmount(stackSize);
                                displayItems.add(stack);
                                amount -= stackSize;
                            }
                        }

                        int index = (page - 1) * 45 + slot;
                        if (index < displayItems.size()) {
                            ItemStack picked = displayItems.get(index);
                            Map<Integer, ItemStack> left = player.getInventory().addItem(picked);
                            
                            if (left.isEmpty()) {
                                displayItems.remove(index);
                            } else {
                                ItemStack remaining = left.values().iterator().next();
                                displayItems.set(index, remaining);
                            }
                            
                            order.setItems(displayItems);
                            this.plugin.getOrderManager().saveOrders();
                            
                            if (order.isCompleted() && !order.hasItemsToCollect()) {
                                this.plugin.getOrderManager().removeOrder(order);
                                this.guiManager.activeCollectOrder.remove(uuid);
                                player.closeInventory();
                                this.guiManager.openYourOrders(player);
                            } else {
                                this.guiManager.openCollectItemsMenu(player, order, page);
                            }
                        }
                    }
                    event.setCancelled(true);
                } else {
                    event.setCancelled(true);
                }
            }
        } else if (this.guiManager.activeConfirmSellOrder.containsKey(uuid) && titleStr.contains("Confirm Sell")) { // Using contains since title has formatting
             event.setCancelled(true);
             if (event.getSlot() == 11) { // Cancel
                this.guiManager.activeConfirmSellOrder.remove(uuid);
                this.guiManager.sellItems.remove(uuid);
                this.guiManager.sellPrice.remove(uuid);
                Order order = this.guiManager.activeCollectOrder.get(uuid);
                if (order != null) {
                   this.guiManager.openCollectItemsMenu(player, order, 1);
                } else {
                   player.closeInventory();
                }
             } else if (event.getSlot() == 15) { // Confirm
                Order order = this.guiManager.activeConfirmSellOrder.remove(uuid);
                List<ItemStack> items = this.guiManager.sellItems.remove(uuid);
                Double price = this.guiManager.sellPrice.remove(uuid);
                if (order != null && items != null && price != null) {
                   player.closeInventory();
                   order.clearInventory();
                   this.plugin.getOrderManager().saveOrders();
                   OrderPlugin.getEconomy().depositPlayer(player, price);
                   String formattedPrice = Utils.formatNumber(price);
                   player.sendActionBar(Utils.color("You sold items for " + formattedPrice));
                   player.sendMessage(Utils.color("&#a+&#a$" + formattedPrice));
                   if (order.isCompleted() && !order.hasItemsToCollect()) {
                      this.plugin.getOrderManager().removeOrder(order);
                      this.guiManager.activeCollectOrder.remove(uuid);
                      this.guiManager.openYourOrders(player);
                   } else {
                      this.guiManager.openYourOrders(player);
                   }
                } else {
                   player.closeInventory();
                }
             }
        }
    }

    @Override
    public void onDrag(InventoryDragEvent event) {
        String titleStr = Utils.stripColor(LegacyComponentSerializer.legacySection().serialize(event.getView().title()));
        if (titleStr.equals("ORDERS -> Collect Items") || this.guiManager.activeConfirmSellOrder.containsKey(event.getWhoClicked().getUniqueId())) {
            for (int slot : event.getRawSlots()) {
                if (slot < event.getView().getTopInventory().getSize()) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }

    @Override
    public void onClose(InventoryCloseEvent event) {
        final Player player = (Player)event.getPlayer();
        UUID uuid = player.getUniqueId();
        String titleStr = LegacyComponentSerializer.legacySection().serialize(event.getView().title());
        
        if (this.guiManager.activeConfirmSellOrder.containsKey(uuid) && titleStr.contains("Confirm Sell")) {
             this.guiManager.activeConfirmSellOrder.remove(uuid);
             this.guiManager.sellItems.remove(uuid);
             this.guiManager.sellPrice.remove(uuid);
        }
    }

    @Override
    public void onChat(AsyncPlayerChatEvent event) {}
}