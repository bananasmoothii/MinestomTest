package fr.bananasmoothii.selection;

import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.tinylog.Logger;

import java.util.Objects;

public class Selection {
    private final @NotNull Player player;
    private int xFrom, yFrom, zFrom, xTo, yTo, zTo;
    private boolean pos1Selected, pos2Selected;
    // private SelectionType type = SelectionType.CUBIC; // if needed

    public Selection(@NotNull Player player) {
        this.player = Objects.requireNonNull(player, "player cannot be null");
        Logger.info("selection create thread: " + Thread.currentThread());
    }

    public void selectPos1(@NotNull Point pos) {
        xFrom = pos.blockX();
        yFrom = pos.blockY();
        zFrom = pos.blockZ();
        pos1Selected = true;
        reorderPositions();
    }

    public void selectPos2(@NotNull Point pos) {
        xTo = pos.blockX();
        yTo = pos.blockY();
        zTo = pos.blockZ();
        pos2Selected = true;
        reorderPositions();
    }

    private void reorderPositions() {
        if (!pos1Selected || !pos2Selected) return;
        if (xFrom > xTo) {
            final int temp = xFrom;
            xFrom = xTo;
            xTo = temp;
        }
        if (yFrom > yTo) {
            final int temp = yFrom;
            yFrom = yTo;
            yTo = temp;
        }
        if (zFrom > zTo) {
            final int temp = zFrom;
            zFrom = zTo;
            zTo = temp;
        }
    }

    public void clear() {
        pos1Selected = false;
        pos2Selected = false;
    }

    public @NotNull Player getPlayer() {
        return player;
    }

    public Point getMinPoint() {
        return new Pos(xFrom, yFrom, zFrom);
    }

    public Point getMaxPoint() {
        return new Pos(xTo, yTo, zTo);
    }

    public boolean isPos1Selected() {
        return pos1Selected;
    }

    public boolean isPos2Selected() {
        return pos2Selected;
    }

    public boolean isComplete() {
        return pos1Selected && pos2Selected;
    }

    /**
     * @return the volume of the selection or 0 if the selection is not complete
     */
    public int volume() {
        return (xTo - xFrom + 1) * (yTo - yFrom + 1) * (zTo - zFrom + 1);
    }
}
