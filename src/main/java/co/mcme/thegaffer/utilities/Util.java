package co.mcme.thegaffer.utilities;

import co.mcme.thegaffer.TheGaffer;
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
            Util.info("[TheGaffer] DEBUG: " + msg);
        }
    }
    public static String dino = "§f███████████§8████████§f█\n§f██████████§8██████████\n§f██████████§8██§f█§8███████\n§f██████████§8██████████\n§f██████████§8██████████\n§f██████████§8██████████\n§f██████████§8█████§f█████\n§f██████████§8████████§f██\n§8█§f████████§8█████§f██████\n§8█§f██████§8███████§f██████\n§8██§f████§8██████████§f████\n§8███§f███§8████████§f█§8█§f████\n§8██████████████§f██████\n§8██████████████§f██████\n§f█§8████████████§f███████\n§f██§8███████████§f███████\n§f███§8█████████§f████████\n§f████§8███████§f█████████\n§f█████§8███§f█§8██§f█████████\n§f█████§8██§f███§8█§f█████████\n§f█████§8█§f████§8█§f█████████\n§f█████§8██§f███§8██§f████████\n";
}
