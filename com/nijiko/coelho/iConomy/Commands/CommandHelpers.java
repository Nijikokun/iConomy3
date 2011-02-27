package com.nijiko.coelho.iConomy.Commands;

import com.nijiko.coelho.iConomy.iConomy;
import com.nijiko.coelho.iConomy.system.Account;
import com.nijiko.coelho.iConomy.util.Constants;
import com.nijiko.coelho.iConomy.util.Messaging;
import com.nijiko.coelho.iConomy.util.Template;
import java.util.ArrayList;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 *
 * @author Wolfwood and original writer of iPlayerListener
 */
public class CommandHelpers {

    private Template Template = null;
    private iConomy plugin;

    public CommandHelpers(iConomy plugin, String directory) {
        this.plugin = plugin;
        Template = new Template(directory, "Messages.tpl");
    }

    /**
     * Sends simple condensed help lines to the current player
     */
    public void showSimpleHelp() {
        Messaging.send("&e----------------------------------------------------");
        Messaging.send("&f iConomy (&c" + Constants.Codename + "&f)           ");
        Messaging.send("&e----------------------------------------------------");
        Messaging.send("&f [] Required, () Optional                            ");
        Messaging.send("&e----------------------------------------------------");
        Messaging.send("&f/money &6-&e Check your balance                     ");
        Messaging.send("&f/money ? &6-&e For help & Information               ");
        Messaging.send("&f/money rank (player) &6-&e Rank on the topcharts.   ");
        Messaging.send("&f/money top (amount) &6-&e Richest players listing.  ");
        Messaging.send("&f/money pay [player] [amount] &6-&e Send money to a player.");
        Messaging.send("&e----------------------------------------------------");
        Messaging.send("&f Admin Commands:                                     ");
        Messaging.send("&e----------------------------------------------------");
        Messaging.send("&f/money grant [player] (amount) &6-&e Give money to a player.");
        Messaging.send("&f/money grant [player] -(amount) &6-&e Take money from a player.");
        Messaging.send("&f/money reset [player] &6-&e Puts a players account at initial.");
        Messaging.send("&f/money stats  &6-&e Check all economic stats.");
        Messaging.send("&e----------------------------------------------------");
    }

    /**
     * Shows the balance to the requesting player.
     *
     * @param name The name of the player we are viewing
     * @param viewing The player who is viewing the account
     * @param mine Is it the player who is trying to view?
     */
    public void showBalance(String name, CommandSender viewing, boolean mine) {
        Player player = (Player) viewing;
        try {
            if (mine) {
                Messaging.send(viewing, Template.color("tag") + Template.parse("personal.balance", new String[]{"+balance,+b"}, new String[]{iConomy.getBank().format(player.getName())}));
            } else {
                Messaging.send(viewing, Template.color("tag") + Template.parse("player.balance", new String[]{"+balance,+b", "+name,+n"}, new String[]{iConomy.getBank().format(name), name}));
            }
        } catch (NullPointerException e) {
            viewing.sendMessage("Something went wrong");
            iConomy.log.severe("NPE in showBalance");
            e.printStackTrace();
        }
    }

    /**
     * Reset a players account easily.
     *
     * @param resetting The player being reset. Cannot be null.
     * @param by The player resetting the account. Cannot be null.
     * @param notify Do we want to show the updates to each player?
     */
    public void showPayment(String from, String to, double amount) {
        Player paymentFrom = iConomy.getBukkitServer().getPlayer(from);
        Player paymentTo = iConomy.getBukkitServer().getPlayer(to);
        Account balanceFrom = iConomy.getBank().getAccount(from);
        Account balanceTo = iConomy.getBank().getAccount(to);

        if (from.equals(to)) {
            if (paymentFrom != null) {
                Messaging.send(paymentFrom, Template.color("pay.self"));
            }
        } else if (amount < 0.0) {
            if (paymentFrom != null) {
                Messaging.send(paymentFrom, Template.color("no.funds"));
            }
        } else if (!balanceFrom.hasEnough(amount)) {
            if (paymentFrom != null) {
                Messaging.send(paymentFrom, Template.color("no.funds"));
            }
        } else {
            balanceFrom.subtract(amount);
            balanceFrom.save();
            balanceTo.add(amount);
            balanceTo.save();

            if (paymentFrom != null) {
                Messaging.send(
                        paymentFrom,
                        Template.color("tag")
                        + Template.parse(
                        "payment.to",
                        new String[]{"+name,+n", "+amount,+a"},
                        new String[]{from, iConomy.getBank().format(amount)}));
            }

            if (paymentTo != null) {
                Messaging.send(
                        paymentTo,
                        Template.color("tag")
                        + Template.parse(
                        "payment.from",
                        new String[]{"+name,+n", "+amount,+a"},
                        new String[]{from, iConomy.getBank().format(amount)}));
            }

            // Transaction logger
            iConomy.getTransactions().insert(from, to, balanceFrom.getBalance(), balanceTo.getBalance(), 0.0, 0.0, amount);
            iConomy.getTransactions().insert(to, from, balanceTo.getBalance(), balanceFrom.getBalance(), 0.0, amount, 0.0);

            if (paymentFrom != null) {
                showBalance(from, paymentFrom, true);
            }

            if (paymentTo != null) {
                showBalance(to, paymentTo, true);
            }
        }
    }

