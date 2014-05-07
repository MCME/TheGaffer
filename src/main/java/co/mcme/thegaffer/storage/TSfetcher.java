/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package co.mcme.thegaffer.storage;

/**
 *
 * @author Donovan
 */
import co.mcme.thegaffer.TheGaffer;
import java.io.File;
import java.io.FileNotFoundException;
//import java.nio.file.Path;
//import java.nio.file.Paths;
//import java.nio.file.StandardWatchEventKinds;
//import java.nio.file.WatchEvent;
//import java.nio.file.WatchKey;
//import java.nio.file.WatchService;
import java.util.ArrayList;
//import java.util.List;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.scheduler.BukkitRunnable;

public class TSfetcher extends BukkitRunnable {
  
  public static ArrayList<String> InLobby = new ArrayList<String>();
  
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
        JobDatabase.TSfetch();
    }
  }
}
