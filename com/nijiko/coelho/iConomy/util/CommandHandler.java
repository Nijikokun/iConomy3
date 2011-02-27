package com.nijiko.coelho.iConomy.util;

import com.nijiko.coelho.iConomy.iConomy;
import java.util.List;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 *
 * @author Wolfwood and ChatBukkit author
 */

public abstract class CommandHandler {

    protected final iConomy plugin;

    public CommandHandler(iConomy plugin) {
        this.plugin = plugin;
    }

    public abstract boolean perform(CommandSender sender, String[] args);

    protected static boolean anonymousCheck(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Cannot execute that command, I don't know who you are!");
            return true;
        } else {
            return false;
        }
    }

    protected static Player getPlayer(CommandSender sender, String[] args, int index) {
        if (args.length > index) {
            List<Player> players = sender.getServer().matchPlayer(args[index]);

            if (players.isEmpty()) {
                sender.sendMessage("I don't know who '" + args[index] + "' is!");
                return null;
            } else {
                return players.get(0);
            }
        } else {
            if (anonymousCheck(sender)) {
                return null;
            } else {
                return (Player) sender;
            }
        }
    }

    protected static boolean getPermissions(CommandSender sender, String node) {
        try {
            return iConomy.Permissions.has((Player) sender, node);
        } catch (NullPointerException e) {
            iConomy.log.warning("Permissions not working for InventorySort defaulting to Op");
            return sender.isOp();
        }
    }
}
