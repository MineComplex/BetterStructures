package com.magmaguy.betterstructures.worldedit;

import com.magmaguy.betterstructures.MetadataHandler;
import com.magmaguy.betterstructures.config.DefaultConfig;
import com.magmaguy.betterstructures.util.WorldEditUtils;
import com.magmaguy.magmacore.util.Logger;
import com.magmaguy.magmacore.util.WorkloadRunnable;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.util.io.Closer;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockTypes;
import lombok.experimental.UtilityClass;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.util.Vector;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Function;

@UtilityClass
public class Schematic {
    // Queue to hold pending paste operations
    private final Queue<PasteBlockOperation> pasteQueue = new ConcurrentLinkedQueue<>();
    private boolean erroredOnce;
    private boolean isDistributedPasting;

    /**
     * Loads a schematic from a file
     *
     * @param schematicFile The schematic file to load
     * @return The loaded clipboard or null if loading failed
     */
    public Clipboard load(File schematicFile) {
        Clipboard clipboard;

        ClipboardFormat format = ClipboardFormats.findByFile(schematicFile);

        boolean found = false;

        try (ClipboardReader reader = format.getReader(new FileInputStream(schematicFile))) {
            clipboard = reader.read();
            for (int x = 0; x <= clipboard.getDimensions().x(); x++) {
                for (int y = 0; y <= clipboard.getDimensions().y(); y++) {
                    for (int z = 0; z <= clipboard.getDimensions().z(); z++) {
                        BlockVector3 translatedLocation = BlockVector3.at(x, y, z).add(clipboard.getMinimumPoint());
                        BlockState weBlockState = clipboard.getBlock(translatedLocation);
                        Material minecraftMaterial = BukkitAdapter.adapt(weBlockState.getBlockType());
                        if (minecraftMaterial == null) continue;

                        if (Tag.SIGNS.isTagged(minecraftMaterial)) {
                            BaseBlock baseBlock = clipboard.getFullBlock(translatedLocation);
                            //For future reference, I don't know how to get the data in any other way than parsing the string. Sorry!
                            String line1 = WorldEditUtils.getLine(baseBlock, 1).toLowerCase();

                            //Case for spawning a vanilla mob
                            if (line1.contains("[spawn]") ||
                                    line1.contains("[elitemobs]") ||
                                    line1.contains("[mythicmobs]")) {
                                clipboard.setBlock(translatedLocation, BlockTypes.AIR.getDefaultState());
                                found = true;
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } catch (NoSuchElementException e) {
            Logger.warn("Failed to get element from schematic " + schematicFile.getName());
            e.printStackTrace();
            return null;
        } catch (Exception e) {
            Logger.warn("Failed to load schematic " + schematicFile.getName() + " ! 99% of the time, this is because you are not using the correct WorldEdit version for your Minecraft server. You should be downloading WorldEdit from here https://dev.bukkit.org/projects/worldedit . You can check which versions the download links are compatible with by hovering over them.");
            if (!erroredOnce) {
                e.printStackTrace();
                erroredOnce = true;
            } else {
                Logger.warn("Hiding stacktrace for this error, as it has already been printed once");
            }
            return null;
        }


        if(found) {
            try (Closer closer = Closer.create()) {
                FileOutputStream fos = closer.register(new FileOutputStream(schematicFile));
                BufferedOutputStream bos = closer.register(new BufferedOutputStream(fos));
                ClipboardWriter writer = closer.register(format.getWriter(bos));
                writer.write(clipboard);
            } catch (IOException e) {
                throw new RuntimeException(e);
            } finally {
                MetadataHandler.PLUGIN.getLogger().info("Found and saved " + schematicFile.getName());
            }
        }
        return clipboard;
    }

    /**
     * Creates a list of paste blocks from a schematic
     *
     * @param schematicClipboard The clipboard containing the schematic
     * @param location The location to paste at
     * @param schematicOffset The offset of the schematic
     * @param pedestalMaterialProvider Function that provides pedestal material based on whether it's a surface block
     * @return List of paste blocks
     */
    private List<PasteBlock> createPasteBlocks(
            Clipboard schematicClipboard,
            Location location,
            Vector schematicOffset,
            Function<Boolean, Material> pedestalMaterialProvider) {

        List<PasteBlock> pasteBlocks = new ArrayList<>();

        BlockVector3 dimensions = schematicClipboard.getDimensions();
        BlockVector3 minimumPoint = schematicClipboard.getMinimumPoint();
        // Iterate through the schematic and create PasteBlock objects
        Location adjustedLocation = location.clone().add(schematicOffset);
        for (int x = 0; x < dimensions.x(); x++)
            for (int y = 0; y < dimensions.y(); y++)
                for (int z = 0; z < dimensions.z(); z++) {
                    BlockVector3 blockPos = BlockVector3.at(
                            x + minimumPoint.x(),
                            y + minimumPoint.y(),
                            z + minimumPoint.z()
                    );
                    BaseBlock baseBlock = schematicClipboard.getFullBlock(blockPos);
                    if (baseBlock.getBlockType() == BlockTypes.__RESERVED__)
                        continue;

                    // special behavior: do not replace barriers, so do nothing
                    if (baseBlock.getBlockType() == BlockTypes.BARRIER)
                        continue;

                    Block worldBlock = adjustedLocation.clone().add(x, y, z).getBlock();
                    boolean isGround = !BukkitAdapter.adapt(schematicClipboard.getBlock(
                            BlockVector3.at(blockPos.x(),
                                    blockPos.y() + 1,
                                    blockPos.z())
                    ).getBlockType()).isSolid();

                    if (baseBlock.getBlockType() == BlockTypes.BEDROCK) {
                        // special behavior: if it's not solid, replace with solid filler block
                        if (!worldBlock.getType().isSolid()) {
                            Material pedestalMaterial = pedestalMaterialProvider.apply(isGround);
                            BlockState state = BukkitAdapter.asBlockType(pedestalMaterial).getDefaultState().toImmutableState();
                            pasteBlocks.add(new PasteBlock(worldBlock,
                                    WorldEditUtils.createSingleBlockClipboard(adjustedLocation, state.toBaseBlock(), state)));

                        }
                        continue;
                    }

                    BlockState blockState = baseBlock.toImmutableState();
                    pasteBlocks.add(new PasteBlock(worldBlock,
                            WorldEditUtils.createSingleBlockClipboard(adjustedLocation, baseBlock, blockState)));
                }

        return pasteBlocks;
    }

    /**
     * Pastes a schematic using the provided pedestal material provider
     *
     * @param schematicClipboard The clipboard containing the schematic
     * @param location The location to paste at
     * @param schematicOffset The offset of the schematic
     * @param pedestalMaterialProvider Function that provides pedestal material based on whether it's a surface block
     * @param onComplete Callback to run when paste is complete
     */
    public void pasteSchematic(
            Clipboard schematicClipboard,
            Location location,
            Vector schematicOffset,
            Function<Boolean, Material> pedestalMaterialProvider,
            Runnable onComplete) {

        List<PasteBlock> pasteBlocks = createPasteBlocks(
                schematicClipboard,
                location,
                schematicOffset,
                pedestalMaterialProvider);

        pasteDistributed(pasteBlocks, location, onComplete);
    }

    /**
     * Pastes a schematic using a distributed workload over multiple ticks.
     * If another paste operation is already in progress, this operation
     * will be queued and executed when the current operation completes.
     *
     * @param pasteBlocks List of blocks to paste
     * @param location    The location to paste at
     * @param onComplete  Optional callback to run when paste is complete
     */
    public void pasteDistributed(List<PasteBlock> pasteBlocks, Location location, Runnable onComplete) {
        // Add this paste operation to the queue
        pasteQueue.add(new PasteBlockOperation(pasteBlocks, location, onComplete));

        // If we're not currently pasting, start processing the queue
        if (!isDistributedPasting) {
            processNextPaste();
        }
    }

    /**
     * Processes the next paste operation in the queue
     */
    private void processNextPaste() {
        if (pasteQueue.isEmpty()) {
            isDistributedPasting = false;
            return;
        }

        isDistributedPasting = true;
        PasteBlockOperation operation = pasteQueue.poll();

        // Create a workload for this paste operation
        WorkloadRunnable workload = new WorkloadRunnable(DefaultConfig.getPercentageOfTickUsedForPasting(), () -> {
            // Run the completion callback if provided
            if (operation.onComplete != null) {
                operation.onComplete.run();
            }
            // Process the next paste in the queue
            processNextPaste();
        });

        for (PasteBlock pasteBlock : operation.blocks) {
            workload.addWorkload(() -> {
                try (EditSession editSession = WorldEdit.getInstance().newEditSession(
                        BukkitAdapter.adapt(pasteBlock.block().getLocation().getWorld()))) {
                    Operation worldeditPaste = new ClipboardHolder(pasteBlock.clipboard())
                            .createPaste(editSession)
                            .to(BlockVector3.at(pasteBlock.block().getX(), pasteBlock.block().getY(), pasteBlock.block().getZ()))
                            // configure here
                            .build();

                    Operations.complete(worldeditPaste);
                } catch (WorldEditException e) {
                    throw new RuntimeException(e);
                }
            });
        }

        // Start the workload
        workload.runTaskTimer(MetadataHandler.PLUGIN, 0, 1);
    }

    /**
     * Represents a single paste operation
     */
    private record PasteBlockOperation(List<PasteBlock> blocks, Location location, Runnable onComplete) {
    }

    public record PasteBlock(Block block, Clipboard clipboard) {
    }
}