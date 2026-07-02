package com.peli.calamityevents.commands;

import com.peli.calamityevents.CalamityEventsCore;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class QueueInfoCommand implements CommandExecutor {

    private final CalamityEventsCore plugin;

    public QueueInfoCommand(CalamityEventsCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        send(sender, plugin);
        return true;
    }

    /**
     * Prints the queue info block to any CommandSender. Shared by:
     *  - /queueinfo
     *  - /openqueue and /closequeue (appended after their status message)
     *  - the auto-display shown to queue.join.op holders on entering the event world
     */
    public static void send(CommandSender sender, CalamityEventsCore plugin) {
        sender.sendMessage("§8§m--------------------");
        sender.sendMessage("§6§lCalamityEventsCore Info");

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
    }
}
