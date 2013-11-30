package co.mcme.jobs;

import co.mcme.thegaffer.utilities.Util;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonWriter;
import java.awt.Polygon;
import java.awt.geom.Rectangle2D;
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
    private boolean inviteOnly;
    private ArrayList<String> invitedworkers = new ArrayList();
    private Polygon area;
    private Rectangle2D bounds;

    public Job(String n, String a, boolean s, Location loc, String w, boolean i) {
        this.admin = Bukkit.getOfflinePlayer(a);
        this.name = n;
        this.status = s;
        this.warpto = loc;
        started = System.currentTimeMillis();
        this.world = Bukkit.getWorld(w);
        this.inviteOnly = i;
        int zbounds[] = {loc.getBlockZ() - 100, loc.getBlockZ() + 250};
        int xbounds[] = {loc.getBlockX() - 100, loc.getBlockX() + 250};
        area = new Polygon(xbounds, zbounds, xbounds.length);
        bounds = area.getBounds2D();
    }

    public Job(String n, String a, boolean s, ArrayList<String> helpers, Location loc, Long started, ArrayList<String> parti, String w, ArrayList<String> banned, String fname, boolean i) {
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
        this.filename = fname;
        this.inviteOnly = i;
        int zbounds[] = {loc.getBlockZ() - 100, loc.getBlockZ() + 250};
        int xbounds[] = {loc.getBlockX() - 100, loc.getBlockX() + 250};
        area = new Polygon(xbounds, zbounds, xbounds.length);
        bounds = area.getBounds2D();
    }

    public Job(String n, String a, boolean s, ArrayList<String> helpers, Location loc, Long started, ArrayList<String> parti, String w, ArrayList<String> banned, String fname, boolean i, ArrayList<String> ip) {
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
        this.filename = fname;
        this.inviteOnly = i;
        this.invitedworkers = ip;
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
        if (!isInviteOnly()) {
            if (!workers.contains(p.getName()) && status && !bannedworkers.contains(p.getName())) {
                workers.add(p.getName());
                sendToAll(ChatColor.AQUA + p.getName() + ChatColor.GRAY + " has joined the job.");
                dirty = true;
                p.teleport(warpto);
                return true;
            } else {
                return false;
            }
        } else {
            if (!workers.contains(p.getName()) && status && !bannedworkers.contains(p.getName()) && invitedworkers.contains(p.getName())) {
                workers.add(p.getName());
                sendToAll(ChatColor.AQUA + p.getName() + ChatColor.GRAY + " has joined the job.");
                dirty = true;
                p.teleport(warpto);
                return true;
            } else {
                return false;
            }
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

    public boolean inviteWorker(OfflinePlayer p) {
        if (!invitedworkers.contains(p.getName())) {
            invitedworkers.add(p.getName());
            sendToRunners(ChatColor.AQUA + p.getName() + ChatColor.GRAY + " has been invited to the job.");
            dirty = true;
            return true;
        }
        return false;
    }

    public boolean unInviteWorker(OfflinePlayer p) {
        if (invitedworkers.contains(p.getName())) {
            invitedworkers.remove(p.getName());
            sendToRunners(ChatColor.AQUA + p.getName() + ChatColor.GRAY + " has been uninvited from the job.");
            dirty = true;
            return true;
        }
        return false;
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
        dirty = true;
        int zbounds[] = {newloc.getBlockZ() - 100, newloc.getBlockZ() + 250};
        int xbounds[] = {newloc.getBlockX() - 100, newloc.getBlockX() + 250};
        area = new Polygon(xbounds, zbounds, xbounds.length);
        bounds = area.getBounds2D();
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
            sendToAll(ChatColor.RED + p.getName() + " has been banned from the " + name + " job.");
            dirty = true;
            return true;
        } else {
            return false;
        }
    }

    public boolean unBanWorker(OfflinePlayer p) {
        if (bannedworkers.contains(p.getName())) {
            bannedworkers.remove(p.getName());
            sendToAll(ChatColor.GRAY + p.getName() + " has been unbanned from the " + name + " job.");
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

    public boolean isInviteOnly() {
        return inviteOnly;
    }
    
    public Rectangle2D getBounds() {
        return bounds;
    }
    
    public Polygon getArea() {
        return area;
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
        }
        File file = new File(file_location);
        if (!status) {
            filename = file.getName();
        }
        try (JsonWriter writer = new JsonWriter(new FileWriter(file))) {
            writer.beginArray().beginObject();
            writer.name("name").value(name);
            writer.name("version").value(Jobs.version);
            writer.name("runby").value(admin.getName());
            writer.name("status").value(status);
            writer.name("inviteonly").value(inviteOnly);
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

            writer.name("invited").beginArray();
            for (String invitee : invitedworkers) {
                writer.value(invitee);
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

    public void sendToAll(String msg) {
        for (String Pname : workers) {
            OfflinePlayer target = Bukkit.getOfflinePlayer(Pname);
            if (target.isOnline()) {
                target.getPlayer().sendMessage(msg);
            }
        }
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
}
