package github.vanes430.orderplugin.task;

import github.vanes430.orderplugin.OrderPlugin;
import github.vanes430.orderplugin.manager.OrderManager;
import github.vanes430.orderplugin.model.Order;
import github.vanes430.orderplugin.utils.Utils;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class DeliveryAnimationTask {
    private final OrderPlugin plugin;
    private final Player player;
    private final Order order;
    private final List<ItemStack> items;
    private final Double payment;
    private final List<ItemStack> shulkers;
    private int step = 0;
    private final String[] anim = new String[]{"Delivering.", "Delivering..", "Delivering..."};

    public DeliveryAnimationTask(OrderPlugin plugin, Player player, Order order, List<ItemStack> items, Double payment, List<ItemStack> shulkers) {
        this.plugin = plugin;
        this.player = player;
        this.order = order;
        this.items = items;
        this.payment = payment;
        this.shulkers = shulkers;
    }

    public void run() {
        player.getScheduler().runAtFixedRate(this.plugin, (task) -> {
            if (step < 6) {
                player.sendActionBar(Utils.color("&7&o" + anim[step % 3]));
                step++;
            } else {
                task.cancel();
                if (shulkers != null) {
                    for (ItemStack s : shulkers) player.getInventory().addItem(s);
                    player.sendMessage(Utils.color("&a[Order] " + shulkers.size() + " shulker box(es) returned!"));
                }
                if (order != null && items != null) {
                    int totalAmount = items.stream().mapToInt(ItemStack::getAmount).sum();
                    this.plugin.getOrderManager().fulfillOrder(player, order, totalAmount, payment, items);
                }
            }
        }, null, 1L, 10L);
    }
}