package com.mcmiddleearth.thegaffer;

public enum Permission {
    JOIN("thegaffer.join"),
    CREATE("thegaffer.create"),
    IGNORE_WORLD_PROTECTION("thegaffer.ignoreprotection");

    private final String permissionNode;

    Permission(String permissionNode) {
        this.permissionNode = permissionNode;
    }

    public String getNode() {
        return permissionNode;
    }
}