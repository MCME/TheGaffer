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
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

public class JobProtectionHangingBreakEvent extends JobProtectionEvent {

    @Getter
    private final Player player;
    @Getter
    private final Location location;
    @Getter
    private final ProtectionType protectionType = ProtectionType.HANGING_BREAK;
    @Getter
    private final Entity entityBroken;
    private static final HandlerList handlers = new HandlerList();
    @Getter
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
}
