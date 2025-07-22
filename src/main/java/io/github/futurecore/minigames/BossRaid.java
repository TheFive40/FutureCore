package io.github.futurecore.minigames;
import io.github.futurecore.utils.RegionUtils;
import io.github.futurecore.utils.menus.RaidMenu;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import java.util.UUID;

public class BossRaid implements Listener {

    private static final String REGION_RAID = "raidt1";

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Location to = event.getTo();
        Location from = event.getFrom();

        if (to == null || from == null) return;

        // Si no ha cambiado de bloque, no hacer nada
        if (from.getBlockX() == to.getBlockX() &&
                from.getBlockY() == to.getBlockY() &&
                from.getBlockZ() == to.getBlockZ()) return;

        boolean wasIn = RegionUtils.isLocationInRegion(from, REGION_RAID);
        boolean isNowIn = RegionUtils.isLocationInRegion(to, REGION_RAID);
        UUID uuid = player.getUniqueId();

        // Entró sin permiso
        if (isNowIn && !RaidMenu.getJugadores().contains(uuid)) {
            player.sendMessage(ChatColor.RED + "No estás registrado en esta raid. Te regresamos al spawn.");
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "warp spawn " + player.getName());
        }

        // Salió de la región
        if (wasIn && !isNowIn && RaidMenu.getJugadores().contains(uuid)) {
            RaidMenu.removeJugador(uuid);
            player.sendMessage(ChatColor.RED + "Has salido de la zona de raid. Fuiste eliminado de la raid.");
        }
    }

    @EventHandler
    public void onPlayerDeath( PlayerDeathEvent event) {
        Player player = event.getEntity();
        UUID uuid = player.getUniqueId();

        if (RaidMenu.getJugadores().contains(uuid)) {
            RaidMenu.removeJugador(uuid);
            Bukkit.broadcastMessage(ChatColor.RED + "[RAID] " + player.getName() + " ha sido eliminado de la raid por morir.");
        }
    }


}
