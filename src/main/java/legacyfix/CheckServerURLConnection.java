package legacyfix;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import legacyfix.request.HasJoinedRequest;
import legacyfix.request.Response;

public class CheckServerURLConnection extends HttpURLConnection {
    public CheckServerURLConnection(URL url) {
        super(url);
    }

    @Override
    public void disconnect() {

    }

    @Override
    public boolean usingProxy() {
        return false;
    }

    private String response = "NO";

    @Override
    public void connect() throws IOException {

    }

    @Override
    public InputStream getInputStream() throws IOException {
        String serverId = this.url.toString().substring(this.url.toString().indexOf("&serverId=") + 10);
        String username = this.url.toString().substring(this.url.toString().indexOf("?user=") + 6, this.url.toString().indexOf("&serverId="));

        Response r = new HasJoinedRequest(
                username,
                serverId
                ).perform();

        if (r.code == 200) {
            response = "YES";
        }

        return new ByteArrayInputStream(response.getBytes());
    }

    @Override
    public int getResponseCode() {
        return 200;
    }

    @Override
    public String getResponseMessage() {
        return this.response;
    }
}