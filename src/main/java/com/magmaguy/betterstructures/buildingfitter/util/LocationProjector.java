package com.magmaguy.betterstructures.buildingfitter.util;

import org.bukkit.Location;
import org.bukkit.util.Vector;


public class LocationProjector {

    public static Location project(Location worldAnchorPoint, Vector schematicOffset) {
        return worldAnchorPoint.clone().add(schematicOffset);
    }

    public static Location project(Location worldAnchorPoint, Vector schematicOffset, Vector relativeBlockLocation) {
        return project(worldAnchorPoint, schematicOffset).add(relativeBlockLocation);
    }
}
