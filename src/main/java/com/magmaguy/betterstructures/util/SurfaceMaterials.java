package com.magmaguy.betterstructures.util;

import org.bukkit.Material;

import java.util.EnumSet;

public class SurfaceMaterials {

    private static final EnumSet<Material> IGNORABLE = EnumSet.of(
            Material.ACACIA_LOG,
            Material.BIRCH_LOG,
            Material.DARK_OAK_LOG,
            Material.JUNGLE_LOG,
            Material.MANGROVE_LOG,
            Material.OAK_LOG,
            Material.SPRUCE_LOG,

            Material.STRIPPED_ACACIA_LOG,
            Material.STRIPPED_BIRCH_LOG,
            Material.STRIPPED_DARK_OAK_LOG,
            Material.STRIPPED_JUNGLE_LOG,
            Material.STRIPPED_MANGROVE_LOG,
            Material.STRIPPED_OAK_LOG,
            Material.STRIPPED_SPRUCE_LOG,

            Material.ACACIA_WOOD,
            Material.BIRCH_WOOD,
            Material.JUNGLE_WOOD,
            Material.DARK_OAK_WOOD,
            Material.OAK_WOOD,
            Material.MANGROVE_WOOD,
            Material.SPRUCE_WOOD,

            Material.STRIPPED_ACACIA_WOOD,
            Material.STRIPPED_BIRCH_WOOD,
            Material.STRIPPED_DARK_OAK_WOOD,
            Material.STRIPPED_JUNGLE_WOOD,
            Material.STRIPPED_MANGROVE_WOOD,
            Material.STRIPPED_OAK_WOOD,
            Material.STRIPPED_SPRUCE_WOOD,

            Material.MUSHROOM_STEM,
            Material.BROWN_MUSHROOM_BLOCK,
            Material.RED_MUSHROOM_BLOCK,

            Material.SUGAR_CANE,
            Material.BAMBOO,
            Material.TALL_GRASS,
            Material.SHORT_GRASS,

            Material.VINE,
            Material.WEEPING_VINES,
            Material.WEEPING_VINES_PLANT,
            Material.TWISTING_VINES,
            Material.TWISTING_VINES_PLANT,
            Material.CAVE_VINES,
            Material.CAVE_VINES_PLANT,

            Material.FLOWERING_AZALEA,
            Material.FLOWERING_AZALEA_LEAVES,
            Material.AZALEA_LEAVES,

            Material.CHORUS_FLOWER,
            Material.CHORUS_PLANT,

            Material.ACACIA_LEAVES,
            Material.BIRCH_LEAVES,
            Material.DARK_OAK_LEAVES,
            Material.JUNGLE_LEAVES,
            Material.MANGROVE_LEAVES,
            Material.OAK_LEAVES,
            Material.SPRUCE_LEAVES,

            Material.DEAD_BUSH,
            Material.SWEET_BERRY_BUSH,

            Material.ROSE_BUSH,
            Material.PEONY,
            Material.LILAC,
            Material.SUNFLOWER,

            Material.POPPY,
            Material.DANDELION,
            Material.BLUE_ORCHID,
            Material.ALLIUM,
            Material.AZURE_BLUET,
            Material.ORANGE_TULIP,
            Material.PINK_TULIP,
            Material.RED_TULIP,
            Material.WHITE_TULIP,
            Material.OXEYE_DAISY,
            Material.LILY_OF_THE_VALLEY,
            Material.CORNFLOWER,
            Material.WITHER_ROSE,

            Material.COCOA,
            Material.COCOA_BEANS,

            Material.BAMBOO_SAPLING,
            Material.ACACIA_SAPLING,
            Material.BIRCH_SAPLING,
            Material.DARK_OAK_SAPLING,
            Material.JUNGLE_SAPLING,
            Material.OAK_SAPLING,
            Material.SPRUCE_SAPLING,

            Material.MELON,
            Material.POTATOES,
            Material.CARROTS,
            Material.BEETROOTS,
            Material.WHEAT,

            Material.AIR,
            Material.CAVE_AIR,
            Material.VOID_AIR,

            Material.FERN,
            Material.LARGE_FERN,

            Material.SNOW,
            Material.SNOW_BLOCK,
            Material.POWDER_SNOW,

            Material.FIRE,

            // Nether
            Material.CRIMSON_STEM,
            Material.STRIPPED_CRIMSON_STEM,
            Material.CRIMSON_HYPHAE,
            Material.STRIPPED_CRIMSON_HYPHAE,
            Material.CRIMSON_ROOTS,
            Material.CRIMSON_FUNGUS,

            Material.WARPED_STEM,
            Material.STRIPPED_WARPED_STEM,
            Material.WARPED_HYPHAE,
            Material.STRIPPED_WARPED_HYPHAE,
            Material.WARPED_ROOTS,
            Material.WARPED_FUNGUS,
            Material.WARPED_NYLIUM,

            Material.NETHER_WART,
            Material.NETHER_WART_BLOCK,
            Material.NETHER_SPROUTS,
            Material.SHROOMLIGHT,

            Material.BONE_BLOCK
    );

    public static boolean ignorable(Material material) {
        return IGNORABLE.contains(material);
    }

}
