package github.vanes430.orderplugin.utils;

import java.util.concurrent.TimeUnit;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class Utils {
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.builder()
            .character('&')
            .hexColors()
            .build();

    public static Component color(String text) {
        if (text == null) {
            return Component.empty();
        }
        
        Component component;
        if (text.contains("<") && text.contains(">")) {
            component = MINI_MESSAGE.deserialize(text);
        } else {
            component = LEGACY_SERIALIZER.deserialize(text);
        }
        
        // Disable italics by default for all colored text
        return Component.text().decoration(TextDecoration.ITALIC, false).append(component).build();
    }

    public static String stripColor(String text) {
        if (text == null) {
            return "";
        }
        return LegacyComponentSerializer.legacySection().serialize(color(text));
    }

    private static final String[] SMALL_CAPS = new String[] {
            "ᴀ", "ʙ", "ᴄ", "ᴅ", "ᴇ", "ꜰ", "ɢ", "ʜ", "ɪ", "ᴊ", "ᴋ", "ʟ", "ᴍ", "ɴ", "ᴏ", "ᴘ", "ꞯ", "ʀ", "ꜱ", "ᴛ", "ᴜ", "ᴠ", "ᴡ", "x", "ʏ", "ᴢ"
    };

    public static String toSmallCaps(String text) {
        if (text == null) return null;
        StringBuilder builder = new StringBuilder();
        int len = text.length();
        for (int i = 0; i < len; i++) {
            char c = text.charAt(i);
            if (c == '&' || c == '§') {
                builder.append(c);
                if (i + 1 < len) {
                    char next = text.charAt(i + 1);
                    builder.append(next);
                    i++;
                    if (next == '#') {
                        // Hex code: &#RRGGBB
                        for (int j = 0; j < 6; j++) {
                            if (i + 1 < len) {
                                builder.append(text.charAt(i + 1));
                                i++;
                            }
                        }
                    }
                }
            } else {
                if (c >= 'a' && c <= 'z') {
                    builder.append(SMALL_CAPS[c - 'a']);
                } else if (c >= 'A' && c <= 'Z') {
                    builder.append(SMALL_CAPS[c - 'A']);
                } else {
                    builder.append(c);
                }
            }
        }
        return builder.toString();
    }

    public static String formatNumber(double value) {
        String result;
        if (value < 1000.0) {
            result = value == (long) value ? String.format("%d", (long) value) : String.format("%.2f", value);
        } else if (value < 1000000.0) {
            double k = value / 1000.0;
            result = k == (long) k ? String.format("%dK", (long) k) : String.format("%.2fK", k);
        } else if (value < 1.0E9) {
            double m = value / 1000000.0;
            result = m == (long) m ? String.format("%dM", (long) m) : String.format("%.2fM", m);
        } else if (value < 1.0E12) {
            double b = value / 1.0E9;
            result = b == (long) b ? String.format("%dB", (long) b) : String.format("%.2fB", b);
        } else {
            double t = value / 1.0E12;
            result = t == (long) t ? String.format("%dT", (long) t) : String.format("%.2fT", t);
        }
        return toSmallCaps(result);
    }

    public static String formatMaterialName(String name) {
        if (name != null && !name.isEmpty()) {
            String[] parts = name.toLowerCase().split("_");
            StringBuilder builder = new StringBuilder();

            for (String part : parts) {
                if (!part.isEmpty()) {
                    builder.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1)).append(" ");
                }
            }

            return toSmallCaps(builder.toString().trim());
        } else {
            return toSmallCaps("Unknown");
        }
    }

    public static String formatTime(long expiresAt) {
        long remaining = expiresAt - System.currentTimeMillis();
        if (remaining <= 0L) {
            return toSmallCaps("Expired");
        } else {
            long days = TimeUnit.MILLISECONDS.toDays(remaining);
            remaining -= TimeUnit.DAYS.toMillis(days);
            long hours = TimeUnit.MILLISECONDS.toHours(remaining);
            remaining -= TimeUnit.HOURS.toMillis(hours);
            long minutes = TimeUnit.MILLISECONDS.toMinutes(remaining);
            StringBuilder builder = new StringBuilder();
            if (days > 0L) {
                builder.append(days).append("d ");
            }

            if (hours > 0L) {
                builder.append(hours).append("h ");
            }

            builder.append(minutes).append("m");
            return toSmallCaps(builder.toString());
        }
    }

    public static String formatPotionName(String type) {
        if (type != null && !type.isEmpty()) {
            String upper = type.toUpperCase();
            String result;
            switch (upper) {
                case "INSTANT_HEAL":
                    result = "Instant Health";
                    break;
                case "INSTANT_DAMAGE":
                    result = "Instant Damage";
                    break;
                case "REGEN":
                    result = "Regeneration";
                    break;
                case "SPEED":
                case "SWIFTNESS":
                    result = "Swiftness";
                    break;
                case "LONG_SWIFTNESS":
                    result = "Long Swiftness";
                    break;
                case "STRONG_SWIFTNESS":
                    result = "Strong Swiftness";
                    break;
                case "JUMP":
                    result = "Leaping";
                    break;
                case "LONG_LEAPING":
                    result = "Long Leaping";
                    break;
                case "STRONG_LEAPING":
                    result = "Strong Leaping";
                    break;
                default:
                    // Avoid double small caps conversion if calling formatMaterialName
                    if (!type.contains("_")) {
                         // Simple case
                         result = type.substring(0, 1).toUpperCase() + type.substring(1).toLowerCase();
                    } else {
                         String[] parts = type.toLowerCase().split("_");
                         StringBuilder builder = new StringBuilder();
                         for (String part : parts) {
                             if (!part.isEmpty()) {
                                 builder.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1)).append(" ");
                             }
                         }
                         result = builder.toString().trim();
                    }
            }
            return toSmallCaps(result);
        } else {
            return "";
        }
    }

    public static String formatPotion(String type) {
        if (type != null && !type.isEmpty()) {
            String[] parts = type.split("_");
            if (parts.length >= 3) {
                String base = parts[0];
                boolean isLong = Boolean.parseBoolean(parts[1]);
                boolean isStrong = Boolean.parseBoolean(parts[2]);
                String name = formatPotionName(base); // Already small caps
                // We need to handle the suffixes manually or they won't be small caps if added to small caps string
                // But wait, formatPotionName returns small caps.
                // We should construct the full string first, then small cap it?
                // formatPotionName(base) returns small caps.
                // Let's rely on formatPotionName returning normal string first?
                // No, I changed formatPotionName to return small caps.
                
                // Let's just return what formatPotionName returns + suffix converted
                String suffix = "";
                if (isStrong) {
                    suffix = " II";
                } else if (isLong) {
                    suffix = " (Long)";
                }
                return name + toSmallCaps(suffix);
            } else {
                return formatPotionName(type);
            }
        } else {
            return "";
        }
    }

    public static String formatUnknown(String name) {
        return name != null && !name.isEmpty() ? formatMaterialName(name) : toSmallCaps("Unknown");
    }

    public static String capitalize(String text) {
        if (text != null && !text.isEmpty()) {
            String[] parts = text.toLowerCase().split("_");
            StringBuilder builder = new StringBuilder();

            for (String part : parts) {
                if (builder.length() > 0) {
                    builder.append(" ");
                }

                if (!part.isEmpty()) {
                    builder.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
                }
            }

            return toSmallCaps(builder.toString());
        } else {
            return toSmallCaps("Unknown");
        }
    }

    public static String formatEnchantedBook(String enchStr) {
        if (enchStr != null && !enchStr.isEmpty()) {
            String[] parts = enchStr.split("_");
            if (parts.length < 2) {
                return formatUnknown(enchStr);
            } else {
                try {
                    int level = Integer.parseInt(parts[parts.length - 1]);
                    String enchName = parts[0];

                    for (int i = 1; i < parts.length - 1; i++) {
                        enchName = enchName + "_" + parts[i];
                    }

                    String levelStr = level > 1 ? " " + toRoman(level) : "";
                    // formatUnknown calls formatMaterialName which returns small caps.
                    // levelStr needs small caps.
                    // " Book" needs small caps.
                    
                    // To be safe, let's construct normal string then convert.
                    // Re-implementing logic to avoid double conversion issues
                     String[] nameParts = enchName.toLowerCase().split("_");
                     StringBuilder builder = new StringBuilder();
                     for (String part : nameParts) {
                         if (!part.isEmpty()) {
                             builder.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1)).append(" ");
                         }
                     }
                     String niceName = builder.toString().trim();
                     
                     return toSmallCaps(niceName + levelStr + " Book");
                } catch (NumberFormatException e) {
                    return formatUnknown(enchStr) + toSmallCaps(" Book");
                }
            }
        } else {
            return toSmallCaps("Enchanted Book");
        }
    }

    public static String toRoman(int value) {
        if (value > 0 && value <= 10) {
            String[] numerals = new String[]{"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};
            return numerals[value];
        } else {
            return String.valueOf(value);
        }
    }
}