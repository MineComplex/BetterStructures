package com.magmaguy.betterstructures.buildingfitter;

import com.magmaguy.betterstructures.buildingfitter.util.LocationProjector;
import com.magmaguy.betterstructures.buildingfitter.util.SchematicPicker;
import com.magmaguy.betterstructures.config.DefaultConfig;
import com.magmaguy.betterstructures.config.generators.GeneratorConfigFields;
import com.magmaguy.betterstructures.schematics.SchematicContainer;
import com.magmaguy.betterstructures.util.SurfaceMaterials;
import com.magmaguy.betterstructures.util.WorldEditUtils;
import com.magmaguy.betterstructures.worldedit.Schematic;
import com.magmaguy.magmacore.util.Logger;
import com.magmaguy.magmacore.util.SpigotMessage;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.math.BlockVector3;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Container;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;

@NoArgsConstructor
public class FitAnything {

    protected final int searchRadius = 1;
    protected final int scanStep = 3;

    @Getter
    protected Location location;
    protected GeneratorConfigFields.StructureType structureType;

    private final HashMap<Material, Integer> surfacePedestalMaterials = new HashMap<>();
    private final HashMap<Material, Integer> undergroundPedestalMaterials = new HashMap<>();
    private Material pedestalMaterial;

    @Getter
    protected SchematicContainer schematicContainer;
    @Getter
    protected Vector schematicOffset;
    protected int verticalOffset = 0;

    //At 10% it is assumed a fit is so bad it's better just to skip
    protected double startingScore = 100;
    protected double highestScore = 10;

    public FitAnything(SchematicContainer schematicContainer) {
        this.schematicContainer = schematicContainer;
        this.verticalOffset = schematicContainer.getClipboard().getMinimumPoint().y() -
                schematicContainer.getClipboard().getOrigin().y();
    }

    public static void commandBasedCreation(Chunk chunk, GeneratorConfigFields.StructureType structureType, SchematicContainer container) {
        switch (structureType) {
            case SKY:
                new FitAirBuilding(chunk, container);
                break;
            case SURFACE:
                new FitSurfaceBuilding(chunk, container);
                break;
            case LIQUID_SURFACE:
                new FitLiquidBuilding(chunk, container);
                break;
            case UNDERGROUND_DEEP:
                FitUndergroundDeepBuilding.fit(chunk, container);
                break;
            case UNDERGROUND_SHALLOW:
                FitUndergroundShallowBuilding.fit(chunk, container);
                break;
            default:
        }
    }

    protected void randomizeSchematicContainer(Location location, GeneratorConfigFields.StructureType structureType) {
        if (schematicContainer != null)
            return;

        schematicContainer = SchematicPicker.pick(location, structureType);
        if (schematicContainer != null) {
            verticalOffset = schematicContainer.getClipboard().getMinimumPoint().y() - schematicContainer.getClipboard().getOrigin().y();
        }
    }

    protected void paste(Location location) {
        FitAnything fitAnything = this;

        // Set pedestal material before the paste so bedrock blocks get replaced correctly
        assignPedestalMaterial(location);
        if (pedestalMaterial == null)
            switch (location.getWorld().getEnvironment()) {
                case NETHER:
                    pedestalMaterial = Material.NETHERRACK;
                    break;
                case THE_END:
                    pedestalMaterial = Material.END_STONE;
                    break;
                default:
                    pedestalMaterial = Material.STONE;
            }

        // Create a function to provide pedestal material
        Function<Boolean, Material> pedestalMaterialProvider = this::getPedestalMaterial;

        // Paste the schematic with the moved logic
        Schematic.pasteSchematic(
                schematicContainer.getClipboard(),
                location,
                schematicOffset,
                pedestalMaterialProvider,
                onPasteComplete(fitAnything, location)
        );
    }

    private BukkitRunnable onPasteComplete(FitAnything fitAnything, Location location) {
        return new BukkitRunnable() {
            @Override
            public void run() {
                if (DefaultConfig.isNewBuildingWarn()) {
                    String structureTypeString = fitAnything.structureType.toString().toLowerCase().replace("_", " ");
                    for (Player player : Bukkit.getOnlinePlayers())
                        if (player.hasPermission("betterstructures.warn"))
                            player.spigot().sendMessage(
                                    SpigotMessage.commandHoverMessage("[BetterStructures] New " + structureTypeString + " building generated! Click to teleport. Do \"/bs silent\" to stop getting warnings!",
                                            "Click to teleport to " + location.getWorld().getName() + ", " + location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ() + "\n Schem name: " + schematicContainer.getConfigFilename(),
                                            "/bs teleport " + location.getWorld().getName() + " " + location.getBlockX() + " " + location.getBlockY() + " " + location.getBlockZ())
                            );
                }

                if (!(fitAnything instanceof FitAirBuilding)) {
                    try {
                        addPedestal(location);
                    } catch (Exception exception) {
                        Logger.warn("Failed to correctly assign pedestal material!");
                        exception.printStackTrace();
                    }
                    try {
                        if (fitAnything instanceof FitSurfaceBuilding)
                            clearTrees(location);
                    } catch (Exception exception) {
                        Logger.warn("Failed to correctly clear trees!");
                        exception.printStackTrace();
                    }
                }
                try {
                    fillChests();
                } catch (Exception exception) {
                    Logger.warn("Failed to correctly fill chests!");
                    exception.printStackTrace();
                }
            }
        };
    }

