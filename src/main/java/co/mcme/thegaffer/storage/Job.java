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
package co.mcme.thegaffer.storage;

import co.mcme.thegaffer.GafferResponses.BanWorkerResponse;
import co.mcme.thegaffer.GafferResponses.HelperResponse;
import co.mcme.thegaffer.GafferResponses.InviteResponse;
import co.mcme.thegaffer.GafferResponses.KickWorkerResponse;
import co.mcme.thegaffer.GafferResponses.WorkerResponse;
import co.mcme.thegaffer.TheGaffer;
import co.mcme.thegaffer.utilities.PermissionsUtil;
import java.awt.Polygon;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Date;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.codehaus.jackson.annotate.JsonIgnore;

public class Job implements Listener {

    @Getter
    @Setter
    private String name;
    @Getter
    @Setter
    private String owner;
    @Getter
    @Setter
    private boolean running;
    @Getter
    @Setter
    private JobWarp warp;
    @Getter
    @Setter
    private ArrayList<String> helpers = new ArrayList();
    @Getter
    @Setter
    private ArrayList<String> workers = new ArrayList();
    @Getter
    @Setter
    private ArrayList<String> bannedWorkers = new ArrayList();
    @Getter
    @Setter
    private ArrayList<String> invitedWorkers = new ArrayList();
    @Getter
    @Setter
    private Long startTime;
    @Getter
    @Setter
    private Long endTime;
    @Getter
    @Setter
    private String world;
    @Getter
    @Setter
    private boolean Private;
    @Getter
    @Setter
    private int jobRadius;
    @Getter
    @Setter
    @JsonIgnore
    private Polygon area;
    @Getter
    @Setter
    @JsonIgnore
    private Rectangle2D bounds;
    @Getter
    @Setter
    @JsonIgnore
    private boolean dirty;

    public Job(String name, String owner, boolean running, JobWarp warp, String world, boolean Private, int jr) {
        this.name = name;
        this.owner = owner;
        this.running = running;
        this.warp = warp;
        this.world = world;
        this.Private = Private;
        this.startTime = System.currentTimeMillis();
        if (jr > 1000) {
            jr = 1000;
        }
        this.jobRadius = jr;
        Location bukkitLoc = warp.toBukkitLocation();
        int zbounds[] = {bukkitLoc.getBlockZ() - jobRadius, bukkitLoc.getBlockZ() + jobRadius};
        int xbounds[] = {bukkitLoc.getBlockX() - jobRadius, bukkitLoc.getBlockX() + jobRadius};
        this.area = new Polygon(xbounds, zbounds, xbounds.length);
        this.bounds = area.getBounds2D();
    }

    public Job() {
    }

    @JsonIgnore
    public void generateBounds() {
        if (jobRadius > 1000) {
            jobRadius = 1000;
        }
        Location bukkitLoc = warp.toBukkitLocation();
        int zbounds[] = {bukkitLoc.getBlockZ() - jobRadius, bukkitLoc.getBlockZ() + jobRadius};
        int xbounds[] = {bukkitLoc.getBlockX() - jobRadius, bukkitLoc.getBlockX() + jobRadius};
        this.area = new Polygon(xbounds, zbounds, xbounds.length);
        this.bounds = area.getBounds2D();
    }

    @JsonIgnore
    public OfflinePlayer getOwnerAsOfflinePlayer() {
        return TheGaffer.getServerInstance().getOfflinePlayer(owner);
    }

    @JsonIgnore
    public boolean isPlayerHelper(OfflinePlayer p) {
        return helpers.contains(p.getName());
    }

    @JsonIgnore
    public boolean isPlayerWorking(OfflinePlayer p) {
        return workers.contains(p.getName());
    }

    @JsonIgnore
    public World getBukkitWorld() {
        return TheGaffer.getServerInstance().getWorld(world);
    }

    @JsonIgnore
    public Player[] getWorkersAsPlayers() {
        ArrayList<Player> players = new ArrayList();
        for (String pName : workers) {
            OfflinePlayer p = TheGaffer.getServerInstance().getOfflinePlayer(pName);
            if (p.isOnline()) {
                players.add(p.getPlayer());
            }
        }
        return players.toArray(new Player[players.size()]);
    }

    @JsonIgnore
    public String getInfo() {
        StringBuilder out = new StringBuilder();
        out.append(ChatColor.GRAY).append(getName()).append("\n");
        out.append("Started by: ").append(ChatColor.AQUA).append(getOwner()).append("\n").append(ChatColor.GRAY);
        out.append("Started on: ").append(ChatColor.AQUA).append(new Date(getStartTime()).toGMTString()).append("\n").append(ChatColor.GRAY);
        out.append("Location: ").append(ChatColor.AQUA).append(getWorld()).append(" (x: ").append((int) getWarp().getX()).append(", y: ").append((int) getWarp().getY()).append(", z: ").append((int) getWarp().getZ()).append(")").append("\n").append(ChatColor.GRAY);
        String status = (running) ? ChatColor.GREEN + "OPEN" : ChatColor.RED + "CLOSED";
        out.append("Status: ").append(status);
        return out.toString();
    }

    public HelperResponse addHelper(OfflinePlayer p) {
        if (helpers.contains(p.getName())) {
            return HelperResponse.ALREADY_HELPER;
        }
        if (!p.isOnline()) {
            return HelperResponse.NOT_ONLINE;
        }
        if (!p.getPlayer().hasPermission(PermissionsUtil.getCreatePermission())) {
            return HelperResponse.NO_PERMISSIONS;
        }
        helpers.add(p.getName());
        setDirty(true);
        JobDatabase.saveJobs();
        return HelperResponse.ADD_SUCCESS;
    }

