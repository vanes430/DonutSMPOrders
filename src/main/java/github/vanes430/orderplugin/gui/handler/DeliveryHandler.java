package github.vanes430.orderplugin.gui.handler;

import github.vanes430.orderplugin.OrderPlugin;
import github.vanes430.orderplugin.gui.GuiManager;
import github.vanes430.orderplugin.model.Order;
import github.vanes430.orderplugin.task.DeliveryAnimationTask;
import github.vanes430.orderplugin.utils.Utils;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DeliveryHandler implements MenuHandler {
    private final OrderPlugin plugin;
    private final GuiManager guiManager;

    public DeliveryHandler(OrderPlugin plugin) {
        this.plugin = plugin;
        this.guiManager = plugin.getGuiManager();
    }

    private boolean isShulker(Material material) {
        return material != null && material.name().contains("SHULKER_BOX");
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        final Player player = (Player)event.getWhoClicked();
        final UUID uuid = player.getUniqueId();
        String titleStr = LegacyComponentSerializer.legacySection().serialize(event.getView().title());
        
        String deliverTitle = this.plugin.getConfig().getString("gui-titles.deliver-items", "ORDERS -> Deliver Items");
        String confirmTitle = this.plugin.getConfig().getString("gui-titles.confirm-delivery", "ORDERS -> Confirm Delivery");

        if (titleStr.equals(deliverTitle)) {
            Order order = this.guiManager.activeDeliverOrder.get(uuid);
            if (order == null) {
                player.closeInventory();
            } else {
                if (event.getClickedInventory() != null && event.getClickedInventory().equals(event.getView().getTopInventory())) {
                    if (event.getCursor() != null && event.getCursor().getType() != Material.AIR && !order.matches(event.getCursor()) && !this.isShulker(event.getCursor().getType())) {
                        player.sendMessage(this.plugin.getMessage("errors.wrong-item", "{prefix}&cWrong item type!"));
                        event.setCancelled(true);
                    } else {
                        event.setCancelled(false);
                    }
                } else if (event.isShiftClick()) {
                    if (event.getCurrentItem() != null && !order.matches(event.getCurrentItem()) && !this.isShulker(event.getCurrentItem().getType())) {
                        player.sendMessage(this.plugin.getMessage("errors.wrong-item", "{prefix}&cWrong item type!"));
                        event.setCancelled(true);
                    } else {
                        event.setCancelled(false);
                    }
                } else {
                    event.setCancelled(false);
                }
            }
        } else if (titleStr.equals(confirmTitle)) {
            event.setCancelled(true);
            if (event.getSlot() == 11) { // Cancel
                List<ItemStack> items = this.guiManager.deliveryItems.remove(uuid);
                if (items != null) {
                    for (ItemStack item : items) player.getInventory().addItem(item);
                }
                List<ItemStack> shulkers = this.guiManager.deliveryShulkers.remove(uuid);
                if (shulkers != null) {
                    for (ItemStack s : shulkers) player.getInventory().addItem(s);
                }
                this.guiManager.activeDeliverOrder.remove(uuid);
                player.sendMessage(this.plugin.getMessage("success.delivery-cancelled", "{prefix}&cDelivery cancelled. Your items have been returned."));
                this.guiManager.openMainMenu(player);
            } else if (event.getSlot() == 15) { // Confirm
                final Order order = this.guiManager.activeDeliverOrder.remove(uuid);
                final List<ItemStack> items = this.guiManager.deliveryItems.remove(uuid);
                final Double payment = this.guiManager.deliveryPayment.remove(uuid);
                final List<ItemStack> shulkers = this.guiManager.deliveryShulkers.remove(uuid);
                player.closeInventory();

                new DeliveryAnimationTask(plugin, player, order, items, payment, shulkers).run();
            }
        }
    }

    @Override
    public void onClose(InventoryCloseEvent event) {
        final Player player = (Player)event.getPlayer();
        UUID uuid = player.getUniqueId();
        String titleStr = LegacyComponentSerializer.legacySection().serialize(event.getView().title());

        String deliverTitle = this.plugin.getConfig().getString("gui-titles.deliver-items", "ORDERS -> Deliver Items");
        String confirmTitle = this.plugin.getConfig().getString("gui-titles.confirm-delivery", "ORDERS -> Confirm Delivery");

        if (titleStr.equals(deliverTitle) && this.guiManager.activeDeliverOrder.containsKey(uuid)) {
            final Order order = this.guiManager.activeDeliverOrder.get(uuid);
            List<ItemStack> delivered = new ArrayList<>();
            List<ItemStack> shulkers = new ArrayList<>();
            int totalNeeded = order.getNeededAmount();
            int currentlyFilled = order.getFilledAmount();
            int remainingNeeded = totalNeeded - currentlyFilled;
            int totalAddedValue = 0;
            int returnedCount = 0;

            for (ItemStack item : event.getInventory().getContents()) {
                if (item != null && item.getType() != Material.AIR) {
                    if (!this.isShulker(item.getType())) {
                        if (!order.matches(item)) {
                            player.getInventory().addItem(item);
                        } else {
                            int space = remainingNeeded - totalAddedValue;
                            if (space > 0) {
                                int amount = item.getAmount();
                                int toAdd = Math.min(amount, space);
                                int back = amount - toAdd;
                                ItemStack clone = item.clone();
                                clone.setAmount(toAdd);
                                delivered.add(clone);
                                totalAddedValue += toAdd;
                                if (back > 0) {
                                    ItemStack backStack = item.clone();
                                    backStack.setAmount(back);
                                    player.getInventory().addItem(backStack);
                                    returnedCount += back;
                                }
                            } else {
                                player.getInventory().addItem(item);
                                returnedCount += item.getAmount();
                            }
                        }
                    } else if (!(item.getItemMeta() instanceof BlockStateMeta)) {
                        player.getInventory().addItem(item);
                    } else {
                        BlockStateMeta meta = (BlockStateMeta)item.getItemMeta();
                        if (!(meta.getBlockState() instanceof ShulkerBox)) {
                            player.getInventory().addItem(item);
                        } else {
                            ShulkerBox shulker = (ShulkerBox)meta.getBlockState();
                            boolean modified = false;

                            for (int i = 0; i < shulker.getInventory().getSize(); i++) {
                                ItemStack content = shulker.getInventory().getItem(i);
                                if (content != null && content.getType() != Material.AIR && order.matches(content)) {
                                    int space = remainingNeeded - totalAddedValue;
                                    if (space > 0) {
                                        int amount = content.getAmount();
                                        int toAdd = Math.min(amount, space);
                                        int back = amount - toAdd;
                                        ItemStack clone = content.clone();
                                        clone.setAmount(toAdd);
                                        delivered.add(clone);
                                        totalAddedValue += toAdd;
                                        modified = true;
                                        if (back > 0) {
                                            content.setAmount(back);
                                        } else {
                                            shulker.getInventory().setItem(i, null);
                                        }
                                    }
                                }
                            }

                            meta.setBlockState(shulker);
                            item.setItemMeta(meta);
                            if (modified) {
                                shulkers.add(item.clone());
                            } else {
                                player.getInventory().addItem(item);
                            }
                        }
                    }
                }
            }

            if (!shulkers.isEmpty()) {
                this.guiManager.deliveryShulkers.put(uuid, shulkers);
            }

            if (returnedCount > 0) {
                player.sendMessage(Utils.color("&e[Order] " + returnedCount + " items returned (Limit reached)."));
            }

            if (totalAddedValue == 0) {
                this.guiManager.activeDeliverOrder.remove(uuid);
                return;
            }

            final double payment = totalAddedValue * order.getPricePerItem();
            final int amountToFulfill = totalAddedValue;
            this.guiManager.deliveryItems.put(uuid, delivered);
            this.guiManager.deliveryPayment.put(uuid, payment);

            player.getScheduler().runDelayed(this.plugin, (task) -> {
                this.guiManager.openConfirmDelivery(player, order, amountToFulfill, payment);
            }, null, 1L);
        } else if (titleStr.equals(confirmTitle) && this.guiManager.activeDeliverOrder.containsKey(uuid)) {
            // If closed without confirming (e.g. ESC), reopen confirm menu?
            // Or return items? Usually return items logic is triggered.
            // But we have logic here:
            final Order order = this.guiManager.activeDeliverOrder.get(uuid);
            final List<ItemStack> items = this.guiManager.deliveryItems.remove(uuid);
            this.guiManager.deliveryPayment.remove(uuid);
            final List<ItemStack> shulkers = this.guiManager.deliveryShulkers.remove(uuid);

            player.getScheduler().runDelayed(this.plugin, (task) -> {
                String deliverTitleInternal = this.plugin.getConfig().getString("gui-titles.deliver-items", "ORDERS -> Deliver Items");
                Inventory inv = Bukkit.createInventory(null, 27, Utils.color(deliverTitleInternal));
                if (items != null) {
                    for (ItemStack item : items) {
                        inv.addItem(item);
                    }
                }

                if (shulkers != null) {
                    for (ItemStack item : shulkers) {
                        inv.addItem(item);
                    }
                }

                player.openInventory(inv);
            }, null, 1L);
        }
    }

    @Override
    public void onChat(AsyncPlayerChatEvent event) {}

    @Override
    public void onDrag(InventoryDragEvent event) {}
}