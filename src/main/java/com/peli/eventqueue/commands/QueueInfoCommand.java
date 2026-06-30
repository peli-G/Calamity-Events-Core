package com.peli.eventqueue.commands;

import com.peli.eventqueue.EventQueuePlugin;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class QueueInfoCommand implements CommandExecutor {

    private final EventQueuePlugin plugin;

    public QueueInfoCommand(EventQueuePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        sender.sendMessage("§8§m--------------------");
        sender.sendMessage("§6§lEventQueueSystem Info");

        String queueStatus = plugin.isQueueOpen() ? "§aOPEN" : "§cCLOSED";
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
        return true;
    }
}
