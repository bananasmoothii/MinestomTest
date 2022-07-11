package fr.bananasmoothii.selection;

import net.minestom.server.MinecraftServer;
import net.minestom.server.command.CommandManager;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.command.builder.CommandExecutor;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.inventory.PlayerInventory;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
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
        c.register(new Wand());

        areRegistered = true;
    }

    /**
     * Another {@link CommandExecutor} that checks if
     * <ol>
     *     <li>The {@link CommandSender} is a {@link Player}</li>
     *     <li>The {@link Player} is in an {@link Instance}</li>
     *     <li>Selections are enabled in this instance</li>
     * </ol>
     */
    @FunctionalInterface
    public interface SelectionsExecutor extends CommandExecutor {

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
            final Selection selection = Selector.getSelection(player);
            if (! selection.isComplete()) {
                player.sendMessage(miniMessage().deserialize("<red>You must select a region first with a diamond hoe"));
                return;
            }

            final int plusX = direction.normalX(),
                    plusY = direction.normalY(),
                    plusZ = direction.normalZ();
            final Point pos1 = selection.getPos1();
            final Point pos2 = selection.getPos2();
            if ((plusX * (pos2.x() - pos1.x()) > 0) || (plusY * (pos2.y() - pos1.y()) > 0) || (plusZ * (pos2.z() - pos1.z()) > 0)) {
                selection.selectPos2(selection.getPos2().add(plusX * amount, plusY * amount, plusZ * amount)
                        .withY(y -> y < -64 ? -64 : y > 319 ? 319 : y));
            } else {
                selection.selectPos1(selection.getPos1().add(plusX * amount, plusY * amount, plusZ * amount)
                        .withY(y -> y < -64 ? -64 : y > 319 ? 319 : y));
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
                Selector.getSelection(player).clear();
                player.sendMessage(miniMessage().deserialize("<light_purple>Selection cleared"));
            });
        }
    }

    static class Wand extends Command {
        public Wand() {
            super("/wand");
            setDefaultExecutor((SelectionsExecutor) (player, context) -> {
                final PlayerInventory inventory = player.getInventory();
                final ItemStack itemInMainHand = inventory.getItemInMainHand();
                inventory.setItemInMainHand(ItemStack.of(Material.DIAMOND_HOE));
                if (!itemInMainHand.isAir()) {
                    inventory.addItemStack(itemInMainHand);
                }
                player.sendMessage(miniMessage().deserialize("<light_purple>You have been given a wand"));
            });
        }
    }
}
