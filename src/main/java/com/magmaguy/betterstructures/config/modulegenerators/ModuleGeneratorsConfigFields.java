package com.magmaguy.betterstructures.config.modulegenerators;

import com.magmaguy.betterstructures.config.modules.ModulesConfig;
import com.magmaguy.betterstructures.config.modules.ModulesConfigFields;
import com.magmaguy.magmacore.config.CustomConfigFields;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.List;

@Getter
public class ModuleGeneratorsConfigFields extends CustomConfigFields {
    protected int radius;
    protected boolean edges;
    protected List<String> startModules;
    protected int minChunkY;
    protected int maxChunkY;
    protected int moduleSizeXZ;
    protected int moduleSizeY;
    protected boolean debug;
    protected boolean useGradientLevels;
    protected String spawnPoolSuffix;
    protected boolean isWorldGeneration;
    protected String treasureFile;
    @Setter
    protected int centerModuleAltitude = 0;
    @Setter
    private List<String> validWorlds = null;
    @Setter
    private List<World.Environment> validWorldEnvironments = null;

    public ModuleGeneratorsConfigFields(String filename, boolean isEnabled) {
        super(filename, isEnabled);
    }

    public ModuleGeneratorsConfigFields(String filename) {
        super(filename, true);
    }

    public List<String> getStartModules() {
        List<String> existingModules = new ArrayList<>();
        for (ModulesConfigFields value : ModulesConfig.getModuleConfigurations().values())
            if (startModules.contains(value.getFilename().replace(".yml", ".schem")))
                existingModules.add(value.getFilename().replace(".yml", ".schem"));
        return existingModules;
    }

    @Override
    public void processConfigFields() {
        this.radius = processInt("radius", radius, 1, true);
        this.edges = processBoolean("edges", edges, false, true);
        this.startModules = processStringList("startModule", startModules, null, true);
        this.minChunkY = processInt("minChunkY", minChunkY, 0, true);
        this.maxChunkY = processInt("maxChunkY", maxChunkY, 0, true);
        this.moduleSizeXZ = processInt("moduleSizeXZ", moduleSizeXZ, 16, true);
        this.moduleSizeY = processInt("moduleSizeY", moduleSizeY, 16, true);
        this.debug = processBoolean("debug", debug, false, true);
        this.useGradientLevels = processBoolean("useGradientLevels", useGradientLevels, useGradientLevels, true);
        this.spawnPoolSuffix = processString("spawnPoolSuffix", spawnPoolSuffix, spawnPoolSuffix, true);
        this.isWorldGeneration = processBoolean("isWorldGeneration", isWorldGeneration, isWorldGeneration, true);
        this.treasureFile = processString("treasureFile", treasureFile, null, false);
        this.validWorlds = processStringList("validWorlds", validWorlds, new ArrayList<>(), false);
        this.validWorldEnvironments = processEnumList("validWorldEnvironments", validWorldEnvironments, null, World.Environment.class, false);
        this.centerModuleAltitude = processInt("centerModuleAltitude", centerModuleAltitude, 0, false);
    }
}
