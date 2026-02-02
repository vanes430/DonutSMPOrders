package github.vanes430.orderplugin.utils;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

public class WorthUtils {
   public static double getWorth(Player player, ItemStack item) {
      if (item == null) {
         return -1.0;
      } else {
         try {
            if (Bukkit.getPluginManager().getPlugin("CMI") != null) {
               Object instance = Class.forName("com.Zrips.CMI.CMI").getMethod("getInstance").invoke(null);
               Object worthManager = instance.getClass().getMethod("getWorthManager").invoke(instance);
               Object worth = worthManager.getClass().getMethod("getWorth", ItemStack.class).invoke(worthManager, item);
               if (worth instanceof Double) {
                  double price = (Double)worth;
                  if (price > 0.0) {
                     return price;
                  }
               }
            }
         } catch (Exception e) {
         }

         try {
            if (Bukkit.getPluginManager().getPlugin("ShopGUIPlus") != null) {
               Class api = Class.forName("net.brcdev.shopgui.ShopGuiPlusApi");
               Method method = api.getMethod("getItemStackPriceSell", ItemStack.class);
               Object worth = method.invoke(null, item);
               if (worth instanceof Double) {
                  double price = (Double)worth;
                  if (price > 0.0) {
                     return price;
                  }
               }
            }
         } catch (Exception e) {
         }

         try {
            if (Bukkit.getPluginManager().getPlugin("EconomyShopGUI") != null) {
               Class api = Class.forName("me.gyropo.economyshopgui.api.EconomyShopGUIHook");
               Object instance = api.getMethod("instance").invoke(null);
               Object worth = api.getMethod("getItemSellPrice", ItemStack.class).invoke(instance, item);
               if (worth instanceof Double) {
                  double price = (Double)worth;
                  if (price > 0.0) {
                     return price;
                  }
               }
            }
         } catch (Exception e) {
         }

         try {
            if (Bukkit.getPluginManager().getPlugin("Essentials") != null) {
               try {
                  Class api = Class.forName("com.earth2me.essentials.api.Economy");
                  Method method = api.getMethod("getWorth", ItemStack.class);
                  Object worth = method.invoke(null, item);
                  if (worth instanceof BigDecimal) {
                     return ((BigDecimal)worth).doubleValue();
                  }
               } catch (Exception e) {
               }

               Plugin essentials = Bukkit.getPluginManager().getPlugin("Essentials");
               Method worthMethod = essentials.getClass().getMethod("getWorth");
               Object worthObj = worthMethod.invoke(essentials);
               Method getPriceMethod = worthObj.getClass().getMethod("getPrice", ItemStack.class);
               Object price = getPriceMethod.invoke(worthObj, item);
               if (price instanceof BigDecimal) {
                  return ((BigDecimal)price).doubleValue();
               }
            }
         } catch (Exception e) {
         }

         return -1.0;
      }
   }
}
