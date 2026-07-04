package com.peli.calamityevents.commands;

import com.peli.calamityevents.CalamityEventsCore;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.node.Node;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * /openqueue        — opens the queue to everyone (grants queue.join.event
 *                      to the default group).
 * /openqueue trusted — opens the queue to trusted players only. Revokes
 *                      queue.join.event from the default group (in case the
 *                      queue was previously public) and switches into
 *                      trusted-only mode, where JoinQueueCommand and
 *                      QueueGateListener require queue.join.trusted instead.
 *                      Trusted players still land on the same queue spawn.
 */
public class OpenEventQueueCommand implements CommandExecutor, TabCompleter {

    private final CalamityEventsCore plugin;

    public OpenEventQueueCommand(CalamityEventsCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!plugin.isLuckPermsAvailable()) {
            sender.sendMessage("§cLuckPerms is not installed — cannot modify group permissions.");
            return true;
        }

        boolean trusted = args.length == 1 && args[0].equalsIgnoreCase("trusted");
        if (args.length > 0 && !trusted) {
            sender.sendMessage("§cUsage: /openqueue [trusted]");
            return true;
        }

        LuckPerms lp = LuckPermsProvider.get();
        lp.getGroupManager().loadGroup("default").thenAccept(opt -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (opt.isEmpty()) {
                    sender.sendMessage("§cCould not find the 'default' group in LuckPerms.");
                    return;
                }
                Group group = opt.get();

                if (trusted) {
                    // Restrict to trusted only: make sure the default group
                    // doesn't still have public access from an earlier open.
                    group.data().remove(Node.builder("queue.join.event").value(true).build());
                    lp.getGroupManager().saveGroup(group);
                    plugin.setTrustedOnlyMode(true);
                    plugin.setQueueOpen(true);
                    sender.sendMessage("§a§lQueue Opened (Trusted Only)!");
                    QueueInfoCommand.send(sender, plugin);
                    plugin.debug("Event queue opened (trusted only) by " + sender.getName()
                            + " — queue.join.event revoked from default group, queue.join.trusted required.");
                } else {
                    group.data().add(Node.builder("queue.join.event").value(true).build());
                    lp.getGroupManager().saveGroup(group);
                    plugin.setTrustedOnlyMode(false);
                    plugin.setQueueOpen(true);
                    sender.sendMessage("§a§lQueue Opened!");
                    QueueInfoCommand.send(sender, plugin);
                    plugin.debug("Event queue opened by " + sender.getName() + " — queue.join.event granted to default group.");
                }
            });
        });
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) return Collections.emptyList();
        List<String> results = new ArrayList<>();
        StringUtil.copyPartialMatches(args[0], Collections.singletonList("trusted"), results);
        return results;
    }
}
