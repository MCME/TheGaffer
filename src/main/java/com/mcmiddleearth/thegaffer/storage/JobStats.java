package com.mcmiddleearth.thegaffer.storage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** A finished (or in-progress, when endTime == 0) job's build statistics. */
public class JobStats {

    public static class BuilderStat {
        private int placed;
        private int broke;

        public BuilderStat() { }
        public BuilderStat(int placed, int broke) { this.placed = placed; this.broke = broke; }

        public int getPlaced() { return placed; }
        public int getBroke() { return broke; }
        public void addPlaced(int n) { this.placed += n; }
        public void addBroke(int n) { this.broke += n; }
    }

    private String name;
    private UUID owner;
    private String project;
    private String world;
    private int centerX;
    private int centerZ;
    private int radius;
    private long startTime;
    private long endTime; // 0 while in progress
    private final List<UUID> participants = new ArrayList<>();
    private final Map<UUID, BuilderStat> builders = new HashMap<>();

    public JobStats() { }

    public JobStats(String name, UUID owner, String project, String world,
                    int centerX, int centerZ, int radius, long startTime, long endTime) {
        this.name = name;
        this.owner = owner;
        this.project = project;
        this.world = world;
        this.centerX = centerX;
        this.centerZ = centerZ;
        this.radius = radius;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public void addParticipant(UUID id) {
        if (id != null && !participants.contains(id)) {
            participants.add(id);
        }
    }

    public void recordPlace(UUID id, int n) {
        builders.computeIfAbsent(id, k -> new BuilderStat()).addPlaced(n);
        addParticipant(id);
    }

    public void recordBreak(UUID id, int n) {
        builders.computeIfAbsent(id, k -> new BuilderStat()).addBroke(n);
        addParticipant(id);
    }

    /** Restores stored counts directly (idempotent, unlike the additive recordPlace/recordBreak). */
    public void setBuilderStat(UUID id, int placed, int broke) {
        builders.put(id, new BuilderStat(placed, broke));
        addParticipant(id);
    }

    public long getDurationMillis() {
        long end = endTime > 0 ? endTime : System.currentTimeMillis();
        return Math.max(0, end - startTime);
    }

    public int getTotalPlaced() {
        int t = 0;
        for (BuilderStat b : builders.values()) { t += b.getPlaced(); }
        return t;
    }

    public int getTotalBroke() {
        int t = 0;
        for (BuilderStat b : builders.values()) { t += b.getBroke(); }
        return t;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public UUID getOwner() { return owner; }
    public void setOwner(UUID owner) { this.owner = owner; }
    public String getProject() { return project; }
    public void setProject(String project) { this.project = project; }
    public String getWorld() { return world; }
    public void setWorld(String world) { this.world = world; }
    public int getCenterX() { return centerX; }
    public void setCenterX(int centerX) { this.centerX = centerX; }
    public int getCenterZ() { return centerZ; }
    public void setCenterZ(int centerZ) { this.centerZ = centerZ; }
    public int getRadius() { return radius; }
    public void setRadius(int radius) { this.radius = radius; }
    public long getStartTime() { return startTime; }
    public void setStartTime(long startTime) { this.startTime = startTime; }
    public long getEndTime() { return endTime; }
    public void setEndTime(long endTime) { this.endTime = endTime; }
    public List<UUID> getParticipants() { return participants; }
    public Map<UUID, BuilderStat> getBuilders() { return builders; }
}
