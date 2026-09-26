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
package com.mcmiddleearth.thegaffer.events;

import com.mcmiddleearth.thegaffer.utilities.ProtectionType;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

public class JobProtectionHangingBreakEvent extends JobProtectionEvent {

    private final Player player;
    private final Location location;
    private final ProtectionType protectionType = ProtectionType.HANGING_BREAK;
    private final Entity entityBroken;
    private static final HandlerList handlers = new HandlerList();
    private final boolean blocked;

    public JobProtectionHangingBreakEvent(Player player, Location location, Entity entity, boolean blocked) {
        this.player = player;
        this.location = location;
        this.entityBroken = entity;
        this.blocked = blocked;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    public Player getPlayer() {
        return player;
    }

    public Location getLocation() {
        return location;
    }

    public ProtectionType getProtectionType() {
        return protectionType;
    }

    public Entity getEntityBroken() {
        return entityBroken;
    }

    @Override
    public boolean isBlocked() {
        return blocked;
    }
}
