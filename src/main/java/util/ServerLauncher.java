package util;

import java.io.File;
import java.util.ArrayList;

public class ServerLauncher {

    public static void main(String[] args) {
        String JAVA_HOME = System.getProperty("java.home");
        String javapath = new File(JAVA_HOME, "bin/java" + (Util.isWindows() ? ".exe" : "")).getAbsolutePath();
        try {

            ArrayList<String> pargs = new ArrayList<String>();

            pargs.add(javapath);

            if (Util.getCurrentMajorJavaVersion() > 8) {
                pargs.add("--add-opens=java.base/sun.net.www.protocol.http=ALL-UNNAMED");
            }

            pargs.add("-javaagent:serverlist-agent.jar");
            pargs.add("-jar");
            pargs.add("minecraft_server.jar");

            ProcessBuilder pb = new ProcessBuilder(pargs);
            pb.inheritIO();

            Process process = pb.start();
            process.waitFor();
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}
