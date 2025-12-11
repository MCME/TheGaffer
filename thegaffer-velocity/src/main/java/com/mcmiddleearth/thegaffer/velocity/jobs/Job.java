package com.mcmiddleearth.thegaffer.velocity.jobs;

public record Job(String name, String creator) {
    @Override
    public boolean equals(Object o) {
        return o instanceof Job job && name.equals(job.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}
