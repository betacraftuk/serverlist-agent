package uk.betacraft.serverlist;

import java.lang.instrument.ClassDefinition;
import java.lang.instrument.Instrumentation;
import java.net.URL;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import legacyfix.LegacyURLStreamHandlerFactory;
import uk.betacraft.serverlist.AccessHelper.ServerType;

public class ServerlistAgent {

    public static void premain(String agentArgs, Instrumentation inst) {
        agentmain(agentArgs, inst);
    }

    public static void agentmain(String agentArgs, Instrumentation inst) {
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        ClassReader reader = null;

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
            reader = new ClassReader(main.replace(".", "/"));
            reader.accept(new MinecraftServerVisitor("<init>", writer), ClassReader.EXPAND_FRAMES);
            byte[] cache = writer.toByteArray();
            inst.redefineClasses(new ClassDefinition(Class.forName(main), cache));
        } catch (Throwable e) {
            e.printStackTrace();
        }
        
        URL.setURLStreamHandlerFactory(new LegacyURLStreamHandlerFactory());
    }

    public static class MinecraftServerVisitor extends ClassVisitor {
        private String methodName;

        public MinecraftServerVisitor(String methodName, ClassVisitor cv) {
            super(Opcodes.ASM4, cv);
            this.cv = cv;
            this.methodName = methodName;
        }

        @Override
        public MethodVisitor visitMethod(
                int access, String name, String desc, String signature, String[] exceptions) {
            if (name.equals(methodName)) {
                return new MinecraftInitAdapter(Opcodes.ASM4, cv.visitMethod(access, name, desc, signature, exceptions), access, name, desc);
            }
            return cv.visitMethod(access, name, desc, signature, exceptions);
        }

        public static class MinecraftInitAdapter extends org.objectweb.asm.commons.AdviceAdapter {

            protected MinecraftInitAdapter(int api, MethodVisitor methodVisitor, int access, String name,
                    String descriptor) {
                super(api, methodVisitor, access, name, descriptor);
            }

            @Override
            protected void onMethodExit(final int opcode) {
                if (opcode == RETURN) {
                    mv.visitVarInsn(Opcodes.ALOAD, 0);
                    mv.visitMethodInsn(
                            Opcodes.INVOKESTATIC, 
                            "uk/betacraft/serverlist/AccessHelper", 
                            (AccessHelper.type == ServerType.NMS ? "initNMS" : "initCMMS"), 
                            "(Ljava/lang/Object;)V", 
                            false);
                }
            }
        }

    }

}
