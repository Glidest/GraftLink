package com.graftlink;

import com.graftlink.api.Graft;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;
import org.spongepowered.asm.mixin.Mixins;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class GraftLoader extends LaunchClassLoader {

    private final List<Graft> loadedGrafts = new ArrayList<>();
    private final List<File> graftFiles = new ArrayList<>();

    public GraftLoader(ClassLoader parent) {
        super(getSystemURLs(parent));
        
        Launch.classLoader = this;

        if (Launch.blackboard == null) {
            Launch.blackboard = new HashMap<>();
        }

        // Exclude GraftLink API package so all Grafts share the exact same interface instance
        this.addClassLoaderExclusion("com.graftlink.api.");
    }

    private static URL[] getSystemURLs(ClassLoader parent) {
        List<URL> urls = new ArrayList<>();

        if (parent instanceof URLClassLoader) {
            for (URL url : ((URLClassLoader) parent).getURLs()) {
                urls.add(url);
            }
        }

        String classPath = System.getProperty("java.class.path");
        if (classPath != null) {
            for (String path : classPath.split(File.pathSeparator)) {
                try {
                    URL url = new File(path).getAbsoluteFile().toURI().toURL();
                    if (!urls.contains(url)) {
                        urls.add(url);
                    }
                } catch (Exception ignored) {}
            }
        }

        return urls.toArray(new URL[0]);
    }

    public void addFile(File file) {
        try {
            File absoluteFile = file.getAbsoluteFile();
            this.addURL(absoluteFile.toURI().toURL());
            
            try {
                Field field = LaunchClassLoader.class.getDeclaredField("negativeResourceCache");
                field.setAccessible(true);
                Set<?> cache = (Set<?>) field.get(this);
                if (cache != null) {
                    cache.clear();
                }
            } catch (Exception ignored) {}
        } catch (IOException e) {
            System.err.println("[GraftLink] Failed to add file to classpath: " + file.getName());
            e.printStackTrace();
        }
    }

    @Override
    public byte[] getClassBytes(String name) throws IOException {
        String resourcePath = name.replace('.', '/').concat(".class");
        for (File jarFile : graftFiles) {
            try (JarFile jar = new JarFile(jarFile)) {
                JarEntry entry = jar.getJarEntry(resourcePath);
                if (entry != null) {
                    try (var is = jar.getInputStream(entry)) {
                        return is.readAllBytes();
                    }
                }
            } catch (Exception ignored) {}
        }

        return super.getClassBytes(name);
    }

    @Override
    public URL findResource(String name) {
        for (File jarFile : graftFiles) {
            try (JarFile jar = new JarFile(jarFile)) {
                JarEntry entry = jar.getJarEntry(name);
                if (entry != null) {
                    return new URL("jar:" + jarFile.toURI().toURL() + "!/" + name);
                }
            } catch (Exception ignored) {}
        }
        return super.findResource(name);
    }

    public void locateAndRegisterGrafts(File graftsDir) {
        if (!graftsDir.exists()) {
            graftsDir.mkdirs();
            return;
        }

        File[] files = graftsDir.listFiles((dir, name) -> name.endsWith(".jar"));
        if (files == null) return;

        for (File file : files) {
            addFile(file);
            graftFiles.add(file.getAbsoluteFile());
            System.out.println("[GraftLink] Found and loaded Graft jar: " + file.getName());

            try (JarFile jarFile = new JarFile(file)) {
                var entries = jarFile.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    String name = entry.getName();
                    if (name.startsWith("mixins.") && name.endsWith(".json")) {
                        Mixins.addConfiguration(name);
                        System.out.println("[GraftLink] Registered Graft Mixin configuration: " + name);
                    }
                }
            } catch (Exception e) {
                System.err.println("[GraftLink] Failed to scan Mixins in Graft: " + file.getName());
            }
        }
    }

    public void initGrafts() {
        for (File jarFile : graftFiles) {
            boolean initializedAny = false;

            // 1. Try reading META-INF/services/com.graftlink.api.Graft
            try (JarFile jar = new JarFile(jarFile)) {
                JarEntry entry = jar.getJarEntry("META-INF/services/com.graftlink.api.Graft");
                if (entry != null) {
                    try (var is = jar.getInputStream(entry);
                         var reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            line = line.trim();
                            if (!line.isEmpty() && !line.startsWith("#")) {
                                try {
                                    Class<?> clazz = Class.forName(line, true, this);
                                    if (Graft.class.isAssignableFrom(clazz)) {
                                        Graft graft = (Graft) clazz.getDeclaredConstructor().newInstance();
                                        graft.onInitialize();
                                        loadedGrafts.add(graft);
                                        initializedAny = true;
                                        System.out.println("[GraftLink] Successfully initialized Graft: " + line);
                                    }
                                } catch (Exception e) {
                                    System.err.println("[GraftLink] Failed to instantiate Graft class: " + line);
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("[GraftLink] Error reading service provider from " + jarFile.getName());
            }

            // 2. Fallback auto-discovery if service entry was missing
            if (!initializedAny) {
                try (JarFile jar = new JarFile(jarFile)) {
                    var entries = jar.entries();
                    while (entries.hasMoreElements()) {
                        JarEntry entry = entries.nextElement();
                        String name = entry.getName();
                        if (name.endsWith(".class") && !name.contains("$")) {
                            String className = name.replace('/', '.').replace(".class", "");
                            try {
                                Class<?> clazz = Class.forName(className, true, this);
                                if (Graft.class.isAssignableFrom(clazz) && !clazz.isInterface()) {
                                    Graft graft = (Graft) clazz.getDeclaredConstructor().newInstance();
                                    graft.onInitialize();
                                    loadedGrafts.add(graft);
                                    initializedAny = true;
                                    System.out.println("[GraftLink] Auto-discovered and initialized Graft: " + className);
                                }
                            } catch (Throwable ignored) {}
                        }
                    }
                } catch (Exception ignored) {}
            }

            if (!initializedAny) {
                System.out.println("[GraftLink] Warning: No class implementing 'Graft' was found in " + jarFile.getName());
            }
        }
    }

    /**
     * Executes post-initialization phase for all loaded Grafts.
     */
    public void postInitGrafts() {
        for (Graft graft : loadedGrafts) {
            try {
                graft.onPostInitialize();
            } catch (Throwable t) {
                System.err.println("[GraftLink] Error during post-initialization of " + graft.getClass().getName());
                t.printStackTrace();
            }
        }
    }

    public List<Graft> getLoadedGrafts() {
        return loadedGrafts;
    }
}