package co.mcme.jobs;

import co.mcme.jobs.util.Util;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonWriter;
import java.io.File;
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
    private ArrayList<String> bannedworkers = new ArrayList();
    private boolean dirty = false;
    private String filename;

    public Job(String n, String a, boolean s, Location loc, String w) {
        this.admin = Bukkit.getOfflinePlayer(a);
        this.name = n;
        this.status = s;
        this.warpto = loc;
        started = System.currentTimeMillis();
        this.world = Bukkit.getWorld(w);
        if (status) {
            Jobs.protected_worlds.add(world);
        }
    }

    public Job(String n, String a, boolean s, ArrayList<String> helpers, Location loc, Long started, ArrayList<String> parti, String w, ArrayList<String> banned, String fname) {
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
        if (status) {
            Jobs.protected_worlds.add(world);
        }
        this.filename = fname;
    }

    public String getName() {
        return name;
    }

    public boolean setName(String newname) {
        if (!name.equals(newname)) {
            name = newname;
            dirty = true;
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
        dirty = true;
    }

    public ArrayList<String> getHelpers() {
        return runners;
    }

    public ArrayList<String> getWorkers() {
        return workers;
    }

    public boolean addWorker(Player p) {
        if (!workers.contains(p.getName()) && status && !bannedworkers.contains(p.getName())) {
            workers.add(p.getName());
            sendToRunners(ChatColor.AQUA + p.getName() + ChatColor.GRAY + " has joined the job.");
            sendToWorkers(ChatColor.AQUA + p.getName() + ChatColor.GRAY + " has joined the job.");
            dirty = true;
            p.teleport(warpto);
            return true;
        } else {
            return false;
        }
    }

    public boolean removeWorker(OfflinePlayer p) {
        if (workers.contains(p.getName())) {
            workers.remove(p.getName());
            sendToRunners(ChatColor.AQUA + p.getName() + ChatColor.GRAY + " has been removed from the workers list.");
            dirty = true;
            return true;
        } else {
            return false;
        }
    }

    public boolean isWorking(OfflinePlayer p) {
        return workers.contains(p.getName());
    }

    public Location getWarp() {
        return warpto;
    }

    public void setWarp(Location newloc) {
        newloc.setX((int) newloc.getX());
        newloc.setY((int) newloc.getY());
        newloc.setZ((int) newloc.getZ());
        warpto = newloc;
        world = newloc.getWorld();
        Jobs.opened_worlds.put(this, world);
        dirty = true;
    }

    public void setStatus(boolean news) {
        status = news;
        dirty = true;
    }

    public boolean getStatus() {
        return status;
    }

    public boolean addHelper(OfflinePlayer p) {
        if (runners.contains(p.getName()) && !(bannedworkers.contains(p.getName()))) {
            sendToRunners(ChatColor.GREY + "Proper command usage is: /jobadmin [command] [jobname] [helpername]");
            return false;
        } else {
            runners.add(p.getName());
            sendToRunners(ChatColor.AQUA + p.getName() + ChatColor.GRAY + " has been added to the job staff.");
            dirty = true;
            return true;
        }
    }

    public boolean removeHelper(OfflinePlayer p) {
        if (runners.contains(p.getName())) {
            runners.remove(p.getName());
            sendToRunners(ChatColor.AQUA + p.getName() + ChatColor.GRAY + " has been removed from the job staff.");
            dirty = true;
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

    public boolean banWorker(OfflinePlayer p) {
        if (!bannedworkers.contains(p.getName())) {
            bannedworkers.add(p.getName());
            if (workers.contains(p.getName())) {
                workers.remove(p.getName());
            }
            sendToRunners(ChatColor.RED + p.getName() + " has been banned from the " + name + " job.");
            sendToWorkers(ChatColor.RED + p.getName() + " has been banned from the " + name + " job.");
            dirty = true;
            return true;
        } else {
            return false;
        }
    }

    public boolean unBanWorker(OfflinePlayer p) {
        if (bannedworkers.contains(p.getName())) {
            bannedworkers.remove(p.getName());
            sendToRunners(ChatColor.GRAY + p.getName() + " has been unbanned from the " + name + " job.");
            sendToWorkers(ChatColor.GRAY + p.getName() + " has been unbanned from the " + name + " job.");
            dirty = true;
            return true;
        } else {
            return false;
        }
    }

    public boolean isDirty() {
        return dirty;
    }

    public String getFileName() {
        return filename;
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
        String file_location = Jobs.getActiveDir().getPath() + System.getProperty("file.separator") + name + ".job";
        if (!status) {
            new File(file_location).delete();
            file_location = Jobs.getInactiveDir().getPath() + System.getProperty("file.separator") + name + "." + System.currentTimeMillis() + ".job";
            filename = file_location;
        }
        File file = new File(file_location);
        try (JsonWriter writer = new JsonWriter(new FileWriter(file))) {
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

            writer.name("banned").beginArray();
            for (String banned : bannedworkers) {
                writer.value(banned);
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
        if (admin.isOnline()) {
            admin.getPlayer().sendMessage(msg);
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
