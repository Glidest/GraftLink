package com.graftlink;

import com.graftlink.api.Graft;
import com.graftlink.api.util.GraftLogger;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.concurrent.CopyOnWriteArrayList;

public class GraftLoader extends LaunchClassLoader {

    private static final GraftLogger LOGGER = new GraftLogger("GraftLink");
    private static final String SERVICE_PATH = "META-INF/services/com.graftlink.api.Graft";
    private static final String MIXIN_CONFIG_PREFIX = "mixins.";

    private final List<Graft> loadedGrafts = new CopyOnWriteArrayList<>();
    private final List<File> graftFiles = new CopyOnWriteArrayList<>();

    public GraftLoader(ClassLoader parent) {
        super(getSystemURLs(parent));

        Launch.classLoader = this;
        if (Launch.blackboard == null) {
            Launch.blackboard = new HashMap<>();
        }

        // Exclude GraftLink API package so all Grafts share the exact same interface instance
        this.addClassLoaderExclusion("com.graftlink.api.");

        LOGGER.info("GraftLoader initialized with parent: " + (parent != null ? parent.getClass().getName() : "null"));
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
                } catch (Exception e) {
                    LOGGER.warn("Failed to convert classpath entry to URL: " + path + " (" + e.getClass().getSimpleName() + ")");
                }
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
            } catch (Exception e) {
                LOGGER.warn("Could not clear LaunchClassLoader negativeResourceCache: " + e.getMessage());
            }
        } catch (IOException e) {
            LOGGER.error("[GraftLink] Failed to add file to classpath: " + file.getName(), e);
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
            } catch (Exception e) {
                LOGGER.warn("Error reading class bytes for " + name + " from " + jarFile.getName() + ": " + e.getMessage());
            }
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
            } catch (Exception e) {
                LOGGER.warn("Failed to locate resource " + name + " in " + jarFile.getName() + ": " + e.getMessage());
            }
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
            LOGGER.info("[GraftLink] Found and loaded Graft jar: " + file.getName());

            try (JarFile jarFile = new JarFile(file)) {
                var entries = jarFile.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    String name = entry.getName();
                    if (name.startsWith(MIXIN_CONFIG_PREFIX) && name.endsWith(".json")) {
                        Mixins.addConfiguration(name);
                        LOGGER.info("[GraftLink] Registered Graft Mixin configuration: " + name);
                    }
                }
            } catch (Exception e) {
                LOGGER.error("[GraftLink] Failed to scan Mixins in Graft: " + file.getName(), e);
            }
        }
    }

    public void initGrafts() {
        for (File jarFile : graftFiles) {
            boolean initializedAny = false;

            // 1. Try reading service provider file defined by SERVICE_PATH
            try (JarFile jar = new JarFile(jarFile)) {
                JarEntry entry = jar.getJarEntry(SERVICE_PATH);
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
                                        LOGGER.info("[GraftLink] Successfully initialized Graft: " + line);
                                    }
                                } catch (Exception e) {
                                    LOGGER.error("[GraftLink] Failed to instantiate Graft class: " + line, e);
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                LOGGER.warn("[GraftLink] Error reading service provider from " + jarFile.getName() + ": " + e.getMessage());
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
                                Class<?> clazz = Class.forName(className, false, this);
                                if (Graft.class.isAssignableFrom(clazz) && !clazz.isInterface()) {
                                    try {
                                        Class<? extends Graft> graftClass = clazz.asSubclass(Graft.class);
                                        Graft graft = graftClass.getDeclaredConstructor().newInstance();
                                        graft.onInitialize();
                                        loadedGrafts.add(graft);
                                        initializedAny = true;
                                        LOGGER.info("[GraftLink] Auto-discovered and initialized Graft: " + className);
                                    } catch (Throwable t) {
                                        LOGGER.warn("[GraftLink] Failed to initialize discovered Graft: " + className + " - " + t.getMessage());
                                    }
                                }
                            } catch (Throwable t) {
                                // Log and continue; many classes may fail to load, that's expected
                                LOGGER.warn("Skipping class during auto-discovery: " + className + " (" + t.getClass().getSimpleName() + ")");
                            }
                        }
                    }
                } catch (Exception e) {
                    LOGGER.warn("Error during auto-discovery in " + jarFile.getName() + ": " + e.getMessage());
                }
            }

            if (!initializedAny) {
                LOGGER.warn("[GraftLink] Warning: No class implementing 'Graft' was found in " + jarFile.getName());
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
                LOGGER.error("[GraftLink] Error during post-initialization of " + graft.getClass().getName(), t);
            }
        }
    }

    public List<Graft> getLoadedGrafts() {
        return Collections.unmodifiableList(loadedGrafts);
    }
}
