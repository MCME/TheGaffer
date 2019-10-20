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
package com.mcmiddleearth.thegaffer.storage;

import com.mcmiddleearth.thegaffer.GafferResponses.BanWorkerResponse;
import com.mcmiddleearth.thegaffer.GafferResponses.HelperResponse;
import com.mcmiddleearth.thegaffer.GafferResponses.InviteResponse;
import com.mcmiddleearth.thegaffer.GafferResponses.KickWorkerResponse;
import com.mcmiddleearth.thegaffer.GafferResponses.WorkerResponse;
import com.mcmiddleearth.thegaffer.TheGaffer;
import com.mcmiddleearth.thegaffer.utilities.PermissionsUtil;
import com.mcmiddleearth.thegaffer.utilities.Util;
import com.mcmiddleearth.thegaffer.utilities.VentureChatUtil;
import java.awt.Polygon;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.codehaus.jackson.annotate.JsonIgnore;

public class Job implements Listener {

    @Getter
    @Setter
    private String name;
    @Getter
    @Setter
    private String owner;
    @Getter
    private boolean running;
    @Getter
    @Setter
    private boolean paused;
    @Getter
    @Setter
    private JobWarp warp;
    @Getter
    @Setter
    private JobWarp tsWarp;
    @Getter
    @Setter
    private String ts;
    @Getter
    @Setter
    private boolean discordSend;
    @Getter
    @Setter
    private String[] discordTags;
    @Getter
    @Setter
    private String description;
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
    private ArrayList<String> admitedWorkers = new ArrayList();
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
    @Getter
    @Setter
    private JobKit kit;
    
    private Team helperTeam;
    private String helperTeamName;
    private Team workerTeam;
    private String workerTeamName;
    private Scoreboard scoreboard;

    @Getter
    @JsonIgnore
    private HashMap<UUID, Long> left = new HashMap<>();
    public Job(String name, String description, String owner, boolean running, JobWarp warp, String world, boolean Private, int jr, 
               boolean discordSend, String[] discordTags, String ts, JobWarp tswarp) {
        this.name = name;
        this.description = description;
        this.owner = owner;
        this.running = running;
        this.warp = warp;
        this.world = world;
        this.Private = Private;
        this.startTime = System.currentTimeMillis();
        this.discordSend = discordSend;
        this.discordTags = discordTags;
        this.ts = ts;
        this.tsWarp = tswarp;
        admitedWorkers.add(this.owner);
        VentureChatUtil.joinJobChannel(owner);
        if (jr > 1000) {
            jr = 1000;
        }
        this.jobRadius = jr;
        Location bukkitLoc = warp.toBukkitLocation();
        int zbounds[] = {bukkitLoc.getBlockZ() - jobRadius, bukkitLoc.getBlockZ() + jobRadius};
        int xbounds[] = {bukkitLoc.getBlockX() - jobRadius, bukkitLoc.getBlockX() + jobRadius};
        this.area = new Polygon(xbounds, zbounds, xbounds.length);
        this.bounds = area.getBounds2D();
        
        if(TheGaffer.isGlowing()) {
            scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
            helperTeamName = name+"Helper";
            helperTeam = scoreboard.registerNewTeam(name+"Helper");
            helperTeam.setColor(ChatColor.valueOf(TheGaffer.getHelperColor()));
            //helperTeam.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.NEVER);
            workerTeamName = name+"Worker";
            workerTeam = scoreboard.registerNewTeam(name+"Worker");
            workerTeam.setColor(ChatColor.valueOf(TheGaffer.getWorkerColor()));
            //workerTeam.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.NEVER);

            addHelperTeam(owner);
        }
    }

    public Job() {
    }

