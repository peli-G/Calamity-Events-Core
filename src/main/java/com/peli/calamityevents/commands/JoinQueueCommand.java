package com.peli.calamityevents.commands;

import com.peli.calamityevents.CalamityEventsCore;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Player-facing queue join command. Ported from the original Skript's
 * /joinevent, with two additions:
 *  - teleports to the plugin's own queue spawn (set via /setqueuespawn)
 *    rather than shelling out to a Multiverse "mvtp ... --unsafe" console
 *    command, since we already track that location ourselves.
 *  - requires a queue operator to be present in the event world, same as
 *    the QueueGateListener's teleport-based gate.
 */
public class JoinQueueCommand implements CommandExecutor {

    private final CalamityEventsCore plugin;

    public JoinQueueCommand(CalamityEventsCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used in-game.");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("queue.join.event")) {
            player.sendMessage("§cThe queue is closed.");
            plugin.debug("JoinQueue: " + player.getName() + " tried to join without queue.join.event.");
            return true;
        }

        if (!plugin.getQueueSpawnManager().hasQueueSpawn()) {
            player.sendMessage("§cThe queue spawn hasn't been set yet. Ask an admin to run /setqueuespawn.");
            plugin.debug("JoinQueue: " + player.getName() + " tried to join but no queue spawn is set.");
            return true;
        }

        Location spawn = plugin.getQueueSpawnManager().getQueueSpawn();
        World targetWorld = spawn.getWorld();

        if (!plugin.isQueueOperatorPresent(targetWorld)) {
            player.sendMessage("§cYou cannot join the queue if no Operator players are online!");
            plugin.debug("JoinQueue: " + player.getName() + " blocked — no queue operator present in '" + targetWorld.getName() + "'.");
            return true;
        }

        player.teleport(spawn);
        player.sendMessage("§aJoining the event queue...");
        plugin.debug("JoinQueue: " + player.getName() + " joined the queue, teleported to queue spawn in '" + targetWorld.getName() + "'.");
        return true;
    }
}
