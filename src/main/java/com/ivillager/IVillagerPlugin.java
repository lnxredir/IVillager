package com.ivillager;

import com.ivillager.command.IVillagerCommand;
import com.ivillager.config.ConfigLoader;
import com.ivillager.shop.ShopManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collection;

/**
 * IVillager: opens the vanilla villager trading UI as a configurable shop.
 * Target: Paper 1.21.11; forward-compatible with 1.21.x.
 */
public final class IVillagerPlugin extends JavaPlugin {

    private ConfigLoader configLoader;
    private ShopManager shopManager;

    @Override
    public void onEnable() {
        configLoader = new ConfigLoader(this);
        shopManager = new ShopManager();
        loadShops();
        registerCommands();
        getLogger().info("IVillager enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("IVillager disabled.");
    }

    private void loadShops() {
        ConfigLoader.ConfigResult result = configLoader.load();
        shopManager.load(result.getShops(), result.getDefaultShop());
    }

    private void registerCommands() {
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            IVillagerCommand cmd = new IVillagerCommand(this, shopManager);
            event.registrar().register("ivillager", "Open IVillager shop or run admin subcommands", java.util.List.of("ivl"), cmd);
        });
    }

    /**
     * Reload config.yml and shop definitions. Call from main thread.
     */
    public void reloadConfigAndShops() {
        reloadConfig();
        loadShops();
        getLogger().info("Config reloaded, " + shopManager.getShopNames().size() + " shops loaded.");
    }

    /**
     * Create a new shop in config with one example trade and reload. Call from main thread.
     */
    public boolean createShop(String shopName) {
        if (configLoader.createShop(shopName)) {
            loadShops();
            return true;
        }
        return false;
    }

    /**
     * Delete a shop from config and reload. Call from main thread.
     */
    public boolean deleteShop(String shopName) {
        if (configLoader.deleteShop(shopName)) {
            loadShops();
            return true;
        }
        return false;
    }

    public Collection<String> getShopNames() {
        return shopManager.getShopNames();
    }
}
