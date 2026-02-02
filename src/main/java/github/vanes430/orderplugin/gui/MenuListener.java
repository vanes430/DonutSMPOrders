package github.vanes430.orderplugin.gui;

import github.vanes430.orderplugin.OrderPlugin;
import github.vanes430.orderplugin.gui.handler.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.ArrayList;
import java.util.List;

public class MenuListener implements Listener {
    private final List<MenuHandler> handlers = new ArrayList<>();

    public MenuListener(OrderPlugin plugin) {
        handlers.add(new DashboardHandler(plugin));
        handlers.add(new CreationHandler(plugin));
        handlers.add(new PersonalOrdersHandler(plugin));
        handlers.add(new CollectionHandler(plugin));
        handlers.add(new DeliveryHandler(plugin));
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        for (MenuHandler handler : handlers) {
            handler.onChat(event);
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        for (MenuHandler handler : handlers) {
            handler.onClick(event);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        for (MenuHandler handler : handlers) {
            handler.onDrag(event);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        for (MenuHandler handler : handlers) {
            handler.onClose(event);
        }
    }
}