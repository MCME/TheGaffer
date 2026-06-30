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

import org.bukkit.ChatColor;

/** Shared colour palette for the job conversations (Conversation API is String-based, so ChatColor). */
public final class PromptStyle {

    private PromptStyle() { }

    /** Prefix tag — emitted ONCE per prompt by the ConversationPrefix; ends with RESET so the question starts clean. */
    public static final String TAG = ChatColor.DARK_GRAY + "[" + ChatColor.GOLD + "Jobs" + ChatColor.DARK_GRAY + "] " + ChatColor.RESET;

    public static final String QUESTION = ChatColor.WHITE.toString();
    public static final String OPTION   = ChatColor.GRAY.toString();
    public static final String HINT     = ChatColor.DARK_GRAY.toString();
    public static final String VALUE    = ChatColor.AQUA.toString();
    public static final String OK       = ChatColor.GREEN.toString();
    public static final String ERROR    = ChatColor.RED.toString();

    /** A white question (the tag is added by the prefix, not here). */
    public static String ask(String question) { return QUESTION + question; }

    /** Grey parenthesised valid inputs, with a leading space, e.g. {@code opts("true / false")}. */
    public static String opts(String o) { return " " + OPTION + "(" + o + ")"; }

    /** A dim, indented hint line (newline-prefixed). */
    public static String hint(String text) { return "\n" + HINT + "   " + text; }
}
