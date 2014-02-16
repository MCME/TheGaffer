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
package co.mcme.thegaffer.listeners;

import co.mcme.thegaffer.TheGaffer;
import co.mcme.thegaffer.storage.Job;
import co.mcme.thegaffer.storage.JobDatabase;
import co.mcme.thegaffer.utilities.PermissionsUtil;
import java.awt.geom.Rectangle2D;
import java.util.HashMap;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Result;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class ProtectionListener implements Listener {

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        if (event.isCancelled()) {
            return;
        }
        if (event.getPlayer().hasPermission(PermissionsUtil.getIgnoreWorldProtection()) || TheGaffer.getUnprotectedWorlds().contains(event.getBlock().getWorld().getName())) {
            event.setCancelled(false);
        } else {
            World world = event.getBlock().getWorld();
            if (!JobDatabase.getActiveJobs().isEmpty()) {
                HashMap<Job, World> workingworlds = new HashMap();
                HashMap<Job, Rectangle2D> areas = new HashMap();
                for (Job job : JobDatabase.getActiveJobs().values()) {
                    workingworlds.put(job, job.getBukkitWorld());
                    areas.put(job, job.getBounds());
                }
                if (workingworlds.containsValue(world)) {
                    boolean playerisworking = false;
                    for (Job job : workingworlds.keySet()) {
                        if (job.isPlayerWorking(event.getPlayer())) {
                            playerisworking = true;
                        }
                    }
                    if (playerisworking) {
                        boolean isinjobarea = false;
                        int x = event.getBlock().getX();
                        int z = event.getBlock().getZ();
                        for (Job job : JobDatabase.getActiveJobs().values()) {
                            if (job.isPlayerWorking(event.getPlayer()) && job.getBounds().contains(x, z)) {
                                isinjobarea = true;
                                if (job.isPaused()) {
                                    event.getPlayer().sendMessage(ChatColor.RED + "The job is currently paused.");
                                    event.setCancelled(true);
                                    return;
                                }
                            }
                        }
                        if (isinjobarea) {
                            event.setCancelled(false);
                        } else {
                            event.getPlayer().sendMessage(ChatColor.RED + "You have gone out of bounds for the job.");
                            event.setCancelled(true);
                        }
                    } else {
                        event.getPlayer().sendMessage(ChatColor.DARK_RED + "You are not part of any job.");
                        event.setBuild(false);
                    }
                } else {
                    event.getPlayer().sendMessage(ChatColor.DARK_RED + "You are not allowed to build in this world.");
                    event.setBuild(false);
                }
            } else {
                event.getPlayer().sendMessage(ChatColor.DARK_RED + "You are not allowed to build when there are no jobs.");
                event.setBuild(false);
            }
        }
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        if (event.isCancelled()) {
            return;
        }
        if (event.getPlayer().hasPermission(PermissionsUtil.getIgnoreWorldProtection()) || TheGaffer.getUnprotectedWorlds().contains(event.getBlock().getWorld().getName())) {
            event.setCancelled(false);
        } else {
            World world = event.getBlock().getWorld();
            if (!JobDatabase.getActiveJobs().isEmpty()) {
                HashMap<Job, World> workingworlds = new HashMap();
                HashMap<Job, Rectangle2D> areas = new HashMap();
                for (Job job : JobDatabase.getActiveJobs().values()) {
                    workingworlds.put(job, job.getBukkitWorld());
                    areas.put(job, job.getBounds());
                }
                if (workingworlds.containsValue(world)) {
                    boolean playerisworking = false;
                    for (Job job : workingworlds.keySet()) {
                        if (job.isPlayerWorking(event.getPlayer())) {
                            playerisworking = true;
                        }
                    }
                    if (playerisworking) {
                        boolean isinjobarea = false;
                        int x = event.getBlock().getX();
                        int z = event.getBlock().getZ();
                        for (Job job : JobDatabase.getActiveJobs().values()) {
                            if (job.isPlayerWorking(event.getPlayer()) && job.getBounds().contains(x, z)) {
                                isinjobarea = true;
                                if (job.isPaused()) {
                                    event.getPlayer().sendMessage(ChatColor.RED + "The job is currently paused.");
                                    event.setCancelled(true);
                                    return;
                                }
                            }
                        }
                        if (isinjobarea) {
                            event.setCancelled(false);
                        } else {
                            event.getPlayer().sendMessage(ChatColor.RED + "You have gone out of bounds for the job.");
                            event.setCancelled(true);
                        }
                    } else {
                        event.getPlayer().sendMessage(ChatColor.DARK_RED + "You are not part of any job.");
                        event.setCancelled(true);
                    }
                } else {
                    event.getPlayer().sendMessage(ChatColor.DARK_RED + "You are not allowed to build in this world.");
                    event.setCancelled(true);
                }
            } else {
                event.getPlayer().sendMessage(ChatColor.DARK_RED + "You are not allowed to build when there are no jobs.");
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onHangingBreak(HangingBreakByEntityEvent event) {
        if (event.isCancelled()) {
            return;
        }
        if (event.getRemover() instanceof Player) {
            Player player = (Player) event.getRemover();
            if (player.hasPermission(PermissionsUtil.getIgnoreWorldProtection()) || TheGaffer.getUnprotectedWorlds().contains(event.getEntity().getWorld().getName())) {
                event.setCancelled(false);
            } else {
                World world = event.getEntity().getWorld();
                if (!JobDatabase.getActiveJobs().isEmpty()) {
                    HashMap<Job, World> workingworlds = new HashMap();
                    HashMap<Job, Rectangle2D> areas = new HashMap();
                    for (Job job : JobDatabase.getActiveJobs().values()) {
                        workingworlds.put(job, job.getBukkitWorld());
                        areas.put(job, job.getBounds());
                    }
                    if (workingworlds.containsValue(world)) {
                        boolean playerisworking = false;
                        for (Job job : workingworlds.keySet()) {
                            if (job.isPlayerWorking(player)) {
                                playerisworking = true;
                            }
                        }
                        if (playerisworking) {
                            boolean isinjobarea = false;
                            double x = event.getEntity().getLocation().getX();
                            double z = event.getEntity().getLocation().getZ();
                            for (Job job : JobDatabase.getActiveJobs().values()) {
                                if (job.isPlayerWorking(player) && job.getBounds().contains(x, z)) {
                                    isinjobarea = true;
                                    if (job.isPaused()) {
                                        player.sendMessage(ChatColor.RED + "The job is currently paused.");
                                        event.setCancelled(true);
                                        return;
                                    }
                                }
                            }
                            if (isinjobarea) {
                                event.setCancelled(false);
                            } else {
                                player.sendMessage(ChatColor.RED + "You have gone out of bounds for the job.");
                                event.setCancelled(true);
                            }
                        } else {
                            player.sendMessage(ChatColor.DARK_RED + "You are not part of any job.");
                            event.setCancelled(true);
                        }
                    } else {
                        player.sendMessage(ChatColor.DARK_RED + "You are not allowed to build in this world.");
                        event.setCancelled(true);
                    }
                } else {
                    player.sendMessage(ChatColor.DARK_RED + "You are not allowed to build when there are no jobs.");
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void onHangingPlace(HangingPlaceEvent event) {
        if (event.isCancelled()) {
            return;
        }
        Player player = (Player) event.getPlayer();
        if (player.hasPermission(PermissionsUtil.getIgnoreWorldProtection()) || TheGaffer.getUnprotectedWorlds().contains(event.getEntity().getWorld().getName())) {
            event.setCancelled(false);
        } else {
            World world = event.getEntity().getWorld();
            if (!JobDatabase.getActiveJobs().isEmpty()) {
                HashMap<Job, World> workingworlds = new HashMap();
                HashMap<Job, Rectangle2D> areas = new HashMap();
                for (Job job : JobDatabase.getActiveJobs().values()) {
                    workingworlds.put(job, job.getBukkitWorld());
                    areas.put(job, job.getBounds());
                }
                if (workingworlds.containsValue(world)) {
                    boolean playerisworking = false;
                    for (Job job : workingworlds.keySet()) {
                        if (job.isPlayerWorking(player)) {
                            playerisworking = true;
                        }
                    }
                    if (playerisworking) {
                        boolean isinjobarea = false;
                        double x = event.getEntity().getLocation().getX();
                        double z = event.getEntity().getLocation().getZ();
                        for (Job job : JobDatabase.getActiveJobs().values()) {
                            if (job.isPlayerWorking(player) && job.getBounds().contains(x, z)) {
                                isinjobarea = true;
                                if (job.isPaused()) {
                                    event.getPlayer().sendMessage(ChatColor.RED + "The job is currently paused.");
                                    event.setCancelled(true);
                                    return;
                                }
                            }
                        }
                        if (isinjobarea) {
                            event.setCancelled(false);
                        } else {
                            player.sendMessage(ChatColor.RED + "You have gone out of bounds for the job.");
                            event.setCancelled(true);
                        }
                    } else {
                        player.sendMessage(ChatColor.DARK_RED + "You are not part of any job.");
                        event.setCancelled(true);
                    }
                } else {
                    player.sendMessage(ChatColor.DARK_RED + "You are not allowed to build in this world.");
                    event.setCancelled(true);
                }
            } else {
                player.sendMessage(ChatColor.DARK_RED + "You are not allowed to build when there are no jobs.");
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.isCancelled()) {
            return;
        }
        boolean restricted = false;
        Player player = (Player) event.getPlayer();
        if (player.hasPermission(PermissionsUtil.getIgnoreWorldProtection()) || TheGaffer.getUnprotectedWorlds().contains(event.getPlayer().getWorld().getName())) {
            event.setCancelled(false);
        } else if (event.hasItem() && event.hasBlock()) {
            if (event.getItem().getType().equals(Material.INK_SACK)) {
                if (event.getItem().getData().getData() == 15
                        && (event.getClickedBlock().getType() == Material.GRASS
                        || event.getClickedBlock().getType() == Material.SAPLING
                        || event.getClickedBlock().getType() == Material.CROPS
                        || event.getClickedBlock().getType() == Material.BROWN_MUSHROOM
                        || event.getClickedBlock().getType() == Material.RED_MUSHROOM
                        || event.getClickedBlock().getType() == Material.PUMPKIN_STEM
                        || event.getClickedBlock().getType() == Material.MELON_STEM
                        || event.getClickedBlock().getType() == Material.POTATO
                        || event.getClickedBlock().getType() == Material.CARROT
                        || event.getClickedBlock().getType() == Material.COCOA
                        || event.getClickedBlock().getType() == Material.LONG_GRASS)) {
                    restricted = true;
                } else if (event.getItem().getData().getData() == 3) {
                    restricted = true;
                }
            }
            if (event.getClickedBlock().getType().equals(Material.FLOWER_POT)) {
                //cancelling this currently does nothing.
                if (event.getItem().getType() == Material.RED_ROSE
                        || event.getItem().getType() == Material.YELLOW_FLOWER
                        || event.getItem().getType() == Material.SAPLING
                        || event.getItem().getType() == Material.RED_MUSHROOM
                        || event.getItem().getType() == Material.BROWN_MUSHROOM
                        || event.getItem().getType() == Material.CACTUS
                        || event.getItem().getType() == Material.LONG_GRASS
                        || event.getItem().getType() == Material.DEAD_BUSH) {
                    restricted = true;
                }
            }
        } else if (event.hasBlock() && event.getClickedBlock().getRelative(event.getBlockFace()).getType().equals(Material.FIRE)) {
            restricted = true;
        }
        if (restricted) {
            World world = event.getClickedBlock().getWorld();
            if (!JobDatabase.getActiveJobs().isEmpty()) {
                HashMap<Job, World> workingworlds = new HashMap();
                HashMap<Job, Rectangle2D> areas = new HashMap();
                for (Job job : JobDatabase.getActiveJobs().values()) {
                    workingworlds.put(job, job.getBukkitWorld());
                    areas.put(job, job.getBounds());
                }
                if (workingworlds.containsValue(world)) {
                    boolean playerisworking = false;
                    for (Job job : workingworlds.keySet()) {
                        if (job.isPlayerWorking(event.getPlayer())) {
                            playerisworking = true;
                        }
                    }
                    if (playerisworking) {
                        boolean isinjobarea = false;
                        int x = event.getClickedBlock().getX();
                        int z = event.getClickedBlock().getZ();
                        for (Job job : JobDatabase.getActiveJobs().values()) {
                            if (job.isPlayerWorking(event.getPlayer()) && job.getBounds().contains(x, z)) {
                                isinjobarea = true;
                                if (job.isPaused()) {
                                    event.getPlayer().sendMessage(ChatColor.RED + "The job is currently paused.");
                                    event.setCancelled(true);
                                    return;
                                }
                            }
                        }
                        if (isinjobarea) {
                            event.setCancelled(false);
                        } else {
                            event.getPlayer().sendMessage(ChatColor.RED + "You have gone out of bounds for the job.");
                            event.setUseItemInHand(Result.DENY);
                            event.setCancelled(true);
                        }
                    } else {
                        event.getPlayer().sendMessage(ChatColor.DARK_RED + "You are not part of any job.");
                        event.setUseItemInHand(Result.DENY);
                        event.setCancelled(true);
                    }
                } else {
                    event.getPlayer().sendMessage(ChatColor.DARK_RED + "You are not allowed to build in this world.");
                    event.setUseItemInHand(Result.DENY);
                    event.setCancelled(true);
                }
            } else {
                event.getPlayer().sendMessage(ChatColor.DARK_RED + "You are not allowed to build when there are no jobs.");
                event.setUseItemInHand(Result.DENY);
                event.setCancelled(true);
            }
        }
    }
}
