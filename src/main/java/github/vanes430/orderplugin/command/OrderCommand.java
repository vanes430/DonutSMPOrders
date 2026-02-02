package github.vanes430.orderplugin.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import github.vanes430.orderplugin.OrderPlugin;

public class OrderCommand implements CommandExecutor {
    private final OrderPlugin plugin;

    public OrderCommand(OrderPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(this.plugin.getMessage("general.player-only", "&cOnly players can use this command."));
            return true;
        } else {
            Player player = (Player) sender;
            if (!player.hasPermission("donutorders.use")) {
                player.sendMessage(this.plugin.getMessage("general.no-permission", "§cYou don't have permission."));
                return true;
            } else {
                this.plugin.getGuiManager().openMainMenu(player);
                return true;
            }
        }
    }
}
