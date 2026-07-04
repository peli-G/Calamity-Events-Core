package com.peli.calamityevents.commands;

import com.peli.calamityevents.CalamityEventsCore;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class EventQueueAdminCommand implements CommandExecutor, TabCompleter {

    private final CalamityEventsCore plugin;

    public EventQueueAdminCommand(CalamityEventsCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            plugin.loadSettings();
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&aCalamityEventsCore config reloaded."));
            return true;
        }

        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&eUsage: /calamityeventscore reload"));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) return Collections.emptyList();
        List<String> results = new ArrayList<>();
        StringUtil.copyPartialMatches(args[0], Collections.singletonList("reload"), results);
        return results;
    }
}
