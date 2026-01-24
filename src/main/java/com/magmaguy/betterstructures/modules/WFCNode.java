package com.magmaguy.betterstructures.modules;

import com.magmaguy.magmacore.util.Logger;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.World;
import org.joml.Vector3i;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;

public class WFCNode {
    private final Vector3i nodePosition;
    private final WFCLattice lattice;
    @Getter
    private final World world;
    private final Map<Vector3i, WFCNode> nodeMap;
    @Getter
    private final int magnitudeSquared;
    @Getter
    private final WFCGenerator wfcGenerator;
    private final Map<Direction, WFCNode> adjacentNodes = new EnumMap<>(Direction.class);
    @Getter
    @Setter
    private ModulesContainer modulesContainer;
    @Getter
    private HashSet<ModulesContainer> possibleStates = null;

    /**
     * Creates a new WFCNode.
     *
     * @param nodePosition The lattice coordinates of this node
     * @param world        The world this node belongs to
     * @param lattice      The WFC lattice this node belongs to
     * @param nodeMap      The global node map reference
     */
    public WFCNode(Vector3i nodePosition, World world, WFCLattice lattice, Map<Vector3i, WFCNode> nodeMap, WFCGenerator wfcGenerator) {
        this.nodePosition = new Vector3i(nodePosition);  // Defensive copy
        this.world = world;
        this.lattice = lattice;
        this.nodeMap = nodeMap;
        this.magnitudeSquared = (int) nodePosition.lengthSquared();
        this.wfcGenerator = wfcGenerator;
        if (isBoundary())
            modulesContainer = ModulesContainer.nothingContainer;
    }

    public void initializeNeighbors() {
        for (Direction direction : Direction.values()) {
            Vector3i offset = WFCLattice.getDirectionOffset(direction);
            Vector3i neighborPos = new Vector3i(nodePosition).add(offset);
            adjacentNodes.put(direction, nodeMap.get(neighborPos));
        }
    }

    public boolean isBoundary() {
        return Math.abs(nodePosition.x) == lattice.getLatticeRadius() || Math.abs(nodePosition.z) == lattice.getLatticeRadius() || nodePosition.y < lattice.getMinYLevel() || nodePosition.y > lattice.getMaxYLevel();
    }

    /**
     * Gets a safe copy of the cell location.
     *
     * @return A new Vector3i containing the cell location
     */
    public Vector3i getCellLocation() {
        return new Vector3i(nodePosition);
    }

    /**
     * Updates the possible states for this node based on its adjacent nodes.
     */
    public void updatePossibleStates() {
        possibleStates = ModulesContainer.getValidModulesFromSurroundings(this);
    }

    /**
     * Gets the count of valid module options for this cell.
     *
     * @return The number of valid options, or 0 if none are available
     */
    public int getValidOptionCount() {
        if (possibleStates == null) {
            updatePossibleStates();
        }
        if (possibleStates == null) {
            Logger.warn("Valid options were null when trying to get the size for cell at " + nodePosition);
            return 0;
        }
        return possibleStates.size();
    }

    /**
     * Gets a map of neighboring cells in each direction.
     *
     * @return Map of Direction to WFCNode for each neighbor
     */
    public Map<Direction, WFCNode> getOrientedNeighbors() {
        return adjacentNodes;
    }

    /**
     * Gets the possible states for this node.
     *
     * @return Set of possible module states for this node
     */
    public HashSet<ModulesContainer> getValidOptions() {
        if (possibleStates == null) {
            updatePossibleStates();
        }
        return possibleStates;
    }

    /**
     * Gets the real world location of this cell's origin point.
     *
     * @return Location object representing the cell's origin in the world
     */
    public Location getRealLocation(Location startLocation) {
        Vector3i worldCoord;
        if (startLocation != null)
            worldCoord = lattice.latticeToWorld(nodePosition).add(startLocation.getBlockX(), startLocation.getBlockY(), startLocation.getBlockZ());
        else
            worldCoord = lattice.latticeToWorld(nodePosition);
        return new Location(world, worldCoord.x, worldCoord.y, worldCoord.z);
    }

    private Location getLocalCenterLocation() {
        double y = lattice.getNodeSizeY() / 2d;
        if (modulesContainer != null && modulesContainer.getClipboard() != null)
            y = modulesContainer.getClipboard().getDimensions().y() / 2d;
        Vector3i worldPos = lattice.latticeToWorld(nodePosition).add((int) (lattice.getNodeSizeXZ() / 2d), (int) y, (int) (lattice.getNodeSizeXZ() / 2d));
        return new Location(world, worldPos.x, worldPos.y, worldPos.z);
    }

    public Location getRealCenterLocation() {
        return getLocalCenterLocation().add(wfcGenerator.getStartLocation());
    }

    /**
     * Checks if this cell has been generated.
     *
     * @return true if the cell has a module container
     */
    public boolean isCollapsed() {
        return modulesContainer != null;
    }

    public boolean isNothing() {
        return modulesContainer != null && modulesContainer.isNothing();
    }

    /**
     * Clears generation data for this cell.
     */
    public void clearGenerationData() {
        possibleStates = null;
    }

}