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

public class OpenEventQueueCommand implements CommandExecutor {

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

        LuckPerms lp = LuckPermsProvider.get();
        lp.getGroupManager().loadGroup("default").thenAccept(opt -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (opt.isEmpty()) {
                    sender.sendMessage("§cCould not find the 'default' group in LuckPerms.");
                    return;
                }
                Group group = opt.get();
                group.data().add(Node.builder("queue.join.event").value(true).build());
                lp.getGroupManager().saveGroup(group);
                plugin.setQueueOpen(true);
                sender.sendMessage("§a§lQueue Opened!");
                QueueInfoCommand.send(sender, plugin);
                plugin.debug("Event queue opened by " + sender.getName() + " — queue.join.event granted to default group.");
            });
        });
        return true;
    }
}
