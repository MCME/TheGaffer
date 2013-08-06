package co.mcme.jobs;

import java.util.logging.Logger;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.Configuration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public final class Jobs extends JavaPlugin implements Listener {

    private final Logger log = Logger.getLogger("Minecraft");
    private static int jobCount;
    static Configuration conf;

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        setupConfig();
    }

    public void setupConfig() {
        conf = getConfig();
        getConfig().options().copyDefaults(true);
        saveConfig();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if ((args.length > 0) && ((sender instanceof Player))) {
            Player player = (Player) sender;
            if (label.equalsIgnoreCase("job")) {
                if (args.length == 2 && player.hasPermission("jobs.run")) {
                    if (args[1].equals("on")) {
                        storeJob(args[0], player.getName(), true);
                        player.sendMessage(ChatColor.GREEN + "Created job called " + args[0] + ".");
                    }
                    if (args[1].equals("off")) {
                        storeJob(args[0], player.getName(), false);
                        player.sendMessage(ChatColor.RED + "Removed job called " + args[0] + ".");
                    }
                }
                if (args[0].equalsIgnoreCase("check")) {
                }

            }

        }
        return true;
    }

    public void storeJob(String jobname, String admin, boolean status) {
    }
}
