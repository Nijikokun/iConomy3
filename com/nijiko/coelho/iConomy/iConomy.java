package com.nijiko.coelho.iConomy;

import com.nijiko.coelho.iConomy.util.CommandHandler;
import com.nijiko.coelho.iConomy.Commands.moneyCommand;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import org.bukkit.event.Event;
import org.bukkit.Server;
import org.bukkit.event.Event.Priority;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.config.Configuration;

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
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

/**
 *
 * Updated to use the onCommand instead of player_command event
 */

public class iConomy extends JavaPlugin {

    private static Server Server = null;
    private static Bank Bank = null;
    private static iDatabase iDatabase = null;
    public static PermissionHandler Permissions = null;
    private static iPlayerListener playerListener = null;
    private static iPluginListener pluginListener = null;
    private static Transactions Transactions = null;
    public static final Logger log = Logger.getLogger("Minecraft");
    private Map<String, CommandHandler> commands = new HashMap<String, CommandHandler>();

    @Override
    public void onEnable() {
        setupPermissions();
        try {
            if (!getDataFolder().exists()) {
                getDataFolder().mkdir();
            }
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
        } catch (Exception e) {
            this.getServer().getPluginManager().disablePlugin(this);
            System.out.println("[iConomy] Failed to retrieve configuration from directory.");
            System.out.println("[iConomy] Please back up your current settings and let iConomy recreate it.");
            return;
        }

        // Create flatfile database
        if (Constants.Database_Type.equalsIgnoreCase("flatfile")) {
            setupDefaultFile("iConomy.flatfile");
        }

        // Load the database
        try {
            iDatabase = new iDatabase();
        } catch (Exception e) {
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
        } catch (Exception e) {
            System.out.println("[iConomy] Failed to load accounts from database: " + e.getMessage());
            return;
        }

        // Initializing Listeners
        pluginListener = new iPluginListener();
        playerListener = new iPlayerListener();

        commands.put("money", new moneyCommand(this));
        // Event Registration
        pm.registerEvent(Event.Type.PLUGIN_ENABLE, pluginListener, Priority.Monitor, this);
        //pm.registerEvent(Event.Type.PLAYER_COMMAND, playerListener, Priority.Normal, this);
        pm.registerEvent(Event.Type.PLAYER_JOIN, playerListener, Priority.Normal, this);

        // Console Detail
        System.out.println("[iConomy] v" + pdfFile.getVersion() + " (" + Constants.Codename + ") loaded.");
        System.out.println("[iConomy] Developed By: " + pdfFile.getAuthors());
    }

    @Override
    public void onDisable() {
        for (String account : Bank.getAccounts().keySet()) {
            Bank.getAccount(account).save();
        }

        System.out.println("[iConomy] saved accounts.");
        System.out.println("[iConomy] Has been disabled.");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String commandLabel, String[] args) {
        CommandHandler handler = commands.get(command.getName().toLowerCase());

        if (handler != null) {
            return handler.perform(sender, args);
        } else {
            return false;
        }
    }

    private void upgrade(FileManager file, double version) {
        if (file.exists()) {
            file.read();

            try {
                double current = Double.parseDouble(file.getSource());

                if (current != version) {
                    file.write(version);
                }
            } catch (Exception e) {
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

                    if (rs.next()) {
                        System.out.println(" - Updating " + Constants.Database_Type + " Database for latest iConomy");

                        int i = 1;
                        SQL = (Constants.Database_Type.equalsIgnoreCase("mysql")) ? MySQL : SQLite;

                        for (String Query : SQL) {
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
                        if (input != null) {
                            input.close();
                        }
                    } catch (Exception e) {
                    }

                    try {
                        if (output != null) {
                            output.close();
                        }
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
        return Permissions;
    }

    private void setupPermissions() {
        Plugin test = this.getServer().getPluginManager().getPlugin("Permissions");

        if (iConomy.Permissions == null) {
            if (test != null) {
                this.getServer().getPluginManager().enablePlugin(test); // This line.
                iConomy.Permissions = ((Permissions) test).getHandler();
            } else {
                log.info("Permission system not detected, defaulting to OP");
            }
        }
    }

    public static Server getBukkitServer() {
        return Server;
    }
}
