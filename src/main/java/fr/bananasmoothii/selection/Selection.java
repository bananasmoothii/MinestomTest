package fr.bananasmoothii.selection;

import fr.bananasmoothii.mcwfc.core.util.Bounds;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

import static java.lang.Math.abs;
import static java.lang.Math.min;

public class Selection {
    private final @NotNull Player player;
    private int x1, y1, z1, x2, y2, z2;
    private boolean pos1Selected, pos2Selected;
    // private SelectionType type = SelectionType.CUBIC; // for later maybe

    public Selection(@NotNull Player player) {
        this.player = Objects.requireNonNull(player, "player cannot be null");
    }

    public void selectPos1(@NotNull Point pos) {
        x1 = pos.blockX();
        y1 = pos.blockY();
        z1 = pos.blockZ();
        pos1Selected = true;
    }

    public void selectPos2(@NotNull Point pos) {
        x2 = pos.blockX();
        y2 = pos.blockY();
        z2 = pos.blockZ();
        pos2Selected = true;
    }

    public void clear() {
        pos1Selected = false;
        pos2Selected = false;
    }

    public @NotNull Player getPlayer() {
        return player;
    }

    public Point getMinPoint() {
        return new Pos(min(x1, x2), min(y1, y2), min(z1, z2));
    }

    public Point getMaxPoint() {
        return new Pos(min(x1, x2), min(y1, y2), min(z1, z2));
    }

    public Point getPos1() {
        return new Pos(x1, y1, z1);
    }

    public Point getPos2() {
        return new Pos(x2, y2, z2);
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
        return xSize() * ySize() * zSize();
    }

    public int xSize() {
        return abs(x2 - x1) + 1;
    }

    public int ySize() {
        return abs(y2 - y1) + 1;
    }

    public int zSize() {
        return abs(z2 - z1) + 1;
    }

    public Bounds toBounds() throws IncompleteSelectionException {
        if (! isComplete()) throw new IncompleteSelectionException("selection is not complete");
        return Bounds.fromTo(x1, y1, z1, x2, y2, z2);
    }
}