    /**
     * Reset a players account, accessable via Console & In-Game
     *
     * @param account The account we are resetting.
     * @param controller If set to null, won't display messages.
     * @param console Is it sent via console?
     */
    public void showReset(String account, CommandSender controller, boolean console) {
        Player CSplayer = (Player) controller;
        Player player = iConomy.getBukkitServer().getPlayer(account);

        // Log Transaction
        iConomy.getTransactions().insert(account, "[System]", 0.0, 0.0, 0.0, 0.0, iConomy.getBank().getAccount(account).getBalance());

        // Reset
        iConomy.getBank().resetAccount(account);

        if (player != null) {
            Messaging.send(player, Template.color("personal.reset"));
        }

        if (controller != null) {
            Messaging.send(
                    Template.parse(
                    "player.reset",
                    new String[]{"+name,+n"},
                    new String[]{account}));
        }

        if (console) {
            System.out.println("Player " + account + "'s account has been reset.");
        } else {
            System.out.println(Messaging.bracketize("iConomy") + "Player " + account + "'s account has been reset by " + CSplayer.getName() + ".");
        }
    }

    /**
     *
     * @param account
     * @param controller If set to null, won't display messages.
     * @param amount
     * @param console Is it sent via console?
     */
    public void showGrant(String name, CommandSender controller, double amount, boolean console) {
        Player player = (Player) controller;
        Player online = iConomy.getBukkitServer().getPlayer(name);
        Account account = iConomy.getBank().getAccount(name);
        account.add(amount);
        account.save();

        // Log Transaction
        if (amount < 0.0) {
            iConomy.getTransactions().insert("[System]", name, 0.0, account.getBalance(), 0.0, 0.0, amount);
        } else {
            iConomy.getTransactions().insert("[System]", name, 0.0, account.getBalance(), 0.0, amount, 0.0);
        }

        if (online != null) {
            Messaging.send(online,
                    Template.color("tag")
                    + Template.parse(
                    (amount < 0.0) ? "personal.debit" : "personal.credit",
                    new String[]{"+by", "+amount,+a"},
                    new String[]{(console) ? "console" : player.getName(), iConomy.getBank().format(amount)}));

            showBalance(name, online, true);
        }

        if (controller != null) {
            Messaging.send(
                    Template.color("tag")
                    + Template.parse(
                    (amount < 0.0) ? "player.debit" : "player.credit",
                    new String[]{"+name,+n", "+amount,+a"},
                    new String[]{name, iConomy.getBank().format(amount)}));
        }

        if (console) {
            System.out.println("Player " + account + "'s account had " + amount + " grant to it.");
        } else {
            System.out.println(Messaging.bracketize("iConomy") + "Player " + account + "'s account had " + amount + " grant to it by " + player.getName() + ".");
        }
    }

    /**
     *
     * @param account
     * @param controller If set to null, won't display messages.
     * @param amount
     * @param console Is it sent via console?
     */
    public void showSet(String name, CommandSender controller, double amount, boolean console) {
        Player player = (Player) controller;
        Player online = iConomy.getBukkitServer().getPlayer(name);
        Account account = iConomy.getBank().getAccount(name);
        account.setBalance(amount);
        account.save();

        // Log Transaction
        iConomy.getTransactions().insert("[System]", name, 0.0, account.getBalance(), amount, 0.0, 0.0);

        if (online != null) {
            Messaging.send(online,
                    Template.color("tag")
                    + Template.parse(
                    "personal.set",
                    new String[]{"+by", "+amount,+a"},
                    new String[]{(console) ? "console" : player.getName(), iConomy.getBank().format(amount)}));

            showBalance(name, online, true);
        }

        if (controller != null) {
            Messaging.send(
                    Template.color("tag")
                    + Template.parse(
                    "player.set",
                    new String[]{"+name,+n", "+amount,+a"},
                    new String[]{name, iConomy.getBank().format(amount)}));
        }

        if (console) {
            System.out.println("Player " + account + "'s account had " + amount + " set to it.");
        } else {
            System.out.println(Messaging.bracketize("iConomy") + "Player " + account + "'s account had " + amount + " set to it by " + player.getName() + ".");
        }
    }

    /**
     * Shows the ranking through templates
     *
     * @param viewing
     * @param player
     */
    public void showRank(CommandSender viewing, String playerN) {
        Player player = (Player) viewing;
        if (iConomy.getBank().hasAccount(playerN)) {
            int rank = iConomy.getBank().getAccountRank(playerN);

            Messaging.send(
                    viewing,
                    Template.color("tag")
                    + Template.parse(
                    ((player.getName().equalsIgnoreCase(playerN)) ? "personal.rank" : "player.rank"),
                    new Object[]{"+name,+n", "+rank,+r"},
                    new Object[]{player, rank}));
        } else {
            Messaging.send(
                    viewing,
                    Template.parse(
                    "no.account",
                    new Object[]{"+name,+n"},
                    new Object[]{player}));
        }
    }

    /**
     * Grabs the top amount of players and shows them through the templating system.
     *
     * @param viewing
     * @param amount
     */
    public void showTop(CommandSender viewing, int amount) {
        ArrayList<String> als = iConomy.getBank().getAccountRanks(amount);

        Messaging.send(
                viewing,
                Template.parse(
                "top.opening",
                new Object[]{"+amount,+a"},
                new Object[]{als.size()}));

        for (int i = 0; i < als.size(); i++) {
            int current = i + 1;

            Account account = iConomy.getBank().getAccount(als.get(i));

            Messaging.send(
                    viewing,
                    Template.parse(
                    "top.line",
                    new String[]{"+i,+number", "+player,+name,+n", "+balance,+b"},
                    new Object[]{current, account.getName(), iConomy.getBank().format(account.getBalance())}));
        }
    }
}
