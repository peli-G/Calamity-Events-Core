package com.peli.calamityevents.commands;

import com.peli.calamityevents.CalamityEventsCore;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class EventQueueAdminCommand implements CommandExecutor {

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

        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&eUsage: /eventqueue reload"));
        return true;
    }
}
