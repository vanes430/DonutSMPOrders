package github.vanes430.orderplugin.utils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class ItemBuilder {
    private final ItemStack itemStack;
    private final ItemMeta meta;

    public ItemBuilder(Material material) {
        this.itemStack = new ItemStack(material);
        this.meta = this.itemStack.getItemMeta();
    }

    public ItemBuilder(ItemStack itemStack) {
        this.itemStack = itemStack.clone();
        this.meta = this.itemStack.getItemMeta();
    }

    public ItemBuilder name(String name) {
        this.meta.displayName(Utils.color(name).decoration(TextDecoration.ITALIC, false));
        return this;
    }

    public ItemBuilder name(Component component) {
        this.meta.displayName(component.decoration(TextDecoration.ITALIC, false));
        return this;
    }

    public ItemBuilder lore(String... lore) {
        this.meta.lore(Arrays.stream(lore).map(Utils::color).collect(Collectors.toList()));
        return this;
    }

    public ItemBuilder lore(Component... lore) {
        this.meta.lore(Arrays.stream(lore).map(c -> c.decoration(TextDecoration.ITALIC, false)).collect(Collectors.toList()));
        return this;
    }

    public ItemBuilder lore(List<Component> lore) {
        this.meta.lore(lore.stream().map(c -> c.decoration(TextDecoration.ITALIC, false)).collect(Collectors.toList()));
        return this;
    }

    public ItemBuilder amount(int amount) {
        this.itemStack.setAmount(amount);
        return this;
    }

    public ItemBuilder hideFlags() {
        this.meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        this.meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        this.meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
        this.meta.addItemFlags(ItemFlag.HIDE_DESTROYS);
        this.meta.addItemFlags(ItemFlag.HIDE_PLACED_ON);
        this.meta.addItemFlags(ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        this.meta.addItemFlags(ItemFlag.HIDE_DYE);
        return this;
    }

    public ItemStack build() {
        this.itemStack.setItemMeta(this.meta);
        return this.itemStack;
    }
}