    @JsonIgnore
    public File getFile() {
        return new File(TheGaffer.getPluginDataFolder(),
                TheGaffer.getFileSeperator() + "jobs" + TheGaffer.getFileSeperator()
                + name + TheGaffer.getFileExtension());
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
    public String getTSchannel() {
        return this.ts;
    }
    
    @JsonIgnore
    public void clearTS() {
        admitedWorkers.removeAll(admitedWorkers);
        admitedWorkers.clear();
        admitedWorkers.add(owner);
    }
    
    @JsonIgnore
    public void addAdmitedWorker(String worker){
        admitedWorkers.add(worker);
    }
//    @JsonIgnore
//    public JobWarp getTSwarp() {
//        return this.tsWarp;
//    }

    @JsonIgnore
    public Player[] getWorkersAsPlayersArray() {
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
    public Player[] getAllAsPlayersArray() {
        ArrayList<Player> players = new ArrayList<>();
        for (String pName : workers) {
            OfflinePlayer p = TheGaffer.getServerInstance().getOfflinePlayer(pName);
            if (p.isOnline()) {
                players.add(p.getPlayer());
            }
        }
        for (String pName : helpers) {
            OfflinePlayer p = TheGaffer.getServerInstance().getOfflinePlayer(pName);
            if (p.isOnline()) {
                players.add(p.getPlayer());
            }
        }
        if (TheGaffer.getServerInstance().getOfflinePlayer(owner).isOnline()) {
            players.add(TheGaffer.getServerInstance().getOfflinePlayer(owner).getPlayer());
        }
        Set<Player> dedupedPlayers = new LinkedHashSet<>(players);
        players.clear();
        players.addAll(dedupedPlayers);
        return players.toArray(new Player[players.size()]);
    }

    @JsonIgnore
    public ArrayList<Player> getWorkersAsPlayersList() {
        ArrayList<Player> players = new ArrayList<>();
        for (String pName : workers) {
            OfflinePlayer p = TheGaffer.getServerInstance().getOfflinePlayer(pName);
            if (p.isOnline()) {
                players.add(p.getPlayer());
            }
        }
        Set<Player> dedupedPlayers = new LinkedHashSet<>(players);
        players.clear();
        players.addAll(dedupedPlayers);
        return players;
    }

    @JsonIgnore
    public String getInfo() {
        StringBuilder out = new StringBuilder();
        String inviteOnly = (Private) ? ChatColor.RED + "Private" : ChatColor.GREEN + "Public";
        out.append(ChatColor.AQUA).append(getName()).append(ChatColor.GRAY).append(" (").append(inviteOnly).append(ChatColor.GRAY).append(")").append("\n");
        out.append("Started by: ").append(ChatColor.AQUA).append(getOwner()).append("\n").append(ChatColor.GRAY);
        out.append("Started on: ").append(ChatColor.AQUA).append(new Date(startTime).toGMTString()).append("\n").append(ChatColor.GRAY);
        if (!running) {
            out.append("Stopped on: ").append(ChatColor.AQUA).append(new Date(endTime).toGMTString()).append("\n").append(ChatColor.GRAY);
        }
        out.append("Location: ").append(ChatColor.AQUA).append(getWorld()).append(" (x: ").append((int) getWarp().getX()).append(", y: ").append((int) getWarp().getY()).append(", z: ").append((int) getWarp().getZ()).append(")").append("\n").append(ChatColor.GRAY);
        String status = (running) ? ChatColor.GREEN + "OPEN" : ChatColor.RED + "CLOSED";
        out.append("Status: ").append(status);
        return out.toString();
    }

    public void pauseJob(String pauser) {
        this.paused = true;
        sendToAll(ChatColor.BLUE + "" + ChatColor.BOLD + pauser + " has paused the job.");
        for (Player p : getAllAsPlayersArray()) {
            p.playSound(p.getLocation(), Sound.BLOCK_ANVIL_LAND, 1, 0.2f);
        }
    }

    public void unpauseJob(String pauser) {
        this.paused = false;
        sendToAll(ChatColor.BLUE + "" + ChatColor.BOLD + pauser + " has unpaused the job.");
        for (Player p : getAllAsPlayersArray()) {
            p.playSound(p.getLocation(), Sound.BLOCK_ANVIL_LAND, 1, 2f);
        }
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
        addHelperTeam(p.getName());
        VentureChatUtil.joinJobChannel(p.getUniqueId());
        setDirty(true);
        JobDatabase.saveJobs();
        sendToHelpers(ChatColor.AQUA + p.getName() + " has been added as a helper to the job.");
        return HelperResponse.ADD_SUCCESS;
    }

    public HelperResponse removeHelper(OfflinePlayer p, String reason) {
        if (!helpers.contains(p.getName())) {
            return HelperResponse.NOT_HELPER;
        }
        helpers.remove(p.getName());
        removeHelperTeam(p.getName());
        VentureChatUtil.leaveJobChannel(p);
        setDirty(true);
        JobDatabase.saveJobs();
        Util.debug(p.getName() + " was helper kicked from " + name + " with reason: " + reason);
        return HelperResponse.REMOVE_SUCCESS;
    }

    public WorkerResponse addWorker(OfflinePlayer p) {
        if (workers.contains(p.getName())) {
            return WorkerResponse.ALREADY_WORKER;
        }
        if (bannedWorkers.contains(p.getName())) {
            return WorkerResponse.WORKER_BANNED;
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
        addWorkerTeam(p.getName());
        VentureChatUtil.joinJobChannel(p.getUniqueId());
        if (p.isOnline()) {
            p.getPlayer().teleport(warp.toBukkitLocation());
            if (kit != null) {
                kit.replaceInventory(p.getPlayer());
            }
        }
        setDirty(true);
        JobDatabase.saveJobs();
        sendToAll(ChatColor.AQUA + p.getName() + " has joined the job.");
        return WorkerResponse.ADD_SUCCESS;
    }

    public WorkerResponse removeWorker(OfflinePlayer p, String reason) {
        if (!workers.contains(p.getName())) {
            return WorkerResponse.NOT_WORKER;
        }
        if (p.isOnline()) {
            p.getPlayer().getInventory().clear();
        }
        workers.remove(p.getName());
        removeWorkerTeam(p.getName());
        VentureChatUtil.leaveJobChannel(p);
        setDirty(true);
        JobDatabase.saveJobs();
        sendToAll(ChatColor.AQUA + p.getName() + " has been removed from the job.");
        Util.debug(p.getName() + " was worker removed from " + name + " with reason: " + reason);
        return WorkerResponse.REMOVE_SUCCESS;
    }

    public InviteResponse inviteWorker(List<OfflinePlayer> ps) {
        for(OfflinePlayer p : ps){
            if (invitedWorkers.contains(p.getName())) {
                return InviteResponse.ALREADY_INVITED;
            }
            if (bannedWorkers.contains(p.getName())) {
                return InviteResponse.WORKER_BANNED;
            }
            if (!p.isOnline()) {
                return InviteResponse.NOT_ONLINE;
            }
            if (!p.getPlayer().hasPermission(PermissionsUtil.getJoinPermission())) {
                return InviteResponse.NO_PERMISSIONS;
            }
            invitedWorkers.add(p.getName());
        }
        setDirty(true);
        JobDatabase.saveJobs();
        return InviteResponse.ADD_SUCCESS;
    }

    public InviteResponse uninviteWorker(List<OfflinePlayer> ps){
        for(OfflinePlayer p : ps){
            if (!invitedWorkers.contains(p.getName())) {
                return InviteResponse.NOT_INVITED;
            }
            if (workers.contains(p.getName())) {
                workers.remove(p.getName());
                workerTeam.removeEntry(p.getName());
                VentureChatUtil.leaveJobChannel(p);
            }
            invitedWorkers.remove(p.getName());
        }
        setDirty(true);
        JobDatabase.saveJobs();
        return InviteResponse.REMOVE_SUCCESS;
    }

    public BanWorkerResponse banWorker(List<OfflinePlayer> ps) {
        for(OfflinePlayer p : ps){
            if (workers.contains(p.getName())) {
                workers.remove(p.getName());
                removeWorkerTeam(p.getName());
                VentureChatUtil.leaveJobChannel(p);
            }
            if (bannedWorkers.contains(p.getName())) {
                return BanWorkerResponse.ALREADY_BANNED;
            }
            bannedWorkers.add(p.getName());
        }
        setDirty(true);
        JobDatabase.saveJobs();
        return BanWorkerResponse.BAN_SUCCESS;
    }

    public BanWorkerResponse unbanWorker(List<OfflinePlayer> ps) {
        for(OfflinePlayer p : ps){
            if (bannedWorkers.contains(p.getName())) {
                return BanWorkerResponse.ALREADY_UNBANNED;
            }
            bannedWorkers.remove(p.getName());
        }
        setDirty(true);
        JobDatabase.saveJobs();
        return BanWorkerResponse.UNBAN_SUCCESS;
    }

    public KickWorkerResponse kickWorker(List<OfflinePlayer> ps, String reason) {
        for(OfflinePlayer p : ps){
            if (!workers.contains(p.getName())) {
                return KickWorkerResponse.NOT_IN_JOB;
            }
            workers.remove(p.getName());
            removeWorkerTeam(p.getName());
            VentureChatUtil.leaveJobChannel(p);
            Util.debug(p.getName() + " was worker kicked from " + name + " with reason: " + reason);
        }
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
        generateBounds();
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
Logger.getGlobal().info("left: "+event.getPlayer().getName());
        if (event.getPlayer().getName().equals(owner)) {
Logger.getGlobal().info("owner");
            TheGaffer.scheduleOwnerTimeout(this);
        }
        if (workers.contains(event.getPlayer().getName())) {
Logger.getGlobal().info("worker");
            left.put(event.getPlayer().getUniqueId(), System.currentTimeMillis());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
Logger.getGlobal().info("join: "+event.getPlayer().getName());
        if (left.containsKey(event.getPlayer().getUniqueId())) {
Logger.getGlobal().info("worker");
            left.remove(event.getPlayer().getUniqueId());
            if(TheGaffer.isGlowing()) {
                event.getPlayer().setGlowing(true);
                event.getPlayer().setScoreboard(scoreboard);
            }
        } else if(event.getPlayer().getName().equals(owner) 
                || helpers.contains(event.getPlayer().getName())) {
Logger.getGlobal().info("owner/helper");
            if(TheGaffer.isGlowing()) {
                event.getPlayer().setGlowing(true);
                event.getPlayer().setScoreboard(scoreboard);
            }
        }
    }

    public void jobChat(Player p, String[] chat) {
        String prefix;
        String message;
        if (p.getName().equals(owner) || helpers.contains(p.getName())) {
            prefix = "[" + ChatColor.DARK_RED + "J" + ChatColor.RESET + "] ";
            message = ChatColor.AQUA + chat[1] + ChatColor.RESET;
        } else {
            prefix = "[" + ChatColor.YELLOW + "J" + ChatColor.RESET + "] ";
            message = ChatColor.WHITE + chat[1] + ChatColor.RESET;
        }
        for (Player player : getAllAsPlayersArray()) {
            player.sendMessage(chat[0] + prefix + message);
        }
    }
    
    public void setRunning(boolean running) {
        this.running = running;
        if(TheGaffer.isGlowing() && !running) {
            helpers.forEach(helper -> removeHelperTeam(helper));
            workers.forEach(worker -> removeWorkerTeam(worker));
            removeHelperTeam(owner);
            helperTeam.unregister();
            workerTeam.unregister();
        }
    }
    
    private void addHelperTeam(String playerName) {
        if(TheGaffer.isGlowing()) {
            helperTeam.addEntry(playerName);
            setGlow(playerName,true);
        }
    }
    private void addWorkerTeam(String playerName) {
        if(TheGaffer.isGlowing()) {
            workerTeam.addEntry(playerName);
            setGlow(playerName,true);
        }
    }
    private void removeHelperTeam(String playerName) {
        if(TheGaffer.isGlowing()) {
            helperTeam.removeEntry(playerName);
            setGlow(playerName,false);
        }
    }
    private void removeWorkerTeam(String playerName) {
        if(TheGaffer.isGlowing()) {
            workerTeam.removeEntry(playerName);
            setGlow(playerName,false);
        }
    }
    
    private void setGlow(String playerName, boolean flag) {
        Player player = Bukkit.getPlayer(playerName);
        if(player!=null && TheGaffer.isGlowing()) {
            player.setGlowing(flag);
            if(flag) {
                player.setScoreboard(scoreboard);
            } else {
                player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
            }
            /*if(flag) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING,10000,1));
            } else {
                player.removePotionEffect(PotionEffectType.GLOWING);
            }*/
        }
    }
}
