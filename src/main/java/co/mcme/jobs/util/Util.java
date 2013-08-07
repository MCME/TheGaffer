package co.mcme.jobs.util;

import java.util.logging.Logger;

public class Util {

    private static final Logger log = Logger.getLogger("Minecraft");
    private static int maxLength = 105;

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
        if (true) {
            Util.info("DEBUG: " + msg);
        }
    }
}
