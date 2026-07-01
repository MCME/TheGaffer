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
package com.mcmiddleearth.thegaffer.utilities;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * Small helpers for building Adventure message components — mainly the clickable
 * command "buttons" used in job notices and listings, which legacy ChatColor
 * could not provide.
 */
public final class Msg {

    private Msg() {
    }

    /**
     * A coloured, clickable label that runs {@code command} when clicked and
     * shows {@code hover} as a tooltip.
     */
    public static Component button(String label, NamedTextColor color, String command, String hover) {
        return Component.text(label, color)
                .clickEvent(ClickEvent.runCommand(command))
                .hoverEvent(HoverEvent.showText(Component.text(hover)));
    }

    /**
     * A coloured, clickable label that pre-fills {@code command} in the chat box
     * (suggest, not run) — useful when the player may want to edit it first.
     */
    public static Component suggest(String label, NamedTextColor color, String command, String hover) {
        return Component.text(label, color)
                .clickEvent(ClickEvent.suggestCommand(command))
                .hoverEvent(HoverEvent.showText(Component.text(hover)));
    }

    // #17 — shared job-chat formatter used by BOTH the sticky listener (JobChatListener)
    // and the one-off command (/jc <message>, JobChatCommand) so they are byte-for-byte identical.
    // Format: aqua "[Job] " + grey "senderName: " + body Component
    /**
     * Formats a job-chat message in the canonical channel style:
     * {@code [Job] } in aqua, {@code senderName: } in grey, then {@code body}.
     * Both the sticky listener and the one-off /jc command call this so they
     * produce an identical result.
     */
    public static Component jobChat(String senderName, Component body) {
        return Component.text("[Job] ", NamedTextColor.AQUA)
                .append(Component.text(senderName + ": ", NamedTextColor.GRAY))
                .append(body);
    }
}
