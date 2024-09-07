package serverlist;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONObject;

public class PingThread extends Thread {

    @Override
    public void run() {
        HttpURLConnection con = null;
        try {
            URL url = new URL(BCPing.HOST + "/server_update");
            int failsInARow = -1;

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

                    JSONObject payload = new JSONObject(BCPing.config.toString());

                    payload.put("max_players", AccessHelper.getMaxPlayers());
                    payload.put("online_players", online.size());

                    JSONObject software = new JSONObject();
                    software.put("name", "Vanilla Minecraft Server");
                    software.put("version", "unknown");

                    payload.put("software", software);
                    payload.put("online_mode", AccessHelper.getOnlineMode());

                    if (payload.getBoolean("send_players")) {
                        JSONArray playersArray = new JSONArray();

                        for (String username : online) {
                            JSONObject playerObject = new JSONObject();
                            playerObject.put("username", username);

                            playersArray.put(playerObject);
                        }

                        payload.put("players", playersArray);
                    } else {
                        payload.put("players", new JSONArray());
                    }

                    String data = payload.toString();
                    //BCPing.log.info(data);

                    byte[] json = data.getBytes("UTF-8");

                    os.write(json);
                    os.flush();
                    os.close();

                    // process response
                    JSONObject response = readResponse(con.getInputStream());

                    if (response != null) {
                        if (!response.getBoolean("error")) {
                            if (failsInARow != 0) {
                                BCPing.log.info("[BetacraftPing] Server list ping was successful");

                                SendIcon.sendIcon();
                            }

                            failsInARow = 0;
                        } else {
                            failsInARow++;

                            if (failsInARow <= 5) {
                                BCPing.log.info("[BetacraftPing] Failed to ping the server list");
                                BCPing.log.info("[BetacraftPing] Error: \"" + response.getString("message") + "\"");
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

                        try {
                            String result = new BufferedReader(new InputStreamReader(con.getErrorStream()))
                                    .lines().collect(Collectors.joining("\n"));

                            BCPing.log.info("[BetacraftPing] Error: \"" + result + "\"");
                        } catch (Throwable t2) {
                            t2.printStackTrace();
                        }
                        
                    }
                    try {
                        Thread.sleep(60000);
                    } catch (Throwable t2) {
                        if (!BCPing.running)
                            return;

                        t2.printStackTrace();
                    }
                }

            }
        } catch (Throwable t) {
            // Prevent fail messages at server shutdown
            if (!BCPing.running)
                return;

            BCPing.log.warning("[BetacraftPing] The heartbeat was permanently interrupted (" + t.getMessage() + ")");
        }
    }
    
    public static String readStringResponse(InputStream is) {
        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();

            int nRead;
            byte[] readData = new byte[is.available()];

            while ((nRead = is.read(readData, 0, readData.length)) != -1) {
                buffer.write(readData, 0, nRead);
            }

            buffer.flush();
            return new String(buffer.toByteArray());
        } catch (Throwable t) {
            BCPing.log.warning("Failed to read response: " + t.getMessage());
            return null;
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
            String responseString = new String(buffer.toByteArray());

            return new JSONObject(responseString);
        } catch (Throwable t) {
            BCPing.log.warning("Failed to read response: " + t.getMessage());
            return null;
        }
    }
}
