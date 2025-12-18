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

import com.mcmiddleearth.thegaffer.GafferResponses.*;
import com.mcmiddleearth.thegaffer.TheGaffer;
import com.mcmiddleearth.thegaffer.utilities.PermissionsUtil;
import com.mcmiddleearth.thegaffer.utilities.Util;
import com.mcmiddleearth.thegaffer.utilities.VentureChatUtil;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.util.List;
import java.util.*;
import java.util.logging.Logger;

public class Job implements Listener {

    private String name;
    private String owner;
    private boolean running;
    private boolean paused;
    private JobWarp warp;
    private JobWarp tsWarp;
    private String ts;
    private boolean discordSend;
    private String[] discordTags;
    private String description;
    private ArrayList<String> helpers = new ArrayList();
    private ArrayList<String> workers = new ArrayList();
    private ArrayList<String> bannedWorkers = new ArrayList();
    private ArrayList<String> invitedWorkers = new ArrayList();
    private ArrayList<String> admitedWorkers = new ArrayList();
    private Long startTime;
    private Long endTime;
    private String world;
    private boolean Private;
    private int jobRadius;
    private Polygon area;
    private Rectangle2D bounds;
    private boolean dirty;
    private String projectname;
    private JobKit kit;

    private boolean glowing;

    private Team helperTeam;
    private String helperTeamName;
    private Team workerTeam;
    private String workerTeamName;
    private Scoreboard scoreboard;

    private HashMap<UUID, Long> left = new HashMap<>();

    public Job(String name, String description, String owner, boolean running, JobWarp warp, String world, boolean Private, int jr,
            boolean discordSend, String[] discordTags, String ts, JobWarp tswarp, String project) {
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
        this.projectname = project;
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

    }

    public void setGlowing() {
        scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        helperTeamName = name.substring(0,Math.min(name.length(),14)) + "H";
        helperTeam = scoreboard.registerNewTeam(helperTeamName);
        helperTeam.setColor(ChatColor.valueOf(TheGaffer.getHelperColor()));
        //helperTeam.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.NEVER);
        workerTeamName = name.substring(0, Math.min(name.length(),14)) + "W";
        workerTeam = scoreboard.registerNewTeam(workerTeamName);
        workerTeam.setColor(ChatColor.valueOf(TheGaffer.getWorkerColor()));
        //workerTeam.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.NEVER);

        glowing = true;

        addHelperTeam(owner);
    }

    public Job() {
    }

    public File getFile() {
        return new File(TheGaffer.getPluginDataFolder(),
                TheGaffer.getFileSeperator() + "jobs" + TheGaffer.getFileSeperator()
                + name + TheGaffer.getFileExtension());
    }

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

    public OfflinePlayer getOwnerAsOfflinePlayer() {
        return TheGaffer.getServerInstance().getOfflinePlayer(owner);
    }

    public boolean isPlayerHelper(OfflinePlayer p) {
        return helpers.contains(p.getName());
    }

    public boolean isPlayerWorking(OfflinePlayer p) {
        return workers.contains(p.getName());
    }

    public World getBukkitWorld() {
        return TheGaffer.getServerInstance().getWorld(world);
    }

    public String getTSchannel() {
        return this.ts;
    }

    public void clearTS() {
        admitedWorkers.removeAll(admitedWorkers);
        admitedWorkers.clear();
        admitedWorkers.add(owner);
    }

