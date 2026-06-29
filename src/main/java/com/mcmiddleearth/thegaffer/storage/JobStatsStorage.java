package com.mcmiddleearth.thegaffer.storage;

import com.mcmiddleearth.thegaffer.TheGaffer;
import com.mcmiddleearth.thegaffer.utilities.Util;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Serializes JobStats to/from YAML and reads finished records from a folder. */
public class JobStatsStorage {

    public static YamlConfiguration toYaml(JobStats s) {
        YamlConfiguration c = new YamlConfiguration();
        c.set("name", s.getName());
        c.set("owner", s.getOwner() == null ? null : s.getOwner().toString());
        c.set("project", s.getProject());
        c.set("world", s.getWorld());
        c.set("centerX", s.getCenterX());
        c.set("centerZ", s.getCenterZ());
        c.set("radius", s.getRadius());
        c.set("startTime", s.getStartTime());
        c.set("endTime", s.getEndTime());
        List<String> parts = new ArrayList<>();
        for (UUID id : s.getParticipants()) { parts.add(id.toString()); }
        c.set("participants", parts);
        for (Map.Entry<UUID, JobStats.BuilderStat> e : s.getBuilders().entrySet()) {
            c.set("builders." + e.getKey() + ".placed", e.getValue().getPlaced());
            c.set("builders." + e.getKey() + ".broke", e.getValue().getBroke());
        }
        return c;
    }

    public static JobStats fromYaml(YamlConfiguration c) {
        UUID owner = null;
        String ownerId = c.getString("owner");
        if (ownerId != null) {
            try { owner = UUID.fromString(ownerId); } catch (IllegalArgumentException ignored) { }
        }
        JobStats s = new JobStats(c.getString("name"), owner, c.getString("project"), c.getString("world"),
                c.getInt("centerX"), c.getInt("centerZ"), c.getInt("radius"),
                c.getLong("startTime"), c.getLong("endTime"));
        for (String p : c.getStringList("participants")) {
            try { s.addParticipant(UUID.fromString(p)); } catch (IllegalArgumentException ignored) { }
        }
        ConfigurationSection builders = c.getConfigurationSection("builders");
        if (builders != null) {
            for (String key : builders.getKeys(false)) {
                try {
                    UUID id = UUID.fromString(key);
                    s.recordPlace(id, builders.getInt(key + ".placed"));
                    s.recordBreak(id, builders.getInt(key + ".broke"));
                } catch (IllegalArgumentException ignored) { }
            }
        }
        return s;
    }

    /** Builds the YAML on the calling (main) thread; writes async unless async==false. */
    public static void save(JobStats s, File dir, boolean async) {
        final YamlConfiguration config = toYaml(s);
        final File target = new File(dir, s.getName() + "-" + s.getEndTime() + TheGaffer.getFileExtension());
        Runnable write = () -> {
            if (!dir.exists()) { dir.mkdirs(); }
            File tmp = new File(dir, target.getName() + ".new");
            try {
                config.save(tmp);
                if (target.exists()) { target.delete(); }
                tmp.renameTo(target);
            } catch (IOException ex) {
                Util.severe("Failed to save stats for " + s.getName() + ": " + ex.getMessage());
            }
        };
        if (async && TheGaffer.getPluginInstance() != null) {
            new BukkitRunnable() { @Override public void run() { write.run(); } }
                    .runTaskAsynchronously(TheGaffer.getPluginInstance());
        } else {
            write.run(); // synchronous fallback (tests, or before the plugin instance is set)
        }
    }

    public static List<JobStats> readAll(File dir) {
        List<JobStats> out = new ArrayList<>();
        if (!dir.exists()) { return out; }
        File[] files = dir.listFiles((d, n) -> n.endsWith(TheGaffer.getFileExtension()));
        if (files == null) { return out; }
        for (File f : files) {
            if (f.isDirectory()) { continue; }
            try {
                out.add(fromYaml(YamlConfiguration.loadConfiguration(f)));
            } catch (Exception ex) {
                Util.severe("Failed to read stats file " + f.getName() + ": " + ex.getMessage());
            }
        }
        return out;
    }
}
