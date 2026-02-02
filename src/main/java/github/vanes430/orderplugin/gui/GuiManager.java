package github.vanes430.orderplugin.gui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;
import github.vanes430.orderplugin.OrderPlugin;
import github.vanes430.orderplugin.model.Order;
import github.vanes430.orderplugin.model.SortType;
import github.vanes430.orderplugin.model.FilterType;
import github.vanes430.orderplugin.utils.ItemBuilder;
import github.vanes430.orderplugin.utils.Utils;

public class GuiManager {
    private final OrderPlugin plugin;
    public final Map<UUID, Material> selectedMaterial = new ConcurrentHashMap<>();
    public final Map<UUID, Integer> selectedAmount = new ConcurrentHashMap<>();
    public final Map<UUID, Double> selectedPrice = new ConcurrentHashMap<>();
    public final Map<UUID, String> selectedPotionType = new ConcurrentHashMap<>();
    public final Map<UUID, String> selectedEnchantmentType = new ConcurrentHashMap<>();
    public final Map<UUID, Set<String>> selectedEnchantments = new ConcurrentHashMap<>();
    public final Map<UUID, SortType> playerSort = new ConcurrentHashMap<>();
    public final Map<UUID, FilterType> playerFilter = new ConcurrentHashMap<>();
    public final Map<UUID, String> playerSearch = new ConcurrentHashMap<>();
    
    // Session state maps moved from MenuListener
    public final Map<UUID, Integer> menuPage = new ConcurrentHashMap<>();
    public final Map<UUID, Integer> itemSelectorPage = new ConcurrentHashMap<>();
    public final Map<UUID, Integer> collectionPage = new ConcurrentHashMap<>();
    public final Map<UUID, Integer> enchantmentPickerPage = new ConcurrentHashMap<>();
    public final Map<UUID, String> chatInputMode = new ConcurrentHashMap<>();
    public final Map<UUID, String> itemSearchQuery = new ConcurrentHashMap<>();
    public final Map<UUID, Order> activeDeliverOrder = new ConcurrentHashMap<>();
    public final Map<UUID, List<ItemStack>> deliveryItems = new ConcurrentHashMap<>();
    public final Map<UUID, Double> deliveryPayment = new ConcurrentHashMap<>();
    public final Map<UUID, List<ItemStack>> deliveryShulkers = new ConcurrentHashMap<>();
    public final Map<UUID, Order> activeEditOrder = new ConcurrentHashMap<>();
    public final Map<UUID, Order> activeCollectOrder = new ConcurrentHashMap<>();
    public final Map<UUID, Order> activeConfirmSellOrder = new ConcurrentHashMap<>();
    public final Map<UUID, List<ItemStack>> sellItems = new ConcurrentHashMap<>();
    public final Map<UUID, Double> sellPrice = new ConcurrentHashMap<>();
    
    public List<ItemStack> itemRegistry = null;

    public GuiManager(OrderPlugin plugin) {
        this.plugin = plugin;
        this.initRegistry();
    }

    private Component getMsg(String key, String def) {
        return this.plugin.getMessage(key, def);
    }

    private void initRegistry() {
        ArrayList<ItemStack> items = new ArrayList<>();
        Arrays.stream(Material.values())
                .filter(Material::isItem)
                .filter(m -> !m.name().startsWith("LEGACY_"))
                .filter(m -> m != Material.AIR)
                .filter(m -> m != Material.POTION && m != Material.SPLASH_POTION && m != Material.LINGERING_POTION && m != Material.TIPPED_ARROW)
                .filter(m -> m != Material.ENCHANTED_BOOK)
                .filter(this::isAllowed)
                .sorted(Comparator.comparing(Enum::name))
                .forEach(m -> items.add(new ItemStack(m)));
        this.addPotions(items);
        this.addEnchantedBooks(items);
        this.itemRegistry = items;
    }

    private void addPotions(List<ItemStack> list) {
        Material[] types = new Material[]{Material.POTION, Material.SPLASH_POTION, Material.LINGERING_POTION, Material.TIPPED_ARROW};
        for (Material mat : types) {
            for (PotionType type : PotionType.values()) {
                try {
                    ItemStack item = new ItemStack(mat);
                    PotionMeta meta = (PotionMeta) item.getItemMeta();
                    meta.setBasePotionType(type);
                    String name = this.formatPotionName(mat, type);
                    meta.displayName(Utils.color("§f" + name).decoration(TextDecoration.ITALIC, false));
                    item.setItemMeta(meta);
                    list.add(item);
                } catch (Exception e) {
                }
            }
        }
    }

