package serverlist;

import java.io.IOException;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.net.URL;
import java.util.Arrays;
import java.util.Optional;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtField;
import javassist.CtMethod;
import javassist.NotFoundException;
import proxy.LegacyURLStreamHandlerFactory;
import serverlist.AccessHelper.ServerType;

public class ServerlistAgent {

    static final ClassPool pool = ClassPool.getDefault();

    static CtClass mcServerClass;

    public static void premain(String agentArgs, Instrumentation inst) {
        agentmain(agentArgs, inst);
    }

    public static void agentmain(String agentArgs, Instrumentation inst) {

        String advice = null;
        if (ClassLoader.getSystemResourceAsStream("net/minecraft/server/MinecraftServer.class") != null) {
            advice = "net.minecraft.server.MinecraftServer";
        } else if (ClassLoader.getSystemResourceAsStream("com/mojang/minecraft/server/MinecraftServer.class") != null) {
            advice = "com.mojang.minecraft.server.MinecraftServer";
        }

        String main = System.getProperty("main", advice);

        if (main == null) {
            System.out.println("[BetacraftPing] No main class found!!!! Nothing to work with!!");
            return;
        }

        // defaults
        if (main.startsWith("net.minecraft.server")) {
            AccessHelper.type = ServerType.NMS;
        } else if (main.startsWith("com.mojang.minecraft.server")) {
            AccessHelper.type = ServerType.CMMS;
        }

        // user-chosen overwrite
        String typeprop = System.getProperty("type", null);
        if (typeprop != null) {
            if (typeprop.equalsIgnoreCase("nms")) {
                System.out.println("[BetacraftPing] Starting as an Alpha-Release version server");
                AccessHelper.type = ServerType.NMS;
            } else if (typeprop.equalsIgnoreCase("cmms")) {
                System.out.println("[BetacraftPing] Starting as a Classic version server");
                AccessHelper.type = ServerType.CMMS;
            } else {
                System.out.println("[BetacraftPing] ??? Unknown server type specified: \"" + typeprop + "\"");
            }
        }

        try {
            mcServerClass = pool.get(main);

            hookServerConstructor(inst);

            if (AccessHelper.type == ServerType.CMMS) {
                silenceHeartbeatSpam(inst);
                injectMojangAuthClassic(inst);
            }
            
            if ("true".equals(System.getProperty("mojangAuth", "false"))) {
                injectMojangAuthEarlyAlpha(inst);
            }

        } catch (Throwable t) {
            t.printStackTrace();
        }

        URL.setURLStreamHandlerFactory(new LegacyURLStreamHandlerFactory());
    }

    public static void hookServerConstructor(Instrumentation inst) throws CannotCompileException, ClassNotFoundException, UnmodifiableClassException, IOException, NotFoundException {
        CtConstructor serverConstructor = mcServerClass.getConstructors()[0];

        serverConstructor.insertAfter(
            (AccessHelper.type == ServerType.NMS) ?
                "serverlist.AccessHelper.initNMS($0);"
            :
                "serverlist.AccessHelper.initCMMS($0);"
        );

        inst.redefineClasses(new ClassDefinition(Class.forName(mcServerClass.getName()), mcServerClass.toBytecode()));
    }
    
    public static void silenceHeartbeatSpam(Instrumentation inst) throws NotFoundException, ClassNotFoundException, UnmodifiableClassException, IOException, CannotCompileException {
        CtClass loggerClass = pool.get("java.util.logging.Logger");
        
        CtClass stringClass = pool.get("java.lang.String");
        
        CtMethod infoMethod = loggerClass.getDeclaredMethod("info", new CtClass[] {stringClass});

        infoMethod.insertBefore(
            "if (\"Sending heartbeat\".equals($1)) { return; }"
        );

        inst.redefineClasses(new ClassDefinition(Class.forName(loggerClass.getName()), loggerClass.toBytecode()));
    }
    
