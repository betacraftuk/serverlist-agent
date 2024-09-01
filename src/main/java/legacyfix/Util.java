package legacyfix;

public class Util {
    public static String OS = System.getProperty("os.name").toLowerCase();
    public static String ARCH = System.getProperty("os.arch");
    public static String VER = System.getProperty("os.version").toLowerCase();

    public static boolean isWindows() {
        return (OS.indexOf("win") >= 0);
    }

    public static boolean isMac() {
        return (OS.indexOf("mac") >= 0);
    }

    public static boolean isLinux() {
        return (OS.indexOf("nix") >= 0 || OS.indexOf("nux") >= 0 || OS.indexOf("aix") > 0);
    }

    public static int getCurrentMajorJavaVersion() {
        String ver = System.getProperty("java.runtime.version");
        if (ver.startsWith("1.")) {
            return Integer.parseInt(ver.split("\\.")[1]);
        } else {
            int cut = ver.indexOf('.'); 
            if (cut == -1) {
                cut = ver.length();
            }

            return Integer.parseInt(ver.substring(0, cut));
        }
    }
}