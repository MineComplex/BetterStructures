package com.magmaguy.betterstructures.schematics;

import com.google.common.collect.ArrayListMultimap;
import com.magmaguy.betterstructures.chests.ChestContents;
import com.magmaguy.betterstructures.config.generators.GeneratorConfigFields;
import com.magmaguy.betterstructures.config.schematics.SchematicConfigField;
import com.magmaguy.betterstructures.config.treasures.TreasureConfig;
import com.magmaguy.betterstructures.config.treasures.TreasureConfigFields;
import com.magmaguy.betterstructures.util.WorldEditUtils;
import com.magmaguy.magmacore.util.Logger;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.entity.EntityType;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Getter
public class SchematicContainer {

    @Getter
    private static final ArrayListMultimap<GeneratorConfigFields.StructureType, SchematicContainer> schematics = ArrayListMultimap.create();

    private final Clipboard clipboard;
    private final SchematicConfigField schematicConfigField;
    private final GeneratorConfigFields generatorConfigFields;
    private final String clipboardFilename;
    private final String configFilename;
    private final List<Vector> chestLocations = new ArrayList<>();
    private ChestContents chestContents;

    public SchematicContainer(Clipboard clipboard, String clipboardFilename, SchematicConfigField schematicConfigField, String configFilename) {
        this.clipboard = clipboard;
        this.clipboardFilename = clipboardFilename;
        this.schematicConfigField = schematicConfigField;
        this.configFilename = configFilename;
        generatorConfigFields = schematicConfigField.getGeneratorConfigFields();
        if (generatorConfigFields == null) {
            Logger.warn("Failed to assign generator for configuration of schematic " + schematicConfigField.getFilename() + " ! This means this structure will not appear in the world.");
            return;
        }
        for (int x = 0; x <= clipboard.getDimensions().x(); x++)
            for (int y = 0; y <= clipboard.getDimensions().y(); y++)
                for (int z = 0; z <= clipboard.getDimensions().z(); z++) {
                    BlockVector3 translatedLocation = BlockVector3.at(x, y, z).add(clipboard.getMinimumPoint());
                    BlockState weBlockState = clipboard.getBlock(translatedLocation);
                    Material minecraftMaterial = BukkitAdapter.adapt(weBlockState.getBlockType());
                    if (minecraftMaterial == null) continue;

                    //register chest location
                    if (minecraftMaterial.equals(Material.CHEST) ||
                            minecraftMaterial.equals(Material.TRAPPED_CHEST) ||
                            minecraftMaterial.equals(Material.SHULKER_BOX)) {
                        chestLocations.add(new Vector(x, y, z));
                    }
                }

        chestContents = generatorConfigFields.getChestContents();
        if (schematicConfigField.getTreasureFile() != null && !schematicConfigField.getTreasureFile().isEmpty()) {
            TreasureConfigFields treasureConfigFields = TreasureConfig.getConfigFields(schematicConfigField.getFilename());
            if (treasureConfigFields == null) {
                Logger.warn("Failed to get treasure configuration " + schematicConfigField.getTreasureFile());
                return;
            }
            chestContents = schematicConfigField.getChestContents();
        }

        generatorConfigFields.getStructureTypes().forEach(structureType -> schematics.put(structureType, this));
    }

    public static void shutdown() {
        schematics.clear();
    }

    public boolean isValidEnvironment(World.Environment environment) {
        return generatorConfigFields.getValidWorldEnvironments() == null ||
                generatorConfigFields.getValidWorldEnvironments().isEmpty() ||
                generatorConfigFields.getValidWorldEnvironments().contains(environment);
    }

    /**
     * Validates if a biome is in the list of valid biomes, handling both newer interface-based
     * biomes and older class-based biomes.
     *
     * @param biome The biome to validate
     * @return True if the biome is valid, false otherwise
     */
    public boolean isValidBiome(Biome biome) {
        if (generatorConfigFields.getValidBiomesNamespaces() == null) return true;
        if (generatorConfigFields.getValidBiomesNamespaces().isEmpty()) return true;

        // Extract biome identifier based on version
        String biomeString = biome.getKey().toString();

        for (String validBiome : generatorConfigFields.getValidBiomesNamespaces()) {
            if (biomeString.equals(validBiome)) {
                return true;
            }
        }

        return false;
    }

    public boolean isValidYLevel(int yLevel) {
        return generatorConfigFields.getLowestYLevel() <= yLevel && generatorConfigFields.getHighestYLevel() >= yLevel;
    }

    public boolean isValidWorld(String worldName) {
        return generatorConfigFields.getValidWorlds() == null ||
                generatorConfigFields.getValidWorlds().isEmpty() ||
                generatorConfigFields.getValidWorlds().contains(worldName);
    }
}
