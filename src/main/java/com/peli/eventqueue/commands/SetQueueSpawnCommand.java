package com.peli.eventqueue.commands;

import com.peli.eventqueue.EventQueuePlugin;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SetQueueSpawnCommand implements CommandExecutor {

    private final EventQueuePlugin plugin;

    public SetQueueSpawnCommand(EventQueuePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used in-game.");
            return true;
        }

        Player player = (Player) sender;

        if (!plugin.isEventWorld(player.getWorld())) {
            player.sendMessage("§cYou can only set the queue spawn in an event world.");
            return true;
        }

        Location loc = player.getLocation();
        plugin.getQueueSpawnManager().setQueueSpawn(loc);

        // e.g.  Set queue spawn in minecraft:overworld at (100.50, 64.00, -200.30) yaw: 45.00  pitch: 0.00
        String worldKey = "minecraft:" + loc.getWorld().getName().toLowerCase();
        player.sendMessage(String.format(
                "§aSet queue spawn in §e%s §aat §e(%.2f, %.2f, %.2f) §ayaw: §e%.2f §apitch: §e%.2f",
                worldKey, loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch()
        ));
        return true;
    }
}
