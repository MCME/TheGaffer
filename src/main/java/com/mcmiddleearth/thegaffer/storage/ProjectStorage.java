package com.mcmiddleearth.thegaffer.storage;

import org.bukkit.configuration.file.YamlConfiguration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Maps a {@link Project} to/from a Bukkit {@link YamlConfiguration}. */
public class ProjectStorage {

    public static YamlConfiguration toYaml(Project p) {
        YamlConfiguration c = new YamlConfiguration();
        c.set("name", p.getName());
        c.set("description", p.getDescription());
        c.set("goal", p.getGoal());
        c.set("lead", p.getLead() == null ? null : p.getLead().toString());
        c.set("managers", toStrings(p.getManagers()));
        c.set("status", p.getStatus().name());
        c.set("createdTime", p.getCreatedTime());
        c.set("completedTime", p.getCompletedTime());
        return c;
    }

    public static Project fromYaml(YamlConfiguration c) {
        Project p = new Project();
        p.setName(c.getString("name"));
        p.setDescription(c.getString("description", ""));
        p.setGoal(c.getString("goal", ""));
        String lead = c.getString("lead");
        if (lead != null) {
            try { p.setLead(UUID.fromString(lead)); } catch (IllegalArgumentException ignored) { }
        }
        p.setManagers(toUuids(c.getStringList("managers")));
        try {
            p.setStatus(Project.Status.valueOf(c.getString("status", "ACTIVE")));
        } catch (IllegalArgumentException ex) {
            p.setStatus(Project.Status.ACTIVE);
        }
        p.setCreatedTime(c.getLong("createdTime"));
        p.setCompletedTime(c.getLong("completedTime"));
        return p;
    }

    private static List<String> toStrings(List<UUID> ids) {
        List<String> out = new ArrayList<>();
        for (UUID id : ids) { out.add(id.toString()); }
        return out;
    }

    private static List<UUID> toUuids(List<String> strings) {
        List<UUID> out = new ArrayList<>();
        for (String s : strings) {
            try { out.add(UUID.fromString(s)); } catch (IllegalArgumentException ignored) { }
        }
        return out;
    }
}
