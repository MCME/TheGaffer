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
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

public class JobProtectionInteractEvent extends JobProtectionEvent {

    private final Player player;
    private final Location location;
    private final ProtectionType protectionType = ProtectionType.INTERACT;
    private final Block clickedBlock;
    private final ItemStack itemClickedWith;
    private final boolean clickingBlock;
    private final boolean clickingWithItem;
    private static final HandlerList handlers = new HandlerList();
    private final boolean blocked;

    public JobProtectionInteractEvent(Player player, Location location, Block block, ItemStack item, boolean blocked) {
        this.player = player;
        this.location = location;
        this.clickedBlock = block;
        this.itemClickedWith = item;
        this.clickingBlock = block != null;
        this.clickingWithItem = item != null;
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

    public Block getClickedBlock() {
        return clickedBlock;
    }

    public ItemStack getItemClickedWith() {
        return itemClickedWith;
    }

    public boolean isClickingBlock() {
        return clickingBlock;
    }

    public boolean isClickingWithItem() {
        return clickingWithItem;
    }

    @Override
    public boolean isBlocked() {
        return blocked;
    }
}
