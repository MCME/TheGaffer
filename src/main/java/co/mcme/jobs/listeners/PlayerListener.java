package co.mcme.jobs.listeners;

import static co.mcme.jobs.Jobs.runningJobs;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerListener implements Listener {

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (runningJobs.size() > 0) {
            event.getPlayer().sendMessage(ChatColor.GRAY + "There is a job running! Use /job check to find out what it is!");
        }
    }
}
