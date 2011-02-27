package com.nijiko.coelho.iConomy.entity;

import java.util.ArrayList;
import java.util.Collection;

import com.nijiko.coelho.iConomy.iConomy;
import com.nijiko.coelho.iConomy.system.Account;
import com.nijiko.coelho.iConomy.util.Constants;
import com.nijiko.coelho.iConomy.util.Messaging;
import com.nijiko.coelho.iConomy.util.Misc;
import com.nijiko.coelho.iConomy.util.Template;

import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.player.PlayerListener;

public class iPlayerListener extends PlayerListener {
	private Template Template = null;

	public iPlayerListener(String directory) {
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

	public void showBalance(String name, Player viewing, boolean mine) {
		if (mine) {
			Messaging.send(viewing, Template.color("tag") + Template.parse("personal.balance", new String[]{"+balance,+b"}, new String[]{iConomy.getBank().format(viewing.getName())}));
		} else {
			Messaging.send(viewing, Template.color("tag") + Template.parse("player.balance", new String[]{"+balance,+b", "+name,+n"}, new String[]{iConomy.getBank().format(name), name}));
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
			balanceFrom.subtract(amount); balanceFrom.save();
			balanceTo.add(amount); balanceTo.save();

			if (paymentFrom != null) {
				Messaging.send(
						paymentFrom,
						Template.color("tag")
						+ Template.parse(
								"payment.to",
								new String[]{"+name,+n", "+amount,+a"},
								new String[]{from, iConomy.getBank().format(amount) }
						)
				);
			}

			if (paymentTo != null) {
				Messaging.send(
						paymentTo,
						Template.color("tag")
						+ Template.parse(
								"payment.from",
								new String[]{"+name,+n", "+amount,+a"},
								new String[]{from, iConomy.getBank().format(amount) }
						)
				);
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

	public void showReset(String account, Player controller, boolean console) {
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
							new String[]{ "+name,+n" },
							new String[]{ account }
					)
			);
		}

		if (console) {
			System.out.println("Player " + account + "'s account has been reset.");
		} else {
			System.out.println(Messaging.bracketize("iConomy") + "Player " + account + "'s account has been reset by " + controller.getName() + ".");
		}
	}

	/**
	 *
	 * @param account
	 * @param controller If set to null, won't display messages.
	 * @param amount
	 * @param console Is it sent via console?
	 */

	public void showGrant(String name, Player controller, double amount, boolean console) {
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
							new String[]{(console) ? "console" : controller.getName(), iConomy.getBank().format(amount) }
					)
			);

