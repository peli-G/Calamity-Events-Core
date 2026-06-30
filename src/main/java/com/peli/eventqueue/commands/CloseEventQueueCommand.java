package com.peli.eventqueue.commands;

import com.peli.eventqueue.EventQueuePlugin;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.node.Node;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class CloseEventQueueCommand implements CommandExecutor {

    private final EventQueuePlugin plugin;

    public CloseEventQueueCommand(EventQueuePlugin plugin) {
        this.plugin = plugin;
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
                sender.sendMessage("§cEvent Queue is now closed.");
                plugin.debug("Event queue closed by " + sender.getName() + " — queue.join.event removed from default group.");
            });
        });
        return true;
    }
}
