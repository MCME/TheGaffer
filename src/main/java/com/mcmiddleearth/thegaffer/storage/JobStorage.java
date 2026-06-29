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

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Maps a {@link Job} to/from a Bukkit {@link YamlConfiguration}. Only the
 * persistent state is written; derived/runtime fields (area, bounds, the glow
 * scoreboard/teams, dirty flag, the abandoner "left" map) are regenerated or
 * reset at load time. ItemStacks serialize natively via YamlConfiguration.
 */
public class JobStorage {

    public static YamlConfiguration toYaml(Job job) {
        YamlConfiguration c = new YamlConfiguration();
        c.set("name", job.getName());
        c.set("owner", job.getOwner() == null ? null : job.getOwner().toString());
        c.set("running", job.isRunning());
        c.set("paused", job.isPaused());
        c.set("private", job.isPrivate());
        c.set("world", job.getWorld());
        c.set("radius", job.getJobRadius());
        c.set("startTime", job.getStartTime());
        c.set("endTime", job.getEndTime());
        c.set("description", job.getDescription());
        c.set("discordSend", job.isDiscordSend());
        if (job.getDiscordTags() != null) {
            c.set("discordTags", new ArrayList<>(Arrays.asList(job.getDiscordTags())));
        }
        c.set("project", job.getProjectname());
        c.set("helpers", toStrings(job.getHelpers()));
        c.set("workers", toStrings(job.getWorkers()));
        c.set("bannedWorkers", toStrings(job.getBannedWorkers()));
        c.set("invitedWorkers", toStrings(job.getInvitedWorkers()));

        JobWarp w = job.getWarp();
        if (w != null) {
            c.set("warp.x", w.getX());
            c.set("warp.y", w.getY());
            c.set("warp.z", w.getZ());
            c.set("warp.yaw", w.getYaw());
            c.set("warp.pitch", w.getPitch());
            c.set("warp.world", w.getWorld());
        }

        JobKit kit = job.getKit();
        if (kit != null) {
            c.set("kit.contents", kit.getContents());
            c.set("kit.helmet", kit.getHelmet());
            c.set("kit.chestplate", kit.getChestplate());
            c.set("kit.pants", kit.getPants());
            c.set("kit.boots", kit.getBoots());
        }
        return c;
    }

    public static Job fromYaml(YamlConfiguration c) {
        Job job = new Job();
        job.setName(c.getString("name"));
        String ownerId = c.getString("owner");
        if (ownerId != null) {
            try {
                job.setOwner(UUID.fromString(ownerId));
            } catch (IllegalArgumentException ignored) {
            }
        }
        job.setRunning(c.getBoolean("running"));
        job.setPaused(c.getBoolean("paused"));
        job.setPrivate(c.getBoolean("private"));
        job.setWorld(c.getString("world"));
        job.setJobRadius(c.getInt("radius"));
        job.setStartTime(c.getLong("startTime"));
        job.setEndTime(c.getLong("endTime"));
        job.setDescription(c.getString("description"));
        job.setDiscordSend(c.getBoolean("discordSend"));
        if (c.contains("discordTags")) {
            job.setDiscordTags(c.getStringList("discordTags").toArray(new String[0]));
        }
        job.setProjectname(c.getString("project"));
        job.setHelpers(toUuids(c.getStringList("helpers")));
        job.setWorkers(toUuids(c.getStringList("workers")));
        job.setBannedWorkers(toUuids(c.getStringList("bannedWorkers")));
        job.setInvitedWorkers(toUuids(c.getStringList("invitedWorkers")));

        if (c.contains("warp")) {
            JobWarp w = new JobWarp();
            w.setX(c.getDouble("warp.x"));
            w.setY(c.getDouble("warp.y"));
            w.setZ(c.getDouble("warp.z"));
            w.setYaw((float) c.getDouble("warp.yaw"));
            w.setPitch((float) c.getDouble("warp.pitch"));
            w.setWorld(c.getString("warp.world"));
            job.setWarp(w);
        }

        if (c.contains("kit")) {
            JobKit kit = new JobKit();
            List<ItemStack> contents = new ArrayList<>();
            for (Object o : c.getList("kit.contents", new ArrayList<>())) {
                if (o instanceof ItemStack) {
                    contents.add((ItemStack) o);
                }
            }
            kit.setContents(contents);
            kit.setHelmet(c.getItemStack("kit.helmet"));
            kit.setChestplate(c.getItemStack("kit.chestplate"));
            kit.setPants(c.getItemStack("kit.pants"));
            kit.setBoots(c.getItemStack("kit.boots"));
            job.setKit(kit);
        }
        return job;
    }

    private static List<String> toStrings(List<UUID> ids) {
        List<String> out = new ArrayList<>();
        for (UUID id : ids) {
            out.add(id.toString());
        }
        return out;
    }

    private static ArrayList<UUID> toUuids(List<String> strings) {
        ArrayList<UUID> out = new ArrayList<>();
        for (String s : strings) {
            try {
                out.add(UUID.fromString(s));
            } catch (IllegalArgumentException ignored) {
            }
        }
        return out;
    }
}
