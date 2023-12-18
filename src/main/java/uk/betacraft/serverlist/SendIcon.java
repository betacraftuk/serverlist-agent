package uk.betacraft.serverlist;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.stream.Collectors;

import org.json.simple.JSONObject;

public class SendIcon {

	public static void sendIcon() {
		
		String icon = BCPing.getIcon();
		
		HttpURLConnection con = null;
		try {
			URL url = new URL("https://api.betacraft.uk/v2/server_update_icon");
			try {
				con = (HttpURLConnection) url.openConnection();
				con.setRequestMethod("POST");
				con.addRequestProperty("Content-Type", "application/json");
				con.setUseCaches(false);
				con.setDoOutput(true);
				con.setDoInput(true);

				OutputStream os = con.getOutputStream();
				
				JSONObject jobj = new JSONObject();
				jobj.put("connect_socket", BCPing.config.get("connect_socket"));
				if (icon == null) {
					jobj.put("icon", "");
				} else {
					jobj.put("icon", icon);
				}
				
				String data = jobj.toString();
				
				byte[] json = data.getBytes("UTF-8");

				os.write(json);
				os.flush();
				os.close();

				String privatekey = new BufferedReader(new InputStreamReader(con.getInputStream()))
						   .lines().collect(Collectors.joining("\n"));
				if (privatekey != null && privatekey.contains("Success")) {
					BCPing.log.info("[BetacraftPing] Server icon updated successfully.");
				} else {
					BCPing.log.info("[BetacraftPing] Failed to update server icon");
					BCPing.log.info("[BetacraftPing] Error: \"" + privatekey + "\"");
				}
			} catch (Throwable t) {
				BCPing.log.warning("[BetacraftPing] Failed to update server icon (" + t.getMessage() + ")");
				String result = new BufferedReader(new InputStreamReader(con.getErrorStream()))
						   .lines().collect(Collectors.joining("\n"));
				BCPing.log.info("[BetacraftPing] Error: \"" + result + "\"");
			}
		} catch (Throwable t) {
			BCPing.log.warning("[BetacraftPing] Failed to update server icon (" + t.getMessage() + ")");
		}
	}
}
