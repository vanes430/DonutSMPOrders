package github.vanes430.orderplugin.model;

import org.bukkit.Material;

public enum FilterType {
   ALL("All"),
   BLOCKS("Blocks"),
   TOOLS("Tools"),
   FOOD("Food"),
   COMBAT("Combat"),
   POTIONS("Potions"),
   BOOKS("Books"),
   INGREDIENTS("Ingredients"),
   UTILITIES("Utilities");

   private final String displayName;

   private FilterType(String displayName) {
      this.displayName = displayName;
   }

   public String getDisplayName() {
      return this.displayName;
   }

   public FilterType next() {
      FilterType[] values = values();
      return values[(this.ordinal() + 1) % values.length];
   }

   public boolean matches(Material material) {
      if (this == ALL) {
         return true;
      } else if (material == null) {
         return false;
      } else {
         String name = material.name();
         switch (this) {
            case BLOCKS:
               return material.isBlock();
            case TOOLS:
               return name.contains("PICKAXE")
                  || name.contains("AXE")
                  || name.contains("SHOVEL")
                  || name.contains("HOE")
                  || name.contains("SHEARS")
                  || name.contains("FLINT_AND_STEEL")
                  || name.contains("FISHING_ROD")
                  || name.contains("BRUSH")
                  || name.contains("SPYGLASS");
            case FOOD:
               return material.isEdible();
            case COMBAT:
               return name.contains("SWORD")
                  || name.contains("BOW")
                  || name.contains("CROSSBOW")
                  || name.contains("ARROW")
                  || name.contains("SHIELD")
                  || name.contains("TRIDENT")
                  || name.contains("HELMET")
                  || name.contains("CHESTPLATE")
                  || name.contains("LEGGINGS")
                  || name.contains("BOOTS")
                  || name.contains("TOTEM")
                  || name.contains("MACE");
            case POTIONS:
               return name.contains("POTION") || material == Material.GLASS_BOTTLE || material == Material.DRAGON_BREATH;
            case BOOKS:
               return name.contains("BOOK")
                  || material == Material.WRITABLE_BOOK
                  || material == Material.WRITTEN_BOOK
                  || material == Material.ENCHANTED_BOOK
                  || material == Material.KNOWLEDGE_BOOK;
            case INGREDIENTS:
               return name.contains("DYE")
                  || material == Material.BLAZE_POWDER
                  || material == Material.BLAZE_ROD
                  || material == Material.GHAST_TEAR
                  || material == Material.MAGMA_CREAM
                  || material == Material.NETHER_WART
                  || material == Material.SPIDER_EYE
                  || material == Material.FERMENTED_SPIDER_EYE
                  || material == Material.GLISTERING_MELON_SLICE
                  || material == Material.RABBIT_FOOT
                  || material == Material.PHANTOM_MEMBRANE
                  || material == Material.GUNPOWDER
                  || material == Material.REDSTONE
                  || material == Material.GLOWSTONE_DUST
                  || material == Material.SUGAR
                  || material == Material.SLIME_BALL
                  || material == Material.HONEY_BOTTLE
                  || material == Material.INK_SAC
                  || material == Material.GLOW_INK_SAC
                  || material == Material.LAPIS_LAZULI
                  || material == Material.BONE_MEAL;
            case UTILITIES:
               return material == Material.ENDER_PEARL
                  || material == Material.ENDER_EYE
                  || material == Material.FIREWORK_ROCKET
                  || material == Material.LEAD
                  || material == Material.NAME_TAG
                  || material == Material.SADDLE
                  || material == Material.ELYTRA
                  || material == Material.COMPASS
                  || material == Material.CLOCK
                  || material == Material.MAP
                  || material == Material.FILLED_MAP
                  || material == Material.BUCKET
                  || name.contains("BUCKET")
                  || name.contains("MINECART")
                  || name.contains("BOAT")
                  || material == Material.TNT;
            default:
               return false;
         }
      }
   }
}
