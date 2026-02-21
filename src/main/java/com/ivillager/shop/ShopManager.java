package com.ivillager.shop;

import com.ivillager.config.ShopDefinition;
import com.ivillager.config.TradeDefinition;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Merchant;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.inventory.MenuType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Holds loaded shops and opens the vanilla villager trading UI for players.
 * All Merchant/UI operations must run on the main server thread.
 */
public final class ShopManager {

    private final Map<String, ShopDefinition> shops = new ConcurrentHashMap<>();
    private String defaultShopName;

    public void load(Map<String, ShopDefinition> newShops, String defaultShop) {
        shops.clear();
        if (newShops != null) {
            for (Map.Entry<String, ShopDefinition> e : newShops.entrySet()) {
                shops.put(e.getKey().toLowerCase(Locale.ROOT), e.getValue());
            }
        }
        defaultShopName = defaultShop != null ? defaultShop.trim() : null;
        if (defaultShopName != null && defaultShopName.isEmpty()) defaultShopName = null;
    }

    public boolean hasShop(String name) {
        return name != null && shops.containsKey(name.toLowerCase(Locale.ROOT));
    }

    public String getDefaultShopName() {
        return defaultShopName;
    }

    /**
     * Open the vanilla villager trading UI for the player with the given shop.
     * Must be called from the main server thread.
     */
    public void openShop(Player player, String shopName) {
        if (player == null || shopName == null) return;
        String key = shopName.toLowerCase(Locale.ROOT);
        ShopDefinition shop = shops.get(key);
        if (shop == null) return;

        Merchant merchant = Bukkit.getServer().createMerchant();
        String title = shop.getDisplayName() != null ? shop.getDisplayName() : "IVillager";
        merchant.setRecipes(buildRecipes(shop));

        // Paper 1.21.11: MenuType.MERCHANT.builder() with merchant and title.
        // Future 1.21.x/1.22: if API changes, adjust builder usage here.
        MenuType.MERCHANT.builder()
                .merchant(merchant)
                .title(Component.text(title))
                .build(player)
                .open();
    }

    /**
     * Build MerchantRecipe list from shop definition.
     * Vanilla MerchantRecipe supports at most 2 ingredients and 1 result per trade.
     * We clone every ItemStack so recipes are independent and display correctly
     * (shared references can cause trades to not show or get marked as used).
     */
    private List<MerchantRecipe> buildRecipes(ShopDefinition shop) {
        List<MerchantRecipe> recipes = new ArrayList<>();
        int maxUses = shop.getMaxUses() > 0 ? shop.getMaxUses() : 999999;
        float priceMultiplier = (float) (shop.getPriceMultiplier() > 0 ? shop.getPriceMultiplier() : 0.05f);
        int villagerXp = shop.getBuyXp();

        for (TradeDefinition t : shop.getTrades()) {
            if (t.getResult() == null || t.getIngredients().isEmpty()) continue;
            List<org.bukkit.inventory.ItemStack> ingredients = new ArrayList<>();
            for (org.bukkit.inventory.ItemStack ing : t.getIngredients()) {
                if (ing != null && !ing.getType().isAir()) {
                    ingredients.add(ing.clone());
                }
            }
            if (ingredients.isEmpty()) continue;
            MerchantRecipe recipe = new MerchantRecipe(
                    t.getResult().clone(),
                    0,
                    maxUses,
                    true,
                    villagerXp,
                    priceMultiplier
            );
            recipe.setIngredients(ingredients);
            recipes.add(recipe);
        }
        return recipes;
    }

    public Set<String> getShopNames() {
        return Collections.unmodifiableSet(shops.keySet());
    }
}
