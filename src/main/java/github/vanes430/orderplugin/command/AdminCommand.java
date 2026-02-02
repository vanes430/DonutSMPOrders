package github.vanes430.orderplugin.command;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import github.vanes430.orderplugin.OrderPlugin;
import github.vanes430.orderplugin.model.Order;
import github.vanes430.orderplugin.utils.Utils;

public class AdminCommand implements CommandExecutor, TabCompleter {
    private final OrderPlugin plugin;

    public AdminCommand(OrderPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("donutorders.admin")) {
            sender.sendMessage(this.plugin.getMessage("general.no-permission", "§cYou don't have permission to use this command."));
            return true;
        } else if (args.length == 0) {
            this.sendHelp(sender);
            return true;
        } else if (args[0].equalsIgnoreCase("reload")) {
            this.plugin.reloadAllConfigs();
            sender.sendMessage(this.plugin.getMessage("general.reload-success", "§aDonutOrders config and messages reloaded successfully!"));
            return true;
        } else if (!args[0].equalsIgnoreCase("removeorder")) {
            this.sendHelp(sender);
            return true;
        } else if (args.length < 3) {
            sender.sendMessage(this.plugin.getMessage("admin.usage-removeorder", "§cUsage: /donutordersadmin removeorder <player> <order>"));
            return true;
        } else {
            String playerName = args[1];
            String orderRef = args[2];
            OfflinePlayer target = Bukkit.getOfflinePlayer(playerName);
            if (!target.hasPlayedBefore() && !target.isOnline()) {
                sender.sendMessage(this.plugin.getMessage("errors.player-not-found", "§cPlayer %player% not found.", "%player%", playerName));
                return true;
            } else {
                Order match = null;

                for (Order o : this.plugin.getOrderManager().getOrdersByOwner(target.getUniqueId())) {
                    if (!o.isCompleted()) {
                        String shortId = o.getOrderId().toString().substring(0, 8);
                        String ref = o.getMaterial().name() + "_x" + o.getNeededAmount() + "_" + shortId;
                        if (orderRef.equalsIgnoreCase(ref) || orderRef.equals(o.getOrderId().toString())) {
                            match = o;
                            break;
                        }
                    }
                }

                if (match == null) {
                    sender.sendMessage(this.plugin.getMessage("errors.order-not-found", "§cOrder not found."));
                    return true;
                } else {
                    double refund = match.getRemainingAmount() * match.getPricePerItem();
                    String matName = Utils.formatMaterialName(match.getMaterial().name());
                    OrderPlugin.getEconomy().depositPlayer(target, refund);
                    match.setRemovedByAdmin(true);
                    this.plugin.getOrderManager().saveOrders();
                    int collectedCount = 0;

                    for (ItemStack item : match.getAllCollectedItems()) {
                        if (item != null) {
                            collectedCount += item.getAmount();
                        }
                    }

                    Component adminMsg = collectedCount > 0
                            ? this.plugin
                            .getMessage(
                                    "admin.order-removed-with-items",
                                    "§aOrder of §e%amount% %item% §ahas been removed. Refund: §e%price% §7(Player can still collect %collected% items)",
                                    "%amount%",
                                    Utils.formatNumber((double) match.getNeededAmount()),
                                    "%item%",
                                    matName,
                                    "%price%",
                                    "$" + String.format("%,.2f", refund),
                                    "%collected%",
                                    String.valueOf(collectedCount)
                            )
                            : this.plugin
                            .getMessage(
                                    "admin.order-removed",
                                    "§aOrder of §e%amount% %item% §ahas been removed. Refund: §e%price%",
                                    "%amount%",
                                    Utils.formatNumber((double) match.getNeededAmount()),
                                    "%item%",
                                    matName,
                                    "%price%",
                                    "$" + String.format("%,.2f", refund)
                            );
                    sender.sendMessage(adminMsg);
                    
                    String playerMsgStr = this.plugin.getMessagesConfig().getString("admin.order-removed-player");
                    if (playerMsgStr == null) playerMsgStr = "§cYour order of §e%amount% %item% §chas been removed by an admin. Refund: §e%price%§c.";
                    
                    playerMsgStr = playerMsgStr.replace("%amount%", Utils.formatNumber((double) match.getNeededAmount()))
                            .replace("%item%", matName)
                            .replace("%price%", "$" + String.format("%,.2f", refund));

                    if (collectedCount > 0) {
                        String collectMsg = this.plugin.getMessagesConfig().getString("admin.order-removed-collect");
                        if (collectMsg == null) collectMsg = " §7Use §e/order §7-> §eYour Orders §7to collect your items.";
                        playerMsgStr = playerMsgStr + collectMsg;
                    }
                    
                    Component playerMsg = Utils.color(playerMsgStr);

                    Player onlineTarget = target.getPlayer();
                    if (onlineTarget != null && onlineTarget.isOnline()) {
                        onlineTarget.sendMessage(playerMsg);
                    } else {
                        // PendingMessageManager takes String for storage, so we convert back or change it to take Component
                        // For now let's keep it as String for the config storage
                        this.plugin.getPendingMessageManager().addMessage(target.getUniqueId(), playerMsgStr);
                    }

                    return true;
                }
            }
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(this.plugin.getMessage("admin.help-header", "§6§l=== DonutOrders Admin ==="));
        sender.sendMessage(this.plugin.getMessage("admin.help-reload", "§e/donutordersadmin reload §7- Reload config"));
        sender.sendMessage(this.plugin.getMessage("admin.help-removeorder", "§e/donutordersadmin removeorder <player> <order_id> §7- Remove an order"));
    }

    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        ArrayList<String> suggestions = new ArrayList<>();
        if (!sender.hasPermission("donutorders.admin")) {
            return suggestions;
        } else {
            if (args.length == 1) {
                suggestions.add("reload");
                suggestions.add("removeorder");
            } else if (args.length == 2 && args[0].equalsIgnoreCase("removeorder")) {
                for (UUID uuid : this.plugin.getOrderManager().getActiveOrders().stream().filter(o -> !o.isCompleted()).map(Order::getOwnerId).distinct().collect(Collectors.toList())) {
                    OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
                    if (op.getName() != null) {
                        suggestions.add(op.getName());
                    }
                }
            } else if (args.length == 3 && args[0].equalsIgnoreCase("removeorder")) {
                String name = args[1];
                OfflinePlayer target = Bukkit.getOfflinePlayer(name);
                if (target.hasPlayedBefore() || target.isOnline()) {
                    for (Order o : this.plugin.getOrderManager().getActiveOrders().stream().filter(o -> o.getOwnerId().equals(target.getUniqueId()) && !o.isCompleted()).collect(Collectors.toList())) {
                        String shortId = o.getOrderId().toString().substring(0, 8);
                        String ref = o.getMaterial().name() + "_x" + o.getNeededAmount() + "_" + shortId;
                        suggestions.add(ref);
                    }
                }
            }

            String current = args[args.length - 1].toLowerCase();
            return suggestions.stream().filter(s -> s.toLowerCase().startsWith(current)).collect(Collectors.toList());
        }
    }
}