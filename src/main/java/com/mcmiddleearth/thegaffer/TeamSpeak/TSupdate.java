/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.mcmiddleearth.thegaffer.TeamSpeak;

//import com.mcmiddleearth.thegaffer.TeamSpeak.JTS3ServerQuery;
//import com.mcmiddleearth.thegaffer.TeamSpeak.TeamspeakActionListener;
import com.mcmiddleearth.thegaffer.TheGaffer;
import com.mcmiddleearth.thegaffer.storage.Job;
import com.mcmiddleearth.thegaffer.storage.JobDatabase;
import de.stefan1200.jts3serverquery.JTS3ServerQuery;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import static org.bukkit.Bukkit.getPlayer;
import org.bukkit.entity.Player;


/**
 *
 * @author Donovan
 */
public class TSupdate {
    public static void TSfetch(){
        TSjobFetch();
        TSchannelFetch();
    }
    public static void TSjobFetch(){
        ArrayList<String> InLobby = new ArrayList<String>();
//        ArrayList<String> uncleared = new ArrayList<String>();
        String dbPath = System.getProperty("user.dir") + "/plugins/TheGaffer/LobbyDB";
//        Path dbDir = Paths.get(dbPath);
        if(!JobDatabase.getActiveJobs().isEmpty()){
            for(String JobName : JobDatabase.getActiveJobs().keySet()){
                    Job job = JobDatabase.getActiveJobs().get(JobName);
                    if(!job.getTSchannel().equalsIgnoreCase("0")){
                        try {
                            Scanner s;
                            s = new Scanner(new File(dbPath + "/" + job.getTSchannel().toLowerCase() + ".txt"));
                            while (s.hasNext()){
                                String player = s.nextLine();
                                if(!job.getAdmitedWorkers().contains(player)){
                                    Player worker = getPlayer(player);
                                    worker.teleport(job.getWarp().toBukkitLocation());
                                    job.addAdmitedWorker(player);
                                }
                                InLobby.add(player);
                            }
                            s.close();
                            for(String player : job.getAdmitedWorkers()){
                                if(!InLobby.contains(player) && !job.getOwner().equalsIgnoreCase(player)){
                                    Player worker = getPlayer(player);
                                    if(job.getWorkers().contains(player) && worker.isOnline()){
                                        worker.teleport(job.getTsWarp().toBukkitLocation());
                                        worker.sendMessage("You must be on TeamSpeak!");
                                        job.getAdmitedWorkers().remove(player);
                                    }
                                }
                            }
                            job.setDirty(true);
                            JobDatabase.saveJobs();
                    } catch (FileNotFoundException ex) {
                        Logger.getLogger(TSfetcher.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
             }
        }
    }
    public static void TSchannelFetch(){
        JTS3ServerQuery query = new JTS3ServerQuery();
        query.connectTS3Query("ts.mcmiddleearth.com", 9987);
        
        TheGaffer.getPluginInstance().getLogger().info(String.valueOf(query.connectTS3Query("ts.mcmiddleearth.com", 9987)));
        TheGaffer.getPluginInstance().getLogger().info(String.valueOf(query.loginTS3("test_bot", "beefburgers")));
        TheGaffer.getPluginInstance().getLogger().info(String.valueOf(query.isConnected()));
    }
}
