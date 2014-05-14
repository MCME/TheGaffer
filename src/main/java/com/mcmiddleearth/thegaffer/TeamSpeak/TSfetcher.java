/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.mcmiddleearth.thegaffer.TeamSpeak;

/**
 *
 * @author Donovan
 */
//import java.nio.file.Path;
//import java.nio.file.Paths;
//import java.nio.file.StandardWatchEventKinds;
//import java.nio.file.WatchEvent;
//import java.nio.file.WatchKey;
//import java.nio.file.WatchService;
import com.mcmiddleearth.thegaffer.storage.Job;
import com.mcmiddleearth.thegaffer.storage.JobDatabase;
import java.util.ArrayList;
//import java.util.List;
import org.bukkit.scheduler.BukkitRunnable;

public class TSfetcher extends BukkitRunnable {
  
  
  @Override
  public void run() {
    boolean ShouldGo = false;
    for(String JobName : JobDatabase.getActiveJobs().keySet()){
        Job curr = JobDatabase.getActiveJobs().get(JobName);
        if(!JobDatabase.getActiveJobs().isEmpty()){
            ShouldGo = true;
        }
    }
    //!curr.getTSchannel().equalsIgnoreCase("0") || 
    if(ShouldGo){
        TSupdate.TSjobFetch();
    }
  }
}
