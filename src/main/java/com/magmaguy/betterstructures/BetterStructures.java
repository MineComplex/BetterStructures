package com.magmaguy.betterstructures;

import com.magmaguy.betterstructures.commands.*;
import com.magmaguy.betterstructures.config.DefaultConfig;
import com.magmaguy.betterstructures.config.ValidWorldsConfig;
import com.magmaguy.betterstructures.config.generators.GeneratorConfig;
import com.magmaguy.betterstructures.config.modulegenerators.ModuleGeneratorsConfig;
import com.magmaguy.betterstructures.config.modules.ModulesConfig;
import com.magmaguy.betterstructures.config.schematics.SchematicConfig;
import com.magmaguy.betterstructures.config.treasures.TreasureConfig;
import com.magmaguy.betterstructures.listeners.NewChunkLoadEvent;
import com.magmaguy.betterstructures.modules.ModulesContainer;
import com.magmaguy.betterstructures.modules.WFCGenerator;
import com.magmaguy.betterstructures.schematics.SchematicContainer;
import com.magmaguy.magmacore.MagmaCore;
import com.magmaguy.magmacore.command.CommandManager;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;

public final class BetterStructures extends JavaPlugin {

    @Override
    public void onEnable() {
        MetadataHandler.PLUGIN = this;
        // Plugin startup logic
        Bukkit.getLogger().info("[BetterStructures] Initialized version " + this.getDescription().getVersion() + "!");
        Bukkit.getPluginManager().registerEvents(new NewChunkLoadEvent(), this);
        Bukkit.getPluginManager().registerEvents(new ValidWorldsConfig.ValidWorldsConfigEvents(), this);
        try {
            this.getConfig().save("config.yml");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        new DefaultConfig();
        new ValidWorldsConfig();
        //Creates import folder if one doesn't exist, imports any content inside
        MagmaCore.onEnable();
        MagmaCore.initializeImporter();

        new TreasureConfig();
        new GeneratorConfig();
        new ModuleGeneratorsConfig();
        new SchematicConfig();
        new ModulesConfig();

        CommandManager commandManager = new CommandManager(this, "bs");
        commandManager.registerCommand(new LootifyCommand());
        commandManager.registerCommand(new PlaceCommand());
        commandManager.registerCommand(new ReloadCommand());
        commandManager.registerCommand(new SilentCommand());
        commandManager.registerCommand(new TeleportCommand());
    }

    @Override
    public void onLoad() {
        MagmaCore.createInstance(this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        SchematicContainer.shutdown();
        Bukkit.getServer().getScheduler().cancelTasks(MetadataHandler.PLUGIN);
        MagmaCore.shutdown();
        HandlerList.unregisterAll(MetadataHandler.PLUGIN);
        ModulesContainer.shutdown();
        WFCGenerator.shutdown();
        Bukkit.getLogger().info("[BetterStructures] Shutdown!");
    }
}
