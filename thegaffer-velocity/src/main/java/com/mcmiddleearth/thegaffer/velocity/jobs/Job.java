package com.mcmiddleearth.thegaffer.velocity.jobs;

public record Job(String name, String creator, String server) {
    @Override
    public boolean equals(Object o) {
        return o instanceof Job job && name.equals(job.name) && server.equals(job.server);
    }

    public boolean equals(String name, String server) {
        return this.name.equals(name) && this.server.equals(server);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}
