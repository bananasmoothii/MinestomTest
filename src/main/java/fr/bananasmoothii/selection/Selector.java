package fr.bananasmoothii.selection;

import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerBlockBreakEvent;
import net.minestom.server.event.player.PlayerBlockInteractEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnknownNullability;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static net.kyori.adventure.text.minimessage.MiniMessage.miniMessage;

public class Selector {
    private static final Map<Instance, Selector> selectors = new ConcurrentHashMap<>();
    private static boolean autoEnable = false;

    private final Instance instance;
    private final Map<Player, Selection> selections = new HashMap<>();
    private Selector(@NotNull Instance instance) {
        this.instance = instance;

        if (!autoEnable) {
            instance.eventNode().addListener(PlayerBlockBreakEvent.class, event -> {
                if (event.getPlayer().getItemInMainHand().material() == Material.DIAMOND_HOE) {
                    event.setCancelled(true);
                    selectPos1(event.getPlayer(), event.getBlockPosition());
                }
            });
            instance.eventNode().addListener(PlayerBlockInteractEvent.class, event -> {
                if (event.getHand() == Player.Hand.MAIN && event.getPlayer().getItemInMainHand().material() == Material.DIAMOND_HOE) {
                    event.setCancelled(true);
                    selectPos2(event.getPlayer(), event.getBlockPosition());
                }
            });
        }

    }

    public static @UnknownNullability Selector withInstance(Instance instance) {
        if (autoEnable) {
            return selectors.computeIfAbsent(instance, Selector::new);
        } else {
            return selectors.get(instance);
        }
    }

    public void selectPos1(Player player, Point pos) {
        final Selection selection = selections.computeIfAbsent(player, Selection::new);
        selection.selectPos1(pos);
        player.sendMessage(miniMessage().deserialize("<light_purple>First position set to <dark_purple>(%s, %s, %s)</dark_purple>"
                .formatted(pos.blockX(), pos.blockY(), pos.blockZ()) + (selection.isComplete() ? " (" + selection.volume() + ')' : "")));
    }

    public void selectPos2(Player player, Point pos) {
        final Selection selection = selections.computeIfAbsent(player, Selection::new);
        selection.selectPos2(pos);
        player.sendMessage(miniMessage().deserialize("<light_purple>Second position set to <dark_purple>(%s, %s, %s)</dark_purple>"
                .formatted(pos.blockX(), pos.blockY(), pos.blockZ()) + (selection.isComplete() ? " (" + selection.volume() + ')' : "")));
    }

    public Selection getSelection(Player player) {
        return selections.computeIfAbsent(player, Selection::new);
    }

    public static boolean isAutoEnable() {
        return autoEnable;
    }

    /**
     * Enable the Selector for all instances
     */
    public static void autoEnable() {
        if (! selectors.isEmpty()) throw new IllegalStateException("the selector is already enabled on some instances, " +
                "you must choose between enabling the selector per instance or globally");
        autoEnable = true;
        MinecraftServer.getGlobalEventHandler().addListener(PlayerBlockBreakEvent.class, event -> {
            if (event.getPlayer().getItemInMainHand().material() == Material.DIAMOND_HOE) {
                event.setCancelled(true);
                withInstance(event.getInstance()).selectPos1(event.getPlayer(), event.getBlockPosition());
            }
        });
        MinecraftServer.getGlobalEventHandler().addListener(PlayerBlockInteractEvent.class, event -> {
            if (event.getHand() == Player.Hand.MAIN && event.getPlayer().getItemInMainHand().material() == Material.DIAMOND_HOE) {
                event.setCancelled(true);
                withInstance(event.getInstance()).selectPos2(event.getPlayer(), event.getBlockPosition());
            }
        });
    }

    public static void enable(@NotNull Instance instance) {
        if (autoEnable) return;
        selectors.put(instance, new Selector(instance));
    }

    public static boolean isEnabled(@NotNull Instance instance) {
        if (autoEnable) return true;
        return selectors.containsKey(instance);
    }
}
