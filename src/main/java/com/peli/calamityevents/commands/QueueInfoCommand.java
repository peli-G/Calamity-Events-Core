package com.peli.calamityevents.commands;

import com.peli.calamityevents.CalamityEventsCore;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.Collections;
import java.util.List;

public class QueueInfoCommand implements CommandExecutor, TabCompleter {

    private final CalamityEventsCore plugin;

    public QueueInfoCommand(CalamityEventsCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        send(sender, plugin);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }

    /**
     * Prints the queue info block to any CommandSender. Shared by:
     *  - /queueinfo
     *  - /openqueue and /closequeue (appended after their status message)
     *  - the auto-display shown to queue.join.op holders on entering the event world
     */
    public static void send(CommandSender sender, CalamityEventsCore plugin) {
        sender.sendMessage("§8§m--------------------");
        sender.sendMessage(net.md_5.bungee.api.ChatColor.of("#FD4866") + "" + net.md_5.bungee.api.ChatColor.BOLD + "Calamity Events Queue Info");

        String queueStatus;
        if (!plugin.isQueueOpen()) {
            queueStatus = "§cCLOSED";
        } else if (plugin.isTrustedOnlyMode()) {
            queueStatus = "§eOPEN §7(§eTrusted Only§7)";
        } else {
            queueStatus = "§aOPEN";
        }
        sender.sendMessage("§7Queue: " + queueStatus);

        if (plugin.getQueueSpawnManager().hasQueueSpawn()) {
            Location loc = plugin.getQueueSpawnManager().getQueueSpawn();
            String worldKey = "minecraft:" + loc.getWorld().getName().toLowerCase();
            sender.sendMessage(String.format(
                    "§7Queue spawn: §e%s §7at §e(%.2f, %.2f, %.2f) §7yaw: §e%.2f §7pitch: §e%.2f",
                    worldKey, loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch()
            ));
        } else {
            sender.sendMessage("§7Queue spawn: §cnot set §7(use /setqueuespawn)");
        }

        sender.sendMessage("§7Event worlds: §e" + plugin.getEventWorldNames());
        sender.sendMessage("§8§m--------------------");
    }
}
