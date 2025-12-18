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

public class JobKit {

    private JobItem[] contents;
    private JobItem helmet;
    private JobItem chestplate;
    private JobItem pants;
    private JobItem boots;

    public void replaceInventory(Player p) {
        p.getInventory().clear();
        for (JobItem i : contents) {
            p.getInventory().addItem(i.toBukkitItem());
        }
        p.getInventory().setHelmet(helmet.toBukkitItem());
        p.getInventory().setChestplate(chestplate.toBukkitItem());
        p.getInventory().setLeggings(pants.toBukkitItem());
        p.getInventory().setBoots(boots.toBukkitItem());
        p.updateInventory();
    }

    public JobKit() {

    }

    public JobKit(PlayerInventory inv) {
        helmet = new JobItem(inv.getHelmet());
        chestplate = new JobItem(inv.getChestplate());
        pants = new JobItem(inv.getLeggings());
        boots = new JobItem(inv.getBoots());
        List<JobItem> contentS = new ArrayList<>();
        for (ItemStack i : inv.getContents()) {
            contentS.add(new JobItem(i));
        }
        contents = contentS.toArray(new JobItem[contentS.size()]);
    }

    public JobItem[] getContents() {
        return contents;
    }

    public void setContents(JobItem[] contents) {
        this.contents = contents;
    }

    public JobItem getHelmet() {
        return helmet;
    }

    public void setHelmet(JobItem helmet) {
        this.helmet = helmet;
    }

    public JobItem getChestplate() {
        return chestplate;
    }

    public void setChestplate(JobItem chestplate) {
        this.chestplate = chestplate;
    }

    public JobItem getPants() {
        return pants;
    }

    public void setPants(JobItem pants) {
        this.pants = pants;
    }

    public JobItem getBoots() {
        return boots;
    }

    public void setBoots(JobItem boots) {
        this.boots = boots;
    }
}
