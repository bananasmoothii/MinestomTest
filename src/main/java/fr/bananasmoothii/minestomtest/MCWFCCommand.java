package fr.bananasmoothii.minestomtest;

import fr.bananasmoothii.mcwfc.core.*;
import fr.bananasmoothii.mcwfc.core.util.Bounds;
import fr.bananasmoothii.mcwfc.core.util.Coords;
import fr.bananasmoothii.selection.IncompleteSelectionException;
import fr.bananasmoothii.selection.Selection;
import fr.bananasmoothii.selection.SelectionCommands.SelectionsExecutor;
import fr.bananasmoothii.selection.Selector;
import fr.bananasmoothii.util.CommandUtils;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.batch.AbsoluteBlockBatch;
import net.minestom.server.instance.block.Block;

import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ThreadLocalRandom;

import static net.kyori.adventure.text.minimessage.MiniMessage.miniMessage;

public class MCWFCCommand extends Command {

    static final Map<Player, ImmutableSample<Block>> samples = new WeakHashMap<>();

    public MCWFCCommand() {
        super("mcwfc");

        setDefaultExecutor((SelectionsExecutor) (player, context) ->
                player.sendMessage("Usage: /mcwfc pieceset|generate"));

        addSubcommand(new PieceSet());
        addSubcommand(new Generate());
    }

    public static class PieceSet extends Command {
        public PieceSet() {
            super("pieceset", "ps");

            CommandUtils.setDefaultExecutorToMessage(this, """
                            Generates the piece set that you will be able to use in /mcwfc generate.
                            Usage: <u>/mcwfc pieceset \\<piece size> [ud:\\<<b><i>yes</i></b>/no>] [mc:\\<<b><i>yes</i></b>/no>]</u>
                            Example: <u>/mcwfc pieceset 3 mc:yes</u>
                            <b>Piece size</b> is the size of each sample in your selection. The bigger it is, the more generations will look like the original.
                            <b>ud</b> stands for upside down. If set to true/yes, pieces will be allowed to rotated allong <i>all</i> axis, else only along the y-axis.
                            <b>mc</b> stands for modulo coords. If set to true/yes, one face of the selection will be considered equivalent to the opposite face, as if when crossing one face you would re-appear on the other side.""");

            var pieceSizeArg = ArgumentType.Integer("pieceSize");
            pieceSizeArg.setCallback((sender, exception) ->
                    sender.sendMessage(miniMessage().deserialize("<red>Piece size must be an integer, not \"" + exception.getInput() + '"')));

            var udArg = ArgumentType.Boolean("ud");
            udArg.setDefaultValue(false);
            udArg.setCallback((sender, exception) ->
                    sender.sendMessage(miniMessage().deserialize("<red>Upside down must be true or false, not \"" + exception.getInput() + '"')));

            var mcArg = ArgumentType.Boolean("mc");
            mcArg.setDefaultValue(true);
            mcArg.setCallback((sender, exception) ->
                    sender.sendMessage(miniMessage().deserialize("<red>Modulo coords must be true or false, not \"" + exception.getInput() + '"')));

            addSyntax((SelectionsExecutor) (player, context) -> {
                final int pieceSize = context.get("pieceSize");
                final boolean allowUpsideDown = context.get("ud");
                final boolean useModuloCoords = context.get("mc");

                final Instance instance = Objects.requireNonNull(player.getInstance());
                final Selection selection = Selector.withInstance(instance).getSelection(player);
                final Bounds bounds;
                try {
                    bounds = selection.toBounds();
                } catch (IncompleteSelectionException e) {
                    player.sendMessage(miniMessage().deserialize("<red>You must first select a region with the diamond hoe."));
                    return;
                }

                ForkJoinPool.commonPool().submit(() -> {
                    final MCVirtualSpace<Block> selectionCopy = new MCVirtualSpace<>(bounds, Block.AIR);
                    for (Coords coords : bounds) {
                        selectionCopy.set(instance.getBlock(coords.x(), coords.y(), coords.z()), coords.x(), coords.y(), coords.z());
                    }
                    samples.put(player, selectionCopy.generatePieces(pieceSize, allowUpsideDown, useModuloCoords).immutable());
                    player.sendMessage(miniMessage().deserialize("<gradient:#00dbc5:#0073ff>Piece set generated!"));
                });
            }, pieceSizeArg, udArg, mcArg);
        }
    }

