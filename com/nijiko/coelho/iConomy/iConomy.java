package com.nijiko.coelho.iConomy;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import java.util.Collection;

import org.bukkit.event.Event;
import org.bukkit.Server;
import org.bukkit.event.Event.Priority;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.config.Configuration;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.nijiko.coelho.iConomy.util.Misc;
import com.nijiko.coelho.iConomy.system.Account;
import com.nijiko.coelho.iConomy.util.Template;

import com.nijiko.coelho.iConomy.entity.iPlayerListener;
import com.nijiko.coelho.iConomy.entity.iPluginListener;
import com.nijiko.coelho.iConomy.net.iDatabase;
import com.nijiko.coelho.iConomy.system.Bank;
import com.nijiko.coelho.iConomy.util.Constants;
import com.nijiko.coelho.iConomy.system.Transactions;
import com.nijiko.coelho.iConomy.util.FileManager;

import com.nijiko.permissions.PermissionHandler;
import com.nijikokun.bukkit.Permissions.Permissions;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;

public class iConomy extends JavaPlugin {

	private static Server Server = null;
	private static Bank Bank = null;
	private static iDatabase iDatabase = null;
	private static Permissions Permissions = null;
	private static iPlayerListener playerListener = null;
	private static iPluginListener pluginListener = null;
	private static Transactions Transactions = null;
	

	@Override
	public void onEnable() {
        try {
            if(!getDataFolder().exists())
                getDataFolder().mkdir();
        } catch (Exception e) {
            System.out.println("[iConomy] Could not create directory!");
            System.out.println("[iConomy] You must manually make the iConomy/ directory!");
        }

        // Make sure we can read / write
        getDataFolder().setWritable(true);
        getDataFolder().setExecutable(true);

		// Get the server
		Server = getServer();

        // Directory
        Constants.Plugin_Directory = getDataFolder().getPath();
		
		// Grab plugin details..
		PluginManager pm = Server.getPluginManager();
		PluginDescriptionFile pdfFile = this.getDescription();
		
		// Versioning File
		FileManager file = new FileManager(getDataFolder().getPath(), "VERSION", false);
		
		// Default Files
		setupDefaultFile("iConomy.yml");
		setupDefaultFile("Messages.tpl");
		
		// Configuration
		try {
			Constants.load(new Configuration(new File(getDataFolder(), "iConomy.yml")));
		} catch(Exception e) {
			this.getServer().getPluginManager().disablePlugin(this);
			System.out.println("[iConomy] Failed to retrieve configuration from directory.");
			System.out.println("[iConomy] Please back up your current settings and let iConomy recreate it.");
			return;
		}

        // Create flatfile database
        if(Constants.Database_Type.equalsIgnoreCase("flatfile")) {
            setupDefaultFile("iConomy.flatfile");
        }

		// Load the database
		try {
			iDatabase = new iDatabase();
		} catch(Exception e) {
			System.out.println("[iConomy] Failed to connect to database: " + e.getMessage());
			return; 
		}

		// File Logger
		Transactions = new Transactions();

        try {
            Transactions.load();
        } catch (Exception ex) {
            System.out.println("[iConomy] Could not load transaction logger.");
        }
		
		// Check version details before the system loads
		upgrade(file, Double.valueOf(pdfFile.getVersion()));
		
		// Load the bank system
		try {
			Bank = new Bank();
			Bank.load();
		} catch(Exception e) {
			System.out.println("[iConomy] Failed to load accounts from database: " + e.getMessage());
			return; 
		}

		// Initializing Listeners
		pluginListener = new iPluginListener();
		playerListener = new iPlayerListener(getDataFolder().getPath());
		
		// Event Registration
        pm.registerEvent(Event.Type.PLUGIN_ENABLE, pluginListener, Priority.Monitor, this);
        pm.registerEvent(Event.Type.PLAYER_JOIN, playerListener, Priority.Normal, this);

		// Console Detail
		System.out.println("[iConomy] v" + pdfFile.getVersion() + " ("+ Constants.Codename + ") loaded.");
		System.out.println("[iConomy] Developed By: " + pdfFile.getAuthors());
	}

	@Override
	public void onDisable() {
        for(String account : Bank.getAccounts().keySet()) {
            Bank.getAccount(account).save();
        }

		System.out.println("[iConomy] saved accounts.");
        System.out.println("[iConomy] Has been disabled.");
	}
        @Override
        public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
		Player player = (Player) sender;

