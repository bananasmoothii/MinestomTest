package fr.bananasmoothii.util;

import net.minestom.server.command.builder.Command;
import org.jetbrains.annotations.NotNull;

import static net.kyori.adventure.text.minimessage.MiniMessage.miniMessage;

public class CommandUtils {
    private CommandUtils() {
    }

    public static void setDefaultExecutorToMessage(final @NotNull Command command, final @NotNull String message) {
        command.setDefaultExecutor((sender, context) -> sender.sendMessage(miniMessage().deserialize(message)));
    }
}
