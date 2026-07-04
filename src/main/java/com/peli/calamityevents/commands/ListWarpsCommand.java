package com.peli.calamityevents.commands;

import com.peli.calamityevents.CalamityEventsCore;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public class ListWarpsCommand implements CommandExecutor, TabCompleter {

    private final CalamityEventsCore plugin;

    public ListWarpsCommand(CalamityEventsCore plugin) {
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

        Player player = (Player) sender;

        if (!plugin.isWarpWorld(player.getWorld())) {
            player.sendMessage("§cThis world isn't configured for warps.");
            return true;
        }

        Set<String> warps = plugin.getWarpManager().listWarps(player.getWorld());
        if (warps.isEmpty()) {
            player.sendMessage("§7No warps set in " + player.getWorld().getName() + " yet.");
            return true;
        }

        player.sendMessage("§6§lWarps in " + player.getWorld().getName() + "§6§l: §e" + String.join("§7, §e", warps));
        return true;
    }
}
