package com.ivillager.config;

import java.util.ArrayList;
import java.util.List;

/**
 * In-memory definition of a shop: display name, optional villager metadata, and trades.
 */
public final class ShopDefinition {

    private final String name;
    private final String displayName;
    private final List<TradeDefinition> trades;
    private final String profession;
    private final int level;
    private final int maxUses;
    private final int experience;
    private final double priceMultiplier;
    private final int buyXp;

    public ShopDefinition(
            String name,
            String displayName,
            List<TradeDefinition> trades,
            String profession,
            int level,
            int maxUses,
            int experience,
            double priceMultiplier,
            int buyXp
    ) {
        this.name = name != null ? name : "";
        this.displayName = displayName != null ? displayName : "IVillager";
        this.trades = trades != null ? new ArrayList<>(trades) : new ArrayList<>();
        this.profession = profession;
        this.level = level;
        this.maxUses = maxUses;
        this.experience = experience;
        this.priceMultiplier = priceMultiplier;
        this.buyXp = buyXp;
    }

    public String getName() {
        return name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public List<TradeDefinition> getTrades() {
        return new ArrayList<>(trades);
    }

    public String getProfession() {
        return profession;
    }

    public int getLevel() {
        return level;
    }

    public int getMaxUses() {
        return maxUses;
    }

    public int getExperience() {
        return experience;
    }

    public double getPriceMultiplier() {
        return priceMultiplier;
    }

    public int getBuyXp() {
        return buyXp;
    }
}
