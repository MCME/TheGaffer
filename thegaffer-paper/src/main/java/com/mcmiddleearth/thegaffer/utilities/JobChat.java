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

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-player "sticky job chat" toggle. The set is read from the asynchronous
 * chat event and written from the main thread (command / quit), so it is a
 * concurrent set. Only the toggle lives here; job membership is resolved on the
 * main thread when a message is actually routed (see JobChatListener).
 */
public class JobChat {

    private static final Set<String> sticky = ConcurrentHashMap.newKeySet();

    /** Flips sticky job chat for the player; returns the new state (true = on). */
    public static boolean toggle(String playerName) {
        if (sticky.add(playerName)) {
            return true;
        }
        sticky.remove(playerName);
        return false;
    }

    public static boolean isSticky(String playerName) {
        return sticky.contains(playerName);
    }

    public static void clear(String playerName) {
        sticky.remove(playerName);
    }
}
