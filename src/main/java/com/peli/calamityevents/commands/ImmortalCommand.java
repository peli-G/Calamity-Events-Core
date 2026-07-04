package com.peli.calamityevents.commands;

import com.peli.calamityevents.CalamityEventsCore;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * /immortal [player] — toggles immortal mode, OP only (immortal.use
 * defaults to op).
 *
 * While immortal, a player takes damage as normal but can never drop below
 * half a heart — see ImmortalListener for the actual clamping. If they're
 * holding a Totem of Undying in either hand when lethal damage would land,
 * the totem pops as usual; immortality only kicks in when there's no totem
 * to save them.
 */
public class ImmortalCommand implements CommandExecutor, TabCompleter {

    private final CalamityEventsCore plugin;

    public ImmortalCommand(CalamityEventsCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player target;

        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("§cConsole must specify a player: /immortal <player>");
                return true;
            }
            target = (Player) sender;
        } else if (args.length == 1) {
            target = Bukkit.getPlayerExact(args[0]);
            if (target == null) {
                sender.sendMessage("§cPlayer '" + args[0] + "' isn't online.");
                return true;
            }
        } else {
            sender.sendMessage("§cUsage: /immortal [player]");
            return true;
        }

        boolean nowImmortal = plugin.toggleImmortal(target);

        String stateText = nowImmortal ? "§dimmortal" : "§7no longer immortal";
        sender.sendMessage("§a" + target.getName() + " is now " + stateText + "§a.");
        if (!target.equals(sender)) {
            target.sendMessage(nowImmortal
                    ? "§dYou are now immortal — you can't die from normal damage below half a heart."
                    : "§7You are no longer immortal.");
        }
        plugin.debug("Immortal: " + sender.getName() + " set " + target.getName() + "'s immortal state to " + nowImmortal + ".");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) return Collections.emptyList();

        List<String> names = Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
        List<String> results = new ArrayList<>();
        StringUtil.copyPartialMatches(args[0], names, results);
        Collections.sort(results);
        return results;
    }
}
