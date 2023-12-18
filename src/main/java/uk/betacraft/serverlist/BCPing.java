package uk.betacraft.serverlist;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.util.Base64;
import java.util.logging.Logger;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import legacyfix.LegacyURLStreamHandlerFactory;
import uk.betacraft.serverlist.AccessHelper.ServerType;

public class BCPing {
	public static final String BCPING_VER = "2.0.0";
	public static Logger log;
	public static final JSONParser parser = new JSONParser();
	
	PingThread thread;
	public static JSONObject config;
	public static boolean running = true;

	public BCPing() {
		URL.setURLStreamHandlerFactory(new LegacyURLStreamHandlerFactory());
		
		if (AccessHelper.type == ServerType.NMS) {
			log = Logger.getLogger("Minecraft");
		} else if (AccessHelper.type == ServerType.CMMS) {
			log = Logger.getLogger("MinecraftServer");
		} else {
			log = Logger.getGlobal();
		}

		log.info("[BetacraftPing] BetacraftPing v" + BCPING_VER + " enabled.");
		
		File configfile = new File("betacraft/ping_details.json");
		String pingdetails = null;
		try {
			pingdetails = new String(Files.readAllBytes(configfile.toPath()), "UTF-8");
		} catch (Throwable t) {
			if (!(t instanceof NoSuchFileException)) {
				t.printStackTrace();
			}
			configfile.getParentFile().mkdirs();
		}
		if (pingdetails != null) {
			try {
				config = (JSONObject) parser.parse(pingdetails);
			} catch (Throwable t) {
				log.warning("[BetacraftPing] Failed to read configuration! Disabling...");
				t.printStackTrace();
				running = false;
				return;
			}
			// TODO validation?
		} else {
			config = new JSONObject();

			String serverip = getIPFromAmazon();
			config.put("socket", serverip + ":25565");
			config.put("name", "A Minecraft server");
			config.put("description", "");
			config.put("category", "alpha");
			config.put("protocol", "alpha_13");
			config.put("game_version", "a1.0.15");
			config.put("send_players", true);
			
			try {
				Files.write(configfile.toPath(), config.toJSONString().getBytes("UTF-8"));
			} catch (Throwable t) {
				log.warning("[BetacraftPing] Failed to write default configuration! Disabling...");
				running = false;
				return;
			}
			
			log.warning("[BetacraftPing] Failed to load configuration!");
			log.warning("[BetacraftPing] Wrote default configuration --- see plugins/BetacraftPing/ping_details.json");
		}
		
		SendIcon.sendIcon();
		
		thread = new PingThread();
		thread.start();
	}
	
	public void onDisable() {
		log.info("[BetacraftPing] Disabling...");
		running = false;
		if (thread != null) thread.interrupt();
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
			File iconfile = new File("betacraft/server_icon.png");
			if (!iconfile.exists()) {
				BCPing.log.warning("[BetacraftPing] No server icon found at \"betacraft/server_icon.png\"!");
				return icon;
			}
			
			if (iconfile.length() > 64000) {
				BCPing.log.severe("[BetacraftPing] Server icon size is too big! (64 kB max, recommended res: 128x128)");
			} else {
				try {
					byte[] filebytes = Files.readAllBytes(iconfile.toPath());
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
