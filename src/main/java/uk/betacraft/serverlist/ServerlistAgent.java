package uk.betacraft.serverlist;

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
import legacyfix.LegacyURLStreamHandlerFactory;
import uk.betacraft.serverlist.AccessHelper.ServerType;

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

            injectMojangAuth(inst);
        } catch (Throwable t) {
            t.printStackTrace();
        }

        URL.setURLStreamHandlerFactory(new LegacyURLStreamHandlerFactory());
    }

    public static void hookServerConstructor(Instrumentation inst) throws CannotCompileException, ClassNotFoundException, UnmodifiableClassException, IOException {
        CtConstructor serverConstructor = mcServerClass.getConstructors()[0];

        serverConstructor.insertAfter(
            (AccessHelper.type == ServerType.NMS) ?
                "uk.betacraft.serverlist.AccessHelper.initNMS($0);"
            :
                "uk.betacraft.serverlist.AccessHelper.initCMMS($0);"
        );

        inst.redefineClasses(new ClassDefinition(Class.forName(mcServerClass.getName()), mcServerClass.toBytecode()));
    }

    public static void injectMojangAuth(Instrumentation inst) throws NotFoundException, CannotCompileException, ClassNotFoundException, UnmodifiableClassException, IOException {
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

        handlePacketsMethod.insertBefore(
            "if ($1 == $1.b && !legacyfix.request.HasJoinedRequest.fire(((String)$2[1]).trim())) {" +
            "    $0.a(\"Login wasn't authenticated with Mojang!\");" +
            "    return;" +
            "}"
        );

        inst.redefineClasses(new ClassDefinition(Class.forName(playerInstanceClass.getName()), playerInstanceClass.toBytecode()));
    }
}
