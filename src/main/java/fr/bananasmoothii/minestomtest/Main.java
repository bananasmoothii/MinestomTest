package fr.bananasmoothii;

public class Main {
    public static void main(String[] args) {
        // Initialize the server
        MinecraftServer minecraftServer = MinecraftServer.init();

        // REGISTER EVENTS (set spawn instance, teleport player at spawn)

        // Start the server
        minecraftServer.start("0.0.0.0", 25565);
    }
}