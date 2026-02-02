package github.vanes430.orderplugin.manager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import github.vanes430.orderplugin.OrderPlugin;
import github.vanes430.orderplugin.utils.Utils;

public class PendingMessageManager implements Listener {
   private final OrderPlugin plugin;

   public PendingMessageManager(OrderPlugin plugin) {
      this.plugin = plugin;
   }

   public void addMessage(UUID uuid, String message) {
      Bukkit.getAsyncScheduler().runNow(this.plugin, (task) -> {
         try (Connection conn = plugin.getDatabaseManager().getConnection();
              PreparedStatement ps = conn.prepareStatement("INSERT INTO pending_messages (player_id, message) VALUES (?, ?)")) {
            ps.setObject(1, uuid);
            ps.setString(2, message);
            ps.executeUpdate();
         } catch (SQLException e) {
            this.plugin.getLogger().log(Level.SEVERE, "Could not save pending message to H2", e);
         }
      });
   }

   public List<String> getAndClearMessages(UUID uuid) {
      List<String> messages = new ArrayList<>();
      try (Connection conn = plugin.getDatabaseManager().getConnection()) {
         // Query
         try (PreparedStatement ps = conn.prepareStatement("SELECT id, message FROM pending_messages WHERE player_id = ?")) {
            ps.setObject(1, uuid);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
               messages.add(rs.getString("message"));
            }
         }
         // Delete
         if (!messages.isEmpty()) {
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM pending_messages WHERE player_id = ?")) {
               ps.setObject(1, uuid);
               ps.executeUpdate();
            }
         }
      } catch (SQLException e) {
         this.plugin.getLogger().log(Level.SEVERE, "Could not get/clear pending messages from H2", e);
      }
      return messages;
   }

   @EventHandler
   public void onJoin(PlayerJoinEvent event) {
      Player player = event.getPlayer();
      
      Bukkit.getAsyncScheduler().runNow(this.plugin, (task) -> {
         List<String> messages = getAndClearMessages(player.getUniqueId());
         
         if (!messages.isEmpty()) {
            player.getScheduler().runDelayed(this.plugin, (t) -> {
               if (player.isOnline()) {
                  for (String message : messages) {
                     player.sendMessage(Utils.color(message));
                  }
               }
            }, null, 40L);
         }
      });
   }
}