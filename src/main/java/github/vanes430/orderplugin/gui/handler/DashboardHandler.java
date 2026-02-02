package github.vanes430.orderplugin.gui.handler;

import github.vanes430.orderplugin.OrderPlugin;
import github.vanes430.orderplugin.gui.GuiManager;
import github.vanes430.orderplugin.model.FilterType;
import github.vanes430.orderplugin.model.Order;
import github.vanes430.orderplugin.model.SortType;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class DashboardHandler implements MenuHandler {
    private final OrderPlugin plugin;
    private final GuiManager guiManager;
    
    public DashboardHandler(OrderPlugin plugin) {
        this.plugin = plugin;
        this.guiManager = plugin.getGuiManager();
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        final Player player = (Player)event.getWhoClicked();
        final UUID uuid = player.getUniqueId();
        String titleStr = LegacyComponentSerializer.legacySection().serialize(event.getView().title());
        String mainMenuPrefix = this.plugin.getConfig().getString("gui-titles.main-menu", "ORDERS").split("%")[0];

        if (titleStr.startsWith(mainMenuPrefix)) {
            event.setCancelled(true);
            int page = this.guiManager.menuPage.getOrDefault(uuid, 1);
            
            if (event.getSlot() == 53) { // Next Page
                int pageSize = 45;
                List<Order> active = this.plugin.getOrderManager().getActiveOrders().stream().filter(o -> !o.isCompleted()).collect(Collectors.toList());
                if (active.size() > page * pageSize) {
                    this.guiManager.menuPage.put(uuid, page + 1);
                    this.guiManager.openMainMenu(player, page + 1, null);
                }
            } else if (event.getSlot() == 47) { // Sort
                SortType sort = this.guiManager.playerSort.getOrDefault(uuid, SortType.RECENTLY_LISTED);
                this.guiManager.playerSort.put(uuid, sort.next());
                this.guiManager.openMainMenu(player, page, null);
            } else if (event.getSlot() == 48) { // Filter
                FilterType filter = this.guiManager.playerFilter.getOrDefault(uuid, FilterType.ALL);
                this.guiManager.playerFilter.put(uuid, filter.next());
                this.guiManager.menuPage.put(uuid, 1);
                this.guiManager.openMainMenu(player, 1, null);
            } else if (event.getSlot() == 49) { // Refresh
                String search = this.guiManager.playerSearch.get(uuid);
                this.guiManager.openMainMenu(player, page, search);
            } else if (event.getSlot() == 50) { // Search
                player.closeInventory();
                this.guiManager.chatInputMode.put(uuid, "SEARCH_ORDER");
                player.sendMessage(this.plugin.getMessage("gui.type-search", "{prefix}&eType item name to search (or 'clear' to reset):"));
            } else if (event.getSlot() == 51) { // Your Orders
                player.closeInventory();
                this.guiManager.openYourOrders(player);
            } else if (event.getSlot() < 45) { // Order Click
                SortType sort = this.guiManager.playerSort.getOrDefault(uuid, SortType.RECENTLY_LISTED);
                FilterType filter = this.guiManager.playerFilter.getOrDefault(uuid, FilterType.ALL);
                String search = this.guiManager.playerSearch.get(uuid);
                List<Order> active = this.plugin
                        .getOrderManager()
                        .getActiveOrders()
                        .stream()
                        .filter(o -> !o.isCompleted())
                        .filter(o -> filter.matches(o.getMaterial()))
                        .filter(o -> search == null || o.getMaterial().name().toLowerCase().contains(search.toLowerCase()))
                        .collect(Collectors.toList());

                switch (sort) {
                    case MOST_PAID:
                        active.sort((o1, o2) -> Double.compare(o2.getFilledAmount() * o2.getPricePerItem(), o1.getFilledAmount() * o1.getPricePerItem()));
                        break;
                    case MOST_DELIVERED:
                        active.sort((o1, o2) -> Integer.compare(o2.getFilledAmount(), o1.getFilledAmount()));
                        break;
                    case RECENTLY_LISTED:
                        active.sort((o1, o2) -> Long.compare(o2.getCreatedAt(), o1.getCreatedAt()));
                        break;
                    case MOST_MONEY_PER_ITEM:
                        active.sort((o1, o2) -> Double.compare(o2.getPricePerItem(), o1.getPricePerItem()));
                }

                int index = (page - 1) * 45 + event.getSlot();
                if (index < active.size()) {
                    Order order = active.get(index);
                    if (order.getOwnerId().equals(uuid)) {
                        player.sendMessage(this.plugin.getMessage("errors.cant-fulfill-own", "{prefix}&cCan't fulfill own order!"));
                    } else {
                        this.guiManager.activeDeliverOrder.put(uuid, order);
                        this.guiManager.openDeliverItemsInventory(player, order);
                    }
                }
            }
        }
    }

    @Override
    public void onChat(AsyncPlayerChatEvent event) {
        final Player player = event.getPlayer();
        final UUID uuid = player.getUniqueId();
        if (this.guiManager.chatInputMode.containsKey(uuid)) {
            final String mode = this.guiManager.chatInputMode.get(uuid);
            if (mode.equals("SEARCH_ORDER")) {
                this.guiManager.chatInputMode.remove(uuid);
                event.setCancelled(true);
                final String message = event.getMessage();
                player.getScheduler().run(this.plugin, (task) -> {
                    if (!message.equalsIgnoreCase("clear") && !message.equalsIgnoreCase("reset")) {
                        this.guiManager.playerSearch.put(uuid, message);
                        player.sendMessage(this.plugin.getMessage("success.search-applied", "{prefix}&aSearching for: &e%search%", "%search%", message));
                    } else {
                        this.guiManager.playerSearch.remove(uuid);
                        player.sendMessage(this.plugin.getMessage("success.search-cleared", "{prefix}&aSearch cleared!"));
                    }
                    this.guiManager.openMainMenu(player, 1, this.guiManager.playerSearch.get(uuid));
                }, null);
            }
        }
    }

    @Override
    public void onDrag(InventoryDragEvent event) {
    }

    @Override
    public void onClose(InventoryCloseEvent event) {
    }
}