    private void addEnchantedBooks(List<ItemStack> list) {
        for (Enchantment ench : Enchantment.values()) {
            for (int level = 1; level <= ench.getMaxLevel(); level++) {
                try {
                    ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);
                    EnchantmentStorageMeta meta = (EnchantmentStorageMeta) book.getItemMeta();
                    meta.addStoredEnchant(ench, level, true);
                    meta.displayName(Utils.color("§bEnchanted Book").decoration(TextDecoration.ITALIC, false));
                    book.setItemMeta(meta);
                    list.add(book);
                } catch (Exception e) {
                }
            }
        }
    }

    private String formatPotionName(Material mat, PotionType type) {
        String prefix = "";
        if (mat == Material.SPLASH_POTION) prefix = "Splash ";
        else if (mat == Material.LINGERING_POTION) prefix = "Lingering ";
        else if (mat == Material.TIPPED_ARROW) prefix = "Arrow of ";

        String typeName = type.name();
        String suffix = "";
        String baseName;
        if (typeName.startsWith("LONG_")) {
            baseName = Utils.formatPotionName(typeName.substring(5));
            if (mat != Material.TIPPED_ARROW) suffix = " (Long)"; // Arrows don't usually have (Long) in name
        } else if (typeName.startsWith("STRONG_")) {
            baseName = Utils.formatPotionName(typeName.substring(7));
            if (mat != Material.TIPPED_ARROW) suffix = " II"; // Arrows don't usually have II in name
        } else {
            baseName = Utils.formatPotionName(typeName);
        }
        
        if (mat == Material.TIPPED_ARROW) {
             return prefix + baseName;
        }
        return prefix + "Potion of " + baseName + suffix;
    }

    private boolean isAllowed(Material m) {
        String name = m.name();
        
        // Check config blacklist
        List<String> blacklist = this.plugin.getConfig().getStringList("orders.blacklist");
        if (blacklist.contains(name)) return false;
        
        if (name.contains("COMMAND_BLOCK") || name.contains("SPAWN_EGG")) return false;
        return !name.equals("STRUCTURE_BLOCK") && !name.equals("STRUCTURE_VOID") && !name.equals("JIGSAW") && 
               !name.equals("BARRIER") && !name.equals("LIGHT") && !name.equals("DEBUG_STICK") && 
               !name.equals("BEDROCK") && !name.equals("KNOWLEDGE_BOOK") && !name.equals("SPAWNER") && 
               !name.equals("PETRIFIED_OAK_SLAB") && !name.equals("END_PORTAL_FRAME") && !name.startsWith("INFESTED_") && 
               !name.equals("BUDDING_AMETHYST") && !name.equals("REINFORCED_DEEPSLATE") && !name.equals("FROGSPAWN") && 
               !name.equals("TRIAL_SPAWNER") && !name.equals("VAULT") && !name.contains("OMINOUS_TRIAL") && !name.contains("OMINOUS_BOTTLE");
    }

    public boolean isEnchantable(Material m) {
        if (m == null) return false;
        String name = m.name();
        if (m == Material.ENCHANTED_BOOK || name.contains("POTION")) return false;
        return name.endsWith("_SWORD") || name.endsWith("_HELMET") || name.endsWith("_CHESTPLATE") || 
               name.endsWith("_LEGGINGS") || name.endsWith("_BOOTS") || name.endsWith("_PICKAXE") || 
               name.endsWith("_AXE") || name.endsWith("_SHOVEL") || name.endsWith("_HOE") || 
               m == Material.BOW || m == Material.CROSSBOW || m == Material.TRIDENT || m == Material.FISHING_ROD || 
               m == Material.SHEARS || m == Material.FLINT_AND_STEEL || m == Material.SHIELD || m == Material.ELYTRA || 
               m == Material.CARROT_ON_A_STICK || m == Material.WARPED_FUNGUS_ON_A_STICK || m == Material.BRUSH || m == Material.MACE;
    }

    public List<Enchantment> getCompatibleEnchantments(Material m) {
        ArrayList<Enchantment> list = new ArrayList<>();
        ItemStack stack = new ItemStack(m);
        for (Enchantment ench : Enchantment.values()) {
            if (ench.canEnchantItem(stack)) list.add(ench);
        }
        list.sort(Comparator.comparing(e -> e.getKey().getKey()));
        return list;
    }

    public String getDisplayItemName(Order order) {
        if (order.isPotion() && order.getPotionType() != null) {
            try {
                PotionType type = PotionType.valueOf(order.getPotionType());
                return this.formatPotionName(order.getMaterial(), type);
            } catch (Exception e) {
                return Utils.formatMaterialName(order.getMaterial().name());
            }
        } else if (order.isEnchantedBook() && order.getEnchantmentType() != null) {
            return Utils.formatEnchantedBook(order.getEnchantmentType());
        } else {
            return order.getEnchantmentType() != null && !order.getEnchantmentType().isEmpty()
                    ? Utils.formatMaterialName(order.getMaterial().name()) + " (Enchanted)"
                    : Utils.formatMaterialName(order.getMaterial().name());
        }
    }

    public ItemStack createDisplayItem(Order order) {
        ItemStack item = new ItemStack(order.getMaterial());
        if (order.isPotion() && order.getPotionType() != null) {
            try {
                PotionMeta meta = (PotionMeta) item.getItemMeta();
                PotionType type = PotionType.valueOf(order.getPotionType());
                meta.setBasePotionType(type);
                item.setItemMeta(meta);
            } catch (Exception e) {
            }
        }
        if (order.isEnchantedBook() && order.getEnchantmentType() != null) {
            try {
                EnchantmentStorageMeta meta = (EnchantmentStorageMeta) item.getItemMeta();
                String[] parts = order.getEnchantmentType().split("_");
                if (parts.length >= 2) {
                    String enchName = parts[0];
                    for (int i = 1; i < parts.length - 1; i++) enchName = enchName + "_" + parts[i];
                    int level = Integer.parseInt(parts[parts.length - 1]);
                    Enchantment ench = Enchantment.getByName(enchName);
                    if (ench != null) meta.addStoredEnchant(ench, level, true);
                }
                item.setItemMeta(meta);
            } catch (Exception e) {
            }
        } else if (order.getEnchantmentType() != null && !order.getEnchantmentType().isEmpty()) {
            try {
                ItemMeta meta = item.getItemMeta();
                String[] enchs = order.getEnchantmentType().split(";");
                for (String enchStr : enchs) {
                    String[] parts = enchStr.split("_");
                    if (parts.length >= 2) {
                        String enchName = parts[0].toLowerCase();
                        for (int i = 1; i < parts.length - 1; i++) enchName = enchName + "_" + parts[i].toLowerCase();
                        int level = Integer.parseInt(parts[parts.length - 1]);
                        Enchantment ench = (Enchantment) Registry.ENCHANTMENT.get(NamespacedKey.minecraft(enchName));
                        if (ench != null) meta.addEnchant(ench, level, true);
                    }
                }
                item.setItemMeta(meta);
            } catch (Exception e) {
            }
        }
        return item;
    }

    public void openMainMenu(Player player, int page, String search) {
        UUID uuid = player.getUniqueId();
        SortType sort = this.playerSort.getOrDefault(uuid, SortType.RECENTLY_LISTED);
        FilterType filter = this.playerFilter.getOrDefault(uuid, FilterType.ALL);
        String titleStr = this.plugin.getConfig().getString("gui-titles.main-menu", "ᴏʀᴅᴇʀꜱ (Page %page%)").replace("%page%", String.valueOf(page));
        
        List<Order> activeOrders = this.plugin.getOrderManager().getActiveOrders();
        int pageSize = 45;
        int start = (page - 1) * pageSize;
        List<Order> filtered = activeOrders.stream()
                .filter(o -> !o.isCompleted())
                .filter(o -> filter.matches(o.getMaterial()))
                .filter(o -> search == null || o.getMaterial().name().toLowerCase().contains(search.toLowerCase()))
                .collect(Collectors.toList());

        switch (sort) {
            case MOST_PAID: filtered.sort((o1, o2) -> Double.compare(o2.getFilledAmount() * o2.getPricePerItem(), o1.getFilledAmount() * o1.getPricePerItem())); break;
            case MOST_DELIVERED: filtered.sort((o1, o2) -> Integer.compare(o2.getFilledAmount(), o1.getFilledAmount())); break;
            case RECENTLY_LISTED: filtered.sort((o1, o2) -> Long.compare(o2.getCreatedAt(), o1.getCreatedAt())); break;
            case MOST_MONEY_PER_ITEM: filtered.sort((o1, o2) -> Double.compare(o2.getPricePerItem(), o1.getPricePerItem())); break;
        }

        player.getScheduler().run(this.plugin, (task) -> {
            Inventory inv = Bukkit.createInventory(null, 54, Utils.color(titleStr));
            int slot = 0;
            for (int i = start; i < filtered.size() && slot < pageSize; i++) {
                Order order = filtered.get(i);
                OfflinePlayer owner = Bukkit.getOfflinePlayer(order.getOwnerId());
                String ownerName = owner.getName() != null ? owner.getName() : "Unknown";
                String filledStr = Utils.formatNumber((double) order.getFilledAmount());
                String neededStr = Utils.formatNumber((double) order.getNeededAmount());
                String paidStr = Utils.formatNumber(order.getFilledAmount() * order.getPricePerItem());
                String totalPaidStr = Utils.formatNumber(order.getNeededAmount() * order.getPricePerItem());
                String timeStr = Utils.formatTime(order.getExpiresAt());
                
                ArrayList<Component> lore = new ArrayList<>();
                String formattedName = Utils.formatMaterialName(order.getMaterial().name());
                if (order.isPotion() && order.getPotionType() != null) {
                    try {
                        PotionType type = PotionType.valueOf(order.getPotionType());
                        formattedName = this.formatPotionName(order.getMaterial(), type);
                    } catch (Exception e) {}
                }
                lore.add(Utils.color("§f" + formattedName));
                lore.add(Utils.color("&#00d271$" + Utils.formatNumber(order.getPricePerItem()) + " §f").append(this.getMsg("gui.lore.each", "each")));
                lore.add(Component.empty());
                lore.add(Utils.color("§e" + filledStr + "§8/&#00d271" + neededStr + " §8").append(this.getMsg("gui.lore.delivered", "Delivered")));
                lore.add(Utils.color("§e$" + paidStr + "§8/&#00d271$" + totalPaidStr + " §8").append(this.getMsg("gui.lore.paid", "Paid")));
                lore.add(Component.empty());
                lore.add(this.getMsg("gui.lore.click-to-deliver", "§fClick to deliver &#00d271%player% §f%item%", "%player%", ownerName, "%item%", formattedName));
                lore.add(this.getMsg("gui.lore.until-order-expires", "§7%time% Until Order expires", "%time%", timeStr));
                
                inv.setItem(slot++, new ItemBuilder(this.createDisplayItem(order))
                        .name(this.getMsg("gui.lore.owners-order", "§a%player%'s Order", "%player%", ownerName))
                        .amount(1).lore(lore).hideFlags().build());
            }
            List<String> sortLore = new ArrayList<>();
            for (SortType s : SortType.values()) {
                sortLore.add((s == sort ? "§a• " : "§7• ") + s.getDisplayName());
            }
            inv.setItem(47, new ItemBuilder(Material.CAULDRON).name(this.getMsg("gui.buttons.sort", "§5ꜱᴏʀᴛ")).lore(sortLore.toArray(new String[0])).build());

            List<String> filterLore = new ArrayList<>();
            for (FilterType f : FilterType.values()) {
                filterLore.add((f == filter ? "§a• " : "§7• ") + f.getDisplayName());
            }
            inv.setItem(48, new ItemBuilder(Material.HOPPER).name(this.getMsg("gui.buttons.filter", "§5ꜰɪʟᴛᴇʀ")).lore(filterLore.toArray(new String[0])).build());
            inv.setItem(49, new ItemBuilder(Material.PAPER).name(this.getMsg("gui.buttons.orders", "§aᴏʀᴅᴇʀꜱ")).lore(this.getMsg("gui.lore.click-to-refresh", "§fClick to refresh")).build());
            inv.setItem(50, new ItemBuilder(Material.OAK_SIGN).name(this.getMsg("gui.buttons.search", "§eꜱᴇᴀʀᴄʜ")).lore("§fCurrent: §b" + (search == null ? "None" : search)).build());
            inv.setItem(51, new ItemBuilder(Material.CHEST).name(this.getMsg("gui.buttons.your-orders", "§aʏᴏᴜʀ ᴏʀᴅᴇʀꜱ")).lore(this.getMsg("gui.lore.manage-listings", "§fManage your listings")).build());
            if (filtered.size() > page * pageSize) inv.setItem(53, new ItemBuilder(Material.ARROW).name(this.getMsg("gui.buttons.next-page", "§aɴᴇxᴛ ᴘᴀɢᴇ")).build());
            player.openInventory(inv);
        }, null);
    }

    public void openMainMenu(Player player) {
        this.openMainMenu(player, 1, null);
    }

    public void openMainMenu(Player player, String search) {
        this.openMainMenu(player, 1, search);
    }

    public void openYourOrders(Player player, int page) {
        String titleStr = this.plugin.getConfig().getString("gui-titles.your-orders", "ᴏʀᴅᴇʀꜱ -> ʏᴏᴜʀ ᴏʀᴅᴇʀꜱ");
        List<Order> owned = this.plugin.getOrderManager().getOrdersByOwner(player.getUniqueId());
        
        final int pageSize = 45;
        final int totalPages = Math.max(1, (int) Math.ceil((double) owned.size() / pageSize));
        final int finalPage = Math.max(1, Math.min(page, totalPages));
        
        player.getScheduler().run(this.plugin, (task) -> {
            Inventory inv = Bukkit.createInventory(null, 54, Utils.color(titleStr + (totalPages > 1 ? " (" + finalPage + "/" + totalPages + ")" : "")));
            int start = (finalPage - 1) * pageSize;
            int slot = 0;
            
            for (int i = start; i < owned.size() && slot < pageSize; i++) {
                Order order = owned.get(i);
                if (!order.isRemovedByAdmin() || order.hasItemsToCollect()) {
                    String itemName = this.getDisplayItemName(order);
                    ArrayList<Component> lore = new ArrayList<>();
                    
                    if (order.isRemovedByAdmin()) {
                        lore.add(this.getMsg("gui.lore.removed-by-admin", "&c&lREMOVED BY ADMIN"));
                    }
                    
                    lore.add(Utils.color("§7ID: #" + order.getOrderId().toString().substring(0, 8)));
                    lore.add(this.getMsg("gui.lore.progress", "§fProgress: %filled%/%total%", 
                            "%filled%", Utils.formatNumber((double)order.getFilledAmount()), 
                            "%total%", Utils.formatNumber((double)order.getNeededAmount())));
                    lore.add(this.getMsg("gui.lore.price-label", "§fPrice: $%price%", 
                            "%price%", Utils.formatNumber(order.getPricePerItem())));
                    
                    if (order.hasItemsToCollect()) {
                        lore.add(this.getMsg("gui.lore.items-waiting", "§fItems waiting: &6%amount%", 
                                "%amount%", String.valueOf(order.getAllCollectedItems().size())));
                    }
                    
                    if (!order.isCompleted() && !order.isRemovedByAdmin()) {
                        lore.add(this.getMsg("gui.lore.until-order-expires", "§7%time% Until Order expires", 
                                "%time%", Utils.formatTime(order.getExpiresAt())));
                    }

                    lore.add(Component.empty());
                    lore.add(Utils.color("&eClick to manage this order"));
                    
                    inv.setItem(slot++, new ItemBuilder(this.createDisplayItem(order))
                            .name(Utils.color("&#00d271" + itemName))
                            .lore(lore)
                            .build());
                }
            }
            
            // Bottom Bar
            ItemStack glass = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(" ").build();
            for (int i = 45; i < 54; i++) inv.setItem(i, glass);
            
            if (finalPage > 1) inv.setItem(48, new ItemBuilder(Material.ARROW).name(this.getMsg("gui.buttons.previous-page", "§eᴘʀᴇᴠɪᴏᴜꜱ ᴘᴀɢᴇ")).build());
            inv.setItem(49, new ItemBuilder(Material.BARRIER).name(this.getMsg("gui.buttons.go-back", "§cɢᴏ ʙᴀᴄᴋ")).build());
            inv.setItem(50, new ItemBuilder(Material.PAPER).name(this.getMsg("gui.buttons.new-order", "§aɴᴇᴡ ᴏʀᴅᴇʀ")).lore(this.getMsg("gui.lore.click-to-create", "§fClick to create a new buy order")).build());
            if (finalPage < totalPages) inv.setItem(51, new ItemBuilder(Material.ARROW).name(this.getMsg("gui.buttons.next-page", "§aɴᴇxᴛ ᴘᴀɢᴇ")).build());
            
            player.openInventory(inv);
        }, null);
    }

    public void openYourOrders(Player player) {
        this.openYourOrders(player, 1);
    }

    public void openNewOrderEditor(Player player) {
        UUID uuid = player.getUniqueId();
        String titleStr = this.plugin.getConfig().getString("gui-titles.new-order", "ᴏʀᴅᴇʀꜱ -> ɴᴇᴡ ᴏʀᴅᴇʀ");
        Material mat = this.selectedMaterial.getOrDefault(uuid, Material.STONE);
        
        player.getScheduler().run(this.plugin, (task) -> {
            Inventory inv = Bukkit.createInventory(null, 27, Utils.color(titleStr));
            inv.setItem(10, new ItemBuilder(Material.RED_STAINED_GLASS_PANE).name(this.getMsg("gui.buttons.cancel", "§cᴄᴀɴᴄᴇʟ")).build());
            inv.setItem(12, new ItemBuilder(mat).name(this.getMsg("gui.buttons.item", "§eɪᴛᴇᴍ")).build());
            inv.setItem(13, new ItemBuilder(Material.CHEST).name(this.getMsg("gui.buttons.amount", "§aᴀᴍᴏᴜɴᴛ")).lore("§fAmount: " + this.selectedAmount.getOrDefault(uuid, 1)).build());
            inv.setItem(14, new ItemBuilder(Material.EMERALD).name(this.getMsg("gui.buttons.price", "§bᴘʀɪᴄᴇ")).lore("§fPrice: " + this.selectedPrice.getOrDefault(uuid, 1.0)).build());
            inv.setItem(16, new ItemBuilder(Material.LIME_STAINED_GLASS_PANE).name(this.getMsg("gui.buttons.confirm", "§aᴄᴏɴꜰɪʀᴍ")).build());
            player.openInventory(inv);
        }, null);
    }

    public void openConfirmDelivery(Player player, Order order, int amount, double payment) {
        String titleStr = this.plugin.getConfig().getString("gui-titles.confirm-delivery", "ᴏʀᴅᴇʀꜱ -> ᴄᴏɴꜰɪʀᴍ ᴅᴇʟɪᴠᴇʀʏ");
        player.getScheduler().run(this.plugin, (task) -> {
            Inventory inv = Bukkit.createInventory(null, 27, Utils.color(titleStr));
            inv.setItem(11, new ItemBuilder(Material.RED_STAINED_GLASS_PANE).name(this.getMsg("gui.buttons.cancel", "§cᴄᴀɴᴄᴇʟ")).build());
            inv.setItem(13, new ItemBuilder(this.createDisplayItem(order)).name("§aᴄᴏɴꜰɪʀᴍ ᴅᴇʟɪᴠᴇʀʏ").amount(Math.min(amount, 64)).build());
            inv.setItem(15, new ItemBuilder(Material.LIME_STAINED_GLASS_PANE).name(this.getMsg("gui.buttons.confirm", "§aᴄᴏɴꜰɪʀᴍ")).lore("§aReward: $" + payment).build());
            player.openInventory(inv);
        }, null);
    }

    public void openItemSelector(Player player, int page, String filter) {
        List<ItemStack> registry = this.itemRegistry;
        if (filter != null && !filter.trim().isEmpty()) {
            String upper = filter.toUpperCase().replace(" ", "_");
            registry = registry.stream().filter(i -> {
                if (i.getType().name().contains(upper)) return true;
                if (i.hasItemMeta() && i.getItemMeta().hasDisplayName()) {
                    return Utils.stripColor(LegacyComponentSerializer.legacySection().serialize(i.getItemMeta().displayName())).toUpperCase().contains(upper);
                }
                return false;
            }).collect(Collectors.toList());
        }

        final int pageSize = 45;
        final int totalPages = Math.max(1, (int) Math.ceil((double) registry.size() / pageSize));
        final int finalPage = Math.max(1, Math.min(page, totalPages));
        final String titleStr = this.plugin.getConfig().getString("gui-titles.item-selector", "ꜱᴇʟᴇᴄᴛ ɪᴛᴇᴍ (Page %page%)")
                .replace("%page%", String.valueOf(finalPage));
        
        final List<ItemStack> finalRegistry = registry;
        player.getScheduler().run(this.plugin, (task) -> {
            Inventory inv = Bukkit.createInventory(null, 54, Utils.color(titleStr));
            int start = (finalPage - 1) * pageSize;
            int end = Math.min(start + pageSize, finalRegistry.size());
            int slot = 0;
            for (int i = start; i < end; i++) {
                inv.setItem(slot++, finalRegistry.get(i).clone());
            }

            // Navigation
            ItemStack glass = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(" ").build();
            if (finalPage > 1) inv.setItem(48, new ItemBuilder(Material.ARROW).name(this.getMsg("gui.buttons.previous-page", "§eᴘʀᴇᴠɪᴏᴜꜱ ᴘᴀɢᴇ")).build());
            else inv.setItem(48, glass);

            inv.setItem(49, new ItemBuilder(Material.BARRIER).name(this.getMsg("gui.buttons.back-to-editor", "§cʙᴀᴄᴋ ᴛᴏ ᴇᴅɪᴛᴏʀ")).build());

            if (finalPage < totalPages) inv.setItem(50, new ItemBuilder(Material.ARROW).name(this.getMsg("gui.buttons.next-page", "§aɴᴇxᴛ ᴘᴀɢᴇ")).build());
            else inv.setItem(50, glass);

            inv.setItem(53, new ItemBuilder(Material.OAK_SIGN).name(this.getMsg("gui.buttons.search", "§aꜱᴇᴀʀᴄʜ"))
                    .lore("§7Current: §e" + (filter == null ? "None" : filter), "§7Click to type name in chat").build());

            player.openInventory(inv);
        }, null);
    }

    public void openEditOrderMenu(Player player, Order order) {
        String titleStr = this.plugin.getConfig().getString("gui-titles.edit-order", "ᴏʀᴅᴇʀꜱ -> ᴇᴅɪᴛ ᴏʀᴅᴇʀ");
        player.getScheduler().run(this.plugin, (task) -> {
            Inventory inv = Bukkit.createInventory(null, 27, Utils.color(titleStr));
            
            // Info item
            inv.setItem(4, new ItemBuilder(this.createDisplayItem(order))
                    .name("§aᴏʀᴅᴇʀ ᴅᴇᴛᴀɪʟꜱ")
                    .lore("§7ID: #" + order.getOrderId().toString().substring(0, 8),
                          "§fProgress: " + order.getFilledAmount() + "/" + order.getNeededAmount(),
                          "§fPrice: $" + Utils.formatNumber(order.getPricePerItem()))
                    .build());

            // Actions
            inv.setItem(10, new ItemBuilder(Material.RED_TERRACOTTA).name(this.getMsg("gui.buttons.delete-order", "§cᴅᴇʟᴇᴛᴇ ᴏʀᴅᴇʀ")).lore("§7Click to delete order", "§c(Storage must be empty)").build());
            inv.setItem(12, new ItemBuilder(Material.CHEST).name(this.getMsg("gui.buttons.collect-items", "§aᴄᴏʟʟᴇᴄᴛ ɪᴛᴇᴍꜱ")).lore("§7Click to view and pick items").build());
            inv.setItem(14, new ItemBuilder(Material.HOPPER).name(this.getMsg("gui.buttons.drop-all", "&#00f986ᴅʀᴏᴘ ᴀʟʟ")).lore("§7Click to drop ALL items to inventory/ground").build());
            inv.setItem(16, new ItemBuilder(Material.GOLD_INGOT).name(this.getMsg("gui.buttons.sell-all", "&#00f986ꜱᴇʟʟ ᴀʟʟ")).lore("§7Coming Soon!").build());
            
            inv.setItem(22, new ItemBuilder(Material.ARROW).name("§cɢᴏ ʙᴀᴄᴋ").build());
            player.openInventory(inv);
        }, null);
    }

    public void openCollectItemsMenu(Player player, Order order, int page) {
        final int pageSize = 45;
        // Flatten items into displayable stacks (max 64)
        List<ItemStack> displayItems = new ArrayList<>();
        List<ItemStack> rawItems = order.getAllCollectedItems();
        
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

        final int totalPages = Math.max(1, (int) Math.ceil((double) displayItems.size() / pageSize));
        final int finalPage = Math.max(1, Math.min(page, totalPages));

        player.getScheduler().run(this.plugin, (task) -> {
            Inventory inv = Bukkit.createInventory(null, 54, Utils.color("ORDERS -> Collect Items" + (totalPages > 1 ? " (" + finalPage + "/" + totalPages + ")" : "")));
            
            int start = (finalPage - 1) * pageSize;
            int end = Math.min(start + pageSize, displayItems.size());
            int slot = 0;
            for (int i = start; i < end; i++) {
                inv.setItem(slot++, displayItems.get(i));
            }

            // Bottom bar
            ItemStack glass = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(" ").build();
            for (int i = 45; i < 54; i++) inv.setItem(i, glass);

            // Navigation Arrows
            if (finalPage > 1) {
                inv.setItem(45, new ItemBuilder(Material.ARROW).name(this.getMsg("gui.buttons.collection-back", "&#00f986ʙᴀᴄᴋ")).build());
            }
            
            if (finalPage < totalPages) {
                inv.setItem(53, new ItemBuilder(Material.ARROW).name(this.getMsg("gui.buttons.collection-next", "&#00f986ɴᴇxᴛ")).build());
            }
            
            // Action Buttons
            inv.setItem(48, new ItemBuilder(Material.CHEST).name("&#00f986ᴄᴏʟʟᴇᴄᴛ ᴀʟʟ").lore("§7Collect as many as possible to inventory", "§7(Remaining items stay in order storage)").build());
            inv.setItem(49, new ItemBuilder(Material.BARRIER).name(this.getMsg("gui.buttons.back", "§cʙᴀᴄᴋ")).build());
            inv.setItem(50, new ItemBuilder(Material.HOPPER).name(this.getMsg("gui.buttons.drop-all", "&#00f986ᴅʀᴏᴘ ᴀʟʟ")).lore("§7Drop ALL collected items to ground").build());
            
            player.openInventory(inv);
        }, null);
    }

    public void openConfirmSell(Player player, Order order, List<ItemStack> items, double price) {
        String titleStr = this.plugin.getConfig().getString("gui-titles.confirm-sell", "ᴏʀᴅᴇʀꜱ -> ᴄᴏɴꜰɪʀᴍ ꜱᴇʟʟ");
        player.getScheduler().run(this.plugin, (task) -> {
            Inventory inv = Bukkit.createInventory(null, 27, Utils.color(titleStr));
            inv.setItem(11, new ItemBuilder(Material.RED_STAINED_GLASS_PANE).name("&cᴄᴀɴᴄᴇʟ").build());
            inv.setItem(13, new ItemBuilder(this.createDisplayItem(order)).name("&#00f986ꜱᴇʟʟ ɪᴛᴇᴍꜱ").build());
            inv.setItem(15, new ItemBuilder(Material.LIME_STAINED_GLASS_PANE).name("&aᴄᴏɴꜰɪʀᴍ").lore("&aValue: $" + price).build());
            player.openInventory(inv);
        }, null);
    }

    public void openEnchantmentPicker(Player player, int page) {
        UUID uuid = player.getUniqueId();
        final Material mat = this.selectedMaterial.get(uuid);
        if (mat != null && (mat == Material.ENCHANTED_BOOK || this.isEnchantable(mat))) {
            String titleStr = Utils.stripColor(LegacyComponentSerializer.legacySection().serialize(this.getMsg("gui.enchantment-picker.title", "ᴘɪᴄᴋ ᴇɴᴄʜᴀɴᴛᴍᴇɴᴛꜱ")));
            Set<String> selected = this.selectedEnchantments.getOrDefault(uuid, new HashSet<>());
            
            List<Enchantment> compatible;
            if (mat == Material.ENCHANTED_BOOK) {
                compatible = Arrays.asList(Enchantment.values());
                compatible.sort(Comparator.comparing(e -> e.getKey().getKey()));
            } else {
                compatible = this.getCompatibleEnchantments(mat);
            }
            
            ArrayList<ItemStack> books = new ArrayList<>();

            for (Enchantment ench : compatible) {
                for (int level = 1; level <= ench.getMaxLevel(); level++) {
                    // Simplified: No selection check needed
                    ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);
                    ItemMeta bookMeta = book.getItemMeta();
                    String beautyName = Utils.capitalize(ench.getKey().getKey()) + " " + Utils.toRoman(level);
                    final String finalBeautyName = beautyName;
                    
                    bookMeta.displayName(Utils.color("§e" + finalBeautyName));
                    bookMeta.lore(Arrays.asList(this.getMsg("gui.enchantment-picker.click-to-select", "§7Click to select")));
                    
                    book.setItemMeta(bookMeta);
                    books.add(book);
                }
            }

            final int[] slots = new int[]{
                    2, 3, 4, 5, 6, 7, 8, 11, 12, 13, 14, 15, 16, 17, 20, 21, 22, 23, 24, 25, 26, 29, 30, 31, 32, 33, 34, 35, 38, 39, 40, 41, 42, 43, 44
            };
            final int pageSize = slots.length;
            final int totalPages = (int) Math.ceil((double) books.size() / pageSize);
            
            player.getScheduler().run(this.plugin, (task) -> {
                Inventory inv = Bukkit.createInventory(null, 54, Utils.color(titleStr));
                ItemStack display = new ItemStack(mat);
                inv.setItem(0, new ItemBuilder(display).name("§b" + Utils.formatMaterialName(mat.name())).build());
                ItemStack glass = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(" ").build();
                inv.setItem(1, glass);
                inv.setItem(9, glass);
                inv.setItem(10, glass);

                int start = (page - 1) * pageSize;
                int currentSlot = 0;
                for (int i = start; i < Math.min(start + pageSize, books.size()) && currentSlot < slots.length; i++) {
                    inv.setItem(slots[currentSlot++], books.get(i));
                }
                for (int i = 45; i <= 53; i++) inv.setItem(i, glass);
                if (page > 1) inv.setItem(45, new ItemBuilder(Material.ARROW).name(this.getMsg("gui.enchantment-picker.back", "&#00f986ʙᴀᴄᴋ")).build());
                inv.setItem(46, new ItemBuilder(Material.RED_STAINED_GLASS_PANE).name(this.getMsg("gui.enchantment-picker.cancel", "&#00f986ᴄᴀɴᴄᴇʟ")).build());
                // Confirm button removed
                if (page < totalPages) inv.setItem(53, new ItemBuilder(Material.ARROW).name(this.getMsg("gui.enchantment-picker.next", "&#00f986ɴᴇxᴛ")).build());
                player.openInventory(inv);
            }, null);
        } else {
            this.openNewOrderEditor(player);
        }
    }

    public void openDeliverItemsInventory(Player player, Order order) {
        String titleStr = this.plugin.getConfig().getString("gui-titles.deliver-items", "ᴏʀᴅᴇʀꜱ -> ᴅᴇʟɪᴠᴇʀ ɪᴛᴇᴍꜱ");
        player.getScheduler().run(this.plugin, (task) -> {
            Inventory inv = Bukkit.createInventory(null, 27, Utils.color(titleStr));
            player.openInventory(inv);
        }, null);
    }

    private Component getMsg(String key, String def, String... args) {
        return this.plugin.getMessage(key, def, args);
    }
}
