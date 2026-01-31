package com.magmaguy.magmacore.dlc;

import com.magmaguy.magmacore.MagmaCore;
import com.magmaguy.magmacore.util.Logger;
import com.magmaguy.magmacore.util.ZipFile;
import org.bukkit.Bukkit;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class ConfigurationImporter {

    private final Path betterStructuresPath = Path.of(MagmaCore.getInstance().getRequestingPlugin().getDataFolder().getParentFile().getAbsolutePath()).resolve("BetterStructures");
    private final Path modelEnginePath = Path.of(MagmaCore.getInstance().getRequestingPlugin().getDataFolder().getParentFile().getAbsolutePath()).resolve("ModelEngine");
    private File importsFolder;

    public ConfigurationImporter() {
        if (!createImportsDirectory()) return;
        importsFolder = getImportsDirectory();
        if (importsFolder == null || importsFolder.listFiles().length == 0) return;
        processImportsFolder();
    }

    private static void deleteDirectory(File file) {
        if (file == null) return;
        if (file.isDirectory()) {
            for (File iteratedFile : file.listFiles()) {
                if (iteratedFile != null) deleteDirectory(iteratedFile);
            }
        }
        Logger.info("Cleaning up " + file.getPath());
        file.delete();
    }

    private static void moveWorlds(File worldcontainerFile) {
        for (File file : worldcontainerFile.listFiles()) {
            try {
                File worldContainer = Bukkit.getWorldContainer().getCanonicalFile();
                Path worldContainerPath = worldContainer.toPath().normalize().toAbsolutePath();
                Path destinationPath = worldContainerPath.resolve(file.getName());
                File destinationFile = destinationPath.toFile();

                if (destinationFile.exists()) {
                    Logger.info("Overriding existing directory " + destinationFile.getPath());
                    if (Bukkit.getWorld(file.getName()) != null) {
                        Bukkit.unloadWorld(file.getName(), false);
                        Logger.warn("Unloaded world " + file.getName() + " for safe replacement!");
                    }
                    deleteDirectory(destinationFile);
                }
                moveDirectory(file, destinationPath);
            } catch (Exception exception) {
                Logger.warn("Failed to move worlds for " + file.getName() + "! Tell the dev!");
                exception.printStackTrace();
            }
        }
    }

    private static void moveDirectory(File unzippedDirectory, Path targetPath) {
        for (File file : unzippedDirectory.listFiles()) {
            try {
                moveFile(file, targetPath);
            } catch (Exception exception) {
                Logger.warn("Failed to move directories for " + file.getName() + "! Tell the dev!");
                exception.printStackTrace();
            }
        }
    }

    private static void moveFile(File file, Path targetPath) {
        try {
            Path destinationPath = targetPath.resolve(file.getName());
            if (file.isDirectory()) {
                if (Files.exists(destinationPath)) {
                    for (File iteratedFile : file.listFiles()) {
                        moveFile(iteratedFile, destinationPath);
                    }
                } else {
                    Files.createDirectories(targetPath);
                    Files.move(file.toPath().normalize().toAbsolutePath(), destinationPath, StandardCopyOption.REPLACE_EXISTING);
                }
            } else {
                Files.createDirectories(targetPath);
                Files.move(file.toPath().normalize().toAbsolutePath(), destinationPath, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception exception) {
            Logger.warn("Failed to move file/directories for " + file.getName() + "! Tell the dev!");
            exception.printStackTrace();
        }
    }

    private boolean createImportsDirectory() {
        Path configurationsPath = Paths.get(MagmaCore.getInstance().getRequestingPlugin().getDataFolder().getAbsolutePath());
        Path importsPath = configurationsPath.normalize().resolve("imports");
        if (!Files.isDirectory(importsPath)) {
            try {
                File importsFile = importsPath.toFile();
                if (!importsFile.getParentFile().exists())
                    importsPath.toFile().mkdirs();
                Files.createDirectories(importsPath);
                return true;
            } catch (Exception exception) {
                Logger.warn("Failed to create import directory! Tell the dev!");
                exception.printStackTrace();
                return false;
            }
        }
        return true;
    }

    private File getImportsDirectory() {
        try {
            File dir = Paths.get(MagmaCore.getInstance().getRequestingPlugin().getDataFolder().getCanonicalPath()).resolve("imports").toFile();
            return dir;
        } catch (Exception ex) {
            Logger.warn("Failed to get imports folder! Report this to the dev!");
            ex.printStackTrace();
            return null;
        }
    }

    private void processImportsFolder() {
        for (File zippedFile : importsFolder.listFiles()) {
            if (zippedFile.getName().endsWith(".zip")) {
                unzipImportFile(zippedFile);
            } else if (zippedFile.isDirectory()) {
                boolean incorrectlyUnzippedFolder = false;
                for (File iteratedFile : zippedFile.listFiles()) {
                    if (iteratedFile.getName().equalsIgnoreCase("pack.meta")) {
                        incorrectlyUnzippedFolder = true;
                        break;
                    }
                }
                if (incorrectlyUnzippedFolder) {
                    processUnzippedFile(zippedFile);
                } else {
//                    Logger.debug("Directory " + zippedFile.getAbsolutePath() + " does not contain pack.meta, skipping.");
                }
            } else {
                Logger.warn("File " + zippedFile.getPath() + " can't be imported! It will be skipped.");
            }
        }
    }

    private void unzipImportFile(File zippedFile) {
        try {
            File unzippedFolder = ZipFile.unzip(zippedFile, new File(zippedFile.getAbsolutePath().replace(".zip", "")));
            processUnzippedFile(unzippedFolder);
            deleteDirectory(zippedFile);
        } catch (Exception ex) {
            Logger.warn("Failed to unzip " + zippedFile.getPath() + " ! This probably means the file is corrupted.");
            Logger.warn("To fix this, delete this file from the imports folder and download a clean copy!");
            ex.printStackTrace();
        }
    }

    private void processUnzippedFile(File unzippedFolder) {
        for (File unzippedFile : unzippedFolder.listFiles()) {
            moveUnzippedFiles(unzippedFile);
        }
        deleteDirectory(unzippedFolder);
    }

    private void moveUnzippedFiles(File unzippedFile) {
        Path targetPath = getTargetPath(unzippedFile.getName());
        if (targetPath == null) {
            return;
        }
        // Create target directory and all parent directories if they don't exist
        // This ensures directories like plugins/EliteMobs/custombosses are created
        // even when EliteMobs isn't installed, so files are ready when it is
        if (!targetPath.toFile().exists()) {
            targetPath.toFile().mkdirs();
        }

        if (unzippedFile.isDirectory()) {
            if (unzippedFile.getName().equalsIgnoreCase("worldcontainer"))
                moveWorlds(unzippedFile);
            else
                moveDirectory(unzippedFile, targetPath);
        } else {
            moveFile(unzippedFile, targetPath);
        }
    }

    private Path getTargetPath(String folder) {
        switch (folder) {
            case "dungeonpackages":
            case "content_packages":
                return betterStructuresPath.resolve("content_packages");
            case "worldcontainer":
                try {
                    File wc = Bukkit.getWorldContainer().getCanonicalFile();
                    return wc.toPath().normalize().toAbsolutePath();
                } catch (IOException e) {
                    Logger.warn("Failed to resolve world container path canonically!");
                    e.printStackTrace();
                    return null;
                }
            case "ModelEngine":
            case "models":
                if (Bukkit.getPluginManager().isPluginEnabled("ModelEngine"))
                    return modelEnginePath.resolve("blueprints");
            case "schematics":
                return betterStructuresPath.resolve("schematics");
            case "pack.meta":
                // Only for tagging purposes
                return null;
            case "spawn_pools":
                return betterStructuresPath.resolve("spawn_pools");
            case "components":
                return betterStructuresPath.resolve("components");
            case "modules":
                return betterStructuresPath.resolve("modules");
            case "module_generators":
                return betterStructuresPath.resolve("module_generators");
            default:
                Logger.warn("Directory " + folder + " for zipped file was not recognized! Was the zipped file packaged correctly?");
                return null;
        }
    }

}
