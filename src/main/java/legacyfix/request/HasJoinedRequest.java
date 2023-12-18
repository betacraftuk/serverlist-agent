package legacyfix.request;

public class HasJoinedRequest extends Request {

	public HasJoinedRequest(String username, String serverId) {
		this.REQUEST_URL = "https://sessionserver.mojang.com/session/minecraft/hasJoined?username=" + username + "&serverId=" + serverId;
		this.PROPERTIES.put("Content-Type", "application/json");
	}

	@Override
	public Response perform() {
		return RequestUtil.performGETRequest(this);
	}
}