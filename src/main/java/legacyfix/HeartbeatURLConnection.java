package legacyfix;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class HeartbeatURLConnection extends HttpURLConnection {
    public HeartbeatURLConnection(URL url) {
        super(url);
    }

    @Override
    public void disconnect() {

    }

    @Override
    public boolean usingProxy() {
        return false;
    }

    private String response = "Heartbeat/mppass functionality disabled successfully!";

    @Override
    public void connect() throws IOException {

    }

    @Override
    public InputStream getInputStream() throws IOException {
        return new ByteArrayInputStream(response.getBytes());
    }

    @Override
    public OutputStream getOutputStream() {
        return new ByteArrayOutputStream();
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