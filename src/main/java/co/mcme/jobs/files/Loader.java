package co.mcme.jobs.files;

import co.mcme.jobs.Job;
import co.mcme.jobs.Jobs;
import co.mcme.jobs.util.Util;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import org.bukkit.Bukkit;
import org.bukkit.Location;

public class Loader {

    public static int loadActiveJobs() {
        int count = 0;
        String[] fileJobs = Jobs.getActiveDir().list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith(".job");
            }
        });
        if (fileJobs.length > 0) {
            for (String gob : fileJobs) {
                Util.debug("Getting info for " + gob);
                JsonArray meta = loadJobMeta(Jobs.getActiveDir().getPath() + System.getProperty("file.separator") + gob);
                getJobDat(meta, gob);
                count++;
            }
        }
        return count;
    }

    public static int loadInactiveJobs() {
        int count = 0;
        String[] fileJobs = Jobs.getInactiveDir().list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith(".job");
            }
        });
        if (fileJobs.length > 0) {
            for (String gob : fileJobs) {
                Util.debug("Getting info for " + gob);
                JsonArray meta = loadJobMeta(Jobs.getInactiveDir().getPath() + System.getProperty("file.separator") + gob);
                getJobDat(meta, gob);
                count++;
            }
        }
        return count;
    }

    public static JsonArray loadJobMeta(String file) {
        String json = readFile(file);
        JsonParser parser = new JsonParser();
        JsonArray jArray = parser.parse(json).getAsJsonArray();
        return jArray;
    }

    public static String readFile(String file) {
        StringBuilder stringBuilder = new StringBuilder();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line = null;
            String ls = System.getProperty("line.separator");
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line);
                stringBuilder.append(ls);
            }
        } catch (FileNotFoundException ex) {
        } catch (IOException ex) {
        }
        return stringBuilder.toString();
    }

    public static void getJobDat(JsonArray dat, String gob) {
        JsonObject jobObj = dat.get(0).getAsJsonObject();
        if (jobObj.has("version") && jobObj.get("version").getAsString().equalsIgnoreCase(Jobs.version)) {
            String name = jobObj.get("name").getAsString();
            String runby = jobObj.get("runby").getAsString();
            String world = jobObj.get("world").getAsString();
            boolean running = jobObj.get("status").getAsBoolean();
            boolean inviteonly = jobObj.get("inviteonly").getAsBoolean();
            Long started = jobObj.get("started").getAsLong();
            JsonArray helpersObj = jobObj.get("helpers").getAsJsonArray();
            ArrayList<String> helpers = new ArrayList();
            Iterator it = helpersObj.iterator();
            while (it.hasNext()) {
                helpers.add(((JsonElement) it.next()).getAsString());
            }
            JsonArray partiObj = jobObj.get("workers").getAsJsonArray();
            ArrayList<String> partis = new ArrayList();
            Iterator pit = partiObj.iterator();
            while (pit.hasNext()) {
                partis.add(((JsonElement) pit.next()).getAsString());
            }
            JsonArray bannedObj = jobObj.get("banned").getAsJsonArray();
            ArrayList<String> banned = new ArrayList();
            Iterator bit = bannedObj.iterator();
            while (bit.hasNext()) {
                banned.add(((JsonElement) bit.next()).getAsString());
            }
            JsonArray invitedObj = jobObj.get("invited").getAsJsonArray();
            ArrayList<String> invited = new ArrayList();
            Iterator iit = invitedObj.iterator();
            while (iit.hasNext()) {
                invited.add(((JsonElement) iit.next()).getAsString());
            }
            JsonObject jobLocObj = jobObj.get("location").getAsJsonArray().get(0).getAsJsonObject();
            Location jobLoc = new Location(Bukkit.getWorld(world),
                    jobLocObj.get("x").getAsInt(), jobLocObj.get("y").getAsInt(),
                    jobLocObj.get("z").getAsInt(), jobLocObj.get("yaw").getAsFloat(),
                    jobLocObj.get("pitch").getAsFloat());
            if (running) {
                Jobs.runningJobs.put(name, new Job(name, runby, running, helpers, jobLoc, started, partis, world, banned, gob, inviteonly, invited));
            } else {
                Jobs.notRunningJobs.put(name, new Job(name, runby, running, helpers, jobLoc, started, partis, world, banned, gob, inviteonly, invited));
            }
        } else {
            Util.debug("Job `" + gob + "` does not match plugin version.");
        }
    }
}
