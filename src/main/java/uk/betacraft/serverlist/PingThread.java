package uk.betacraft.serverlist;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.stream.Collectors;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;


public class PingThread extends Thread {

	@Override
	public void run() {
		HttpURLConnection con = null;
		try {
			URL url = new URL("https://api.betacraft.uk/v2/server_update");
			int failsInARow = 0;
			while (BCPing.running) {
				try {
					con = (HttpURLConnection) url.openConnection();
					con.setRequestMethod("POST");
					con.addRequestProperty("Content-Type", "application/json");
					con.setUseCaches(false);
					con.setDoOutput(true);
					con.setDoInput(true);

					OutputStream os = con.getOutputStream();

					List<String> online = AccessHelper.getOnlinePlayers();
					
					JSONObject jobj = (JSONObject) BCPing.parser.parse(BCPing.config.toJSONString());

					jobj.put("max_players", AccessHelper.getMaxPlayers());
					jobj.put("online_players", online.size());
					
					JSONObject software = new JSONObject();
					software.put("name", "Vanilla Minecraft Server");
					software.put("version", "unknown");
					jobj.put("software", software);
					
					jobj.put("online_mode", AccessHelper.getOnlineMode());
					
					if ((boolean)jobj.get("send_players")) {
						
						JSONArray jarr = new JSONArray();
						for (String username : online) {
							JSONObject pobj = new JSONObject();
							
							pobj.put("username", username);
							jarr.add(pobj);
						}
						
						jobj.put("players", jarr);
					} else {
						jobj.put("players", new JSONArray());
					}
					
					
					String data = jobj.toString();
					//BCPing.log.info(data);
					
					byte[] json = data.getBytes("UTF-8");

					os.write(json);
					os.flush();
					os.close();

					// process response
					JSONObject response = readResponse(con.getInputStream());

					if (response != null) {
						if (!(boolean)response.getOrDefault("error", false)) {
							if (failsInARow != 0) {
								BCPing.log.info("[BetacraftPing] Server list ping was successful");
								//BCPing.log.info("[BetaCraftPing] You can customize your server's appearance on the list by going to: 'https://api.betacraft.uk/edit_server.jsp?id=" + privatekey + "'");
							}
							failsInARow = 0;
						} else {
							failsInARow++;
							if (failsInARow <= 5) {
								BCPing.log.info("[BetacraftPing] Failed to ping the server list");
								BCPing.log.info("[BetacraftPing] Error: \"" + response.getOrDefault("error", "READER PANIC!") + "\"");
							}
						}
					} else {
						failsInARow++;
						if (failsInARow <= 5) {
							BCPing.log.info("[BetacraftPing] Failed to read ping response (is null)");
						}
					}

					Thread.sleep(60000);
				} catch (Throwable t) {
					// Prevent fail messages at server shutdown
					if (!BCPing.running)
						return;
					failsInARow++;
					if (failsInARow <= 5) {
						BCPing.log.warning("[BetacraftPing] Failed to ping server list. (" + t.getMessage() + ")");
						BCPing.log.warning("[BetacraftPing] Perhaps ping_details.json is not configured properly?");
						
						
						String result = new BufferedReader(new InputStreamReader(con.getErrorStream()))
								   .lines().collect(Collectors.joining("\n"));
						BCPing.log.info("[BetacraftPing] Error: \"" + result + "\"");
					}
					Thread.sleep(60000);
				}
				
			}
		} catch (Throwable t) {
			// Prevent fail messages at server shutdown
			if (!BCPing.running)
				return;
			BCPing.log.warning("[BetacraftPing] The heartbeat was permanently interrupted (" + t.getMessage() + ")");
		}
	}
	
	public static JSONObject readResponse(InputStream is) {
		try {
			ByteArrayOutputStream buffer = new ByteArrayOutputStream();

			int nRead;
			byte[] readData = new byte[is.available()];

			while ((nRead = is.read(readData, 0, readData.length)) != -1) {
				buffer.write(readData, 0, nRead);
			}

			buffer.flush();
			String responString = new String(buffer.toByteArray());

			//System.out.println(responString);

			return (JSONObject) BCPing.parser.parse(responString);
		} catch (Throwable t) {
			BCPing.log.warning("Failed to read response: " + t.getMessage());
			return null;
		}
	}
}
