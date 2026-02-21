package com.ivillager.config;

import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * In-memory definition of a single trade: up to 2 ingredients and 1 result.
 * Vanilla MerchantRecipe supports only one result; multiple result items in config
 * are approximated (first item or merged same material) and documented in code.
 */
public final class TradeDefinition {

    private final List<ItemStack> ingredients; // max 2 for MerchantRecipe
    private final ItemStack result;

    public TradeDefinition(List<ItemStack> ingredients, ItemStack result) {
        this.ingredients = ingredients != null ? new ArrayList<>(ingredients) : new ArrayList<>();
        this.result = result;
    }

    public List<ItemStack> getIngredients() {
        return new ArrayList<>(ingredients);
    }

    public ItemStack getResult() {
        return result;
    }
}
