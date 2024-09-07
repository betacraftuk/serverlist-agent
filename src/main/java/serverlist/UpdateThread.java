package serverlist;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONObject;

public class UpdateThread extends Thread {
    public static JSONObject newestRelease = null;
    public static boolean update = false;

    public void run() {
        HttpURLConnection con = null;
        try {
            URL url = new URL("https://api.github.com/repos/betacraftuk/serverlist-agent/releases?per_page=1");
            while (BCPing.running) {
                try {
                    con = (HttpURLConnection) url.openConnection();
                    con.setRequestMethod("GET");
                    con.setUseCaches(false);
                    con.setDoOutput(true);
                    con.setDoInput(true);

                    // process response
                    try {
                        JSONArray releases = new JSONArray(PingThread.readStringResponse(con.getInputStream()));
                        newestRelease = releases.getJSONObject(0);
                        String newestReleaseTag = newestRelease.getString("tag_name");

                        if (!BCPing.BCPING_VER.equals(newestReleaseTag)) {
                            update = true;

                            BCPing.log.info("[BetacraftPing] New release available! " + newestReleaseTag);
                            BCPing.log.info("[BetacraftPing] Download it at " + newestRelease.getString("html_url"));
                        } else {
                            update = false;
                        }
                    } catch (Throwable t) {
                        if (con.getResponseCode() != 404) {
                            BCPing.log.warning("[BetacraftPing] Failed to get updates. (" + t.getMessage() + ")");

                            try {
                                String result = new BufferedReader(new InputStreamReader(con.getErrorStream()))
                                        .lines().collect(Collectors.joining("\n"));

                                BCPing.log.info("[BetacraftPing] Error: \"" + result + "\"");
                            } catch (Throwable t2) {
                                t2.printStackTrace();
                            }
                        }

                        update = false;
                    }

                    Thread.sleep(1000 * 60 * 60 * 3); // 3 hours
                } catch (Throwable t) {
                    // Prevent fail messages at server shutdown
                    if (!BCPing.running)
                        return;

                    update = false;

                    BCPing.log.warning("[BetacraftPing] Failed to get updates. (" + t.getMessage() + ")");

                    try {
                        String result = new BufferedReader(new InputStreamReader(con.getErrorStream()))
                                .lines().collect(Collectors.joining("\n"));

                        BCPing.log.info("[BetacraftPing] Error: \"" + result + "\"");
                    } catch (Throwable t2) {
                        t2.printStackTrace();
                    }

                    try {
                        Thread.sleep(1000 * 60 * 60 * 3); // 3 hours
                    } catch (Throwable t2) {
                        if (!BCPing.running)
                            return;

                        update = false;

                        t2.printStackTrace();
                    }
                }
            }

        } catch (Throwable t) {
            update = false;

            t.printStackTrace();
        }
    }
}
