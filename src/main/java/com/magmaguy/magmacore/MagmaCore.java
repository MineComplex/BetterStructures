package com.magmaguy.magmacore;

import com.magmaguy.magmacore.command.CommandManager;
import com.magmaguy.magmacore.dlc.ConfigurationImporter;
import com.magmaguy.magmacore.thirdparty.CustomBiomeCompatibility;
import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;

public final class MagmaCore {
    @Getter
    private static MagmaCore instance;
    @Getter
    private final JavaPlugin requestingPlugin;

    private MagmaCore(JavaPlugin requestingPlugin) {
        instance = this;
        this.requestingPlugin = requestingPlugin;
        CustomBiomeCompatibility.initializeMappings();
    }

    public static MagmaCore createInstance(JavaPlugin requestingPlugin) {
        if (instance == null) return new MagmaCore(requestingPlugin);
        else return instance;
    }

    public static void shutdown() {
        CommandManager.shutdown();
        CustomBiomeCompatibility.shutdown();
    }

    public static void initializeImporter() {
        new ConfigurationImporter();
    }

}
