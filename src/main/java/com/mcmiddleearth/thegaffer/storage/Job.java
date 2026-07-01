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
import com.mcmiddleearth.thegaffer.utilities.Msg;
import com.mcmiddleearth.thegaffer.utilities.PermissionsUtil;
import com.mcmiddleearth.thegaffer.utilities.StatsManager;
import com.mcmiddleearth.thegaffer.utilities.Util;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
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

public class Job implements Listener {

    private String name;
    private UUID owner;
    private boolean running;
    private boolean paused;
    // True only when the pause was applied automatically because the owner went
    // offline with no helper online (see CleanupUtil.selectNewOwner). A manual
    // /job pause clears this so an unrelated rejoin can never auto-resume it.
    private boolean autoPaused;
    private JobWarp warp;
    private boolean discordSend;
    private String description;
    private ArrayList<UUID> helpers = new ArrayList();
    private ArrayList<UUID> workers = new ArrayList();
    private ArrayList<UUID> bannedWorkers = new ArrayList();
    private ArrayList<UUID> invitedWorkers = new ArrayList();
    private Long startTime;
    private Long endTime;
    private String world;
    private boolean Private;
    private int jobRadius;
    private Polygon area;
    private Rectangle2D bounds;
    private boolean dirty;
    private String projectname;

    private UUID creator;

    private boolean glowing;

    private Team helperTeam;
    private String helperTeamName;
    private Team workerTeam;
    private String workerTeamName;
    private Scoreboard scoreboard;

    private HashMap<UUID, Long> left = new HashMap<>();

    public Job(String name, String description, UUID owner, boolean running, JobWarp warp, String world, boolean Private, int jr,
            boolean discordSend, String project) {
        this.name = name;
        this.description = description;
        this.owner = owner;
        this.running = running;
        this.warp = warp;
        this.world = world;
        this.Private = Private;
        this.startTime = System.currentTimeMillis();
        this.discordSend = discordSend;
        this.projectname = project;
        this.creator = owner;
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

        addHelperTeam(Util.nameOf(owner));
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
        return helpers.contains(p.getUniqueId());
    }

    public boolean isPlayerWorking(OfflinePlayer p) {
        return workers.contains(p.getUniqueId());
    }

    public World getBukkitWorld() {
        return TheGaffer.getServerInstance().getWorld(world);
    }


    public Player[] getWorkersAsPlayersArray() {
        ArrayList<Player> players = new ArrayList();
        for (UUID pName : workers) {
            OfflinePlayer p = TheGaffer.getServerInstance().getOfflinePlayer(pName);
            if (p.isOnline()) {
                players.add(p.getPlayer());
            }
        }
        return players.toArray(new Player[players.size()]);
    }

