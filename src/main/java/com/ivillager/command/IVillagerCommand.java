package com.ivillager.command;

import com.ivillager.IVillagerPlugin;
import com.ivillager.shop.ShopManager;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Handles /ivillager and /ivl: open shop, reload, create, delete.
 * Feedback is minimal and only to the command sender (admin-only for errors/confirmations).
 */
public final class IVillagerCommand implements BasicCommand {

    private final IVillagerPlugin plugin;
    private final ShopManager shopManager;

    public IVillagerCommand(IVillagerPlugin plugin, ShopManager shopManager) {
        this.plugin = plugin;
        this.shopManager = shopManager;
    }

    @Override
    public void execute(CommandSourceStack source, String[] args) {
        if (args.length == 0) {
            openDefaultOrMessage(source);
            return;
        }
        String first = args[0].trim().toLowerCase(Locale.ROOT);
        if ("reload".equals(first)) {
            doReload(source);
            return;
        }
        if ("delete".equals(first)) {
            if (args.length < 2) {
                send(source, Component.text("Usage: /ivillager delete <shop name>", NamedTextColor.RED));
                return;
            }
            doDelete(source, args[1].trim());
            return;
        }
        if ("create".equals(first)) {
            if (args.length < 2) {
                send(source, Component.text("Usage: /ivillager create <shop name>", NamedTextColor.RED));
                return;
            }
            doCreate(source, args[1].trim());
            return;
        }
        if ("list".equals(first)) {
            doList(source);
            return;
        }
        openShop(source, first);
    }

    private void openDefaultOrMessage(CommandSourceStack source) {
        String defaultShop = shopManager.getDefaultShopName();
        if (defaultShop != null && shopManager.hasShop(defaultShop)) {
            if (!(source.getExecutor() instanceof Player player)) {
                send(source, Component.text("Only players can open shops.", NamedTextColor.RED));
                return;
            }
            if (!canOpenShop(player, defaultShop)) {
                send(source, Component.text("You do not have permission to open this shop.", NamedTextColor.RED));
                return;
            }
            shopManager.openShop(player, defaultShop);
            return;
        }
        send(source, Component.text("No default shop set. Use /ivillager <shop> or set default_shop in config.", NamedTextColor.GRAY));
    }

    private void openShop(CommandSourceStack source, String shopName) {
        if (!(source.getExecutor() instanceof Player player)) {
            send(source, Component.text("Only players can open shops.", NamedTextColor.RED));
            return;
        }
        if (!shopManager.hasShop(shopName)) {
            send(source, Component.text("Unknown shop: " + shopName, NamedTextColor.RED));
            return;
        }
        if (!canOpenShop(player, shopName)) {
            send(source, Component.text("You do not have permission to open this shop.", NamedTextColor.RED));
            return;
        }
        shopManager.openShop(player, shopName);
    }

    /**
     * Permission: ivillager.admin bypasses; else ivillager.use allows all unless
     * only specific ivillager.use.<shopname> is set (then only that shop).
     */
    boolean canOpenShop(Player player, String shopName) {
        if (player.hasPermission("ivillager.admin")) return true;
        if (player.hasPermission("ivillager.use")) return true;
        String node = "ivillager.use." + shopName.toLowerCase(Locale.ROOT);
        return player.hasPermission(node);
    }

    private void doReload(CommandSourceStack source) {
        if (!source.getSender().hasPermission("ivillager.reload") && !source.getSender().hasPermission("ivillager.admin")) {
            send(source, Component.text("You do not have permission to reload.", NamedTextColor.RED));
            return;
        }
        plugin.reloadConfigAndShops();
        send(source, Component.text("Config reloaded.", NamedTextColor.GREEN));
    }

    private void doDelete(CommandSourceStack source, String shopName) {
        if (!source.getSender().hasPermission("ivillager.delete") && !source.getSender().hasPermission("ivillager.admin")) {
            send(source, Component.text("You do not have permission to delete shops.", NamedTextColor.RED));
            return;
        }
        if (!shopManager.hasShop(shopName)) {
            send(source, Component.text("Unknown shop: " + shopName, NamedTextColor.RED));
            return;
        }
        if (plugin.deleteShop(shopName)) {
            send(source, Component.text("Shop '" + shopName + "' deleted.", NamedTextColor.GREEN));
        } else {
            send(source, Component.text("Failed to delete shop.", NamedTextColor.RED));
        }
    }

    private void doList(CommandSourceStack source) {
        java.util.Collection<String> names = plugin.getShopNames();
        if (names.isEmpty()) {
            send(source, Component.text("No shops defined. Use /ivillager create <name> or edit config.yml.", NamedTextColor.GRAY));
            return;
        }
        String list = String.join(", ", names);
        send(source, Component.text("Shops: " + list, NamedTextColor.GRAY));
    }

    private void doCreate(CommandSourceStack source, String shopName) {
        if (!source.getSender().hasPermission("ivillager.admin")) {
            send(source, Component.text("You do not have permission to create shops.", NamedTextColor.RED));
            return;
        }
        if (shopManager.hasShop(shopName)) {
            send(source, Component.text("Shop already exists: " + shopName, NamedTextColor.RED));
            return;
        }
        if (plugin.createShop(shopName)) {
            send(source, Component.text("Shop '" + shopName + "' created. Edit config.yml to add or modify trades.", NamedTextColor.GREEN));
        } else {
            send(source, Component.text("Failed to create shop (check logs).", NamedTextColor.RED));
        }
    }

    private void send(CommandSourceStack source, Component message) {
        source.getSender().sendMessage(message);
    }

    @Override
    public Collection<String> suggest(CommandSourceStack source, String[] args) {
        if (args.length == 0) {
            List<String> out = new ArrayList<>(List.of("list", "reload", "create", "delete"));
            out.addAll(plugin.getShopNames());
            return out;
        }
        if (args.length == 1) {
            String partial = args[0].toLowerCase(Locale.ROOT);
            List<String> out = new ArrayList<>();
            for (String s : List.of("list", "reload", "create", "delete")) {
                if (s.startsWith(partial)) out.add(s);
            }
            for (String shopName : plugin.getShopNames()) {
                if (shopName.toLowerCase(Locale.ROOT).startsWith(partial)) {
                    out.add(shopName);
                }
            }
            return out;
        }
        if (args.length == 2 && ("create".equals(args[0].toLowerCase(Locale.ROOT)) || "delete".equals(args[0].toLowerCase(Locale.ROOT)))) {
            String partial = args[1].toLowerCase(Locale.ROOT);
            return plugin.getShopNames().stream()
                    .filter(s -> s.toLowerCase(Locale.ROOT).startsWith(partial))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
