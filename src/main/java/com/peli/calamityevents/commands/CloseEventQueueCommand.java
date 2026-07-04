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

import java.util.Collections;
import java.util.List;

public class CloseEventQueueCommand implements CommandExecutor, TabCompleter {

    private final CalamityEventsCore plugin;

    public CloseEventQueueCommand(CalamityEventsCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!plugin.isLuckPermsAvailable()) {
            sender.sendMessage("§cLuckPerms is not installed — cannot modify group permissions.");
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
                group.data().remove(Node.builder("queue.join.event").value(true).build());
                lp.getGroupManager().saveGroup(group);
                plugin.setQueueOpen(false);
                plugin.setTrustedOnlyMode(false);
                sender.sendMessage("§c§lQueue Closed.");
                QueueInfoCommand.send(sender, plugin);
                plugin.debug("Event queue closed by " + sender.getName() + " — queue.join.event removed from default group, trusted-only mode cleared.");
            });
        });
        return true;
    }
}
