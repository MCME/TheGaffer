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

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.ArrayList;
import java.util.List;

/**
 * A saved snapshot of a player's inventory, handed to workers who join a job.
 * Items are stored as native Bukkit {@link ItemStack}s so they serialize
 * directly via YamlConfiguration (full NBT, cross-version migration handled by
 * the server) — no manual item DTOs required.
 */
public class JobKit {

    private List<ItemStack> contents = new ArrayList<>();
    private ItemStack helmet;
    private ItemStack chestplate;
    private ItemStack pants;
    private ItemStack boots;

    public JobKit() {
    }

    public JobKit(PlayerInventory inv) {
        contents = new ArrayList<>();
        for (ItemStack i : inv.getContents()) {
            if (i != null) {
                contents.add(i);
            }
        }
        helmet = inv.getHelmet();
        chestplate = inv.getChestplate();
        pants = inv.getLeggings();
        boots = inv.getBoots();
    }

    public void replaceInventory(Player p) {
        p.getInventory().clear();
        if (contents != null) {
            for (ItemStack i : contents) {
                if (i != null) {
                    p.getInventory().addItem(i);
                }
            }
        }
        p.getInventory().setHelmet(helmet);
        p.getInventory().setChestplate(chestplate);
        p.getInventory().setLeggings(pants);
        p.getInventory().setBoots(boots);
        p.updateInventory();
    }

    public List<ItemStack> getContents() {
        return contents;
    }

    public void setContents(List<ItemStack> contents) {
        this.contents = contents;
    }

    public ItemStack getHelmet() {
        return helmet;
    }

    public void setHelmet(ItemStack helmet) {
        this.helmet = helmet;
    }

    public ItemStack getChestplate() {
        return chestplate;
    }

    public void setChestplate(ItemStack chestplate) {
        this.chestplate = chestplate;
    }

    public ItemStack getPants() {
        return pants;
    }

    public void setPants(ItemStack pants) {
        this.pants = pants;
    }

    public ItemStack getBoots() {
        return boots;
    }

    public void setBoots(ItemStack boots) {
        this.boots = boots;
    }
}
