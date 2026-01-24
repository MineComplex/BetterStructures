package com.magmaguy.betterstructures.buildingfitter.util;

import com.magmaguy.betterstructures.util.SurfaceMaterials;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.math.BlockVector3;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.util.Vector;

public class TerrainAdequacy {
    public static double scan(int scanStep, Clipboard schematicClipboard, Location iteratedLocation, Vector schematicOffset, ScanType scanType) {
        int width = schematicClipboard.getDimensions().x();
        int depth = schematicClipboard.getDimensions().z();
        int height = schematicClipboard.getDimensions().y();

        int totalCount = 0;
        int negativeCount = 0;

        for (int x = 0; x < width; x += scanStep) {
            for (int y = 0; y < height; y += scanStep) {
                for (int z = 0; z < depth; z += scanStep) {
                    Material schematicMaterialAtPosition = BukkitAdapter.adapt(schematicClipboard.getBlock(BlockVector3.at(x, y, z)).getBlockType());
                    Location projectedLocation = LocationProjector.project(iteratedLocation, new Vector(x, y, z), schematicOffset);
                    if (!isBlockAdequate(projectedLocation, schematicMaterialAtPosition, iteratedLocation.getBlockY() - 1, scanType))
                        negativeCount++;
                    totalCount++;
                }
            }
        }

        return 100 - negativeCount * 100D / (double) totalCount;
    }

    private static boolean isBlockAdequate(Location projectedWorldLocation, Material schematicBlockMaterial, int floorHeight, ScanType scanType) {
        int floorYValue = projectedWorldLocation.getBlockY();
        Block block = projectedWorldLocation.getBlock();
        Material type = block.getType();
        if (type == Material.VOID_AIR)
            return false;

        switch (scanType) {
            case SURFACE:
                if (floorYValue > floorHeight)
                    //for air level
                    return SurfaceMaterials.ignorable(type) || !schematicBlockMaterial.isAir();
                else
                    //for underground level
                    return !type.isAir();
            case AIR:
                return type.isAir();
            case UNDERGROUND:
                return type.isSolid();
            case LIQUID:
                if (floorYValue > floorHeight) {
                    //for air level
                    return type.isAir();
                } else {
                    //for underwater level
                    if (schematicBlockMaterial == Material.WATER || schematicBlockMaterial == Material.LAVA)
                        return block.isLiquid();
                    else
                        return true;
                }
            default:
                return false;
        }

    }

    public enum ScanType {
        SURFACE,
        UNDERGROUND,
        AIR,
        LIQUID
    }
}
