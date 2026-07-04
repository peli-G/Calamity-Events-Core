package com.peli.calamityevents.commands;

import com.peli.calamityevents.CalamityEventsCore;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class SetWarpCommand implements CommandExecutor, TabCompleter {

    private final CalamityEventsCore plugin;

    public SetWarpCommand(CalamityEventsCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used in-game.");
            return true;
        }
        if (args.length != 1) {
            sender.sendMessage("§cUsage: /setwarp <name>");
            return true;
        }

        Player player = (Player) sender;
        String warpName = args[0];

        if (!plugin.isWarpWorld(player.getWorld())) {
            player.sendMessage("§cThis world isn't configured for warps. Add it to config.yml under warp-worlds.");
            plugin.debug("SetWarp: " + player.getName() + " tried to set a warp in unconfigured world '" + player.getWorld().getName() + "'.");
            return true;
        }

        Location loc = player.getLocation();
        plugin.getWarpManager().setWarp(player.getWorld(), warpName, loc);

        player.sendMessage(String.format(
                "§aSet warp §e%s §ain §e%s §aat §e(%.2f, %.2f, %.2f)",
                warpName, player.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ()
        ));
        plugin.debug("SetWarp: " + player.getName() + " set warp '" + warpName + "' in '" + player.getWorld().getName() + "'.");
        return true;
    }
}
