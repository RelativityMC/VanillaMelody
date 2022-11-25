package com.ishland.vanillamelody.common;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Properties;

public class Config {

    static {
        reload();
    }

    public static boolean DEFAULT_RADIO_ENABLED;
    public static boolean ENABLE_SERVERSIDE_PLAYBACK;
    public static boolean ALLOW_NON_OPERATOR_RADIO_CHANGE;

    public static void init() {
    }

    public static void reload() {
        final Properties properties = new Properties();
        final Properties newProperties = new Properties();
        final Path path = FabricLoader.getInstance().getConfigDir().resolve("vanillamelody").resolve("config.properties");
        path.getParent().toFile().mkdirs();
        if (Files.isRegularFile(path)) {
            try (InputStream in = Files.newInputStream(path, StandardOpenOption.CREATE)) {
                properties.load(in);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        DEFAULT_RADIO_ENABLED = getBoolean(properties, newProperties, "default_radio_enabled", true);
        ENABLE_SERVERSIDE_PLAYBACK = getBoolean(properties, newProperties, "enable_serverside_playback", true);
        ALLOW_NON_OPERATOR_RADIO_CHANGE = getBoolean(properties, newProperties, "allow_non_operator_radio_change", false);

        try (OutputStream out = Files.newOutputStream(path, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            newProperties.store(out, "Configuration file");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static int getInt(Properties properties, Properties newProperties, String key, int def) {
        try {
            final int i = Integer.parseInt(properties.getProperty(key));
            newProperties.setProperty(key, String.valueOf(i));
            return i;
        } catch (NumberFormatException e) {
            newProperties.setProperty(key, String.valueOf(def));
            return def;
        }
    }

    private static boolean getBoolean(Properties properties, Properties newProperties, String key, boolean def) {
        try {
            final boolean b = parseBoolean(properties.getProperty(key));
            newProperties.setProperty(key, String.valueOf(b));
            return b;
        } catch (NumberFormatException e) {
            newProperties.setProperty(key, String.valueOf(def));
            return def;
        }
    }

    private static boolean parseBoolean(String string) {
        if (string == null) throw new NumberFormatException("null");
        if (string.trim().equalsIgnoreCase("true")) return true;
        if (string.trim().equalsIgnoreCase("false")) return false;
        throw new NumberFormatException(string);
    }

}
