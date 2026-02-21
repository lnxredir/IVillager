package com.ivillager.config;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;

/**
 * Parses item strings "material:amount" and enchantment lists "enchant:level,enchant2:level".
 * All Bukkit API usage must run on the main server thread.
 */
public final class ItemParser {

    private static final int MAX_ENCHANT_LEVEL = 255;
    private static final int MIN_ENCHANT_LEVEL = 1;

    private ItemParser() {}

    /**
     * Parse a single item string "material_name:amount" into an ItemStack.
     * Material names are matched case-insensitive and with underscores.
     *
     * @param input e.g. "cobblestone:64", "diamond_sword:1"
     * @param logger for validation warnings; may be null
     * @return ItemStack or null if invalid
     */
    public static ItemStack parseItem(String input, Logger logger) {
        if (input == null || input.isBlank()) {
            return null;
        }
        String s = input.trim();
        int colon = s.indexOf(':');
        String matName = colon >= 0 ? s.substring(0, colon).trim() : s;
        int amount = 1;
        if (colon >= 0 && colon + 1 < s.length()) {
            try {
                amount = Integer.parseInt(s.substring(colon + 1).trim());
                if (amount < 1) amount = 1;
            } catch (NumberFormatException e) {
                if (logger != null) {
                    logger.warning("[IVillager] Invalid amount in item '" + input + "', using 1");
                }
            }
        }
        Material mat = Material.matchMaterial(matName);
        if (mat == null || !mat.isItem()) {
            if (logger != null) {
                logger.warning("[IVillager] Unknown or non-item material: " + matName);
            }
            return null;
        }
        return new ItemStack(mat, Math.min(amount, mat.getMaxStackSize()));
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