		if (cmd.getName().equalsIgnoreCase("money")) {
			switch(args.length) {
			case 0:

				iPlayerListener.showBalance("", player, true);
				return true;

			case 1:

				if (Misc.isAny(args[0], new String[] { "rank", "-r" })) {
					if (!iConomy.getPermissions().permission(player, "iConomy.rank"))
						return false;

					iPlayerListener.showRank(player, player.getName());

					return true;
				}

                if (Misc.isAny(args[0], new String[] { "top", "-t" })) {
					if (!iConomy.getPermissions().has(player, "iConomy.list"))
						return false;

					iPlayerListener.showTop(player, 5);

					return true;
				}

                if (Misc.isAny(args[0], new String[] { "stats", "-s" })) {
					if (!iConomy.getPermissions().has(player, "iConomy.admin.stats"))
						return false;

					Collection<Account> money = iConomy.getBank().getAccounts().values();
					double totalMoney = 0;
					int totalPlayers = money.size();

					for(Object o : money.toArray())
						totalMoney += ((Account) o).getBalance();

                    player.sendMessage(Template.color("statistics.opening"));

                    player.sendMessage(Template.parse("statistics.total",
                            new String[]{ "+currency,+c", "+amount,+money,+a,+m" },
                            new Object[]{ iConomy.getBank().getCurrency(), iConomy.getBank().format(totalMoney) }
                    ));

                    player.sendMessage(Template.parse("statistics.average",
                            new String[]{ "+currency,+c", "+amount,+money,+a,+m" },
                            new Object[]{ iConomy.getBank().getCurrency(), iConomy.getBank().format(totalMoney / totalPlayers) }
                    ));

                    player.sendMessage(Template.parse("statistics.accounts",
                            new String[]{ "+currency,+c", "+amount,+accounts,+a" },
                            new Object[]{ iConomy.getBank().getCurrency(), totalPlayers }
                    ));

					return true;
				}

                if (Misc.isAny(args[0],
                        new String[] { "help", "?", "grant", "-g", "reset", "-x", "set", "-s", "pay", "-p" })) {

					iPlayerListener.showSimpleHelp();

					return false;
				} else {
					if (!iConomy.getPermissions().has(player, "iConomy.access"))
						return false;

					if (iConomy.getBank().hasAccount(args[0])) {
						iPlayerListener.showBalance(args[0], player, false);
					} else {
						player.sendMessage(Template.parse("no.account", new String[]{"+name,+n"}, new String[]{args[0]}));
					}

					return true;
				}

			case 2:

				if (Misc.isAny(args[0], new String[] { "rank", "-r" })) {
					if (!iConomy.getPermissions().has(player, "iConomy.rank"))
						return false;

					if (iConomy.getBank().hasAccount(args[1])) {
						iPlayerListener.showRank(player, args[1]);
					} else {
						player.sendMessage(Template.parse("no.account", new String[]{"+name,+n"}, new String[]{args[1]}));
					}

					return true;
				}

                if (Misc.isAny(args[0], new String[] { "top", "-t" })) {
					if (!iConomy.getPermissions().has(player, "iConomy.list"))
						return false;

					try {
						iPlayerListener.showTop(player, Integer.parseInt(args[1]) < 0 ? 5 : Integer.parseInt(args[1]));
					} catch(Exception e) {
						iPlayerListener.showTop(player, 5);
					}

					return true;
				}

                if(Misc.isAny(args[0], new String[] { "reset", "-x" })) {
					if (!iConomy.getPermissions().has(player, "iConomy.admin.reset"))
						return false;

					if (iConomy.getBank().hasAccount(args[1])) {
						iPlayerListener.showReset(args[1], player, false);
					} else {
						player.sendMessage(Template.parse("no.account", new String[]{"+name,+n"}, new String[]{args[1]}));
					}

					return true;
				}

				break;

			case 3:

				if (Misc.isAny(args[0], new String[] { "pay", "-p" })) {
					if (!iConomy.getPermissions().has(player, "iConomy.payment"))
						return false;

					String name = "";
					double amount = 0.0;

					if (iConomy.getBank().hasAccount(args[1])) {
						name = args[1];
					} else {
						player.sendMessage(Template.parse("no.account", new String[]{"+name,+n"}, new String[]{args[1]}));
						return true;
					}

					try {
						amount = Double.parseDouble(args[2]);

						if (amount < 1)
							throw new NumberFormatException();
					} catch (NumberFormatException ex) {
						player.sendMessage("&cInvalid amount: &f" + amount);
						player.sendMessage("&cUsage: &f/money &c[&f-p&c|&fpay&c] <&fplayer&c> &c<&famount&c>");
						return false;
					}

					iPlayerListener.showPayment(player.getName(), name, amount);

					return true;
				}

                if (Misc.isAny(args[0], new String[] { "grant", "-g" })) {
					if (!iConomy.getPermissions().has(player, "iConomy.admin.grant"))
						return false;

					String name = "";
					double amount = 0.0;

					if (iConomy.getBank().hasAccount(args[1])) {
						name = args[1];
					} else {
						player.sendMessage(Template.parse("no.account", new String[]{"+name,+n"}, new String[]{args[1]}));
					}

					try {
						amount = Double.parseDouble(args[2]);
					} catch (NumberFormatException e) {
						player.sendMessage("&cInvalid amount: &f" + args[2]);
						player.sendMessage("&cUsage: &f/money &c[&f-g&c|&fgrant&c] <&fplayer&c> (&f-&c)&c<&famount&c>");
                                                return false;
					}

					iPlayerListener.showGrant(name, player, amount, true);

					return true;
				}

                if (Misc.isAny(args[0], new String[] { "set", "-s" })) {
					if (!iConomy.getPermissions().has(player, "iConomy.admin.set"))
						return false;

					String name = "";
					double amount = 0.0;

					if (iConomy.getBank().hasAccount(args[1])) {
						name = args[1];
					} else {
						player.sendMessage(Template.parse("no.account", new String[]{"+name,+n"}, new String[]{args[1]}));
					}

					try {
						amount = Double.parseDouble(args[2]);
					} catch (NumberFormatException e) {
						player.sendMessage("&cInvalid amount: &f" + args[2]);
						player.sendMessage("&cUsage: &f/money &c[&f-g&c|&fgrant&c] <&fplayer&c> (&f-&c)&c<&famount&c>");
                                                return false;
					}

					iPlayerListener.showSet(name, player, amount, true);

					return true;
				}

				break;
			}

			iPlayerListener.showSimpleHelp();
		}

