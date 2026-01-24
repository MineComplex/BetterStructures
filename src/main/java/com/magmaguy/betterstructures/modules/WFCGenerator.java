package com.magmaguy.betterstructures.modules;

import com.magmaguy.betterstructures.MetadataHandler;
import com.magmaguy.betterstructures.config.modulegenerators.ModuleGeneratorsConfigFields;
import com.magmaguy.magmacore.util.Logger;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.joml.Vector3i;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static com.magmaguy.betterstructures.modules.ModulesContainer.pickWeightedRandomModule;

public class WFCGenerator {
    public static HashSet<WFCGenerator> wfcGenerators = new HashSet<>();
    @Getter
    private final Location startLocation;
    @Getter
    private ModuleGeneratorsConfigFields moduleGeneratorsConfigFields;
    @Getter
    private WFCLattice spatialGrid;
    private Player player = null;
    private String startingModule;
    @Getter
    private World world;
    private volatile boolean isGenerating;
    private volatile boolean isCancelled;

    private int rollbackCounter = 0;

    public WFCGenerator(ModuleGeneratorsConfigFields moduleGeneratorsConfigFields, Player player) {
        this.player = player;
        this.startLocation = player.getLocation();
        initialize(moduleGeneratorsConfigFields);
    }

    public WFCGenerator(ModuleGeneratorsConfigFields moduleGeneratorsConfigFields, Location startLocation) {
        this.moduleGeneratorsConfigFields = moduleGeneratorsConfigFields;
        this.startLocation = startLocation;
        initialize(moduleGeneratorsConfigFields);
    }

    public static void shutdown() {
        wfcGenerators.forEach(WFCGenerator::cancel);
        wfcGenerators.clear();
    }

    private void initialize(ModuleGeneratorsConfigFields moduleGeneratorsConfigFields) {
        this.moduleGeneratorsConfigFields = moduleGeneratorsConfigFields;
        this.spatialGrid = new WFCLattice(moduleGeneratorsConfigFields.getRadius(), moduleGeneratorsConfigFields.getModuleSizeXZ(), moduleGeneratorsConfigFields.getModuleSizeY(), moduleGeneratorsConfigFields.getMinChunkY(), moduleGeneratorsConfigFields.getMaxChunkY());
        wfcGenerators.add(this);

        List<String> startModules = moduleGeneratorsConfigFields.getStartModules();
        if (startModules.isEmpty()) {
            if (player != null)
                player.sendMessage("No start modules exist, you need to install or make modules first!");
            Logger.warn("No start modules exist, you need to install or make modules first!");
            cancel();
            return;
        }
        this.startingModule = startModules.get(ThreadLocalRandom.current().nextInt(moduleGeneratorsConfigFields.getStartModules().size())) + "_rotation_0";

        reserveChunks();
    }

    private void reserveChunks() {
        this.world = startLocation.getWorld();

        spatialGrid.initializeLattice(world, this);
        startArrangingModules();
    }

    private void startArrangingModules() {
        Bukkit.getScheduler().runTaskAsynchronously(MetadataHandler.PLUGIN, () -> start(startingModule));
    }

    private void start(String startingModule) {
        if (isGenerating) {
            return;
        }
        isGenerating = true;

        try {
            WFCNode startChunk = createStartChunk(startingModule);
            if (startChunk == null) {
                return;
            }

            generateFast();

        } catch (Exception e) {
            Logger.warn("Error during generation: " + e.getMessage());
            e.printStackTrace();
            cleanup();
        }
    }

    private WFCNode createStartChunk(String startingModule) {
        WFCNode startCell = spatialGrid.getNodeMap().get(new Vector3i());

        ModulesContainer modulesContainer = ModulesContainer.getModulesContainers().get(startingModule);
        if (modulesContainer == null) {
            Logger.warn("Starting module was null! Cancelling!");
            return null;
        }

        paste(startCell, modulesContainer);
        return startCell;
    }

    private void generateFast() {
        while (!isCancelled) {
            WFCNode nextCell = spatialGrid.getLowestEntropyNode();
            if (nextCell == null) {
                done();
                break;
            }

            generateNextChunk(nextCell);
        }
    }

    private void paste(WFCNode gridCell, ModulesContainer modulesContainer) {
        // Record the decision for backtracking
        spatialGrid.recordCollapseDecision(gridCell, modulesContainer);

        gridCell.setModulesContainer(modulesContainer);
        gridCell.getOrientedNeighbors().values().forEach(spatialGrid::updateNodeEntropy);
    }

    private void generateNextChunk(WFCNode gridCell) {
        HashSet<ModulesContainer> validOptions = gridCell.getValidOptions();
        if (validOptions == null || validOptions.isEmpty()) {
            rollbackChunk();
            return;
        }

        ModulesContainer modulesContainer = pickWeightedRandomModule(validOptions, gridCell);
        if (modulesContainer == null) {
            rollbackChunk();
            return;
        }

        paste(gridCell, modulesContainer);
    }

    private void rollbackChunk() {
        // Use proper backtracking instead of just resetting
        if (!spatialGrid.backtrack()) {
            cancel();
            return;
        }

        rollbackCounter++;
        if (rollbackCounter > 1000) {
            Logger.warn("Exceeded backtrack limit!");
            cancel();
            //retry
            if (player != null) new WFCGenerator(moduleGeneratorsConfigFields, player);
            else new WFCGenerator(moduleGeneratorsConfigFields, startLocation);
        }
    }

    private void done() {
        if (player != null) {
            player.sendMessage("Done assembling!");
            player.sendMessage("It will take a moment to paste the structure, and will require relogging.");
        }
        isGenerating = false;
        instantPaste();
        spatialGrid.clearGenerationData();
    }

    private void cleanup() {
        spatialGrid.clearAllData();
        wfcGenerators.remove(this);
    }

    /**
     * Cancels the generation process.
     */
    public void cancel() {
        isCancelled = true;
    }

    private void instantPaste() {
        // This guarantees that the paste order is grouped by chunk, making pasting faster down the line.
        Deque<WFCNode> orderedPasteDeque = new ArrayDeque<>();
        for (int x = -spatialGrid.getLatticeRadius(); x < spatialGrid.getLatticeRadius(); x++) {
            for (int z = -spatialGrid.getLatticeRadius(); z < spatialGrid.getLatticeRadius(); z++) {
                for (int y = spatialGrid.getMinYLevel(); y <= spatialGrid.getMaxYLevel(); y++) {
                    // Remove the cell from the map and get it in one go.
                    WFCNode cell = spatialGrid.getNodeMap().remove(new Vector3i(x, y, z));
                    if (cell != null) {
                        orderedPasteDeque.add(cell);
                    }
                }
            }
        }

        new ModulePasting(world, orderedPasteDeque, moduleGeneratorsConfigFields.getSpawnPoolSuffix(), startLocation, moduleGeneratorsConfigFields);

        cleanup();
    }

}