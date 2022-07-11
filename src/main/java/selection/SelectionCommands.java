package selection;

import net.minestom.server.MinecraftServer;
import net.minestom.server.command.CommandManager;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.command.builder.CommandExecutor;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.utils.Direction;
import org.jetbrains.annotations.NotNull;

import static net.kyori.adventure.text.minimessage.MiniMessage.miniMessage;

public class SelectionCommands {
    private SelectionCommands() {}

    private static boolean areRegistered = false;

    public static void registerAll() {
        if (areRegistered) throw new IllegalStateException("Commands are already registered");

        final CommandManager c = MinecraftServer.getCommandManager();
        c.register(new Pos1());
        c.register(new Pos2());
        c.register(new ExpandContract());
        c.register(new Unselect());

        areRegistered = true;
    }

    interface SelectionsExecutor extends CommandExecutor {

        @Override
        default void apply(@NotNull CommandSender sender, @NotNull CommandContext context) {
            if (!(sender instanceof final Player player)) {
                sender.sendMessage(miniMessage().deserialize("<red>You must be a player to use this command."));
                return;
            }
            final Instance instance = player.getInstance();
            if (instance == null) {
                sender.sendMessage(miniMessage().deserialize("<red>You must be in an instance to use this command."));
                return;
            }
            if (!Selector.isEnabled(instance)) {
                sender.sendMessage(miniMessage().deserialize("<red>Sorry, selections are not enabled here."));
                return;
            }
            apply(player, context);
        }

        void apply(@NotNull Player player, @NotNull CommandContext context);
    }

    static class Pos1 extends Command {
        public Pos1() {
            super("/pos1", "/1");

            setDefaultExecutor((SelectionsExecutor) (player, context) ->
                    Selector.withInstance(player.getInstance()).selectPos1(player, player.getPosition()));
        }
    }

    static class Pos2 extends Command {
        public Pos2() {
            super("/pos2", "/2");

            setDefaultExecutor((SelectionsExecutor) (player, context) ->
                    Selector.withInstance(player.getInstance()).selectPos2(player, player.getPosition()));
        }
    }

    static class ExpandContract extends Command {
        public ExpandContract() {
            super("/expand", "/exp", "/contract", "/cont");

            setCondition((sender, command) -> sender instanceof Player);

            final var amountArg = ArgumentType.Integer("amount");
            amountArg.setCallback(((sender, exception) ->
                    sender.sendMessage(miniMessage().deserialize("<red>Invalid amount: <bold>" + exception.getInput()))));

            final var directionArg = ArgumentType.Enum("direction", Direction.class);
            directionArg.setCallback(((sender, exception) ->
                    sender.sendMessage(miniMessage().deserialize("<red>Invalid direction: <bold>" + exception.getInput()))));

            setDefaultExecutor((SelectionsExecutor) (player, context) -> {
                final int amount;
                if (context.getCommandName().equals("/expand") || context.getCommandName().equals("/exp"))
                    amount = 1;
                else amount = -1;
                expandContract(player, player.getPosition().facing(), amount);
            });

            addSyntax((SelectionsExecutor) (player, context) -> {
                int amount = context.get("amount");
                if (context.getCommandName().equals("/contract") || context.getCommandName().equals("/cont")) {
                    amount *= -1;
                }
                expandContract(player, player.getPosition().facing(), amount);
            }, amountArg);

            addSyntax((SelectionsExecutor) (player, context) -> {
                final int amount;
                if (context.getCommandName().equals("/expand") || context.getCommandName().equals("/exp"))
                    amount = 1;
                else amount = -1;
                expandContract(player, context.get("direction"), amount);
            }, directionArg);

            addSyntax((SelectionsExecutor) (player, context) -> {
                Direction direction = context.get("direction");
                int amount = context.get("amount");
                if (context.getCommandName().equals("/contract") || context.getCommandName().equals("/cont")) {
                    amount *= -1;
                }
                expandContract(player, direction, amount);
            }, amountArg, directionArg);
        }

        /**
         * @param direction face of the selection to expand or contract
         * @param amount amount of blocks to expand/contract. Positive values will expand the region, negative value will contract the selection
         */
        public void expandContract(@NotNull Player player, Direction direction, int amount) {
            final Selection selection = Selector.withInstance(player.getInstance()).getSelection(player);
            if (! selection.isComplete()) {
                player.sendMessage(miniMessage().deserialize("<red>You must select a region first with a diamond hoe"));
                return;
            }

            final int plusX = direction.normalX(),
                    plusY = direction.normalY(),
                    plusZ = direction.normalZ();
            if (plusX == 1 || plusY == 1 || plusZ == 1) {
                selection.selectPos2(selection.getMaxPoint().add(plusX * amount, plusY * amount, plusZ * amount));
            } else {
                selection.selectPos1(selection.getMinPoint().add(plusX * amount, plusY * amount, plusZ * amount));
            }

            if (amount > 0) {
                player.sendMessage(miniMessage().deserialize("<light_purple>Selection expanded by <dark_purple>%s block to %s".formatted(amount, direction.toString().toLowerCase())));
            } else if (amount < 0) {
                player.sendMessage(miniMessage().deserialize("<light_purple>Selection contracted by <dark_purple>%s block to %s".formatted(-amount, direction.toString().toLowerCase())));
            } else {
                player.sendMessage(miniMessage().deserialize("<light_purple>Selection unchanged"));
            }
        }
    }

    static class Unselect extends Command {
        public Unselect() {
            super("/unselect", "/;", "/clearselections");
            setDefaultExecutor((SelectionsExecutor) (player, context) -> {
                Selector.withInstance(player.getInstance()).getSelection(player).clear();
                player.sendMessage(miniMessage().deserialize("<light_purple>Selection cleared"));
            });
        }
    }
}
