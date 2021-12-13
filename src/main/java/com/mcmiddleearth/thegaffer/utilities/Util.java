package com.mcmiddleearth.thegaffer.utilities;

import com.mcmiddleearth.thegaffer.TheGaffer;

import java.util.ArrayList;
import java.util.logging.Logger;

public class Util {

    private static final Logger log = TheGaffer.getServerInstance().getLogger();
    private static ArrayList<String> logs = new ArrayList();

    public static void info(String msg) {
        log.info("[TheGaffer] " + msg);
        logs.add("INFO: " + msg);
    }

    public static void warning(String msg) {
        log.warning("[TheGaffer] " + msg);
        logs.add("WARNING: " + msg);
    }

    public static void severe(String msg) {
        log.severe("[TheGaffer] " + msg);
        logs.add("SEVERE: " + msg);
    }

    public static void debug(String msg) {
        if (TheGaffer.isDebug()) {
            log.info("[TheGaffer] DEBUG: " + msg);
        }
        logs.add("DEBUG: " + msg);
    }
    public static String dino = "§f███████████§8████████§f█\n§f██████████§8██████████\n§f██████████§8██§f█§8███████\n§f██████████§8██████████\n§f██████████§8██████████\n§f██████████§8██████████\n§f██████████§8█████§f█████\n§f██████████§8████████§f██\n§8█§f████████§8█████§f██████\n§8█§f██████§8███████§f██████\n§8██§f████§8██████████§f████\n§8███§f███§8████████§f█§8█§f████\n§8██████████████§f██████\n§8██████████████§f██████\n§f█§8████████████§f███████\n§f██§8███████████§f███████\n§f███§8█████████§f████████\n§f████§8███████§f█████████\n§f█████§8███§f█§8██§f█████████\n§f█████§8██§f███§8█§f█████████\n§f█████§8█§f████§8█§f█████████\n§f█████§8██§f███§8██§f████████\n";

    public static Logger getLog() {
        return log;
    }
}
