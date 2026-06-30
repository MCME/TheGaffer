/*  This file is part of TheGaffer.
 * 
 *  TheGaffer is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  TheGaffer is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with TheGaffer.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.mcmiddleearth.thegaffer.listeners;

import com.mcmiddleearth.thegaffer.TheGaffer;
import com.mcmiddleearth.thegaffer.storage.JobDatabase;
import com.mcmiddleearth.thegaffer.utilities.BuildProtection;
import com.mcmiddleearth.thegaffer.utilities.JobBorderManager;
import com.mcmiddleearth.thegaffer.utilities.Msg;
import com.mcmiddleearth.thegaffer.utilities.PermissionsUtil;
import com.mcmiddleearth.thegaffer.utilities.ProtectionUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PlayerListener implements Listener {

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        player.setGlowing(false);
        if (!JobDatabase.getActiveJobs().isEmpty() && player.hasPermission(PermissionsUtil.getJoinPermission())) {
            player.sendMessage(Component.text("There is a job running! ", NamedTextColor.DARK_AQUA, TextDecoration.BOLD)
                    .append(Msg.button("[Click to check]", NamedTextColor.AQUA, "/job check", "Run /job check")));
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.5f, 2f);
        }
        // Restore the job border for players who relog while in a job.
        JobBorderManager.refresh(player);
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        // Reapply (or clear) the job border when the player moves between worlds.
        JobBorderManager.refresh(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        // The client-side border vanishes on disconnect; just clean up our tracking set.
        JobBorderManager.forget(event.getPlayer().getUniqueId());
    }

    private List<UUID> playersSwitchedToCreative = new ArrayList<>();

    @EventHandler(priority = EventPriority.LOW)
    public void playerMove(PlayerMoveEvent event) {
        Location to = event.getTo();
        if (to == null) {
            return;
        }
        Location from = event.getFrom();
        // Only act when the player actually crosses into a different block.
        // (The old reference-equality check on getBlock() was always true, so this ran every tick.)
        if (from.getBlockX() == to.getBlockX()
                && from.getBlockY() == to.getBlockY()
                && from.getBlockZ() == to.getBlockZ()) {
            return;
        }
        Player player = event.getPlayer();
        if (player.hasPermission(PermissionsUtil.getIgnoreWorldProtection())) {
            playersSwitchedToCreative.remove(player.getUniqueId());
            return;
        }
        // Single protection lookup decides the gamemode (no separate getJobWorking scan).
        if (ProtectionUtil.getBuildProtection(player, to).equals(BuildProtection.ALLOWED)) {
            if (player.getGameMode() == GameMode.SURVIVAL) {
                if (!playersSwitchedToCreative.contains(player.getUniqueId())) {
                    playersSwitchedToCreative.add(player.getUniqueId());
                }
                player.setGameMode(GameMode.CREATIVE);
            }
        } else {
            if (playersSwitchedToCreative.contains(player.getUniqueId())) {
                boolean flying = player.isFlying();
                player.setGameMode(GameMode.SURVIVAL);
                if (TheGaffer.getPluginInstance().getConfig().getBoolean("enableFlight", true)) {
                    player.setAllowFlight(true);
                    player.setFlying(flying);
                }
                playersSwitchedToCreative.remove(player.getUniqueId());
            }
        }
    }

}
