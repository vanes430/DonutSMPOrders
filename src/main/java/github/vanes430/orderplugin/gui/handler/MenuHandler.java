package github.vanes430.orderplugin.gui.handler;

import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public interface MenuHandler {
    void onClick(InventoryClickEvent event);
    void onDrag(InventoryDragEvent event);
    void onClose(InventoryCloseEvent event);
    void onChat(AsyncPlayerChatEvent event);
}