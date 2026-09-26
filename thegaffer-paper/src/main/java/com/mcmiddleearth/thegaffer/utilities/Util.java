package com.mcmiddleearth.thegaffer.utilities;

import com.mcmiddleearth.thegaffer.TheGaffer;
import org.bukkit.Bukkit;

import java.util.UUID;
import java.util.logging.Logger;

public class Util {

    private static final Logger log = TheGaffer.getServerInstance().getLogger();

    public static void info(String msg) {
        log.info("[TheGaffer] " + msg);
    }

    public static void warning(String msg) {
        log.warning("[TheGaffer] " + msg);
    }

    public static void severe(String msg) {
        log.severe("[TheGaffer] " + msg);
    }

    public static void debug(String msg) {
        if (TheGaffer.isDebug()) {
            log.info("[TheGaffer] DEBUG: " + msg);
        }
    }

    /**
     * Resolves a player UUID to their last-known name (cached, non-blocking),
     * falling back to the UUID string if the name isn't known to the server.
     */
    public static String nameOf(UUID id) {
        if (id == null) {
            return "?";
        }
        String name = Bukkit.getOfflinePlayer(id).getName();
        return name != null ? name : id.toString();
    }
    public static String dino = "§f███████████§8████████§f█\n§f██████████§8██████████\n§f██████████§8██§f█§8███████\n§f██████████§8██████████\n§f██████████§8██████████\n§f██████████§8██████████\n§f██████████§8█████§f█████\n§f██████████§8████████§f██\n§8█§f████████§8█████§f██████\n§8█§f██████§8███████§f██████\n§8██§f████§8██████████§f████\n§8███§f███§8████████§f█§8█§f████\n§8██████████████§f██████\n§8██████████████§f██████\n§f█§8████████████§f███████\n§f██§8███████████§f███████\n§f███§8█████████§f████████\n§f████§8███████§f█████████\n§f█████§8███§f█§8██§f█████████\n§f█████§8██§f███§8█§f█████████\n§f█████§8█§f████§8█§f█████████\n§f█████§8██§f███§8██§f████████\n";

    public static Logger getLog() {
        return log;
    }
}
