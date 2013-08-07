package co.mcme.jobs;

import co.mcme.jobs.util.Util;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class Job implements Listener {

    private OfflinePlayer admin;
    private String name;
    private boolean status;
    private Location warpto;
    private ArrayList<String> runners = new ArrayList();
    private ArrayList<String> workers = new ArrayList();
    private Long started;
    private World world;

    public Job(String n, String a, boolean s, Location loc, String w) {
        this.admin = Bukkit.getOfflinePlayer(a);
        this.name = n;
        this.status = s;
        this.warpto = loc;
        started = System.currentTimeMillis();
        this.world = Bukkit.getWorld(w);
    }

    public Job(String n, String a, boolean s, ArrayList<String> helpers, Location loc, Long started, ArrayList<String> parti, String w) {
        this.admin = Bukkit.getOfflinePlayer(a);
        this.name = n;
        this.status = s;
        if (!helpers.isEmpty()) {
            this.runners = helpers;
        } else {
            this.runners = new ArrayList();
        }
        this.warpto = loc;
        this.started = started;
        this.workers = parti;
        this.world = Bukkit.getWorld(w);
    }

    public String getName() {
        return name;
    }

    public boolean setName(String newname) {
        if (!name.equals(newname)) {
            name = newname;
            return true;
        } else {
            return false;
        }
    }

    public OfflinePlayer getAdmin() {
        return admin;
    }

    public void setAdmin(OfflinePlayer newadmin) {
        this.admin = newadmin;
    }

    public ArrayList<String> getHelpers() {
        return runners;
    }

    public ArrayList<String> getWorkers() {
        return workers;
    }

    public boolean addWorker(Player p) {
        if (!workers.contains(p.getName()) && status) {
            workers.add(p.getName());
            sendToRunners(ChatColor.AQUA + p.getName() + ChatColor.GRAY + " has joined the job.");
            sendToWorkers(ChatColor.AQUA + p.getName() + ChatColor.GRAY + " has joined the job.");
            return true;
        } else {
            return false;
        }
    }

    public Location getWarp() {
        return warpto;
    }

    public void setWarp(Location newloc) {
        newloc.setX((int) newloc.getX());
        newloc.setY((int) newloc.getY());
        newloc.setZ((int) newloc.getZ());
        warpto = newloc;
    }

    public void setStatus(boolean news) {
        status = news;
    }

    public boolean getStatus() {
        return status;
    }

    public boolean addHelper(OfflinePlayer p) {
        if (runners.contains(p.getName())) {
            return false;
        } else {
            runners.add(p.getName());
            sendToRunners(ChatColor.AQUA + p.getName() + ChatColor.GRAY + " has been added to the job staff.");
            return true;
        }
    }

    public boolean removeHelper(OfflinePlayer p) {
        if (runners.contains(p.getName())) {
            runners.remove(p.getName());
            sendToRunners(ChatColor.AQUA + p.getName() + ChatColor.GRAY + " has been removed from the job staff.");
            return true;
        } else {
            return false;
        }
    }

    public Long getRunningSince() {
        return started;
    }

    public World getWorld() {
        return world;
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        if (event.getBlock().getWorld().equals(world)) {
            if (status || event.getPlayer().hasPermission("jobs.ignorestatus")) {
                event.setCancelled(false);
            } else {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        if (event.getBlock().getWorld().equals(world)) {
            if (status || event.getPlayer().hasPermission("jobs.ignorestatus")) {
                event.setCancelled(false);
            } else {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent event) {
        if (event.getPlayer().getName().equals(admin.getName())) {
            Jobs.scheduleAdminTimeout(this);
            Util.debug("Queued " + name + " for timeout");
        }
    }

    public void writeToFile() throws IOException {
        Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
        try (JsonWriter writer = new JsonWriter(new FileWriter(Bukkit.getPluginManager().getPlugin("TheGaffer").getDataFolder().getPath() + System.getProperty("file.separator") + "Jobs" + System.getProperty("file.separator") + name + ".job"))) {
            writer.beginArray().beginObject();
            writer.name("name").value(name);
            writer.name("runby").value(admin.getName());
            writer.name("status").value(status);
            writer.name("world").value(world.getName());
            writer.name("started").value(started);

            writer.name("helpers").beginArray();
            for (String helper : runners) {
                writer.value(helper);
            }
            writer.endArray();
            writer.name("workers").beginArray();
            for (String worker : workers) {
                writer.value(worker);
            }
            writer.endArray();

            writer.name("location").beginArray().beginObject();
            writer.name("x").value(warpto.getX());
            writer.name("y").value(warpto.getY());
            writer.name("z").value(warpto.getZ());
            writer.name("pitch").value(warpto.getPitch());
            writer.name("yaw").value(warpto.getYaw());
            writer.endObject().endArray();

            writer.endObject().endArray();
            writer.close();
        }
    }

    public void sendToRunners(String msg) {
        for (String Pname : runners) {
            OfflinePlayer target = Bukkit.getOfflinePlayer(Pname);
            if (target.isOnline()) {
                target.getPlayer().sendMessage(msg);
            }
        }
    }

    public void sendToWorkers(String msg) {
        for (String Pname : workers) {
            OfflinePlayer target = Bukkit.getOfflinePlayer(Pname);
            if (target.isOnline()) {
                target.getPlayer().sendMessage(msg);
            }
        }
    }
}
