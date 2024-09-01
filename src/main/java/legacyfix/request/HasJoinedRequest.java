package legacyfix.request;

import java.security.MessageDigest;

import org.json.JSONException;

import uk.betacraft.serverlist.BCPing;

public class HasJoinedRequest extends Request {

    public HasJoinedRequest(String username, String serverId) {
        this.REQUEST_URL = "https://sessionserver.mojang.com/session/minecraft/hasJoined?username=" + username + "&serverId=" + serverId;
        this.PROPERTIES.put("Content-Type", "application/json");
    }

    @Override
    public Response perform() {
        return RequestUtil.performGETRequest(this);
    }

    public static boolean fire(String user, String playerSocket) {
        try {
            Response res = new HasJoinedRequest(user, sha1(playerSocket.substring(1))).perform();

            return res.code == 200;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static String sha1(String input) {
        try {
            MessageDigest mDigest = MessageDigest.getInstance("SHA-1");
            byte[] result = mDigest.digest(input.getBytes());
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < result.length; ++i) {
                sb.append(Integer.toString((result[i] & 0xFF) + 256, 16).substring(1));
            }
            return sb.toString();
        } catch (Throwable t) {
            t.printStackTrace();
            return null;
        }
    }
}