/*
 * This file is part of TheGaffer.
 * 
 * TheGaffer is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * TheGaffer is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with TheGaffer.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * 
 */
package com.mcmiddleearth.thegaffer.commands.AdminCommands;

import com.mcmiddleearth.thegaffer.GafferResponses;
import com.mcmiddleearth.thegaffer.TheGaffer;
import com.mcmiddleearth.thegaffer.storage.Job;
import com.mcmiddleearth.thegaffer.storage.JobKit;
import com.mcmiddleearth.thegaffer.storage.JobWarp;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.PlayerInventory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 *
 * @author Donovan
 */
public class AdminMethods {

    private Job job;

    private Player p;

    private HashMap<String, Integer> Methods = new HashMap<>();

    public AdminMethods(Job job, Player p) {
        this.job = job;
        this.p = p;
        Methods.put("addhelper", 1);
        Methods.put("removehelper", 1);
        Methods.put("kickworker", 1);
        Methods.put("banworker", 1);
        Methods.put("unbanworker", 1);
        Methods.put("setwarp", 0);
        Methods.put("bringall", 0);
        Methods.put("listworkers", 0);
        Methods.put("inviteworker", 1);
        Methods.put("uninviteworker", 1);
        Methods.put("setradius", 1);
        Methods.put("setkit", 0);
        Methods.put("clearworkerinven", 0);
        Methods.put("setTeamSpeakwarp", 0);
        Methods.put("setTeamSpeakchannel", 1);
    }

    public GafferResponses.GafferResponse addhelper(String arg) {
        GafferResponses.GafferResponse response = job.addHelper(TheGaffer.getServerInstance().getOfflinePlayer(arg));
        return response;
    }

    public GafferResponses.GafferResponse removehelper(String arg) {
        GafferResponses.GafferResponse response = job.removeHelper(TheGaffer.getServerInstance().getOfflinePlayer(arg), "adminremoved: " + (p.getName()));
        return response;
    }

    public GafferResponses.GafferResponse kickworker(String arg) {
        List<OfflinePlayer> ls = new ArrayList<>();
        for (String pname : arg.split(",")) {
            ls.add(Bukkit.getOfflinePlayer(pname));
        }
        return job.kickWorker(ls, "adminkicked: " + (p).getName());
    }

    public GafferResponses.GafferResponse banworker(String arg) {
        List<OfflinePlayer> ls = new ArrayList<>();
        for (String pname : arg.split(",")) {
            ls.add(Bukkit.getOfflinePlayer(pname));
        }
        return job.banWorker(ls);
    }

    public GafferResponses.GafferResponse unbanworker(String arg) {
        List<OfflinePlayer> ls = new ArrayList<>();
        for (String pname : arg.split(",")) {
            ls.add(Bukkit.getOfflinePlayer(pname));
        }
        return job.unbanWorker(ls);
    }

    public Object setwarp() {
        job.updateLocation((p).getLocation());
        return true;
    }

    public Object bringall() {
        job.bringAllWorkers((p.getLocation()));
        return true;
    }

    public String listworkers() {
        return String.join("\n",job.getWorkers().toArray(new String[job.getWorkers().size()]));
    }

    public GafferResponses.GafferResponse inviteworker(String arg) {
        List<OfflinePlayer> ls = new ArrayList<>();
        for (String pname : arg.split(",")) {
            ls.add(Bukkit.getOfflinePlayer(pname));
            if (Bukkit.getOfflinePlayer(pname).isOnline()) {
                Bukkit.getOfflinePlayer(pname).getPlayer().sendMessage(ChatColor.AQUA + job.getOwner() + ChatColor.GRAY + " has invited you to " + ChatColor.GREEN + job.getName());
            }
        }
        return job.inviteWorker(ls);
    }

    public GafferResponses.GafferResponse uninviteworker(String arg) {
        List<OfflinePlayer> ls = new ArrayList<>();
        for (String pname : arg.split(",")) {
            ls.add(Bukkit.getOfflinePlayer(pname));
        }
        return job.uninviteWorker(ls);
    }

    public Object setradius(String arg) {
        job.updateJobRadius(Integer.parseInt(arg));
        return true;
    }

    public Object setkit() {
        JobKit kit = new JobKit(p.getInventory());
        job.setKit(kit);
        for (String pname : job.getWorkers()) {
            if (TheGaffer.getServerInstance().getOfflinePlayer(pname).isOnline()) {
                job.getKit().replaceInventory(TheGaffer.getServerInstance().getOfflinePlayer(pname).getPlayer());
            }
        }
        return true;
    }

    public Object clearworkerinvens() {
        PlayerInventory pinven;
        for (Player curr : job.getWorkersAsPlayersArray()) {
            if (TheGaffer.getServerInstance().getOfflinePlayer(curr.getPlayerListName()).isOnline()) {
                pinven = curr.getInventory();
                pinven.clear();
            }
        }
        return true;
    }

    public Object setteamspeakwarp() {
        JobWarp tswarp = new JobWarp((p.getLocation()));
        job.setTsWarp(tswarp);
        job.setDirty(true);
        //JobDatabase.saveJobs();
        return true;
    }

    public Object setteamspeakchannel(String arg) {
        if (TheGaffer.isTSenabled()) {
            job.setTs(String.valueOf(arg));
        }
        return true;
    }
}