			showBalance(name, online, true);
		}

		if (controller != null) {
			Messaging.send(
					Template.color("tag")
					+ Template.parse(
							(amount < 0.0) ? "player.debit" : "player.credit",
							new String[]{"+name,+n", "+amount,+a"},
							new String[]{ name, iConomy.getBank().format(amount) }
					)
			);
		}

		if (console) {
			System.out.println("Player " + account + "'s account had " + amount + " grant to it.");
		} else {
			System.out.println(Messaging.bracketize("iConomy") + "Player " + account + "'s account had " + amount + " grant to it by " + controller.getName() + ".");
		}
	}

	/**
	 *
	 * @param account
	 * @param controller If set to null, won't display messages.
	 * @param amount
	 * @param console Is it sent via console?
	 */

	public void showSet(String name, Player controller, double amount, boolean console) {
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
							new String[]{(console) ? "console" : controller.getName(), iConomy.getBank().format(amount) }
					)
			);

			showBalance(name, online, true);
		}

		if (controller != null) {
			Messaging.send(
					Template.color("tag")
					+ Template.parse(
							"player.set",
							new String[]{"+name,+n", "+amount,+a"},
							new String[]{ name, iConomy.getBank().format(amount) }
					)
			);
		}

		if (console) {
			System.out.println("Player " + account + "'s account had " + amount + " set to it.");
		} else {
			System.out.println(Messaging.bracketize("iConomy") + "Player " + account + "'s account had " + amount + " set to it by " + controller.getName() + ".");
		}
	}

    /**
     * Shows the ranking through templates
     *
     * @param viewing
     * @param player
     */
	public void showRank(Player viewing, String player) {
		if (iConomy.getBank().hasAccount(player)) {
			int rank = iConomy.getBank().getAccountRank(player);

			Messaging.send(
					viewing,
					Template.color("tag")
					+ Template.parse(
							((viewing.getName().equalsIgnoreCase(player)) ? "personal.rank" : "player.rank"),
							new Object[]{"+name,+n", "+rank,+r"},
							new Object[]{ player, rank }
					)
			);
		} else {
			Messaging.send(
					viewing,
					Template.parse(
							"no.account",
							new Object[]{ "+name,+n" }, 
							new Object[]{ player }
					)
			);
		}
	}

    /**
     * Grabs the top amount of players and shows them through the templating system.
     *
     * @param viewing
     * @param amount
     */
	public void showTop(Player viewing, int amount) {
		ArrayList<String> als = iConomy.getBank().getAccountRanks(amount);

		Messaging.send(
				viewing,
				Template.parse(
						"top.opening",
						new Object[]{ "+amount,+a" },
						new Object[]{ als.size() }
				)
		);

		for(int i = 0; i < als.size(); i++) {
			int current = i+1;

            Account account = iConomy.getBank().getAccount(als.get(i));

			Messaging.send(
					viewing,
					Template.parse(
							"top.line",
							new String[]{ "+i,+number", "+player,+name,+n", "+balance,+b" },
							new Object[]{ current, account.getName(),  iConomy.getBank().format(account.getBalance()) }
					)
			);
		}
	}

	/**
	 * Commands sent from in game to us.
	 *
	 * @param player The player who sent the command.
	 * @param split The input line split by spaces.
	 * @return <code>boolean</code> - True denotes that the command existed, false the command doesn't.
	 */

	@Override
	public void onPlayerJoin(PlayerEvent event) {
		Player player = event.getPlayer();

		if(!iConomy.getBank().hasAccount(player.getName())) {
			System.out.println("[iConomy] Player didn't have an account, Creating...");
			iConomy.getBank().addAccount(player.getName());
		} else {
			System.out.println("[iConomy] Player had an account!");
		}
	}

    /**
     * Player commands.
     *
     * @param event
     */
	public void onPlayerCommand(PlayerChatEvent event) {
		Player player = event.getPlayer();
		Messaging.save(player);
		String[] split = event.getMessage().substring(1).split(" ");

		if (split[0].equalsIgnoreCase("money")) {
			switch(split.length) {
			case 1:

				showBalance("", player, true);
				return;

			case 2:

				if (Misc.isAny(split[1], new String[] { "rank", "-r" })) {
					if (!iConomy.getPermissions().permission(player, "iConomy.rank"))
						return;

					showRank(player, player.getName());

					return;
				}
                
                if (Misc.isAny(split[1], new String[] { "top", "-t" })) {
					if (!iConomy.getPermissions().has(player, "iConomy.list"))
						return;

					showTop(player, 5);

					return;
				}
                
                if (Misc.isAny(split[1], new String[] { "stats", "-s" })) {
					if (!iConomy.getPermissions().has(player, "iConomy.admin.stats"))
						return;

					Collection<Account> money = iConomy.getBank().getAccounts().values();
					double totalMoney = 0;
					int totalPlayers = money.size();
					
					for(Object o : money.toArray())
						totalMoney += ((Account) o).getBalance();

                    Messaging.send(Template.color("statistics.opening"));

                    Messaging.send(Template.parse("statistics.total",
                            new String[]{ "+currency,+c", "+amount,+money,+a,+m" },
                            new Object[]{ iConomy.getBank().getCurrency(), iConomy.getBank().format(totalMoney) }
                    ));

                    Messaging.send(Template.parse("statistics.average",
                            new String[]{ "+currency,+c", "+amount,+money,+a,+m" },
                            new Object[]{ iConomy.getBank().getCurrency(), iConomy.getBank().format(totalMoney / totalPlayers) }
                    ));

                    Messaging.send(Template.parse("statistics.accounts",
                            new String[]{ "+currency,+c", "+amount,+accounts,+a" },
                            new Object[]{ iConomy.getBank().getCurrency(), totalPlayers }
                    ));
					
					return;
				}

                if (Misc.isAny(split[1],
                        new String[] { "help", "?", "grant", "-g", "reset", "-x", "set", "-s", "pay", "-p" })) {

					showSimpleHelp();

					return;
				} else {
					if (!iConomy.getPermissions().has(player, "iConomy.access"))
						return;

					if (iConomy.getBank().hasAccount(split[1])) {
						showBalance(split[1], player, false);
					} else {
						Messaging.send(Template.parse("no.account", new String[]{"+name,+n"}, new String[]{split[1]}));
					}

					return;
				}

			case 3:

				if (Misc.isAny(split[1], new String[] { "rank", "-r" })) {
					if (!iConomy.getPermissions().has(player, "iConomy.rank"))
						return;

					if (iConomy.getBank().hasAccount(split[2])) {
						showRank(player, split[2]);
					} else {
						Messaging.send(Template.parse("no.account", new String[]{"+name,+n"}, new String[]{split[2]}));
					}

					return;
				}
                
                if (Misc.isAny(split[1], new String[] { "top", "-t" })) {
					if (!iConomy.getPermissions().has(player, "iConomy.list"))
						return;

					try {
						showTop(player, Integer.parseInt(split[2]) < 0 ? 5 : Integer.parseInt(split[2]));
					} catch(Exception e) {
						showTop(player, 5);
					}

					return;
				}
                
                if(Misc.isAny(split[1], new String[] { "reset", "-x" })) {
					if (!iConomy.getPermissions().has(player, "iConomy.admin.reset"))
						return;

					if (iConomy.getBank().hasAccount(split[2])) {
						showReset(split[2], player, false);
					} else {
						Messaging.send(Template.parse("no.account", new String[]{"+name,+n"}, new String[]{split[2]}));
					}

					return;
				}

				break;

			case 4:

				if (Misc.isAny(split[1], new String[] { "pay", "-p" })) {
					if (!iConomy.getPermissions().has(player, "iConomy.payment"))
						return;

					String name = "";
					double amount = 0.0;

					if (iConomy.getBank().hasAccount(split[2])) {
						name = split[2];
					} else {
						Messaging.send(Template.parse("no.account", new String[]{"+name,+n"}, new String[]{split[2]}));
						return;
					}

					try {
						amount = Double.parseDouble(split[3]);

						if (amount < 1)
							throw new NumberFormatException();
					} catch (NumberFormatException ex) {
						Messaging.send("&cInvalid amount: &f" + amount);
						Messaging.send("&cUsage: &f/money &c[&f-p&c|&fpay&c] <&fplayer&c> &c<&famount&c>");
						return;
					}

					showPayment(player.getName(), name, amount);

					return;
				}
                
                if (Misc.isAny(split[1], new String[] { "grant", "-g" })) {
					if (!iConomy.getPermissions().has(player, "iConomy.admin.grant"))
						return;

					String name = "";
					double amount = 0.0;

					if (iConomy.getBank().hasAccount(split[2])) {
						name = split[2];
					} else {
						Messaging.send(Template.parse("no.account", new String[]{"+name,+n"}, new String[]{split[2]}));
					}

					try {
						amount = Double.parseDouble(split[3]);
					} catch (NumberFormatException e) {
						Messaging.send("&cInvalid amount: &f" + split[3]);
						Messaging.send("&cUsage: &f/money &c[&f-g&c|&fgrant&c] <&fplayer&c> (&f-&c)&c<&famount&c>");
					}

					showGrant(name, player, amount, true);

					return;
				}
                
                if (Misc.isAny(split[1], new String[] { "set", "-s" })) {
					if (!iConomy.getPermissions().has(player, "iConomy.admin.set"))
						return;

					String name = "";
					double amount = 0.0;

					if (iConomy.getBank().hasAccount(split[2])) {
						name = split[2];
					} else {
						Messaging.send(Template.parse("no.account", new String[]{"+name,+n"}, new String[]{split[2]}));
					}

					try {
						amount = Double.parseDouble(split[3]);
					} catch (NumberFormatException e) {
						Messaging.send("&cInvalid amount: &f" + split[3]);
						Messaging.send("&cUsage: &f/money &c[&f-g&c|&fgrant&c] <&fplayer&c> (&f-&c)&c<&famount&c>");
					}

					showSet(name, player, amount, true);

					return;
				}

				break;
			}

			showSimpleHelp();
		}

		return;
	}


}