package protocol.http;

import java.io.IOException;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

import proxy.CheckServerURLConnection;
import proxy.HeartbeatURLConnection;

public class Handler extends URLStreamHandler {
    static boolean disableHeartbeat = "true".equals(System.getProperty("disableHeartbeat", "true"));

    @Override
    protected URLConnection openConnection(URL url, Proxy p) throws IOException {
        return this.openConnection(url);
    }

    @Override
    protected URLConnection openConnection(URL url) throws IOException {
        //System.out.println("Got: " + url.toString());
        if (url.toString().contains("/game/checkserver.jsp"))
            return new CheckServerURLConnection(url);
        else if (url.toString().contains("/heartbeat.jsp") && disableHeartbeat)
            return new HeartbeatURLConnection(url);
        else {
            return new URL((URL)null, url.toString(), new sun.net.www.protocol.http.Handler()).openConnection();
        }
    }
}