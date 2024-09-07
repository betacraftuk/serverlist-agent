package serverlist;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.util.Base64;
import java.util.logging.Logger;

import org.json.JSONObject;

import serverlist.AccessHelper.ServerType;

public class BCPing {
    public static final String BCPING_VER = "2.1.1";
    public static Logger log;

    PingThread pingThread;
    UpdateThread updateThread;
    public static JSONObject config;
    public static boolean running = true;

    //protected static final String HOST = "http://localhost:2137/api/v2";
    protected static final String HOST = "https://api.betacraft.uk/v2";

    public BCPing() {

        if (AccessHelper.type == ServerType.NMS) {
            log = Logger.getLogger("Minecraft");
        } else if (AccessHelper.type == ServerType.CMMS) {
            log = Logger.getLogger("MinecraftServer");
        } else {
            log = Logger.getGlobal();
        }

        log.info("[BetacraftPing] BetacraftPing v" + BCPING_VER + " enabled.");

        File pingDetailsFile = new File("betacraft/ping_details.json");
        String pingDetails = null;
        try {
            pingDetails = new String(Files.readAllBytes(pingDetailsFile.toPath()), "UTF-8");
        } catch (Throwable t) {
            if (!(t instanceof NoSuchFileException)) {
                t.printStackTrace();
            }

            pingDetailsFile.getParentFile().mkdirs();
        }

        if (pingDetails != null) {
            try {
                config = new JSONObject(pingDetails);
            } catch (Throwable t) {
                log.warning("[BetacraftPing] Failed to read configuration! Disabling...");
                t.printStackTrace();
                running = false;
                return;
            }
            // TODO validation?
        } else {
            // write defaults
            config = new JSONObject();

            String serverip = getIPFromAmazon();
            config.put("socket", serverip + ":25565");
            config.put("name", "A Minecraft server");
            config.put("description", "");
            config.put("category", "alpha");
            config.put("protocol", "alpha_13");
            config.put("game_version", "a1.0.15");
            config.put("v1_version", "a1.0.15");
            config.put("send_players", true);
            config.put("private_key", "");

            try {
                Files.write(pingDetailsFile.toPath(), config.toString(4).getBytes("UTF-8"));
            } catch (Throwable t) {
                log.warning("[BetacraftPing] Failed to write default configuration! Disabling...");
                running = false;
                return;
            }

            log.warning("[BetacraftPing] Failed to load configuration!");
            log.warning("[BetacraftPing] Wrote default configuration --- see plugins/BetacraftPing/ping_details.json");
        }

        pingThread = new PingThread();
        pingThread.start();
        
        updateThread = new UpdateThread();
        updateThread.start();
        
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                if (UpdateThread.update)
                    log.info("[BetacraftPing] Download latest plugin update from " + UpdateThread.newestRelease.getString("html_url"));

                log.info("[BetacraftPing] Disabling...");
                running = false;

                if (pingThread != null)
                    pingThread.interrupt();
                
                if (updateThread != null)
                    updateThread.interrupt();
            }
        });
    }

    public static String getIPFromAmazon() {
        try {
            URL myIP = new URL("http://checkip.amazonaws.com");
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(myIP.openStream()));

            return bufferedReader.readLine();
        } catch (Exception e) {
            log.warning("[BetacraftPing] Failed to get IP from Amazon! Are you offline?");
            e.printStackTrace();
        }
        return null;
    }

    private static String icon = null;
    public static String getIcon() {
        if (icon == null) {
            File iconFile = new File("betacraft/server_icon.png");
            if (!iconFile.exists()) {
                BCPing.log.warning("[BetacraftPing] No server icon found at \"betacraft/server_icon.png\"!");
                return icon;
            }

            if (iconFile.length() > 64000) {
                BCPing.log.severe("[BetacraftPing] Server icon size is too big! (64 kB max, recommended res: 128x128)");
            } else {
                try {
                    byte[] filebytes = Files.readAllBytes(iconFile.toPath());
                    byte[] b64str = Base64.getEncoder().encode(filebytes);

                    icon = new String(b64str, "UTF-8");
                } catch (Throwable t) {
                    BCPing.log.severe("[BetacraftPing] Failed to read server icon:");
                    t.printStackTrace();
                }
            }
            return icon;
        } else {
            return icon;
        }
    }
}
