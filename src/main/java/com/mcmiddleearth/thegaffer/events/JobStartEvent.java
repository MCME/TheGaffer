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

import com.mcmiddleearth.thegaffer.storage.Job;
import com.mcmiddleearth.thegaffer.storage.JobWarp;
import lombok.Getter;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.HandlerList;

public class JobStartEvent extends JobEvent {

    private static final HandlerList handlers = new HandlerList();
    @Getter
    private final Job job;
    @Getter
    private final OfflinePlayer jobRunner;
    @Getter
    private final String jobName;
    @Getter
    private final JobWarp jobWarp;
    @Getter
    private final boolean open;
     @Getter
    private final String jobProject;

    public JobStartEvent(Job job) {
        this.job = job;
        this.jobRunner = job.getOwnerAsOfflinePlayer();
        this.jobName = job.getName();
        this.jobWarp = job.getWarp();
        this.open = true;
        this.jobProject = job.getProjectname();
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

}