    public static void injectMojangAuthEarlyAlpha(Instrumentation inst) throws NotFoundException, CannotCompileException, ClassNotFoundException, UnmodifiableClassException, IOException {
        //mcServerClass.defrost();
        
        CtField SCMField = null;
        for (CtField field : mcServerClass.getDeclaredFields()) {
            if (field.getType().getName().equals("boolean"))
                break;
            
            SCMField = field;
        }
        
        CtClass SCMClass = SCMField.getType();
        Optional<CtMethod> optSCMLoginMethod = Arrays.asList(SCMClass.getDeclaredMethods()).stream().filter(x -> {
            try {
                CtClass[] types = x.getParameterTypes();
                return types.length == 3 && types[1].getName().equals("java.lang.String") && types[2].getName().equals("java.lang.String");
            } catch (NotFoundException e) {
                e.printStackTrace();
            }
            return false;
        }).findFirst();
        
        if (!optSCMLoginMethod.isPresent()) {
            System.out.println("No SCMLoginMethod found");
            return;
        }
        
        CtMethod SCMLoginMethod = optSCMLoginMethod.get();
        
        CtClass netLoginHandlerClass = SCMLoginMethod.getParameterTypes()[0];
        
        Optional<CtMethod> optNLHKickMethod = Arrays.asList(netLoginHandlerClass.getDeclaredMethods()).stream().filter(x -> {
            try {
                CtClass[] types = x.getParameterTypes();
                return types.length == 1 && types[0].getName().equals("java.lang.String");
            } catch (NotFoundException e) {
                e.printStackTrace();
            }
            return false;
        }).findFirst();
        
        if (!optNLHKickMethod.isPresent()) {
            System.out.println("No NLHKickMethod found");
            return;
        }
        
        CtMethod NLHKickMethod = optNLHKickMethod.get();
        
        Optional<CtMethod> optNLHLoginMethod = Arrays.asList(netLoginHandlerClass.getDeclaredMethods()).stream().filter(x -> {
            try {
                CtClass[] types = x.getParameterTypes();
                return types.length == 1 && !types[0].getName().equals("java.lang.String");
            } catch (NotFoundException e) {
                e.printStackTrace();
            }
            return false;
        }).findFirst();
        
        if (!optNLHLoginMethod.isPresent()) {
            System.out.println("No NLHLoginMethod found");
            return;
        }
        
        CtMethod NLHLoginMethod = optNLHLoginMethod.get();
        
        CtClass packet1LoginClass = NLHLoginMethod.getParameterTypes()[0];

        Optional<CtField> optUsernameField = Arrays.asList(packet1LoginClass.getDeclaredFields()).stream().filter(x -> {
            try {
                return x.getType().getName().equals("java.lang.String");
            } catch (NotFoundException e) {
                e.printStackTrace();
            }
            return false;
        }).findFirst();
        
        if (!optUsernameField.isPresent()) {
            System.out.println("No usernameField found");
            return;
        }
        
        CtField usernameField = optUsernameField.get();
        
        // Now find NetworkManager
        Optional<CtField> optNetworkManagerField = Arrays.asList(netLoginHandlerClass.getDeclaredFields()).stream().filter(x -> {
            try {
                return !x.getType().getName().startsWith("java.");
            } catch (NotFoundException e) {
                e.printStackTrace();
            }
            return false;
        }).findFirst();
        
        if (!optNetworkManagerField.isPresent()) {
            System.out.println("No networkManagerField found");
            return;
        }
        
        CtField networkManagerField = optNetworkManagerField.get();
        
        CtClass networkManagerClass = networkManagerField.getType();
        
        Optional<CtMethod> optGetSocketAddressMethod = Arrays.asList(networkManagerClass.getDeclaredMethods()).stream().filter(x -> {
            try {
                return x.getReturnType().getName().equals("java.net.SocketAddress");
            } catch (NotFoundException e) {
                e.printStackTrace();
            }
            return false;
        }).findFirst();
        
        if (!optGetSocketAddressMethod.isPresent()) {
            System.out.println("No getSocketAddressMethod found");
            return;
        }
        
        CtMethod getSocketAddressMethod = optGetSocketAddressMethod.get();

        NLHLoginMethod.insertBefore(
            "if (!legacyfix.request.HasJoinedRequest.fire($1." + usernameField.getName() + ", $0." + networkManagerField.getName() + "." + getSocketAddressMethod.getName() + "().toString().split(\":\")[0])) {" +
            "    $0." + NLHKickMethod.getName() + "(\"Login wasn't authenticated with Mojang!\");" +
            "    return null;" +
            "}"
        );
        
        inst.redefineClasses(new ClassDefinition(Class.forName(netLoginHandlerClass.getName()), netLoginHandlerClass.toBytecode()));
    }

    public static void injectMojangAuthClassic(Instrumentation inst) throws NotFoundException, CannotCompileException, ClassNotFoundException, UnmodifiableClassException, IOException {
        mcServerClass.defrost();

        Optional<CtField> optPlayersArrayField = Arrays.asList(mcServerClass.getDeclaredFields()).stream()
                .filter(x -> x.getSignature().startsWith("[L")).findFirst();

        if (!optPlayersArrayField.isPresent()) {
            System.out.println("No playersArrayField found");
            return;
        }

        CtField playersArrayField = optPlayersArrayField.get();

        CtClass playerInstanceClass = playersArrayField.getType().getComponentType();

        Optional<CtMethod> optHandlePacketsMethod = Arrays.asList(playerInstanceClass.getDeclaredMethods()).stream().filter(x -> {
            try {
                return x.getReturnType() == CtClass.voidType && x.getParameterTypes().length == 2;
            } catch (NotFoundException e) {
                e.printStackTrace();
            }
            return false;
        }).findFirst();

        if (!optHandlePacketsMethod.isPresent()) {
            System.out.println("No handlePacketsMethod found");
            return;
        }

        CtMethod handlePacketsMethod = optHandlePacketsMethod.get();

        // TODO auto detect?
        String packetIdentificationField = "b";
        String packetUsernameIndex = "1";
        String networkManagerField = "a";
        String ipField = "f";
        String kickMethod = "a";

        if (mcServerClass.getName().equals("p000com.mojang.minecraft.server.MinecraftServer")) {
            packetIdentificationField = "IDENTIFICATION";
            packetUsernameIndex = "0";
            networkManagerField = "networkManager";
            ipField = "username";
            kickMethod = "mo97a";
        }

        handlePacketsMethod.insertBefore(
            "if ($1 == $1." + packetIdentificationField + " && !legacyfix.request.HasJoinedRequest.fire(((String)$2[" + packetUsernameIndex + "]).trim(), $0." + networkManagerField + "." + ipField + ")) {" +
            "    $0." + kickMethod + "(\"Login wasn't authenticated with Mojang!\");" +
            "    return;" +
            "}"
        );

        inst.redefineClasses(new ClassDefinition(Class.forName(playerInstanceClass.getName()), playerInstanceClass.toBytecode()));
    }
}