    private void assignPedestalMaterial(Location location) {
        if (this instanceof FitAirBuilding)
            return;

        pedestalMaterial = schematicContainer.getSchematicConfigField().getPedestalMaterial();
        Location lowestCorner = location.clone().add(schematicOffset);

        int maxSurfaceHeightScan = 20;

        //get underground pedestal blocks

        BlockVector3 dimensions = schematicContainer.getClipboard().getDimensions();
        for (int x = 0; x < dimensions.x(); x++)
            for (int z = 0; z < dimensions.z(); z++)
                for (int y = 0; y < dimensions.y(); y++) {
                    Block groundBlock = lowestCorner.clone().add(x, y, z).getBlock();
                    Block aboveBlock = groundBlock.getRelative(BlockFace.UP);

                    if (aboveBlock.getType().isSolid() && groundBlock.getType().isSolid() && !SurfaceMaterials.ignorable(groundBlock.getType()))
                        undergroundPedestalMaterials.merge(groundBlock.getType(), 1, Integer::sum);
                }

        //get above ground pedestal blocks, if any
        for (int x = 0; x < dimensions.x(); x++)
            for (int z = 0; z < dimensions.z(); z++) {
                boolean scanUp = lowestCorner.clone().add(x, dimensions.y(), z).getBlock().getType().isSolid();
                for (int y = 0; y < maxSurfaceHeightScan; y++) {
                    Block groundBlock = lowestCorner.clone().add(x, scanUp ? y : -y, z).getBlock();
                    Block aboveBlock = groundBlock.getRelative(BlockFace.UP);

                    if (!aboveBlock.getType().isSolid() && groundBlock.getType().isSolid()) {
                        surfacePedestalMaterials.merge(groundBlock.getType(), 1, Integer::sum);
                        break;
                    }
                }
            }
    }

    private Material getPedestalMaterial(boolean isPedestalSurface) {
        if (isPedestalSurface) {
            if (surfacePedestalMaterials.isEmpty())
                return pedestalMaterial;
            return getRandomMaterialBasedOnWeight(surfacePedestalMaterials);
        } else {
            if (undergroundPedestalMaterials.isEmpty())
                return pedestalMaterial;
            return getRandomMaterialBasedOnWeight(undergroundPedestalMaterials);
        }
    }

    public Material getRandomMaterialBasedOnWeight(HashMap<Material, Integer> weightedMaterials) {
        // Calculate the total weight
        int totalWeight = weightedMaterials.values().stream().mapToInt(Integer::intValue).sum();

        // Generate a random number in the range of 0 (inclusive) to totalWeight (exclusive)
        int randomNumber = ThreadLocalRandom.current().nextInt(totalWeight);

        // Iterate through the materials and pick one based on the random number
        int cumulativeWeight = 0;
        for (Map.Entry<Material, Integer> entry : weightedMaterials.entrySet()) {
            cumulativeWeight += entry.getValue();
            if (randomNumber < cumulativeWeight) {
                return entry.getKey();
            }
        }

        // Fallback return, should not occur if the map is not empty and weights are positive
        throw new IllegalStateException("Weighted random selection failed.");
    }

    private void addPedestal(Location location) {
        if (this instanceof FitAirBuilding || this instanceof FitLiquidBuilding)
            return;

        Location lowestCorner = location.clone().add(schematicOffset);
        BlockVector3 clipboard = schematicContainer.getClipboard().getDimensions();

        for (int x = 0; x < clipboard.x(); x++)
            for (int z = 0; z < clipboard.z(); z++) {
                //Only add pedestals for areas with a solid floor, some schematics can have rounded air edges to better fit terrain
                Block groundBlock = lowestCorner.clone().add(x, 0, z).getBlock();
                if (groundBlock.getType().isAir())
                    continue;

                for (int y = -1; y > -11; y--) {
                    Block block = lowestCorner.clone().add(x, y, z).getBlock();
                    if (SurfaceMaterials.ignorable(block.getType()))
                        block.setType(getPedestalMaterial(!block.getRelative(BlockFace.UP).getType().isSolid()));
                    else {
                        //Pedestal only fills until it hits the first solid block
                        break;
                    }
                }
            }
    }

    private void clearTrees(Location location) {
        BlockVector3 clipboard = schematicContainer.getClipboard().getDimensions();
        Location highestCorner = location.clone().add(schematicOffset).add(0, clipboard.y() + 1, 0);
        boolean detectedTreeElement = true;
        for (int x = 0; x < clipboard.x(); x++)
            for (int z = 0; z < clipboard.z(); z++) {
                for (int y = 0; y < 31; y++) {
                    if (!detectedTreeElement)
                        break;

                    detectedTreeElement = false;
                    Block block = highestCorner.clone().add(x, y, z).getBlock();
                    if (SurfaceMaterials.ignorable(block.getType()) && !block.getType().isAir()) {
                        detectedTreeElement = true;
                        block.setType(Material.AIR);
                    }
                }
            }
    }

    private void fillChests() {
        if (schematicContainer.getGeneratorConfigFields().getChestContents() != null)
            for (Vector chestPosition : schematicContainer.getChestLocations()) {
                Location chestLocation = LocationProjector.project(location, schematicOffset, chestPosition);
                if (!(chestLocation.getBlock().getState() instanceof Container container)) {
                    Logger.warn("Expected a container for " + chestLocation.getBlock().getType() + " but didn't get it. Skipping this loot!");
                    continue;
                }

                if (schematicContainer.getChestContents() != null) {
                    schematicContainer.getChestContents().rollChestContents(container);
                } else {
                    schematicContainer.getGeneratorConfigFields().getChestContents().rollChestContents(container);
                }
                container.update(true);
            }
    }

}
