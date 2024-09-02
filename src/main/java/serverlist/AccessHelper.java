package serverlist;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.LinkedList;
import java.util.List;

public class AccessHelper {
    public enum ServerType {
        NMS,
        CMMS;
    }

    public static ServerType type = ServerType.NMS;

    private static Object minecraftServerInstance = null;
    private static Object serverConfigurationManagerInstance = null;

    private static Field serverConfigurationManagerField = null;
    private static Field onlineModeField = null;
    private static Field onlinePlayersField = null;
    private static Field maxPlayersField = null;
    private static Field usernameField = null;
    private static Field gameProfileField = null;

    public static Boolean onlineMode = null;

    public static void initCMMS(Object minecraftInstance) {
        type = ServerType.CMMS;

        new File("betacraft").mkdirs();
        minecraftServerInstance = minecraftInstance;

        try {
            Class s = minecraftServerInstance.getClass();

            int boolcount = 0;
            int intcount = 0;
            int listcount = 0;
            Field[] prev = new Field[2];

            for (Field f: s.getDeclaredFields()) {
                String typename = f.getType().getName();

                if (typename.equals("boolean") && boolcount < 3) {
                    boolcount++;
                    if (boolcount > 2) {
                        // if previous field was int, this is admin-slot!
                        // we need to find one more boolean, this should be verify-names

                        if (!"int".equals(prev[1].getType().getName())) {
                            onlineModeField = f;
                            onlineModeField.setAccessible(true);
                            
                            onlineMode = onlineModeField.getBoolean(minecraftInstance);
                            
                            onlineModeField.set(minecraftInstance, false);
                            break; // this variable is the last we're looking for
                        }
                    }
                } else if (typename.equals("java.util.List")) {
                    listcount++;
                    // first List field is players
                    if (listcount == 1) {
                        onlinePlayersField = f;
                        onlinePlayersField.setAccessible(true);
                    }
                } else if (typename.equals("int")) {
                    intcount++;
                    // first int field is maxPlayers
                    if (intcount == 1) {
                        maxPlayersField = f;
                        maxPlayersField.setAccessible(true);
                    }
                }


                prev[1] = prev[0];
                prev[0] = f;
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }

        // Once everything's ready, start the plugin
        new BCPingThread().start();
    }

    private static void initSCM() {
        if (serverConfigurationManagerInstance == null) {
            try {
                serverConfigurationManagerInstance = serverConfigurationManagerField.get(minecraftServerInstance);
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }

    public static void initNMS(Object minecraftInstance) {
        new File("betacraft").mkdirs();
        minecraftServerInstance = minecraftInstance;

        try {
            Class s = minecraftServerInstance.getClass();

            // 1.3+
            Class sclass = s.getSuperclass();
            if (sclass != null && !sclass.getCanonicalName().equals("java.lang.Object")) {
                s = sclass;
            }

            int boolcount = 0;
            Field[] prev = new Field[2];
            for (Field f: s.getDeclaredFields()) {
                if (f.getType().getName().equalsIgnoreCase("boolean")) {
                    boolcount++;
                    if (boolcount == 1) {
                        // previous field was serverconfigman

                        for (int i = 0; i < 2; i++) {
                            boolean match = false;
                            Class scmc = prev[i].getType();
                            for (Field f1 : scmc.getDeclaredFields()) {
                                //System.out.println(f1.getName() + ", " + f1.getType().getName());
                                if (f1.getType().getName().equalsIgnoreCase("java.util.List")) {
                                    onlinePlayersField = f1;
                                    onlinePlayersField.setAccessible(true);
                                    match = true;
                                } else if (f1.getType().getName().equals("int") &&
                                        !Modifier.isStatic(f1.getModifiers())) {
                                    maxPlayersField = f1;
                                    maxPlayersField.setAccessible(true);
                                    break;
                                }
                            }

                            if (match) {
                                serverConfigurationManagerField = prev[i];
                                //System.out.println(serverConfigurationManagerField.getName() + ", " + serverConfigurationManagerField.getType().getName());
                                serverConfigurationManagerField.setAccessible(true);
                                break;
                            } else {
                                // 1.18+
                                boolcount = -1;
                            }
                        }
                    }
                    if (boolcount == 3) {
                        onlineModeField = f;
                        onlineModeField.setAccessible(true);
                        break;
                    }
                }
                prev[1] = prev[0];
                prev[0] = f;
            }


        } catch (Throwable t) {
            t.printStackTrace();
        }

        // Once everything's ready, start the plugin
        new BCPingThread().start();
    }

    private static String getUsername(Object playerobj) {
        try {
            if (usernameField == null && gameProfileField == null) {
                Class playerclass = playerobj.getClass();

                if (type == ServerType.NMS) {
                    playerclass = playerclass.getSuperclass();
                }

                for (Field f : playerclass.getDeclaredFields()) {
                    if (f.getType().getName().equals("java.lang.String")) {
                        f.setAccessible(true);
                        usernameField = f;
                        break;
                    }
                }
            }
            
            if (usernameField == null && gameProfileField == null) {
                // 1.7.x+
                Class humanClass = playerobj.getClass().getSuperclass();
                
                for (Field f : humanClass.getDeclaredFields()) {
                    if (f.getType().getName().equals("com.mojang.authlib.GameProfile")) {
                        f.setAccessible(true);
                        gameProfileField = f;
                        break;
                    }
                }
                
                for (Field f : gameProfileField.getType().getDeclaredFields()) {
                    if (f.getType().getName().equals("java.lang.String")) {
                        f.setAccessible(true);
                        usernameField = f;
                        break;
                    }
                }
            }
            
            if (gameProfileField != null) {
                return (String) usernameField.get(gameProfileField.get(playerobj));
            }

            return (String) usernameField.get(playerobj);
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return null;
    }

    public static List<String> getOnlinePlayers() {
        if (type == ServerType.NMS) {
            initSCM();
        }

        LinkedList<String> list = new LinkedList<String>();
        try {
            List nmslist;

            if (type == ServerType.NMS) {
                nmslist = (List) onlinePlayersField.get(serverConfigurationManagerInstance);
            } else { // classic
                nmslist = (List) onlinePlayersField.get(minecraftServerInstance);
            }

            for (Object o: nmslist) {
                list.add(getUsername(o));
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }

        return list;
    }

    public static boolean getOnlineMode() {
        // classic's 'verify-names'
        if (onlineMode != null)
            return onlineMode;

        // a1.0.5-a1.0.15 / c1.0-c1.2
        if (onlineModeField == null) {
            return false;
        }

        try {
            return (boolean) onlineModeField.get(minecraftServerInstance);
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return false;
    }

    public static int getMaxPlayers() {
        if (type == ServerType.NMS) {
            initSCM();
        }

        try {
            if (type == ServerType.NMS) {
                return (int) maxPlayersField.get(serverConfigurationManagerInstance);
            } else { // classic
                return (int) maxPlayersField.get(minecraftServerInstance);
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return 0;
    }

    public static class BCPingThread extends Thread {
        @Override
        public void run() {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            new BCPing();
        }
    }
}
