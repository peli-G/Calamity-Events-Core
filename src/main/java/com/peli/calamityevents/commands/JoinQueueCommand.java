package com.peli.calamityevents.commands;

import com.peli.calamityevents.CalamityEventsCore;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

/**
 * Player-facing queue join command. Ported from the original Skript's
 * /joinevent, with additions:
 *  - teleports to the plugin's own queue spawn (set via /setqueuespawn)
 *    rather than shelling out to a Multiverse "mvtp ... --unsafe" console
 *    command, since we already track that location ourselves.
 *  - requires a queue operator to be present in the event world, same as
 *    the QueueGateListener's teleport-based gate.
 *  - queue operators (queue.join.op) bypass all of the gating below — they
 *    ARE the presence requirement, so they can't be locked out by "no
 *    operator online" (that would make it impossible for the very first
 *    operator to ever get in), and they aren't subject to "queue closed"
 *    or "no queue spawn set" either, since those are player-queue concepts
 *    that don't apply to staff.
 */
public class JoinQueueCommand implements CommandExecutor, TabCompleter {

    private final CalamityEventsCore plugin;

    public JoinQueueCommand(CalamityEventsCore plugin) {
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

        if (player.hasPermission(CalamityEventsCore.QUEUE_OPERATOR_PERMISSION)) {
            if (plugin.getQueueSpawnManager().hasQueueSpawn()) {
                Location spawn = plugin.getQueueSpawnManager().getQueueSpawn();
                player.teleport(spawn);
                player.sendMessage("§aTeleported to the queue spawn.");
                plugin.debug("JoinQueue: " + player.getName() + " (queue operator) teleported to queue spawn — bypassed all queue gating.");
            } else {
                player.sendMessage("§eNo queue spawn is set yet. Use /setqueuespawn once you're in the event world.");
                plugin.debug("JoinQueue: " + player.getName() + " (queue operator) — no queue spawn set, not blocking.");
            }
            return true;
        }

        if (plugin.isTrustedOnlyMode()) {
            if (!player.hasPermission(CalamityEventsCore.QUEUE_TRUSTED_PERMISSION)) {
                player.sendMessage("§cThe queue is currently trusted-only.");
                plugin.debug("JoinQueue: " + player.getName() + " tried to join during trusted-only mode without queue.join.trusted.");
                return true;
            }
        } else if (!player.hasPermission("queue.join.event")) {
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
