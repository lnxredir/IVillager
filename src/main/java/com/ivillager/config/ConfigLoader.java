package com.ivillager.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Loads and validates config.yml; returns a map of shop id -> ShopDefinition.
 * Handles unknown keys with a warning. Persists config for create/delete.
 */
public final class ConfigLoader {

    private static final Set<String> TOP_KEYS = Set.of("default_shop", "shops");
    private static final Set<String> SHOP_KEYS = Set.of(
            "type", "trades", "display_name", "profession", "level", "max_uses",
            "experience", "price_multiplier", "buy_xp"
    );
    private static final Set<String> TRADE_KEYS = Set.of("item", "trade", "enchantments", "type");
    private static final int DEFAULT_MAX_USES = 999999;
    private static final float DEFAULT_PRICE_MULTIPLIER = 0.05f;

    private final JavaPlugin plugin;
    private final Logger logger;
    private File configFile;

    public ConfigLoader(JavaPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    /**
     * Load config from plugin data folder. Call from main thread.
     */
    public ConfigResult load() {
        plugin.saveDefaultConfig();
        configFile = new File(plugin.getDataFolder(), "config.yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);

        for (String key : config.getKeys(false)) {
            if (!TOP_KEYS.contains(key)) {
                logger.warning("[IVillager] Unknown top-level config key: " + key);
            }
        }

        String defaultShop = config.getString("default_shop", null);
        if (defaultShop != null) defaultShop = defaultShop.trim();
        if (defaultShop != null && defaultShop.isEmpty()) defaultShop = null;

        ConfigurationSection shopsSection = config.getConfigurationSection("shops");
        Map<String, ShopDefinition> shops = new java.util.HashMap<>();
        if (shopsSection != null) {
            for (String shopId : shopsSection.getKeys(false)) {
                ConfigurationSection shopSec = shopsSection.getConfigurationSection(shopId);
                if (shopSec == null) continue;
                for (String key : shopSec.getKeys(false)) {
                    if (!SHOP_KEYS.contains(key)) {
                        logger.warning("[IVillager] Unknown shop key '" + key + "' in shop '" + shopId + "'");
                    }
                }
                ShopDefinition def = parseShop(shopId, shopSec);
                if (def != null) {
                    shops.put(shopId.toLowerCase(Locale.ROOT), def);
                    if (def.getTrades().isEmpty()) {
                        logger.info("[IVillager] Shop '" + shopId + "' has no valid trades (empty or all invalid).");
                    }
                }
            }
        }
        return new ConfigResult(shops, defaultShop);
    }

    private ShopDefinition parseShop(String shopId, ConfigurationSection shopSec) {
        String displayName = shopSec.getString("display_name", "IVillager");
        String profession = shopSec.getString("profession", null);
        int level = shopSec.getInt("level", 1);
        int maxUses = shopSec.getInt("max_uses", DEFAULT_MAX_USES);
        int experience = shopSec.getInt("experience", 0);
        double priceMultiplier = shopSec.getDouble("price_multiplier", DEFAULT_PRICE_MULTIPLIER);
        int buyXp = shopSec.getInt("buy_xp", 0);

        // YAML list entries ("- item: x / trade: y") are parsed as Map by Bukkit, not ConfigurationSection.
        // Use getMapList to get List<Map<String,Object>> and parse each map.
        List<TradeDefinition> trades = new ArrayList<>();
        List<? extends Map<?, ?>> mapList = shopSec.getMapList("trades");
        if (mapList == null) mapList = List.of();
        for (int i = 0; i < mapList.size(); i++) {
            Map<?, ?> map = mapList.get(i);
            TradeDefinition td = parseTradeFromMap(map, shopId, i);
            if (td != null) trades.add(td);
        }
        return new ShopDefinition(
                shopId,
                displayName,
                trades,
                profession,
                level,
                maxUses,
                experience,
                priceMultiplier,
                buyXp
        );
    }

    /**
     * Parse a single trade from a map (from getMapList). YAML list entries are Maps in Bukkit, not ConfigurationSections.
     */
    private TradeDefinition parseTradeFromMap(Map<?, ?> map, String shopId, int index) {
        List<ItemStack> ingredients = new ArrayList<>();
        Object itemObj = map.get("item");
        if (itemObj instanceof String) {
            ItemStack one = ItemParser.parseItem((String) itemObj, logger);
            if (one != null) ingredients.add(one);
        } else if (itemObj instanceof List) {
            for (Object o : (List<?>) itemObj) {
                if (o instanceof String) {
                    ItemStack s = ItemParser.parseItem((String) o, logger);
                    if (s != null) ingredients.add(s);
                }
            }
        }
        if (ingredients.isEmpty()) {
            logger.warning("[IVillager] Shop '" + shopId + "' trade " + index + ": no valid item(s), skipping");
            return null;
        }
        if (ingredients.size() > 2) {
            logger.warning("[IVillager] Shop '" + shopId + "' trade " + index + ": more than 2 ingredients; using first two.");
            ingredients = ingredients.subList(0, 2);
        }

        Object tradeObj = map.get("trade");
        List<ItemStack> resultList = new ArrayList<>();
        if (tradeObj instanceof String) {
            List<ItemStack> parsed = ItemParser.parseItemList((String) tradeObj, logger);
            resultList.addAll(parsed);
        } else if (tradeObj instanceof List) {
            for (Object o : (List<?>) tradeObj) {
                if (o instanceof String) {
                    ItemStack s = ItemParser.parseItem((String) o, logger);
                    if (s != null) resultList.add(s);
                }
            }
        }
        ItemStack result = ItemParser.singleResult(resultList);
        if (result == null) {
            logger.warning("[IVillager] Shop '" + shopId + "' trade " + index + ": no valid result, skipping");
            return null;
        }

        Object encObj = map.get("enchantments");
        if (encObj instanceof String) {
            String enchantments = (String) encObj;
            if (!enchantments.isBlank()) {
                ItemParser.applyEnchantments(result, enchantments, logger);
            }
        }
        return new TradeDefinition(ingredients, result);
    }

    /**
     * Add a new shop to config and save. Call from main thread.
     */
    public boolean createShop(String shopId) {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        ConfigurationSection shops = config.getConfigurationSection("shops");
        if (shops == null) {
            shops = config.createSection("shops");
        }
        String key = shopId.trim();
        if (shops.get(key) != null) {
            return false;
        }
        ConfigurationSection newShop = shops.createSection(key);
        newShop.set("display_name", "IVillager");
        List<Map<String, Object>> trades = new ArrayList<>();
        Map<String, Object> one = new java.util.HashMap<>();
        one.put("item", "cobblestone:64");
        one.put("trade", "diamond:1");
        trades.add(one);
        newShop.set("trades", trades);
        try {
            config.save(configFile);
        } catch (IOException e) {
            logger.severe("[IVillager] Failed to save config: " + e.getMessage());
            return false;
        }
        return true;
    }

    /**
     * Remove a shop from config and save. Call from main thread.
     */
    public boolean deleteShop(String shopId) {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        ConfigurationSection shops = config.getConfigurationSection("shops");
        if (shops == null) return false;
        String key = shopId.trim();
        if (shops.get(key) == null) return false;
        config.set("shops." + key, null);
        try {
            config.save(configFile);
        } catch (IOException e) {
            logger.severe("[IVillager] Failed to save config: " + e.getMessage());
            return false;
        }
        return true;
    }

    public File getConfigFile() {
        return configFile;
    }

    public static final class ConfigResult {
        private final Map<String, ShopDefinition> shops;
        private final String defaultShop;

        public ConfigResult(Map<String, ShopDefinition> shops, String defaultShop) {
            this.shops = shops != null ? new java.util.HashMap<>(shops) : new java.util.HashMap<>();
            this.defaultShop = defaultShop;
        }

        public Map<String, ShopDefinition> getShops() {
            return new java.util.HashMap<>(shops);
        }

        public String getDefaultShop() {
            return defaultShop;
        }
    }
}
