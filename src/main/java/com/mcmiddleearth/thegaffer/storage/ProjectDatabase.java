package com.mcmiddleearth.thegaffer.storage;

import com.mcmiddleearth.thegaffer.TheGaffer;
import com.mcmiddleearth.thegaffer.utilities.Util;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeMap;

/** In-memory registry of {@link Project}s (keyed by canonical name) with YAML persistence. */
public class ProjectDatabase {

    private static final TreeMap<String, Project> projects = new TreeMap<>(); // canonical -> Project

    /** Test seam: when non-null, used instead of the real plugin data folder. */
    static File projectsDirOverride = null;

    private static File projectsDir() {
        if (projectsDirOverride != null) { return projectsDirOverride; }
        return new File(TheGaffer.getPluginDataFolder(), TheGaffer.getFileSeperator() + "projects");
    }

    public static int loadProjects() {
        projects.clear();
        File dir = projectsDir();
        if (!dir.exists()) { dir.mkdirs(); return 0; }
        File[] files = dir.listFiles((d, fname) -> fname.endsWith(TheGaffer.getFileExtension()));
        if (files == null) { return 0; }
        int count = 0;
        for (File pFile : files) {
            if (pFile.isDirectory()) { continue; }
            try {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(pFile);
                Project p = ProjectStorage.fromYaml(config);
                if (p.getName() == null || p.getName().trim().isEmpty()) {
                    Util.severe("Skipping project file without a valid name: " + pFile.getName());
                    continue;
                }
                p.setFile(pFile);
                p.setDirty(false);
                projects.put(Project.canonical(p.getName()), p);
                count++;
            } catch (Exception ex) {
                Util.severe("Failed to load project file " + pFile.getName() + ": " + ex.getMessage());
            }
        }
        return count;
    }

    public static Project get(String name) {
        return projects.get(Project.canonical(name));
    }

    /** Adds a new project; returns false if one with the same canonical name exists. */
    public static boolean create(Project p) {
        String key = Project.canonical(p.getName());
        if (projects.containsKey(key)) { return false; }
        if (p.getFile() == null) {
            p.setFile(new File(projectsDir(), key + TheGaffer.getFileExtension()));
        }
        projects.put(key, p);
        p.setDirty(true);
        saveProject(p);
        return true;
    }

    public static void delete(String name) {
        Project p = projects.remove(Project.canonical(name));
        if (p != null && p.getFile() != null && p.getFile().exists()) {
            p.getFile().delete();
        }
    }

    public static Collection<Project> all() { return projects.values(); }

    public static List<Project> byStatus(Project.Status s) {
        List<Project> out = new ArrayList<>();
        for (Project p : projects.values()) {
            if (p.getStatus() == s) { out.add(p); }
        }
        return out;
    }

    public static boolean hasActiveProjects() {
        return !byStatus(Project.Status.ACTIVE).isEmpty();
    }

    public static void saveProject(Project p) {
        writeProjectFile(p, true);
        p.setDirty(false);
    }

    public static void saveAllDirty(boolean async) {
        for (Project p : projects.values()) {
            if (p.isDirty()) {
                writeProjectFile(p, async);
                p.setDirty(false);
            }
        }
    }

    private static void writeProjectFile(Project p, boolean async) {
        final YamlConfiguration config = ProjectStorage.toYaml(p);
        final File target = p.getFile();
        final String pName = p.getName();
        Runnable write = () -> {
            File dir = target.getParentFile();
            if (dir != null && !dir.exists()) { dir.mkdirs(); }
            File tmp = new File(dir, target.getName() + ".new");
            try {
                config.save(tmp);
                if (target.exists()) { target.delete(); }
                tmp.renameTo(target);
            } catch (IOException ex) {
                Util.severe("Failed to save project " + pName + ": " + ex.getMessage());
            }
        };
        if (async && TheGaffer.getPluginInstance() != null) {
            new BukkitRunnable() { @Override public void run() { write.run(); } }
                    .runTaskAsynchronously(TheGaffer.getPluginInstance());
        } else {
            write.run(); // synchronous fallback (tests, or before the plugin instance is set)
        }
    }

    /** Test/registry accessor (canonical name -> Project). */
    public static TreeMap<String, Project> getProjects() { return projects; }
}
