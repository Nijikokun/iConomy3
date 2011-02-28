package com.nijiko.coelho.iConomy.Commands;

import com.nijiko.coelho.iConomy.iConomy;
import com.nijiko.coelho.iConomy.util.CommandHandler;
import com.nijiko.coelho.iConomy.system.Account;
import com.nijiko.coelho.iConomy.util.Messaging;
import com.nijiko.coelho.iConomy.util.Misc;
import com.nijiko.coelho.iConomy.util.Template;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 *
 * @author Wolfwood and original writer of iPlayerListener
 */
public class moneyCommand extends CommandHandler {

    private CommandHelpers ch;
    public Misc Misc = new Misc();
    private Template Template = null;

    public moneyCommand(iConomy plugin) {
        super(plugin);
        Template = new Template(plugin.getDataFolder().getPath(), "Messages.tpl");
        ch = new CommandHelpers(plugin, Template);

    }

    @Override
    public boolean perform(CommandSender sender, String[] split) {
        Player player = (Player) sender;
        Messaging.save(sender);
        switch (split.length) {
            case 0:

                ch.showBalance("", player, true);
                return true;

            case 1:

                if (Misc.isAny(split[0], new String[]{"rank", "-r"})) {
                    if (!getPermissions(sender, "iConomy.rank")) {
                        return true;
                    }

                    ch.showRank(player, player.getName());

                    return true;
                }

                if (Misc.isAny(split[0], new String[]{"top", "-t"})) {
                    if (!getPermissions(sender, "iConomy.list")) {
                        return true;
                    }

                    ch.showTop(player, 5);

                    return true;
                }

                if (Misc.isAny(split[0], new String[]{"stats", "-s"})) {
                    if (!getPermissions(sender, "iConomy.admin.stats")) {
                        return true;
                    }

                    Collection<Account> money = iConomy.getBank().getAccounts().values();
                    double totalMoney = 0;
                    int totalPlayers = money.size();

                    for (Object o : money.toArray()) {
                        totalMoney += ((Account) o).getBalance();
                    }

                    try {
                        Messaging.send(Template.color("statistics.opening"));

                        Messaging.send(Template.parse("statistics.total",
                                new String[]{"+currency,+c", "+amount,+money,+a,+m"},
                                new Object[]{iConomy.getBank().getCurrency(), iConomy.getBank().format(totalMoney)}));

                        Messaging.send(Template.parse("statistics.average",
                                new String[]{"+currency,+c", "+amount,+money,+a,+m"},
                                new Object[]{iConomy.getBank().getCurrency(), iConomy.getBank().format(totalMoney / totalPlayers)}));

                        Messaging.send(Template.parse("statistics.accounts",
                                new String[]{"+currency,+c", "+amount,+accounts,+a"},
                                new Object[]{iConomy.getBank().getCurrency(), totalPlayers}));
                    } catch (Exception e) {
                        iConomy.log.severe("[iConomy] Something went wrong when trying to display stats. " + e.getMessage());
                    }

                    return true;
                }

                if (Misc.isAny(split[0],
                        new String[]{"help", "?", "grant", "-g", "reset", "-x", "set", "-s", "pay", "-p"})) {

                    ch.showSimpleHelp();

                    return true;
                } else {
                    if (!getPermissions(sender, "iConomy.access")) {
                        return true;
                    }

                    if (iConomy.getBank().hasAccount(split[0])) {
                        ch.showBalance(split[0], player, false);
                    } else {
                        Messaging.send(Template.parse("Error.account", new String[]{"+name,+n"}, new String[]{split[0]}));
                    }

                    return true;
                }

            case 2:

                if (Misc.isAny(split[0], new String[]{"rank", "-r"})) {
                    if (!getPermissions(sender, "iConomy.rank")) {
                        return true;
                    }

                    if (iConomy.getBank().hasAccount(split[1])) {
                        ch.showRank(player, split[1]);
                    } else {
                        Messaging.send(Template.parse("Error.account", new String[]{"+name,+n"}, new String[]{split[1]}));
                    }

                    return true;
                }

                if (Misc.isAny(split[0], new String[]{"top", "-t"})) {
                    if (!getPermissions(sender, "iConomy.list")) {
                        return true;
                    }

                    try {
                        ch.showTop(player, Integer.parseInt(split[1]) < 0 ? 5 : Integer.parseInt(split[1]));
                    } catch (Exception e) {
                        ch.showTop(player, 5);
                    }

                    return true;
                }

                if (Misc.isAny(split[0], new String[]{"reset", "-x"})) {
                    if (!getPermissions(sender, "iConomy.admin.reset")) {
                        return true;
                    }

                    if (iConomy.getBank().hasAccount(split[1])) {
                        ch.showReset(split[1], player, false);
                    } else {
                        Messaging.send(Template.parse("Error.account", new String[]{"+name,+n"}, new String[]{split[1]}));
                    }

                    return true;
                }

                break;

            case 3:

                if (Misc.isAny(split[0], new String[]{"pay", "-p"})) {
                    if (!getPermissions(sender, "iConomy.payment")) {
                        return true;
                    }

                    String name = "";
                    double amount = 0.0;

                    if (iConomy.getBank().hasAccount(split[1])) {
                        name = split[1];
                    } else {
                        try {
                            String[] Who = new String[]{split[1]};
                            Messaging.send(Template.parse("Error.account", new String[]{"+name,+n"}, Who));
                            return true;
                        } catch (NullPointerException e) {
                            Messaging.send("What did you do.");
                            e.printStackTrace();
                            return true;
                        }
                    }

                    try {
                        amount = Double.parseDouble(split[2]);

                        if (amount < 1) {
                            throw new NumberFormatException();
                        }
                    } catch (NumberFormatException ex) {
                        Messaging.send("&cInvalid amount: &f" + amount);
                        Messaging.send("&cUsage: &f/money &c[&f-p&c|&fpay&c] <&fplayer&c> &c<&famount&c>");
                        return true;
                    }
                    try {
                        ch.showPayment(player.getName(), name, amount);
                    } catch (Exception ex) {
                        Logger.getLogger(moneyCommand.class.getName()).log(Level.SEVERE, null, ex);
                    }

                    return true;
                }

                if (Misc.isAny(split[0], new String[]{"grant", "-g"})) {
                    if (!getPermissions(sender, "iConomy.admin.grant")) {
                        return true;
                    }

                    String name = "";
                    double amount = 0.0;

                    if (iConomy.getBank().hasAccount(split[1])) {
                        name = split[1];
                    } else {
                        Messaging.send(Template.parse("Error.account", new String[]{"+name,+n"}, new String[]{split[1]}));
                        return true;
                    }

                    try {
                        amount = Double.parseDouble(split[2]);
                    } catch (NumberFormatException e) {
                        Messaging.send("&cInvalid amount: &f" + split[2]);
                        Messaging.send("&cUsage: &f/money &c[&f-g&c|&fgrant&c] <&fplayer&c> (&f-&c)&c<&famount&c>");
                        return true;
                    }

                    ch.showGrant(name, player, amount, true);

                    return true;
                }

                if (Misc.isAny(split[0], new String[]{"set", "-s"})) {
                    if (!getPermissions(sender, "iConomy.admin.set")) {
                        return true;
                    }

                    String name = "";
                    double amount = 0.0;

                    if (iConomy.getBank().hasAccount(split[1])) {
                        name = split[1];
                    } else {
                        Messaging.send(Template.parse("Error.account", new String[]{"+name,+n"}, new String[]{split[1]}));
                        return true;
                    }

                    try {
                        amount = Double.parseDouble(split[2]);
                    } catch (NumberFormatException e) {
                        Messaging.send("&cInvalid amount: &f" + split[2]);
                        Messaging.send("&cUsage: &f/money &c[&f-g&c|&fgrant&c] <&fplayer&c> (&f-&c)&c<&famount&c>");
                        return true;
                    }

                    ch.showSet(name, player, amount, true);

                    return true;
                }

                break;
        }

        ch.showSimpleHelp();
        return true;
    }
}
