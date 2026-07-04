package com.peli.calamityevents.commands;

import com.peli.calamityevents.CalamityEventsCore;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DelWarpCommand implements CommandExecutor, TabCompleter {

    private final CalamityEventsCore plugin;

    public DelWarpCommand(CalamityEventsCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used in-game.");
            return true;
        }
        if (args.length != 1) {
            sender.sendMessage("§cUsage: /delwarp <name>");
            return true;
        }

        Player player = (Player) sender;
        String warpName = args[0];

        if (!plugin.isWarpWorld(player.getWorld())) {
            player.sendMessage("§cThis world isn't configured for warps.");
            return true;
        }

        boolean removed = plugin.getWarpManager().deleteWarp(player.getWorld(), warpName);
        if (!removed) {
            player.sendMessage("§cNo warp named §e" + warpName + " §cin this world.");
            return true;
        }

        player.sendMessage("§aDeleted warp §e" + warpName + " §ain " + player.getWorld().getName() + ".");
        plugin.debug("DelWarp: " + player.getName() + " deleted warp '" + warpName + "' in '" + player.getWorld().getName() + "'.");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player) || args.length != 1) return Collections.emptyList();
        Player player = (Player) sender;

        List<String> results = new ArrayList<>();
        StringUtil.copyPartialMatches(args[0], plugin.getWarpManager().listWarps(player.getWorld()), results);
        Collections.sort(results);
        return results;
    }
}