    public static class Generate extends Command {
        public Generate() {
            super("generate", "gen");

            CommandUtils.setDefaultExecutorToMessage(this, """
                    Creates a "wave" and collapses it in your current selection (warning: content of your selection will be overridden).
                    You need to have a piece set generated with /mcwfc pieceset.
                    Usage: <u>/mcwfc generate [mc:\\<<b><i>yes</i></b>/no>] [seed:\\<number>]</u>
                    <b>mc</b> stands for modulo coords. If set to true/yes, one face of the selection will be considered equivalent to the opposite face, as if when crossing one face you would re-appear on the other side.
                    <b>seed</b> is the seed of the random generator. If not set (or set to 0), a random seed will be used.""");

            var mcArg = ArgumentType.Boolean("mc");
            mcArg.setDefaultValue(true);
            mcArg.setCallback((sender, exception) ->
                    sender.sendMessage(miniMessage().deserialize("<red>mc must be true or false, not \"" + exception.getInput() + '"')));

            var seedArg = ArgumentType.Long("seed");
            seedArg.setDefaultValue(0L);
            seedArg.setCallback((sender, exception) ->
                    sender.sendMessage(miniMessage().deserialize("<red>seed must be a number, not \"" + exception.getInput() + '"')));

            addSyntax((SelectionsExecutor) (player, context) -> {
                final boolean useModuloCoords = context.get("mc");
                final long seed = (long) context.get("seed") == 0 ? ThreadLocalRandom.current().nextLong() : context.get("seed");

                final Instance instance = Objects.requireNonNull(player.getInstance());
                final Selection selection = Selector.withInstance(instance).getSelection(player);
                final Bounds bounds;
                try {
                    bounds = selection.toBounds();
                } catch (IncompleteSelectionException e) {
                    player.sendMessage(miniMessage().deserialize("<red>You must first select a region with the diamond hoe."));
                    return;
                }
                final ImmutableSample<Block> sample = samples.get(player);
                if (sample == null) {
                    player.sendMessage(miniMessage().deserialize("<red>You need to generate a piece set first with /mcwfc pieceset."));
                    return;
                }

                ForkJoinPool.commonPool().submit(() -> {
                    player.sendMessage(miniMessage().deserialize("<gradient:#00dbc5:#0073ff>Collapsing the wave..."));
                    final Wave<Block> wave = new Wave<>(sample, bounds, useModuloCoords, seed);
                    try {
                        wave.collapseAll();
                    } catch (Wave.GenerationFailedException e) {
                        player.sendMessage(miniMessage().deserialize("<gradient:#00dbc5:red>Failed to collapse the wave."));
                        return;
                    }
                    player.sendMessage(miniMessage().deserialize("<gradient:#00dbc5:#0073ff>The Wave successfully collapsed!"));
                    final AbsoluteBlockBatch batch = new AbsoluteBlockBatch();
                    for (VirtualSpace.ObjectWithCoordinates<Sample<Block>> node : wave.getWave()) {
                        Sample<Block> piecesAtNode = node.object();
                        if (piecesAtNode.isEmpty()) continue;
                        Piece.Locked<Block> piece = piecesAtNode.peek().getCenterPiece();
                        int xMin = node.x() * piece.xSize;
                        int yMin = node.y() * piece.ySize;
                        int zMin = node.z() * piece.zSize;
                        int xMax = (node.x() + 1) * piece.xSize; // max coords are exclusive
                        int yMax = (node.y() + 1) * piece.ySize;
                        int zMax = (node.z() + 1) * piece.zSize;
                        for (int x = xMin, xInPiece = 0; x < xMax; x++, xInPiece++) {
                            for (int y = yMin, yInPiece = 0; y < yMax; y++, yInPiece++) {
                                for (int z = zMin, zInPiece = 0; z < zMax; z++, zInPiece++) {
                                    batch.setBlock(x, y, z, piece.get(xInPiece, yInPiece, zInPiece));
                                }
                            }
                        }
                    }
                    batch.apply(instance, null);
                });
            }, mcArg, seedArg);
        }
    }
}
