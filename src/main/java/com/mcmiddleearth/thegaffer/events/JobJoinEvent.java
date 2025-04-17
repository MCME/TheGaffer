package com.mcmiddleearth.thegaffer.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class JobJoinEvent extends Event {

    private static final HandlerList handlers = new HandlerList();
    private final Player player;

    public JobJoinEvent(Player player) {
        this.player = player;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }

    public Player getPlayer() {
        return player;
    }
}
