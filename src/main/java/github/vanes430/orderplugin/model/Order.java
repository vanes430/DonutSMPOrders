package github.vanes430.orderplugin.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public class Order {
   private final UUID orderId;
   private final UUID ownerId;
   private final Material material;
   private final int neededAmount;
   private int filledAmount;
   private final double pricePerItem;
   private final long createdAt;
   private final long expiresAt;
   private boolean removedByAdmin = false;
   private String potionType = null;
   private String enchantmentType = null;
   private final List<ItemStack> collectedItems = new ArrayList<>();

   public Order(UUID ownerId, Material material, int neededAmount, double pricePerItem) {
      this(ownerId, material, neededAmount, pricePerItem, null);
   }

   public Order(UUID ownerId, Material material, int neededAmount, double pricePerItem, String potionType) {
      this(ownerId, material, neededAmount, pricePerItem, potionType, null);
   }

   public Order(UUID ownerId, Material material, int neededAmount, double pricePerItem, String potionType, String enchantmentType) {
      this.orderId = UUID.randomUUID();
      this.ownerId = ownerId;
      this.material = material;
      this.neededAmount = neededAmount;
      this.filledAmount = 0;
      this.pricePerItem = pricePerItem;
      this.potionType = potionType;
      this.enchantmentType = enchantmentType;
      this.createdAt = System.currentTimeMillis();
      this.expiresAt = this.createdAt + TimeUnit.DAYS.toMillis(7L);
   }

   public Order(UUID orderId, UUID ownerId, Material material, int neededAmount, int filledAmount, double pricePerItem, long createdAt, long expiresAt) {
      this.orderId = orderId;
      this.ownerId = ownerId;
      this.material = material;
      this.neededAmount = neededAmount;
      this.filledAmount = filledAmount;
      this.pricePerItem = pricePerItem;
      this.createdAt = createdAt;
      this.expiresAt = expiresAt;
   }

   public UUID getOrderId() {
      return this.orderId;
   }

   public UUID getOwnerId() {
      return this.ownerId;
   }

   public Material getMaterial() {
      return this.material;
   }

   public int getNeededAmount() {
      return this.neededAmount;
   }

   public int getFilledAmount() {
      return this.filledAmount;
   }

   public double getPricePerItem() {
      return this.pricePerItem;
   }

   public long getCreatedAt() {
      return this.createdAt;
   }

   public long getExpiresAt() {
      return this.expiresAt;
   }

   public boolean isRemovedByAdmin() {
      return this.removedByAdmin;
   }

   public List<ItemStack> getAllCollectedItems() {
      return this.collectedItems;
   }

   public void addItem(ItemStack item) {
      if (item != null && item.getType() != Material.AIR) {
         this.collectedItems.add(item.clone());
      }
   }

   public void clearInventory() {
      this.collectedItems.clear();
   }

   public void setItems(List<ItemStack> items) {
      this.clearInventory();
      if (items != null) {
         for (ItemStack item : items) {
            this.addItem(item);
         }
      }
   }

   public int getTotalCollectedAmount() {
      return this.collectedItems.stream().mapToInt(ItemStack::getAmount).sum();
   }

   public void setFilledAmount(int filledAmount) {
      this.filledAmount = filledAmount;
   }

   public void setRemovedByAdmin(boolean removedByAdmin) {
      this.removedByAdmin = removedByAdmin;
   }

   public String getPotionType() {
      return this.potionType;
   }

   public void setPotionType(String potionType) {
      this.potionType = potionType;
   }

   public String getEnchantmentType() {
      return this.enchantmentType;
   }

   public void setEnchantmentType(String enchantmentType) {
      this.enchantmentType = enchantmentType;
   }

   public boolean isPotion() {
      return this.material == Material.POTION || this.material == Material.SPLASH_POTION || this.material == Material.LINGERING_POTION || this.material == Material.TIPPED_ARROW;
   }

   public boolean isEnchantedBook() {
      return this.material == Material.ENCHANTED_BOOK;
   }

   public int getRemainingAmount() {
      return this.neededAmount - this.filledAmount;
   }

   public boolean isCompleted() {
      return this.filledAmount >= this.neededAmount;
   }

   public boolean hasItemsToCollect() {
      return !this.collectedItems.isEmpty();
   }

   public String serializeInventory() {
      try {
         ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
         BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);
         
         dataOutput.writeInt(this.collectedItems.size());
         for (ItemStack item : this.collectedItems) {
            dataOutput.writeObject(item);
         }
         
         dataOutput.close();
         return Base64Coder.encodeLines(outputStream.toByteArray());
      } catch (Exception e) {
         throw new IllegalStateException("Unable to serialize inventory.", e);
      }
   }

   public void deserializeInventory(String data) {
      try {
         ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64Coder.decodeLines(data));
         BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);
         
         int size = dataInput.readInt();
         this.collectedItems.clear();
         for (int i = 0; i < size; i++) {
            this.collectedItems.add((ItemStack) dataInput.readObject());
         }
         
         dataInput.close();
      } catch (Exception e) {
         // Silently fail if data is empty or invalid
      }
   }

   public boolean matches(ItemStack item) {
      if (item == null || item.getType() == Material.AIR) {
         return false;
      } else if (item.getType() != this.material) {
         return false;
      } else if (this.isPotion() && this.potionType != null) {
         if (item.getItemMeta() instanceof PotionMeta) {
            PotionMeta meta = (PotionMeta)item.getItemMeta();
            PotionType type = meta.getBasePotionType();
            if (type != null) {
               return type.name().equals(this.potionType);
            }
         }
         return false;
      } else if (this.isEnchantedBook() && this.enchantmentType != null) {
         if (item.getItemMeta() instanceof EnchantmentStorageMeta) {
            EnchantmentStorageMeta meta = (EnchantmentStorageMeta)item.getItemMeta();
            String[] parts = this.enchantmentType.split("_");
            if (parts.length >= 2) {
               try {
                  String enchName = parts[0];
                  for (int i = 1; i < parts.length - 1; i++) {
                     enchName = enchName + "_" + parts[i];
                  }
                  int level = Integer.parseInt(parts[parts.length - 1]);
                  Enchantment enchantment = Enchantment.getByName(enchName);
                  if (enchantment != null) {
                     int itemLevel = meta.getStoredEnchantLevel(enchantment);
                     return itemLevel == level;
                  }
               } catch (Exception e) {
                  return false;
               }
            }
         }
         return false;
      } else if (this.enchantmentType != null && !this.enchantmentType.isEmpty()) {
         ItemMeta meta = item.getItemMeta();
         if (meta == null) {
            return false;
         } else {
            Map<Enchantment, Integer> enchants = meta.getEnchants();
            String[] parts = this.enchantmentType.split(";");
            for (String enchStr : parts) {
               String[] enchParts = enchStr.split("_");
               if (enchParts.length >= 2) {
                  try {
                     String name = enchParts[0].toLowerCase();
                     for (int i = 1; i < enchParts.length - 1; i++) {
                        name = name + "_" + enchParts[i].toLowerCase();
                     }
                     int level = Integer.parseInt(enchParts[enchParts.length - 1]);
                     Enchantment enchantment = (Enchantment)Registry.ENCHANTMENT.get(NamespacedKey.minecraft(name));
                     if (enchantment == null) {
                        return false;
                     }
                     Integer itemLevel = enchants.get(enchantment);
                     if (itemLevel == null || itemLevel != level) {
                        return false;
                     }
                  } catch (Exception e) {
                     return false;
                  }
               }
            }
            return true;
         }
      } else {
         return true;
      }
   }
}