package co.mcme.jobs.listeners;

import co.mcme.jobs.Job;
import co.mcme.jobs.Jobs;
import java.awt.geom.Rectangle2D;
import java.util.HashMap;
import org.bukkit.ChatColor;
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
        if (event.getPlayer().hasPermission("jobs.ignorestatus")) {
            event.setBuild(true);
        } else {
            World world = event.getBlock().getWorld();
            if (!Jobs.runningJobs.isEmpty()) {
                HashMap<Job, World> workingworlds = new HashMap();
                HashMap<Job, Rectangle2D> areas = new HashMap();
                for (Job job : Jobs.runningJobs.values()) {
                    workingworlds.put(job, job.getWorld());
                    areas.put(job, job.getBounds());
                }
                if (workingworlds.containsValue(world)) {
                    boolean playerisworking = false;
                    for (Job job : workingworlds.keySet()) {
                        if (job.isWorking(event.getPlayer())) {
                            playerisworking = true;
                        }
                    }
                    if (playerisworking) {
                        boolean isinjobarea = false;
                        int x = event.getBlock().getX();
                        int z = event.getBlock().getZ();
                        for (Job job : Jobs.runningJobs.values()) {
                            if (job.isWorking(event.getPlayer()) && job.getBounds().contains(x, z)) {
                                isinjobarea = true;
                            }
                        }
                        if (isinjobarea) {
                            event.setBuild(true);
                        } else {
                            event.getPlayer().sendMessage(ChatColor.RED + "You have gone out of bounds for the job.");
                            event.setBuild(false);
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
        if (event.getPlayer().hasPermission("jobs.ignorestatus")) {
            event.setCancelled(false);
        } else {
            World world = event.getBlock().getWorld();
            if (!Jobs.runningJobs.isEmpty()) {
                HashMap<Job, World> workingworlds = new HashMap();
                HashMap<Job, Rectangle2D> areas = new HashMap();
                for (Job job : Jobs.runningJobs.values()) {
                    workingworlds.put(job, job.getWorld());
                    areas.put(job, job.getBounds());
                }
                if (workingworlds.containsValue(world)) {
                    boolean playerisworking = false;
                    for (Job job : workingworlds.keySet()) {
                        if (job.isWorking(event.getPlayer())) {
                            playerisworking = true;
                        }
                    }
                    if (playerisworking) {
                        boolean isinjobarea = false;
                        int x = event.getBlock().getX();
                        int z = event.getBlock().getZ();
                        for (Job job : Jobs.runningJobs.values()) {
                            if (job.isWorking(player) && job.getBounds().contains(x, z)) {
                                isinjobarea = true;
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
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getRemover();
            if (player.hasPermission("jobs.ignorestatus")) {
                event.setCancelled(false);
            } else {
                World world = event.getEntity().getWorld();
                if (!Jobs.runningJobs.isEmpty()) {
                    HashMap<Job, World> workingworlds = new HashMap();
                    HashMap<Job, Rectangle2D> areas = new HashMap();
                    for (Job job : Jobs.runningJobs.values()) {
                        workingworlds.put(job, job.getWorld());
                        areas.put(job, job.getBounds());
                    }
                    if (workingworlds.containsValue(world)) {
                        boolean playerisworking = false;
                        for (Job job : workingworlds.keySet()) {
                            if (job.isWorking(player)) {
                                playerisworking = true;
                            }
                        }
                        if (playerisworking) {
                            boolean isinjobarea = false;
                            double x = event.getEntity().getLocation().getX();
                            double z = event.getEntity().getLocation().getZ();
                            for (Job job : Jobs.runningJobs.values()) {
                                if (job.isWorking(player) && job.getBounds().contains(x, z)) {
                                    isinjobarea = true;
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
        Player player = (Player) event.getPlayer();
        if (player.hasPermission("jobs.ignorestatus")) {
            event.setCancelled(false);
        } else {
            World world = event.getEntity().getWorld();
            if (!Jobs.runningJobs.isEmpty()) {
                HashMap<Job, World> workingworlds = new HashMap();
                HashMap<Job, Rectangle2D> areas = new HashMap();
                for (Job job : Jobs.runningJobs.values()) {
                    workingworlds.put(job, job.getWorld());
                    areas.put(job, job.getBounds());
                }
                if (workingworlds.containsValue(world)) {
                    boolean playerisworking = false;
                    for (Job job : workingworlds.keySet()) {
                        if (job.isWorking(player)) {
                            playerisworking = true;
                        }
                    }
                    if (playerisworking) {
                        boolean isinjobarea = false;
                        double x = event.getEntity().getLocation().getX();
                        double z = event.getEntity().getLocation().getZ();
                        for (Job job : Jobs.runningJobs.values()) {
                            if (job.isWorking(player) && job.getBounds().contains(x, z)) {
                                isinjobarea = true;
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
