/*
 * This file is really messy, and needs a lot of work
 */
package co.mcme.jobs.listeners;

import co.mcme.jobs.Job;
import static co.mcme.jobs.Jobs.opened_worlds;
import static co.mcme.jobs.Jobs.protected_worlds;
import co.mcme.jobs.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;

public class ProtectionListener implements Listener {

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        World happenedin = event.getBlock().getWorld();
        Util.debug("Place event fired in " + happenedin.getName());
        if (protected_worlds.contains(happenedin)) {
            Util.debug("Place event is in protected world");
            if (opened_worlds.containsValue(happenedin)) {
                Util.debug("Place event is is in opened world");
                OfflinePlayer toCheck = Bukkit.getOfflinePlayer(event.getPlayer().getName());
                for (Job job : opened_worlds.keySet()) {
                    if (job.getWorld().equals(happenedin)) {
                        if (toCheck.getPlayer().hasPermission("jobs.ignorestatus")) {
                            event.setCancelled(false);
                        } else {
                            if (job.isWorking(toCheck)) {
                                int x = toCheck.getPlayer().getLocation().getBlockX();
                                int z = toCheck.getPlayer().getLocation().getBlockZ();
                                event.setCancelled(!job.getBounds().contains(x, z));
                            } else {
                                event.setCancelled(true);
                            }
                        }
                    }
                }
            } else {
                Util.debug("Protected world is not open!");
                event.setCancelled(!event.getPlayer().hasPermission("jobs.ignorestatus"));
            }
        }
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        World happenedin = event.getBlock().getWorld();
        Util.debug("Break event fired in " + happenedin.getName());
        if (protected_worlds.contains(happenedin)) {
            Util.debug("Break event is in protected world");
            if (opened_worlds.containsValue(happenedin)) {
                Util.debug("Break event is in opened world");
                OfflinePlayer toCheck = Bukkit.getOfflinePlayer(event.getPlayer().getName());
                for (Job job : opened_worlds.keySet()) {
                    if (job.getWorld().equals(happenedin)) {
                        if (toCheck.getPlayer().hasPermission("jobs.ignorestatus")) {
                            event.setCancelled(false);
                        } else {
                            if (job.isWorking(toCheck)) {
                                int x = toCheck.getPlayer().getLocation().getBlockX();
                                int z = toCheck.getPlayer().getLocation().getBlockZ();
                                event.setCancelled(!job.getBounds().contains(x, z));
                            } else {
                                event.setCancelled(true);
                            }
                        }
                    }
                }
            } else {
                Util.debug("Protected world is not open!");
                event.setCancelled(!event.getPlayer().hasPermission("jobs.ignorestatus"));
            }
        }
    }

    @EventHandler
    public void onHangingBreak(HangingBreakByEntityEvent event) {
        World happenedin = event.getEntity().getWorld();
        Util.debug("Hanging break event fired in " + happenedin.getName());
        if (event.getEntity() instanceof Player) {
            if (protected_worlds.contains(happenedin)) {
                Util.debug("Hanging break event is in protected world");

                OfflinePlayer toCheck = (OfflinePlayer) event.getEntity();
                for (Job job : opened_worlds.keySet()) {
                    if (job.getWorld().equals(happenedin)) {
                        if (toCheck.getPlayer().hasPermission("jobs.ignorestatus")) {
                            event.setCancelled(false);
                        } else {
                            if (job.isWorking(toCheck)) {
                                int x = toCheck.getPlayer().getLocation().getBlockX();
                                int z = toCheck.getPlayer().getLocation().getBlockZ();
                                event.setCancelled(!job.getBounds().contains(x, z));
                            } else {
                                event.setCancelled(true);
                            }
                        }
                    }
                }
            }
        } else {
            Util.debug("Protected world is not open!");
            event.setCancelled(!((Player) event.getEntity()).hasPermission("jobs.ignorestatus"));
        }
    }

    @EventHandler
    public void onHangingPlace(HangingPlaceEvent event) {
        World happenedin = event.getBlock().getWorld();
        Util.debug("Hanging place event fired in " + happenedin.getName());
        if (protected_worlds.contains(happenedin)) {
            Util.debug("Hanging place event is in protected world");
            if (opened_worlds.containsValue(happenedin)) {
                Util.debug("Hanging place event is in opened world");
                OfflinePlayer toCheck = Bukkit.getOfflinePlayer(event.getPlayer().getName());
                for (Job job : opened_worlds.keySet()) {
                    if (job.getWorld().equals(happenedin)) {
                        if (toCheck.getPlayer().hasPermission("jobs.ignorestatus")) {
                            event.setCancelled(false);
                        } else {
                            if (job.isWorking(toCheck)) {
                                int x = toCheck.getPlayer().getLocation().getBlockX();
                                int z = toCheck.getPlayer().getLocation().getBlockZ();
                                event.setCancelled(!job.getBounds().contains(x, z));
                            } else {
                                event.setCancelled(true);
                            }
                        }
                    }
                }
            } else {
                Util.debug("Protected world is not open!");
                event.setCancelled(!event.getPlayer().hasPermission("jobs.ignorestatus"));
            }
        }
    }
}