    public void addAdmitedWorker(String worker) {
        admitedWorkers.add(worker);
    }
//    @JsonIgnore
//    public JobWarp getTSwarp() {
//        return this.tsWarp;
//    }

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
        // JobDatabase.saveJobs();
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
        // JobDatabase.saveJobs();
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
        //  JobDatabase.saveJobs();
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
        // JobDatabase.saveJobs();
        sendToAll(ChatColor.AQUA + p.getName() + " has been removed from the job.");
        Util.debug(p.getName() + " was worker removed from " + name + " with reason: " + reason);
        return WorkerResponse.REMOVE_SUCCESS;
    }

    public InviteResponse inviteWorker(List<OfflinePlayer> ps) {
        for (OfflinePlayer p : ps) {
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
        //  JobDatabase.saveJobs();
        return InviteResponse.ADD_SUCCESS;
    }

    public InviteResponse uninviteWorker(List<OfflinePlayer> ps) {
        for (OfflinePlayer p : ps) {
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
        // JobDatabase.saveJobs();
        return InviteResponse.REMOVE_SUCCESS;
    }

    public BanWorkerResponse banWorker(List<OfflinePlayer> ps) {
        for (OfflinePlayer p : ps) {
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
        // JobDatabase.saveJobs();
        return BanWorkerResponse.BAN_SUCCESS;
    }

    public BanWorkerResponse unbanWorker(List<OfflinePlayer> ps) {
        for (OfflinePlayer p : ps) {
            if (bannedWorkers.contains(p.getName())) {
                return BanWorkerResponse.ALREADY_UNBANNED;
            }
            bannedWorkers.remove(p.getName());
        }
        setDirty(true);
        // JobDatabase.saveJobs();
        return BanWorkerResponse.UNBAN_SUCCESS;
    }

    public KickWorkerResponse kickWorker(List<OfflinePlayer> ps, String reason) {
        for (OfflinePlayer p : ps) {
            if (!workers.contains(p.getName())) {
                return KickWorkerResponse.NOT_IN_JOB;
            }
            workers.remove(p.getName());
            removeWorkerTeam(p.getName());
            VentureChatUtil.leaveJobChannel(p);
            Util.debug(p.getName() + " was worker kicked from " + name + " with reason: " + reason);
        }
        setDirty(true);
        // JobDatabase.saveJobs();
        return KickWorkerResponse.KICK_SUCCESS;
    }

    public WorkerResponse leaveJob(OfflinePlayer p){
        if (!p.getPlayer().hasPermission(PermissionsUtil.getJoinPermission())) {
            return WorkerResponse.NO_PERMISSIONS;
        }
        workers.remove(p.getName());
        removeWorkerTeam(p.getName());
        VentureChatUtil.leaveJobChannel(p);
        setDirty(true);
        sendToAll(ChatColor.AQUA + p.getName() + " has left the job.");
        Util.debug(p.getName() + " was worker removed from " + name + " with reason: Left by themself");
        return WorkerResponse.LEAVE_SUCCESS;
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
        // JobDatabase.saveJobs();
    }

    public void updateJobRadius(int newRadius) {
        setJobRadius(newRadius);
        generateBounds();
        setDirty(true);
        //  JobDatabase.saveJobs();
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
        if (workers.contains(event.getPlayer().getName())) {
            left.put(event.getPlayer().getUniqueId(), System.currentTimeMillis());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        if (left.containsKey(event.getPlayer().getUniqueId())) {
            left.remove(event.getPlayer().getUniqueId());
            if (glowing) {
                event.getPlayer().setGlowing(true);
                event.getPlayer().setScoreboard(scoreboard);
            }
        } else if (event.getPlayer().getName().equals(owner)
                || helpers.contains(event.getPlayer().getName())) {
            if (glowing) {
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
        if (glowing && !running) {
            helpers.forEach(helper -> removeHelperTeam(helper));
            workers.forEach(worker -> removeWorkerTeam(worker));
            removeHelperTeam(owner);
            helperTeam.unregister();
            workerTeam.unregister();
        }
    }

    private void addHelperTeam(String playerName) {
        if (glowing) {
            Logger.getGlobal().info("add helper: " + playerName);
            helperTeam.addEntry(playerName);
            setGlow(playerName, true);
        }
    }

    private void addWorkerTeam(String playerName) {
        if (glowing) {
            Logger.getGlobal().info("add worker: " + playerName);
            workerTeam.addEntry(playerName);
            setGlow(playerName, true);
        }
    }

    private void removeHelperTeam(String playerName) {
        if (glowing) {
            Logger.getGlobal().info("remove helper: " + playerName);
            helperTeam.removeEntry(playerName);
            setGlow(playerName, false);
        }
    }

    private void removeWorkerTeam(String playerName) {
        if (glowing) {
            Logger.getGlobal().info("remove Worker: " + playerName);
            workerTeam.removeEntry(playerName);
            setGlow(playerName, false);
        }
    }

    private void setGlow(String playerName, boolean flag) {
        Player player = Bukkit.getPlayer(playerName);
        if (player != null && glowing) {
            player.setGlowing(flag);
            if (flag) {
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isRunning() {
        return running;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public boolean isPaused() {
        return paused;
    }

    public void setPaused(boolean paused) {
        this.paused = paused;
    }

    public JobWarp getWarp() {
        return warp;
    }

    public void setWarp(JobWarp warp) {
        this.warp = warp;
    }

    public JobWarp getTsWarp() {
        return tsWarp;
    }

    public void setTsWarp(JobWarp tsWarp) {
        this.tsWarp = tsWarp;
    }

    public String getTs() {
        return ts;
    }

    public void setTs(String ts) {
        this.ts = ts;
    }

    public boolean isDiscordSend() {
        return discordSend;
    }

    public void setDiscordSend(boolean discordSend) {
        this.discordSend = discordSend;
    }

    public String[] getDiscordTags() {
        return discordTags;
    }

    public void setDiscordTags(String[] discordTags) {
        this.discordTags = discordTags;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ArrayList<String> getHelpers() {
        return helpers;
    }

    public void setHelpers(ArrayList<String> helpers) {
        this.helpers = helpers;
    }

    public ArrayList<String> getWorkers() {
        return workers;
    }

    public void setWorkers(ArrayList<String> workers) {
        this.workers = workers;
    }

    public ArrayList<String> getBannedWorkers() {
        return bannedWorkers;
    }

    public void setBannedWorkers(ArrayList<String> bannedWorkers) {
        this.bannedWorkers = bannedWorkers;
    }

    public ArrayList<String> getInvitedWorkers() {
        return invitedWorkers;
    }

    public void setInvitedWorkers(ArrayList<String> invitedWorkers) {
        this.invitedWorkers = invitedWorkers;
    }

    public ArrayList<String> getAdmitedWorkers() {
        return admitedWorkers;
    }

    public void setAdmitedWorkers(ArrayList<String> admitedWorkers) {
        this.admitedWorkers = admitedWorkers;
    }

    public Long getStartTime() {
        return startTime;
    }

    public void setStartTime(Long startTime) {
        this.startTime = startTime;
    }

    public Long getEndTime() {
        return endTime;
    }

    public void setEndTime(Long endTime) {
        this.endTime = endTime;
    }

    public String getWorld() {
        return world;
    }

    public void setWorld(String world) {
        this.world = world;
    }

    public boolean isPrivate() {
        return Private;
    }

    public void setPrivate(boolean aPrivate) {
        Private = aPrivate;
    }

    public int getJobRadius() {
        return jobRadius;
    }

    public void setJobRadius(int jobRadius) {
        this.jobRadius = jobRadius;
    }

    public Polygon getArea() {
        return area;
    }

    public void setArea(Polygon area) {
        this.area = area;
    }

    public Rectangle2D getBounds() {
        return bounds;
    }

    public void setBounds(Rectangle2D bounds) {
        this.bounds = bounds;
    }

    public boolean isDirty() {
        return dirty;
    }

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

    public String getProjectname() {
        return projectname;
    }

    public void setProjectname(String projectname) {
        this.projectname = projectname;
    }

    public JobKit getKit() {
        return kit;
    }

    public void setKit(JobKit kit) {
        this.kit = kit;
    }

    public boolean isGlowing() {
        return glowing;
    }

    public void setGlowing(boolean glowing) {
        this.glowing = glowing;
    }

    public Team getHelperTeam() {
        return helperTeam;
    }

    public void setHelperTeam(Team helperTeam) {
        this.helperTeam = helperTeam;
    }

    public String getHelperTeamName() {
        return helperTeamName;
    }

    public void setHelperTeamName(String helperTeamName) {
        this.helperTeamName = helperTeamName;
    }

    public Team getWorkerTeam() {
        return workerTeam;
    }

    public void setWorkerTeam(Team workerTeam) {
        this.workerTeam = workerTeam;
    }

    public String getWorkerTeamName() {
        return workerTeamName;
    }

    public void setWorkerTeamName(String workerTeamName) {
        this.workerTeamName = workerTeamName;
    }

    public Scoreboard getScoreboard() {
        return scoreboard;
    }

    public void setScoreboard(Scoreboard scoreboard) {
        this.scoreboard = scoreboard;
    }

    public HashMap<UUID, Long> getLeft() {
        return left;
    }
}
