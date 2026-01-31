package com.magmaguy.magmacore.config;

import com.magmaguy.magmacore.MagmaCore;
import com.magmaguy.magmacore.util.ChatColorConverter;
import com.magmaguy.magmacore.util.Logger;
import lombok.experimental.UtilityClass;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

@UtilityClass
public class ConfigurationEngine {

    public File fileCreator(String path, String fileName) {
        File file = new File(MagmaCore.getInstance().getRequestingPlugin().getDataFolder().getPath() + "/" + path + "/", fileName);
        return fileCreator(file);
    }

    public File fileCreator(String fileName) {
        File file = new File(MagmaCore.getInstance().getRequestingPlugin().getDataFolder().getPath(), fileName);
        return fileCreator(file);
    }

    public File fileCreator(File file) {
        if (!file.exists())
            try {
                file.getParentFile().mkdirs();
                file.createNewFile();
            } catch (IOException ex) {
                Logger.warn("Error generating the plugin file: " + file.getName());
            }
        return file;
    }

    public FileConfiguration fileConfigurationCreator(File file) {
        try {
            return YamlConfiguration.loadConfiguration(new InputStreamReader(Files.newInputStream(file.toPath().normalize().toAbsolutePath()), StandardCharsets.UTF_8));
        } catch (Exception exception) {
            Logger.warn("Failed to read configuration from file " + file.getName());
            return null;
        }
    }

    public void fileSaverCustomValues(FileConfiguration fileConfiguration, File file) {
        fileConfiguration.options().copyDefaults(true);

        try {
            fileConfiguration.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void fileSaverOnlyDefaults(FileConfiguration fileConfiguration, File file) {
        fileConfiguration.options().copyDefaults(true);
        UnusedNodeHandler.clearNodes(fileConfiguration);

        try {
            fileConfiguration.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void setComments(FileConfiguration fileConfiguration, String key, List<String> comments) {
        fileConfiguration.setComments(key, comments);
    }

    public Boolean setBoolean(FileConfiguration fileConfiguration, String key, boolean defaultValue) {
        try {
            fileConfiguration.addDefault(key, defaultValue);
        } catch (Exception e) {
            Logger.warn("Attempted to write key " + key + " with value " + defaultValue + " to " + fileConfiguration.getName() + " and that contained an illegal argument!");
            e.printStackTrace();
        }
        return fileConfiguration.getBoolean(key);
    }

    public Boolean setBoolean(List<String> comments, FileConfiguration fileConfiguration, String key, boolean defaultValue) {
        boolean value = setBoolean(fileConfiguration, key, defaultValue);
        setComments(fileConfiguration, key, comments);
        return value;
    }

    public String setString(FileConfiguration fileConfiguration, String key, String defaultValue) {
        fileConfiguration.addDefault(key, defaultValue);
        return ChatColorConverter.convert(fileConfiguration.getString(key));
    }

    public String setString(List<String> comments, FileConfiguration fileConfiguration, String key, String defaultValue) {
        String value = setString(fileConfiguration, key, defaultValue);
        setComments(fileConfiguration, key, comments);
        return value;
    }


    public int setInt(FileConfiguration fileConfiguration, String key, int defaultValue) {
        fileConfiguration.addDefault(key, defaultValue);
        return fileConfiguration.getInt(key);
    }

    public int setInt(List<String> comments, FileConfiguration fileConfiguration, String key, int defaultValue) {
        int value = setInt(fileConfiguration, key, defaultValue);
        setComments(fileConfiguration, key, comments);
        return value;
    }

    public double setDouble(FileConfiguration fileConfiguration, String key, double defaultValue) {
        fileConfiguration.addDefault(key, defaultValue);
        return fileConfiguration.getDouble(key);
    }

    public double setDouble(List<String> comments, FileConfiguration fileConfiguration, String key, double defaultValue) {
        double value = setDouble(fileConfiguration, key, defaultValue);
        setComments(fileConfiguration, key, comments);
        return value;
    }

    public List setList(FileConfiguration fileConfiguration, String key, List defaultValue) {
        fileConfiguration.addDefault(key, defaultValue);
        return fileConfiguration.getList(key);
    }

    public List setList(List<String> comment, FileConfiguration fileConfiguration, String key, List defaultValue) {
        List value = setList(fileConfiguration, key, defaultValue);
        setComments(fileConfiguration, key, comment);
        return value;
    }

    public boolean writeValue(Object value, File file, FileConfiguration fileConfiguration, String path) {
        fileConfiguration.set(path, value);
        try {
            fileSaverCustomValues(fileConfiguration, file);
        } catch (Exception exception) {
            Logger.warn("Failed to write value for " + path + " in file " + file.getName());
            return false;
        }
        return true;
    }

    public void removeValue(File file, FileConfiguration fileConfiguration, String path) {
        writeValue(null, file, fileConfiguration, path);
    }
}
