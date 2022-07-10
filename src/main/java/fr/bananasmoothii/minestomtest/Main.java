package fr.bananasmoothii.minestomtest;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.event.player.PlayerLoginEvent;
import net.minestom.server.extras.MojangAuth;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.InstanceManager;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.generator.UnitModifier;
import org.tinylog.Logger;

import java.util.concurrent.ForkJoinPool;

public class Main {
    public static void main(String[] args) {
        // Initialize the server
        MinecraftServer minecraftServer = MinecraftServer.init();
        MojangAuth.init();
        Logger.info("Server version: " + MinecraftServer.getBrandName() + " " + MinecraftServer.VERSION_NAME);

        InstanceManager instanceManager = MinecraftServer.getInstanceManager();
        // Create the instance
        InstanceContainer instanceContainer = instanceManager.createInstanceContainer();
        // Set the ChunkGenerator
        instanceContainer.setGenerator(unit -> {
            final UnitModifier modifier = unit.modifier();
            modifier.fillHeight(-64, -63, Block.BEDROCK);
            modifier.fillHeight(-63, 60, Block.STONE);
            modifier.fillHeight(60, 63, Block.DIRT);
            modifier.fillHeight(63, 64, Block.GRASS_BLOCK);
        });

        // Add an event callback to specify the spawning instance (and the spawn position)
        GlobalEventHandler globalEventHandler = MinecraftServer.getGlobalEventHandler();
        globalEventHandler.addListener(PlayerLoginEvent.class, event -> {
            final Player player = event.getPlayer();
            event.setSpawningInstance(instanceContainer);
            player.setRespawnPoint(new Pos(0, 64, 0));
            player.setGameMode(GameMode.CREATIVE);
        });

        // Server shutdown custom message
        MinecraftServer.getSchedulerManager().buildShutdownTask(() -> {
            for (Instance instance : instanceManager.getInstances()) {
                for (Player player : instance.getPlayers()) {
                    player.kick(Component.text("Server is shutting down", TextColor.color(255, 0, 0)));
                }
            }
        });

        ForkJoinPool.commonPool().submit(() -> {
            try {
                Thread.sleep(10000);
            } catch (InterruptedException ignored) {
            }
            MinecraftServer.stopCleanly();
        });

        // Start the server on port 25565
        minecraftServer.start("0.0.0.0", 25565);
    }
}