package com.mcmiddleearth.thegaffer.storage;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/** A managed build project: a named collection of jobs whose stats roll up together. */
public class Project {

    public enum Status { ACTIVE, COMPLETED, ARCHIVED }

    private String name;
    private String description = "";
    private String goal = "";
    private UUID lead;
    private List<UUID> managers = new ArrayList<>();
    private Status status = Status.ACTIVE;
    private long createdTime;
    private long completedTime;

    private transient boolean dirty = false;
    private transient File file;

    public Project() { }

    public Project(String name, UUID lead, long createdTime) {
        this.name = name;
        this.lead = lead;
        this.createdTime = createdTime;
    }

    /** Canonical key for case-insensitive identity and lookup. */
    public static String canonical(String name) {
        return name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
    }

    public boolean isLead(UUID id)    { return id != null && id.equals(lead); }
    public boolean isManager(UUID id) { return id != null && managers.contains(id); }
    public boolean canManage(UUID id) { return isLead(id) || isManager(id); }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; this.dirty = true; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; this.dirty = true; }
    public String getGoal() { return goal; }
    public void setGoal(String goal) { this.goal = goal; this.dirty = true; }
    public UUID getLead() { return lead; }
    public void setLead(UUID lead) { this.lead = lead; this.dirty = true; }
    public List<UUID> getManagers() { return managers; }
    public void setManagers(List<UUID> managers) { this.managers = new ArrayList<>(managers); this.dirty = true; }
    public void addManager(UUID id) { if (id != null && !managers.contains(id)) { managers.add(id); dirty = true; } }
    public void removeManager(UUID id) { if (managers.remove(id)) { dirty = true; } }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; this.dirty = true; }
    public long getCreatedTime() { return createdTime; }
    public void setCreatedTime(long t) { this.createdTime = t; } // written once (construction/load); intentionally not dirty-tracked
    public long getCompletedTime() { return completedTime; }
    public void setCompletedTime(long t) { this.completedTime = t; this.dirty = true; }

    public boolean isDirty() { return dirty; }
    public void setDirty(boolean dirty) { this.dirty = dirty; }
    public File getFile() { return file; }
    public void setFile(File file) { this.file = file; }
}
