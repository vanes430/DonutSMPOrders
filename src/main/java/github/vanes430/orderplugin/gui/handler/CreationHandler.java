package github.vanes430.orderplugin.gui.handler;

import github.vanes430.orderplugin.OrderPlugin;
import github.vanes430.orderplugin.gui.GuiManager;
import github.vanes430.orderplugin.utils.Utils;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class CreationHandler implements MenuHandler {
    private final OrderPlugin plugin;
    private final GuiManager guiManager;

    public CreationHandler(OrderPlugin plugin) {
        this.plugin = plugin;
        this.guiManager = plugin.getGuiManager();
    }

    private int parseAmount(String input) throws NumberFormatException {
        if (input != null && !input.isEmpty()) {
            input = input.trim().toLowerCase().replace(",", "").replace(" ", "");
            double multiplier = 1.0;
            String numericPart = input;
            if (input.endsWith("k")) {
                multiplier = 1000.0;
                numericPart = input.substring(0, input.length() - 1);
            } else if (input.endsWith("m")) {
                multiplier = 1000000.0;
                numericPart = input.substring(0, input.length() - 1);
            } else if (input.endsWith("b")) {
                multiplier = 1.0E9;
                numericPart = input.substring(0, input.length() - 1);
            }

            double value = Double.parseDouble(numericPart) * multiplier;
            if (!(value > 2.147483647E9) && !(value < 0.0)) {
                return (int)value;
            } else {
                throw new NumberFormatException("Number too large");
            }
        } else {
            throw new NumberFormatException("Empty input");
        }
    }

    @Override
    public void onChat(AsyncPlayerChatEvent event) {
        final Player player = event.getPlayer();
        final UUID uuid = player.getUniqueId();
        
        if (this.guiManager.chatInputMode.containsKey(uuid)) {
            event.setCancelled(true);
            final String message = event.getMessage();
            final String mode = this.guiManager.chatInputMode.remove(uuid);

            player.getScheduler().run(this.plugin, (task) -> {
                if (mode.equals("AMOUNT")) {
                    try {
                        int amount = this.parseAmount(message);
                        if (amount > 0) {
                            this.guiManager.selectedAmount.put(uuid, amount);
                            player.sendMessage(
                                    this.plugin.getMessage("success.amount-set", "{prefix}&aAmount set to: %amount%", "%amount%", Utils.formatNumber((double)amount))
                            );
                        } else {
                            player.sendMessage(this.plugin.getMessage("errors.positive-only", "{prefix}&cPositive numbers only."));
                        }
                        this.guiManager.openNewOrderEditor(player);
                    } catch (NumberFormatException e) {
                        player.sendMessage(this.plugin.getMessage("errors.invalid-number-format", "{prefix}&cInvalid number. Use format: 100, 1k, 10k, 1m"));
                        this.guiManager.openNewOrderEditor(player);
                    }
                } else if (mode.equals("PRICE")) {
                    try {
                        double price = Double.parseDouble(message);
                        double minPrice = this.plugin.getConfig().getDouble("orders.min-price-per-item", 0.01);
                        if (price >= minPrice) {
                            this.guiManager.selectedPrice.put(uuid, price);
                            player.sendMessage(
                                    this.plugin.getMessage("success.price-set", "{prefix}&aPrice set to: $%price%", "%price%", Utils.formatNumber(price))
                            );
                        } else {
                            player.sendMessage(
                                    this.plugin.getMessage("errors.min-price-not-met", "{prefix}&cMinimum price is &e%price%", "%price%", String.valueOf(minPrice))
                            );
                        }
                        this.guiManager.openNewOrderEditor(player);
                    } catch (NumberFormatException e) {
                        player.sendMessage(this.plugin.getMessage("errors.invalid-number", "{prefix}&cInvalid number."));
                        this.guiManager.openNewOrderEditor(player);
                    }
                } else if (mode.equals("SEARCH_ITEM")) {
                    this.guiManager.itemSearchQuery.put(uuid, message);
                    this.guiManager.itemSelectorPage.put(uuid, 1);
                    player.sendMessage(this.plugin.getMessage("success.filter-applied", "{prefix}&aFiltering items for: &e%search%", "%search%", message));
                    this.guiManager.openItemSelector(player, 1, message);
                }
            }, null);
        }
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        final Player player = (Player)event.getWhoClicked();
        final UUID uuid = player.getUniqueId();
        ItemStack currentItem = event.getCurrentItem();
        String titleStr = LegacyComponentSerializer.legacySection().serialize(event.getView().title());
        
        String newOrderTitle = this.plugin.getConfig().getString("gui-titles.new-order", "ORDERS -> New Order");
        String selectorTitle = this.plugin.getConfig().getString("gui-titles.item-selector", "Select Item");
        String enchantTitleRaw = "ᴘɪᴄᴋ ᴇɴᴄʜᴀɴᴛᴍᴇɴᴛꜱ";
        String enchantTitleMsg = this.plugin.getMessagesConfig().getString("gui.enchantment-picker.title", enchantTitleRaw);
        String enchantTitleStr = Utils.stripColor(LegacyComponentSerializer.legacySection().serialize(Utils.color(enchantTitleMsg)));

        boolean isSelector = titleStr.contains(selectorTitle.split("%")[0]) || titleStr.startsWith("Filter:");
        boolean isPotionSelector = titleStr.equals("Select Potion Type"); // Kept for safety if invoked
        boolean isEnchantPicker = titleStr.equals(enchantTitleStr);

        event.setCancelled(true);
        
        if (isEnchantPicker) {
            if (currentItem != null && currentItem.getType() != Material.AIR) {
                int slot = event.getSlot();
                int page = this.guiManager.enchantmentPickerPage.getOrDefault(uuid, 1);
                Material mat = this.guiManager.selectedMaterial.get(uuid);
                
                if (mat == null) {
                    player.closeInventory();
                    return;
                }

                // Recalculate options to find what was clicked
                Set<String> selected = this.guiManager.selectedEnchantments.getOrDefault(uuid, new HashSet<>());
                List<Enchantment> compatible;
                if (mat == Material.ENCHANTED_BOOK) {
                    compatible = Arrays.asList(Enchantment.values());
                    compatible.sort(Comparator.comparing(e -> e.getKey().getKey()));
                } else {
                    compatible = this.guiManager.getCompatibleEnchantments(mat);
                }
                
                ArrayList<String> allOptions = new ArrayList<>();
                for (Enchantment ench : compatible) {
                    for (int i = 1; i <= ench.getMaxLevel(); i++) {
                        allOptions.add(ench.getKey().getKey().toUpperCase() + "_" + i);
                    }
                }

                int[] slots = new int[]{
                        2, 3, 4, 5, 6, 7, 8, 11, 12, 13, 14, 15, 16, 17, 20, 21, 22, 23, 24, 25, 26, 29, 30, 31, 32, 33, 34, 35, 38, 39, 40, 41, 42, 43, 44
                };
                int pageSize = slots.length;
                int maxPages = (int)Math.ceil((double)allOptions.size() / pageSize);
                if (maxPages == 0) maxPages = 1;

                int relativeSlot = -1;
                for (int i = 0; i < slots.length; i++) {
                    if (slots[i] == slot) {
                        relativeSlot = i;
                        break;
                    }
                }

                if (relativeSlot >= 0) {
                    int index = (page - 1) * pageSize + relativeSlot;
                    if (index < allOptions.size()) {
                        String key = allOptions.get(index);
                        // Single selection logic: Set one and finish
                        this.guiManager.selectedEnchantmentType.put(uuid, key);
                        this.guiManager.selectedEnchantments.remove(uuid);
                        this.guiManager.enchantmentPickerPage.remove(uuid);
                        this.guiManager.openNewOrderEditor(player);
                    }
                } else if (slot == 45 && page > 1) {
                    this.guiManager.enchantmentPickerPage.put(uuid, page - 1);
                    this.guiManager.openEnchantmentPicker(player, page - 1);
                } else if (slot == 46) { // Cancel
                    this.guiManager.selectedMaterial.remove(uuid);
                    this.guiManager.selectedEnchantments.remove(uuid);
                    this.guiManager.enchantmentPickerPage.remove(uuid);
                    this.guiManager.itemSelectorPage.put(uuid, 1);
                    this.guiManager.openItemSelector(player, 1, null);
                } else if (slot == 53 && page < maxPages) {
                    this.guiManager.enchantmentPickerPage.put(uuid, page + 1);
                    this.guiManager.openEnchantmentPicker(player, page + 1);
                }
            }
        } else if (isSelector || isPotionSelector) {
            if (currentItem != null && currentItem.getType() != Material.AIR) {
                int page = this.guiManager.itemSelectorPage.getOrDefault(uuid, 1);
                String filter = this.guiManager.itemSearchQuery.get(uuid);
                int slot = event.getSlot();

                if (slot == 48) { // Previous
                    if (page > 1) {
                        this.guiManager.itemSelectorPage.put(uuid, page - 1);
                        this.guiManager.openItemSelector(player, page - 1, filter);
                    }
                } else if (slot == 50) { // Next
                    List<ItemStack> registry = this.guiManager.itemRegistry;
                    if (filter != null && !filter.isEmpty()) {
                        String upper = filter.toUpperCase();
                        registry = registry.stream().filter(i -> i.getType().name().contains(upper) || (i.hasItemMeta() && i.getItemMeta().hasDisplayName() && Utils.stripColor(LegacyComponentSerializer.legacySection().serialize(i.getItemMeta().displayName())).toUpperCase().contains(upper))).collect(Collectors.toList());
                    }
                    int max = (int)Math.ceil(registry.size() / 45.0);
                    if (page < max) {
                        this.guiManager.itemSelectorPage.put(uuid, page + 1);
                        this.guiManager.openItemSelector(player, page + 1, filter);
                    }
                } else if (slot == 49) { // Back to editor
                    this.guiManager.itemSearchQuery.remove(uuid);
                    this.guiManager.openNewOrderEditor(player);
                } else if (slot == 53) { // Search
                    player.closeInventory();
                    this.guiManager.chatInputMode.put(uuid, "SEARCH_ITEM");
                    player.sendMessage(this.plugin.getMessage("gui.type-filter", "{prefix}&eType name to filter (e.g. 'wood'):"));
                } else if (slot >= 0 && slot < 45) { // Item Selection
                    Material mat = currentItem.getType();
                    if (mat != Material.AIR) {
                        this.guiManager.selectedMaterial.put(uuid, mat);
                        
                        if ((mat.name().contains("POTION") || mat == Material.TIPPED_ARROW) && currentItem.hasItemMeta() && currentItem.getItemMeta() instanceof PotionMeta) {
                            PotionMeta meta = (PotionMeta)currentItem.getItemMeta();
                            PotionType type = meta.getBasePotionType();
                            if (type != null) {
                                this.guiManager.selectedPotionType.put(uuid, type.name());
                            }
                            this.guiManager.selectedEnchantmentType.remove(uuid);
                            this.guiManager.selectedEnchantments.remove(uuid);
                            this.guiManager.itemSearchQuery.remove(uuid);
                            this.guiManager.openNewOrderEditor(player);
                        } else if (mat == Material.ENCHANTED_BOOK && currentItem.hasItemMeta() && currentItem.getItemMeta() instanceof EnchantmentStorageMeta) {
                            EnchantmentStorageMeta meta = (EnchantmentStorageMeta)currentItem.getItemMeta();
                            if (!meta.getStoredEnchants().isEmpty()) {
                                Entry<Enchantment, Integer> entry = meta.getStoredEnchants().entrySet().iterator().next();
                                String key = entry.getKey().getName().toUpperCase() + "_" + entry.getValue();
                                this.guiManager.selectedEnchantmentType.put(uuid, key);
                            }
                            this.guiManager.selectedPotionType.remove(uuid);
                            this.guiManager.selectedEnchantments.remove(uuid);
                            this.guiManager.itemSearchQuery.remove(uuid);
                            this.guiManager.openNewOrderEditor(player);
                        } else if (mat == Material.ENCHANTED_BOOK) {
                            this.guiManager.selectedPotionType.remove(uuid);
                            this.guiManager.selectedEnchantmentType.remove(uuid);
                            this.guiManager.selectedEnchantments.put(uuid, new HashSet<>());
                            this.guiManager.enchantmentPickerPage.put(uuid, 1);
                            this.guiManager.itemSearchQuery.remove(uuid);
                            this.guiManager.openEnchantmentPicker(player, 1);
                        } else {
                            this.guiManager.selectedPotionType.remove(uuid);
                            this.guiManager.selectedEnchantmentType.remove(uuid);
                            this.guiManager.selectedEnchantments.remove(uuid);
                            this.guiManager.itemSearchQuery.remove(uuid);
                            this.guiManager.openNewOrderEditor(player);
                        }
                    }
                }
            }
        } else if (titleStr.equals(newOrderTitle)) {
            int slot = event.getSlot();
            if (slot == 10) { // Cancel
                player.closeInventory();
                this.guiManager.openYourOrders(player);
            } else if (slot == 12) { // Item
                this.guiManager.itemSelectorPage.put(uuid, 1);
                this.guiManager.itemSearchQuery.remove(uuid);
                this.guiManager.openItemSelector(player, 1, null);
            } else if (slot == 13) { // Amount
                player.closeInventory();
                this.guiManager.chatInputMode.put(uuid, "AMOUNT");
                player.sendMessage(this.plugin.getMessage("gui.type-amount", "{prefix}&aType amount in chat:"));
            } else if (slot == 14) { // Price
                player.closeInventory();
                this.guiManager.chatInputMode.put(uuid, "PRICE");
                player.sendMessage(this.plugin.getMessage("gui.type-price", "{prefix}&aType price in chat:"));
            } else if (slot == 16) { // Confirm
                Material mat = this.guiManager.selectedMaterial.get(uuid);
                int amount = this.guiManager.selectedAmount.getOrDefault(uuid, 0);
                double price = this.guiManager.selectedPrice.getOrDefault(uuid, 0.0);
                String potType = this.guiManager.selectedPotionType.get(uuid);
                String enchType = this.guiManager.selectedEnchantmentType.get(uuid);

                if (mat == null) {
                    player.sendMessage(this.plugin.getMessage("gui.new-order.item-not-selected", "{prefix}&cPlease select an item."));
                    return;
                }
                if (amount <= 0) {
                    player.sendMessage(this.plugin.getMessage("gui.new-order.amount-not-set", "{prefix}&cPlease set an amount."));
                    return;
                }
                if (price <= 0.0) {
                    player.sendMessage(this.plugin.getMessage("gui.new-order.price-not-set", "{prefix}&cPlease set a price."));
                    return;
                }

                if (this.plugin.getOrderManager().createOrder(player, mat, amount, price, potType, enchType)) {
                    this.guiManager.selectedPotionType.remove(uuid);
                    this.guiManager.selectedEnchantmentType.remove(uuid);
                    player.closeInventory();
                    // Need to reset main menu page or sort? handled in MainMenuHandler usually
                    this.guiManager.openMainMenu(player);
                }
            }
        }
    }

    @Override
    public void onDrag(InventoryDragEvent event) {
        // No drag logic for creation
    }

    @Override
    public void onClose(InventoryCloseEvent event) {
        // No close logic for creation
    }
}