		return false;
	}

    private void upgrade(FileManager file, double version) {
		if(file.exists()) {
			file.read();

			try {
				double current = Double.parseDouble(file.getSource());

				if(current != version) {
					file.write(version);
				}
			} catch(Exception e) {
				System.out.println("[iConomy] Invalid version file, deleting to be re-created on next load.");
                file.delete();
			}
		} else {
            // New Version, check for older databases
            if (Constants.Database_Type.equalsIgnoreCase("flatfile")) {

            } else {
                String[] SQL = {};

                String[] MySQL = {
                    "RENAME TABLE ibalances TO " + Constants.SQL_Table + ";",
                    "ALTER TABLE " + Constants.SQL_Table + " CHANGE  player  username TEXT NOT NULL, CHANGE balance balance DECIMAL(65, 2) NOT NULL;"
                };

                String[] SQLite = {
                    "CREATE TABLE '" + Constants.SQL_Table + "' ('id' INT ( 10 ) PRIMARY KEY , 'username' TEXT , 'balance' DECIMAL ( 65 , 2 ));",
                    "INSERT INTO " + Constants.SQL_Table + "(id, username, balance) SELECT id, player, balance FROM ibalances;",
                    "DROP TABLE ibalances;"
                };

                try {
                    DatabaseMetaData dbm = iDatabase.getConnection().getMetaData();
                    ResultSet rs = dbm.getTables(null, null, "ibalances", null);

                    if(rs.next()) {
                        System.out.println(" - Updating " + Constants.Database_Type + " Database for latest iConomy");

                        int i = 1;
                        SQL = (Constants.Database_Type.equalsIgnoreCase("mysql")) ? MySQL : SQLite;

                        for(String Query : SQL) {
                            iDatabase.executeQuery(Query);

                            System.out.println("   Executing SQL Query #" + i + " of " + (SQL.length));
                            ++i;
                        }

                        System.out.println(" + Database Update Complete.");
                    }
                } catch (SQLException e) {
                    System.out.println("[iConomy] Error updating database: " + e);
                }
            }

            // Create file
			file.create();
			file.write(version);
		}
    }
	
    private void setupDefaultFile(String name) {
        File actual = new File(getDataFolder(), name);
        if (!actual.exists()) {
            
            InputStream input = this.getClass().getResourceAsStream("/default/" + name);
            if (input != null) {
                FileOutputStream output = null;
                
                try {
                    output = new FileOutputStream(actual);
                    byte[] buf = new byte[8192];
                    int length = 0;
                    
                    while ((length = input.read(buf)) > 0) {
                        output.write(buf, 0, length);
                    }
                    
                    System.out.println("[iConomy] Default setup file written: " + name);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    try {
                        if (input != null)
                            input.close();
                    } catch (Exception e) {}

                    try {
                        if (output != null)
                            output.close();
                    } catch (Exception e) {
                    	
                    }
                }
            }
        }
    }

	public static Bank getBank() {
		return Bank;
	}
	
	public static iDatabase getDatabase() {
		return iDatabase;
	}
	
	public static Transactions getTransactions() {
		return Transactions;
	}

	@SuppressWarnings("static-access")
	public static PermissionHandler getPermissions() {
		return Permissions.Security;
	}
	
	public static boolean setPermissions(Permissions permissions) {
		if(Permissions == null)
			Permissions = permissions;
		else
			return false;
		return true;
	}
	
	public static Server getBukkitServer() {
		return Server;
	}

}
