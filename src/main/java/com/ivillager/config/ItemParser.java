package com.ivillager.config;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.PotionContents;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;

/**
 * Parses item strings "material:amount" and "material:amount:potion_type" for potions
 * and tipped arrows, and enchantment lists "enchant:level,enchant2:level".
 * All Bukkit API usage must run on the main server thread.
 */
public final class ItemParser {

    private static final int MAX_ENCHANT_LEVEL = 255;
    private static final int MIN_ENCHANT_LEVEL = 1;

    private static final List<Material> POTION_TYPE_MATERIALS = List.of(
            Material.POTION,
            Material.SPLASH_POTION,
            Material.LINGERING_POTION,
            Material.TIPPED_ARROW
    );

    private ItemParser() {}

    /**
     * Parse a single item string into an ItemStack.
     * Format: "material_name:amount" or for potions "material:amount:potion_type".
     * Material names are matched case-insensitive and with underscores.
     * Potion type is matched case-insensitive with underscores (e.g. healing, strong_healing).
     *
     * @param input e.g. "cobblestone:64", "diamond_sword:1", "potion:1:healing", "tipped_arrow:8:poison"
     * @param logger for validation warnings; may be null
     * @return ItemStack or null if invalid
     */
    public static ItemStack parseItem(String input, Logger logger) {
        if (input == null || input.isBlank()) {
            return null;
        }
        String s = input.trim();
        String[] parts = s.split(":", 3);
        String matName = parts[0].trim();
        int amount = 1;
        String potionTypeName = null;

        if (parts.length >= 2) {
            try {
                amount = Integer.parseInt(parts[1].trim());
                if (amount < 1) amount = 1;
            } catch (NumberFormatException e) {
                if (logger != null) {
                    logger.warning("[IVillager] Invalid amount in item '" + input + "', using 1");
                }
            }
        }
        if (parts.length >= 3) {
            potionTypeName = parts[2].trim();
        }

        Material mat = Material.matchMaterial(matName);
        if (mat == null || !mat.isItem()) {
            if (logger != null) {
                logger.warning("[IVillager] Unknown or non-item material: " + matName);
            }
            return null;
        }

        ItemStack stack = new ItemStack(mat, Math.min(amount, mat.getMaxStackSize()));

        if (POTION_TYPE_MATERIALS.contains(mat) && potionTypeName != null && !potionTypeName.isEmpty()) {
            applyPotionType(stack, potionTypeName, logger);
        }

        return stack;
    }

    /**
     * Apply a potion type to a potion or tipped arrow ItemStack (POTION, SPLASH_POTION,
     * LINGERING_POTION, TIPPED_ARROW). Modifies the stack in place.
     *
     * @param stack the potion or tipped arrow stack
     * @param potionTypeName e.g. "healing", "strong_healing", "long_fire_resistance"
     * @param logger for validation warnings; may be null
     */
    public static void applyPotionType(ItemStack stack, String potionTypeName, Logger logger) {
        if (stack == null || potionTypeName == null || potionTypeName.isBlank()) return;
        if (!POTION_TYPE_MATERIALS.contains(stack.getType())) return;

        String normalized = potionTypeName.toUpperCase(Locale.ROOT).replace("-", "_").replace(" ", "_");
        PotionType type;
        try {
            type = PotionType.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            if (logger != null) {
                logger.warning("[IVillager] Unknown potion type: " + potionTypeName + " (use e.g. healing, strong_healing, long_fire_resistance)");
            }
            return;
        }
        // Use data component API so the potion displays as normal (e.g. "Potion of Healing") not "Uncraftable ..."
        PotionContents contents = PotionContents.potionContents().potion(type).build();
        stack.setData(DataComponentTypes.POTION_CONTENTS, contents);
    }

    /**
     * Parse comma-separated item strings into a list of ItemStacks (e.g. "diamond:1,gold_block:3").
     * Used for trade result; vanilla supports only one result, so caller should take first or merge.
     */
    public static List<ItemStack> parseItemList(String input, Logger logger) {
        List<ItemStack> list = new ArrayList<>();
        if (input == null || input.isBlank()) return list;
        for (String part : input.split(",")) {
            ItemStack stack = parseItem(part.trim(), logger);
            if (stack != null) list.add(stack);
        }
        return list;
    }

    /**
     * Apply enchantments string "enchant:level,enchant2:level" to the given ItemStack.
     * Modifies the stack in place. Validates against Enchantment and level bounds.
     */
    public static void applyEnchantments(ItemStack stack, String enchantmentsStr, Logger logger) {
        if (stack == null || enchantmentsStr == null || enchantmentsStr.isBlank()) return;
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return;
        for (String part : enchantmentsStr.split(",")) {
            String p = part.trim();
            int colon = p.indexOf(':');
            String name = colon >= 0 ? p.substring(0, colon).trim() : p;
            int level = 1;
            if (colon >= 0 && colon + 1 < p.length()) {
                try {
                    level = Integer.parseInt(p.substring(colon + 1).trim());
                    level = Math.max(MIN_ENCHANT_LEVEL, Math.min(MAX_ENCHANT_LEVEL, level));
                } catch (NumberFormatException ignored) {}
            }
            NamespacedKey key = NamespacedKey.minecraft(name.toLowerCase(Locale.ROOT).replace(" ", "_"));
            Enchantment enchant = Registry.ENCHANTMENT.get(key);
            if (enchant == null) {
                if (logger != null) {
                    logger.warning("[IVillager] Unknown enchantment: " + name);
                }
                continue;
            }
            try {
                meta.addEnchant(enchant, level, true);
            } catch (IllegalArgumentException e) {
                if (logger != null) {
                    logger.warning("[IVillager] Cannot apply " + name + " to item: " + e.getMessage());
                }
            }
        }
        stack.setItemMeta(meta);
    }

    /**
     * Reduce multiple result ItemStacks to a single one for MerchantRecipe.
     * Vanilla villager mechanics support only one result ItemStack per trade; multiple output
     * types in config are not representable. We use the first result, or merge amounts when
     * the same material appears multiple times (best approximation).
     */
    public static ItemStack singleResult(List<ItemStack> results) {
        if (results == null || results.isEmpty()) return null;
        ItemStack first = results.get(0);
        if (first == null) return null;
        if (results.size() == 1) return first.clone();
        // Optional: merge same material into one stack
        Material mat = first.getType();
        int total = first.getAmount();
        for (int i = 1; i < results.size(); i++) {
            ItemStack s = results.get(i);
            if (s != null && s.getType() == mat) {
                total = Math.min(total + s.getAmount(), mat.getMaxStackSize());
            }
        }
        ItemStack out = first.clone();
        out.setAmount(total);
        return out;
    }
}