    public Player[] getAllAsPlayersArray() {
        ArrayList<Player> players = new ArrayList<>();
        for (UUID pName : workers) {
            OfflinePlayer p = TheGaffer.getServerInstance().getOfflinePlayer(pName);
            if (p.isOnline()) {
                players.add(p.getPlayer());
            }
        }
        for (UUID pName : helpers) {
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
        for (UUID pName : workers) {
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

    public Component getInfo() {
        Component info = Component.text(getName(), NamedTextColor.AQUA)
                .append(Component.text(" (", NamedTextColor.GRAY))
                .append(Private
                        ? Component.text("Private", NamedTextColor.RED)
                        : Component.text("Public", NamedTextColor.GREEN))
                .append(Component.text(")", NamedTextColor.GRAY))
                .append(Component.newline())
                .append(Component.text("Owner: ", NamedTextColor.GRAY))
                .append(Component.text(Util.nameOf(owner), NamedTextColor.AQUA))
                .append(Component.newline());
        if (!getCreator().equals(owner)) {
            info = info.append(Component.text("Started by: ", NamedTextColor.GRAY))
                    .append(Component.text(Util.nameOf(getCreator()), NamedTextColor.AQUA))
                    .append(Component.newline());
        }
        String helperNames = helpers.isEmpty()
                ? "none"
                : helpers.stream()
                        .map(Util::nameOf)
                        .reduce((a, b) -> a + ", " + b)
                        .orElse("none");
        info = info
                .append(Component.text("Helpers: ", NamedTextColor.GRAY))
                .append(Component.text(helperNames, NamedTextColor.AQUA))
                .append(Component.newline())
                .append(Component.text("Workers: ", NamedTextColor.GRAY))
                .append(Component.text(String.valueOf(getWorkers().size()), NamedTextColor.AQUA))
                .append(Component.newline());
        if (projectname != null && !projectname.equalsIgnoreCase("nothing")) {
            info = info
                    .append(Component.text("Project: ", NamedTextColor.GRAY))
                    .append(Msg.button(projectname, NamedTextColor.GOLD,
                            "/project info " + projectname,
                            "Click to view project info"))
                    .append(Component.newline());
        }
        info = info
                .append(Component.text("Started on: ", NamedTextColor.GRAY))
                .append(Component.text(new Date(startTime).toGMTString(), NamedTextColor.AQUA))
                .append(Component.newline());
        if (!running) {
            info = info.append(Component.text("Stopped on: ", NamedTextColor.GRAY))
                    .append(Component.text(new Date(endTime).toGMTString(), NamedTextColor.AQUA))
                    .append(Component.newline());
        }
        return info
                .append(Component.text("Location: ", NamedTextColor.GRAY))
                .append(Component.text(getWorld() + " (x: " + (int) getWarp().getX()
                        + ", y: " + (int) getWarp().getY()
                        + ", z: " + (int) getWarp().getZ() + ")", NamedTextColor.AQUA))
                .append(Component.newline())
                .append(Component.text("Status: ", NamedTextColor.GRAY))
                .append(running
                        ? Component.text("OPEN", NamedTextColor.GREEN)
                        : Component.text("CLOSED", NamedTextColor.RED))
                .append(paused
                        ? Component.text(" (PAUSED)", NamedTextColor.YELLOW)
                        : Component.empty());
    }

    public void pauseJob(String pauser) {
        this.paused = true;
        sendToAll(Component.text(pauser + " has paused the job.", NamedTextColor.BLUE, TextDecoration.BOLD));
        for (Player p : getAllAsPlayersArray()) {
            p.playSound(p.getLocation(), Sound.BLOCK_ANVIL_LAND, 1, 0.2f);
        }
    }

    public void unpauseJob(String pauser) {
        this.paused = false;
        sendToAll(Component.text(pauser + " has unpaused the job.", NamedTextColor.BLUE, TextDecoration.BOLD));
        for (Player p : getAllAsPlayersArray()) {
            p.playSound(p.getLocation(), Sound.BLOCK_ANVIL_LAND, 1, 2f);
        }
    }

    public HelperResponse addHelper(OfflinePlayer p) {
        if (helpers.contains(p.getUniqueId())) {
            return HelperResponse.ALREADY_HELPER;
        }
        Job current = JobDatabase.getJobWorking(p);
        if (current != null && !current.getName().equals(name)) {
            return HelperResponse.ALREADY_IN_JOB;
        }
        if (!p.isOnline()) {
            return HelperResponse.NOT_ONLINE;
        }
        if (!p.getPlayer().hasPermission(PermissionsUtil.getCreatePermission())) {
            return HelperResponse.NO_PERMISSIONS;
        }
        helpers.add(p.getUniqueId());
        StatsManager.onJoin(name, p.getUniqueId());
        addHelperTeam(p.getName());
        setDirty(true);
        // JobDatabase.saveJobs();
        sendToHelpers(Component.text(p.getName() + " has been added as a helper to the job.", NamedTextColor.AQUA));
        return HelperResponse.ADD_SUCCESS;
    }

    public HelperResponse removeHelper(OfflinePlayer p, String reason) {
        if (!helpers.contains(p.getUniqueId())) {
            return HelperResponse.NOT_HELPER;
        }
        helpers.remove(p.getUniqueId());
        removeHelperTeam(p.getName());
        setDirty(true);
        // JobDatabase.saveJobs();
        Util.debug(p.getName() + " was helper kicked from " + name + " with reason: " + reason);
        return HelperResponse.REMOVE_SUCCESS;
    }

    public WorkerResponse addWorker(OfflinePlayer p) {
        if (workers.contains(p.getUniqueId())) {
            return WorkerResponse.ALREADY_WORKER;
        }
        if (bannedWorkers.contains(p.getUniqueId())) {
            return WorkerResponse.WORKER_BANNED;
        }
        Job current = JobDatabase.getJobWorking(p);
        if (current != null && !current.getName().equals(name)) {
            return WorkerResponse.ALREADY_IN_JOB;
        }
        if (!p.isOnline()) {
            return WorkerResponse.NOT_ONLINE;
        }
        if (!p.getPlayer().hasPermission(PermissionsUtil.getJoinPermission())) {
            return WorkerResponse.NO_PERMISSIONS;
        }
        if (Private && !(invitedWorkers.contains(p.getUniqueId()))) {
            return WorkerResponse.NOT_INVITED;
        }
        workers.add(p.getUniqueId());
        StatsManager.onJoin(name, p.getUniqueId());
        addWorkerTeam(p.getName());
        if (p.isOnline()) {
            p.getPlayer().teleport(warp.toBukkitLocation());
        }
        setDirty(true);
        //  JobDatabase.saveJobs();
        sendToAll(Component.text(p.getName() + " has joined the job.", NamedTextColor.AQUA));
        return WorkerResponse.ADD_SUCCESS;
    }

    public WorkerResponse removeWorker(OfflinePlayer p, String reason) {
        if (!workers.contains(p.getUniqueId())) {
            return WorkerResponse.NOT_WORKER;
        }
        if (p.isOnline()) {
            p.getPlayer().getInventory().clear();
        }
        workers.remove(p.getUniqueId());
        removeWorkerTeam(p.getName());
        setDirty(true);
        // JobDatabase.saveJobs();
        sendToAll(Component.text(p.getName() + " has been removed from the job.", NamedTextColor.AQUA));
        Util.debug(p.getName() + " was worker removed from " + name + " with reason: " + reason);
        return WorkerResponse.REMOVE_SUCCESS;
    }

    public InviteResponse inviteWorker(List<OfflinePlayer> ps) {
        for (OfflinePlayer p : ps) {
            if (invitedWorkers.contains(p.getUniqueId())) {
                return InviteResponse.ALREADY_INVITED;
            }
            if (bannedWorkers.contains(p.getUniqueId())) {
                return InviteResponse.WORKER_BANNED;
            }
            if (!p.isOnline()) {
                return InviteResponse.NOT_ONLINE;
            }
            if (!p.getPlayer().hasPermission(PermissionsUtil.getJoinPermission())) {
                return InviteResponse.NO_PERMISSIONS;
            }
            invitedWorkers.add(p.getUniqueId());
        }
        setDirty(true);
        //  JobDatabase.saveJobs();
        return InviteResponse.ADD_SUCCESS;
    }

    public InviteResponse uninviteWorker(List<OfflinePlayer> ps) {
        for (OfflinePlayer p : ps) {
            if (!invitedWorkers.contains(p.getUniqueId())) {
                return InviteResponse.NOT_INVITED;
            }
            if (workers.contains(p.getUniqueId())) {
                workers.remove(p.getUniqueId());
                workerTeam.removeEntry(p.getName());
            }
            invitedWorkers.remove(p.getUniqueId());
        }
        setDirty(true);
        // JobDatabase.saveJobs();
        return InviteResponse.REMOVE_SUCCESS;
    }

    public BanWorkerResponse banWorker(List<OfflinePlayer> ps) {
        for (OfflinePlayer p : ps) {
            if (workers.contains(p.getUniqueId())) {
                workers.remove(p.getUniqueId());
                removeWorkerTeam(p.getName());
            }
            if (bannedWorkers.contains(p.getUniqueId())) {
                return BanWorkerResponse.ALREADY_BANNED;
            }
            bannedWorkers.add(p.getUniqueId());
            // #7: Tell the target immediately if they are online (mirrors removeWorker's AQUA notice to all).
            if (p.isOnline()) {
                p.getPlayer().sendMessage(Component.text("You were banned from the " + name + " job.", NamedTextColor.RED));
            }
        }
        setDirty(true);
        // JobDatabase.saveJobs();
        return BanWorkerResponse.BAN_SUCCESS;
    }

    public BanWorkerResponse unbanWorker(List<OfflinePlayer> ps) {
        for (OfflinePlayer p : ps) {
            if (!bannedWorkers.contains(p.getUniqueId())) {
                return BanWorkerResponse.ALREADY_UNBANNED;
            }
            bannedWorkers.remove(p.getUniqueId());
        }
        setDirty(true);
        // JobDatabase.saveJobs();
        return BanWorkerResponse.UNBAN_SUCCESS;
    }

    public KickWorkerResponse kickWorker(List<OfflinePlayer> ps, String reason) {
        for (OfflinePlayer p : ps) {
            if (!workers.contains(p.getUniqueId())) {
                return KickWorkerResponse.NOT_IN_JOB;
            }
            workers.remove(p.getUniqueId());
            removeWorkerTeam(p.getName());
            Util.debug(p.getName() + " was worker kicked from " + name + " with reason: " + reason);
            // #7: Tell the target immediately if they are online (mirrors removeWorker's AQUA notice to all).
            if (p.isOnline()) {
                p.getPlayer().sendMessage(Component.text("You were removed from the " + name + " job.", NamedTextColor.RED));
            }
        }
        setDirty(true);
        // JobDatabase.saveJobs();
        return KickWorkerResponse.KICK_SUCCESS;
    }

    public WorkerResponse leaveJob(OfflinePlayer p){
        if (!p.getPlayer().hasPermission(PermissionsUtil.getJoinPermission())) {
            return WorkerResponse.NO_PERMISSIONS;
        }
        workers.remove(p.getUniqueId());
        removeWorkerTeam(p.getName());
        setDirty(true);
        sendToAll(Component.text(p.getName() + " has left the job.", NamedTextColor.AQUA));
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
        for (UUID wName : workers) {
            if (TheGaffer.getServerInstance().getOfflinePlayer(wName).isOnline()) {
                TheGaffer.getServerInstance().getOfflinePlayer(wName).getPlayer().teleport(to);
            }
        }
    }

    public int sendToHelpers(Component message) {
        int count = 0;
        for (UUID hName : helpers) {
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

    public int sendToWorkers(Component message) {
        int count = 0;
        for (UUID wName : workers) {
            if (TheGaffer.getServerInstance().getOfflinePlayer(wName).isOnline()) {
                TheGaffer.getServerInstance().getOfflinePlayer(wName).getPlayer().sendMessage(message);
                count++;
            }
        }
        return count;
    }

    public int sendToAll(Component message) {
        return sendToHelpers(message) + sendToWorkers(message);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onLeave(PlayerQuitEvent event) {
        if (event.getPlayer().getUniqueId().equals(owner)) {
            TheGaffer.scheduleOwnerTimeout(this);
        }
        if (workers.contains(event.getPlayer().getUniqueId())) {
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
        } else if (event.getPlayer().getUniqueId().equals(owner)
                || helpers.contains(event.getPlayer().getUniqueId())) {
            if (glowing) {
                event.getPlayer().setGlowing(true);
                event.getPlayer().setScoreboard(scoreboard);
            }
        }
    }

    public void setRunning(boolean running) {
        this.running = running;
        if (glowing && !running) {
            helpers.forEach(helper -> removeHelperTeam(Util.nameOf(helper)));
            workers.forEach(worker -> removeWorkerTeam(Util.nameOf(worker)));
            removeHelperTeam(Util.nameOf(owner));
            helperTeam.unregister();
            workerTeam.unregister();
        }
    }

    private void addHelperTeam(String playerName) {
        if (glowing) {
            helperTeam.addEntry(playerName);
            setGlow(playerName, true);
        }
    }

    private void addWorkerTeam(String playerName) {
        if (glowing) {
            workerTeam.addEntry(playerName);
            setGlow(playerName, true);
        }
    }

    private void removeHelperTeam(String playerName) {
        if (glowing) {
            helperTeam.removeEntry(playerName);
            setGlow(playerName, false);
        }
    }

    private void removeWorkerTeam(String playerName) {
        if (glowing) {
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

    public UUID getOwner() {
        return owner;
    }

    public void setOwner(UUID owner) {
        this.owner = owner;
    }

    public UUID getCreator() {
        return creator != null ? creator : owner;
    }

    public void setCreator(UUID creator) {
        this.creator = creator;
    }

    public boolean isPaused() {
        return paused;
    }

    public void setPaused(boolean paused) {
        this.paused = paused;
    }

    public boolean isAutoPaused() {
        return autoPaused;
    }

    public void setAutoPaused(boolean autoPaused) {
        this.autoPaused = autoPaused;
    }

    public JobWarp getWarp() {
        return warp;
    }

    public void setWarp(JobWarp warp) {
        this.warp = warp;
    }

    public boolean isDiscordSend() {
        return discordSend;
    }

    public void setDiscordSend(boolean discordSend) {
        this.discordSend = discordSend;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ArrayList<UUID> getHelpers() {
        return helpers;
    }

    public void setHelpers(ArrayList<UUID> helpers) {
        this.helpers = helpers;
    }

    public ArrayList<UUID> getWorkers() {
        return workers;
    }

    public void setWorkers(ArrayList<UUID> workers) {
        this.workers = workers;
    }

    public ArrayList<UUID> getBannedWorkers() {
        return bannedWorkers;
    }

    public void setBannedWorkers(ArrayList<UUID> bannedWorkers) {
        this.bannedWorkers = bannedWorkers;
    }

    public ArrayList<UUID> getInvitedWorkers() {
        return invitedWorkers;
    }

    public void setInvitedWorkers(ArrayList<UUID> invitedWorkers) {
        this.invitedWorkers = invitedWorkers;
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