    public HelperResponse removeHelper(OfflinePlayer p) {
        if (!helpers.contains(p.getName())) {
            return HelperResponse.NOT_HELPER;
        }
        helpers.remove(p.getName());
        setDirty(true);
        JobDatabase.saveJobs();
        return HelperResponse.REMOVE_SUCCESS;
    }

    public WorkerResponse addWorker(OfflinePlayer p) {
        if (workers.contains(p.getName())) {
            return WorkerResponse.ALREADY_WORKER;
        }
        if (!p.isOnline()) {
            return WorkerResponse.NOT_ONLINE;
        }
        if (!p.getPlayer().hasPermission(PermissionsUtil.getJoinPermission())) {
            return WorkerResponse.NO_PERMISSIONS;
        }
        if (Private && !(invitedWorkers.contains(p.getName()))) {
            return WorkerResponse.NOT_INVITED;
        }
        workers.add(p.getName());
        if (p.isOnline()) {
            p.getPlayer().teleport(warp.toBukkitLocation());
        }
        setDirty(true);
        JobDatabase.saveJobs();
        return WorkerResponse.ADD_SUCCESS;
    }

    public WorkerResponse removeWorker(OfflinePlayer p) {
        if (!workers.contains(p.getName())) {
            return WorkerResponse.NOT_WORKER;
        }
        workers.remove(p.getName());
        setDirty(true);
        JobDatabase.saveJobs();
        return WorkerResponse.REMOVE_SUCCESS;
    }

    public InviteResponse inviteWorker(OfflinePlayer p) {
        if (invitedWorkers.contains(p.getName())) {
            return InviteResponse.ALREADY_INVITED;
        }
        if (!p.isOnline()) {
            return InviteResponse.NOT_ONLINE;
        }
        if (!p.getPlayer().hasPermission(PermissionsUtil.getJoinPermission())) {
            return InviteResponse.NO_PERMISSIONS;
        }
        invitedWorkers.add(p.getName());
        setDirty(true);
        JobDatabase.saveJobs();
        return InviteResponse.ADD_SUCCESS;
    }

    public InviteResponse uninviteWorker(OfflinePlayer p) {
        if (!invitedWorkers.contains(p.getName())) {
            return InviteResponse.NOT_INVITED;
        }
        if (workers.contains(p.getName())) {
            workers.remove(p.getName());
        }
        invitedWorkers.remove(p.getName());
        setDirty(true);
        JobDatabase.saveJobs();
        return InviteResponse.REMOVE_SUCCESS;
    }

    public BanWorkerResponse banWorker(OfflinePlayer p) {
        if (workers.contains(p.getName())) {
            workers.remove(p.getName());
        }
        if (bannedWorkers.contains(p.getName())) {
            return BanWorkerResponse.ALREADY_BANNED;
        }
        bannedWorkers.add(p.getName());
        setDirty(true);
        JobDatabase.saveJobs();
        return BanWorkerResponse.BAN_SUCCESS;
    }

    public BanWorkerResponse unbanWorker(OfflinePlayer p) {
        if (bannedWorkers.contains(p.getName())) {
            return BanWorkerResponse.ALREADY_UNBANNED;
        }
        bannedWorkers.remove(p.getName());
        setDirty(true);
        JobDatabase.saveJobs();
        return BanWorkerResponse.UNBAN_SUCCESS;
    }

    public KickWorkerResponse kickWorker(OfflinePlayer p) {
        if (!workers.contains(p.getName())) {
            return KickWorkerResponse.NOT_IN_JOB;
        }
        workers.remove(p.getName());
        setDirty(true);
        JobDatabase.saveJobs();
        return KickWorkerResponse.KICK_SUCCESS;
    }
    
    public void updateLocation(Location loc) {
        getWarp().setX(loc.getX());
        getWarp().setY(loc.getY());
        getWarp().setZ(loc.getZ());
        getWarp().setYaw(loc.getYaw());
        getWarp().setPitch(loc.getPitch());
        getWarp().setWorld(loc.getWorld().getName());
        setDirty(true);
        JobDatabase.saveJobs();
    }
    
    public void updateJobRadius(int newRadius) {
        setJobRadius(newRadius);
        generateBounds();
        setDirty(true);
        JobDatabase.saveJobs();
    }

    public void bringAllWorkers(Location to) {
        for (String wName : workers) {
            if (TheGaffer.getServerInstance().getOfflinePlayer(wName).isOnline()) {
                TheGaffer.getServerInstance().getOfflinePlayer(wName).getPlayer().teleport(to);
            }
        }
    }

    public int sendToHelpers(String message) {
        int count = 0;
        for (String hName : helpers) {
            if (TheGaffer.getServerInstance().getOfflinePlayer(hName).isOnline()) {
                TheGaffer.getServerInstance().getOfflinePlayer(hName).getPlayer().sendMessage(message);
                count++;
            }
        }
        if (getOwnerAsOfflinePlayer().isOnline()) {
            getOwnerAsOfflinePlayer().getPlayer().sendMessage(message);
            count++;
        }
        return count;
    }

    public int sendToWorkers(String message) {
        int count = 0;
        for (String wName : workers) {
            if (TheGaffer.getServerInstance().getOfflinePlayer(wName).isOnline()) {
                TheGaffer.getServerInstance().getOfflinePlayer(wName).getPlayer().sendMessage(message);
                count++;
            }
        }
        return count;
    }

    public int sendToAll(String message) {
        return sendToHelpers(message) + sendToWorkers(message);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onLeave(PlayerQuitEvent event) {
        if (event.getPlayer().getName().equals(owner)) {
            TheGaffer.scheduleOwnerTimeout(this);
        }
    }
}
