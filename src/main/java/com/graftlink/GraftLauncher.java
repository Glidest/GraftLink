package com.graftlink;

import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.Mixins;

import java.io.File;
import java.lang.reflect.Method;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

public class GraftLauncher {

    public static void main(String[] args) {
        System.out.println("--------------------------------------------------");
        System.out.println("GraftLink: Breathing life into Minicraft, one Graft at a time.");
        System.out.println("--------------------------------------------------");

        // 1. Instantiate custom ClassLoader with full classpath populated
        GraftLoader graftLoader = new GraftLoader(GraftLauncher.class.getClassLoader());
        Thread.currentThread().setContextClassLoader(graftLoader);

        // 2. Find and register game jar in libs/
        File libsFolder = new File("libs");
        File gameJar = null;

        if (libsFolder.exists() && libsFolder.isDirectory()) {
            File[] jarFiles = libsFolder.listFiles((dir, name) -> name.endsWith(".jar"));
            if (jarFiles != null && jarFiles.length > 0) {
                gameJar = jarFiles[0];
            }
        }

        String mainClassName = null;

        if (gameJar != null && gameJar.exists()) {
            System.out.println("[GraftLink] Found game jar: " + gameJar.getName());
            graftLoader.addFile(gameJar);
            
            try (JarFile jar = new JarFile(gameJar)) {
                Manifest manifest = jar.getManifest();
                if (manifest != null && manifest.getMainAttributes() != null) {
                    mainClassName = manifest.getMainAttributes().getValue("Main-Class");
                }
            } catch (Exception e) {
                System.err.println("[GraftLink] Could not read manifest from " + gameJar.getName());
            }
        } else {
            System.err.println("[GraftLink] Warning: No jar file found inside 'libs/' folder.");
        }

        if (mainClassName == null || mainClassName.trim().isEmpty()) {
            mainClassName = "minicraft.core.Game";
        }

        // 3. Initialize Mixin subsystem BEFORE registering the Proxy transformer
        try {
            MixinBootstrap.init();
            Mixins.addConfiguration("mixins.graftlink.json");
            graftLoader.registerTransformer("org.spongepowered.asm.mixin.transformer.Proxy");
            MixinEnvironment.getDefaultEnvironment().setSide(MixinEnvironment.Side.CLIENT);
            System.out.println("[GraftLink] Mixin subsystem initialized successfully.");
        } catch (Throwable t) {
            System.err.println("[GraftLink] Failed to initialize Mixins.");
            t.printStackTrace();
        }

        // 4. Scan for external Grafts (mods)
        File graftsFolder = new File("grafts");
        graftLoader.locateAndRegisterGrafts(graftsFolder);

        // 5. Initialize discovered Grafts
        graftLoader.initGrafts();
        graftLoader.postInitGrafts();

        // 6. Launch Minicraft+
        System.out.println("[GraftLink] Launching Minicraft+ Revived (" + mainClassName + ")...");

        try {
            Class<?> mainClass = Class.forName(mainClassName, true, graftLoader);
            Method mainMethod = mainClass.getMethod("main", String[].class);
            mainMethod.invoke(null, (Object) args);
        } catch (ClassNotFoundException e) {
            System.err.println("[GraftLink] Could not find main class: " + mainClassName);
        } catch (Exception e) {
            System.err.println("[GraftLink] An error occurred while launching the game.");
            e.printStackTrace();
        }
    }
}