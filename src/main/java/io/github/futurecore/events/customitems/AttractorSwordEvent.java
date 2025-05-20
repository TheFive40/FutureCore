package io.github.futurecore.events.customitems;

import com.gmail.filoghost.holograms.api.Hologram;
import com.gmail.filoghost.holograms.api.HolographicDisplaysAPI;
import io.github.futurecore.Main;
import io.github.futurecore.commands.player.cmdUsages.CmdItemUsage;
import net.minecraft.server.v1_7_R4.PacketPlayOutWorldParticles;
import noppes.npcs.api.entity.IDBCPlayer;
import noppes.npcs.scripted.NpcAPI;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.craftbukkit.v1_7_R4.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.delaware.tools.CC;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static io.github.futurecore.commands.player.cmdUsages.CmdItemUsage.hasPermanent;

public class AttractorSwordEvent implements Listener {

    private final Map<UUID, UUID> atrapados = new HashMap<>();
    private final Map<UUID, Instant> cooldowns = new HashMap<>();
    private final Duration COOLDOWN = Duration.ofMinutes(10);

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent event) {
        Player lanzador = event.getPlayer();
        UUID objetivoId = atrapados.get(lanzador.getUniqueId());
        if (objetivoId == null) return;

        Player objetivo = lanzador.getServer().getPlayer(objetivoId);
        if (objetivo == null || !objetivo.isOnline()) {
            atrapados.remove(lanzador.getUniqueId());
            return;
        }

        IDBCPlayer dbcPlayer = NpcAPI.Instance().getPlayer(objetivo.getName()).getDBCPlayer();
        liberarJugador(lanzador, objetivo, dbcPlayer);
        atrapados.remove(lanzador.getUniqueId());
        lanzador.sendMessage(CC.translate("&cEfecto atractor desactivado."));
    }
    public static void spawnHologram ( Player player, String text ) {
        Location loc = player.getLocation ( );
        loc.setY ( loc.getY ( ) + 1.5 );
        loc.setZ ( loc.getZ () + 1.0 );
        Hologram hologram = HolographicDisplaysAPI.createHologram ( Main.instance, loc, CC.translate ( text ) );
        Bukkit.getScheduler ( ).runTaskLater ( Main.instance, hologram::delete, 20L );
    }
    @EventHandler
    public void onRightClick(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Player)) return;

        Player lanzador = event.getPlayer();
        Player objetivo = (Player) event.getRightClicked();
        if (lanzador.getItemInHand().getTypeId() != 6131) return;

        Instant now = Instant.now();
        UUID lanzadorId = lanzador.getUniqueId();
        Instant lastUse = cooldowns.get(lanzadorId);

        if (lastUse != null && Duration.between(lastUse, now).compareTo(COOLDOWN) < 0) {
            Duration restante = COOLDOWN.minus(Duration.between(lastUse, now));
            long min = restante.toMinutes();
            long sec = restante.minusMinutes(min).getSeconds();
            spawnHologram( event.getPlayer ( ), "&cDebes esperar " + min + "m " + sec + "s ");
            return;
        }
        try {
            ItemStack item = lanzador.getItemInHand();

            if (!hasPermanent(item)) {
                ItemStack updated = CmdItemUsage.hasUses(item, lanzador);
                if (updated == null) {
                    lanzador.setItemInHand(null);
                } else {
                    lanzador.setItemInHand(updated);
                }
                lanzador.updateInventory();
            }
        } catch (Exception ignored) {
        }
        atrapados.put(lanzadorId, objetivo.getUniqueId());
        cooldowns.put(lanzadorId, now);
        lanzador.sendMessage(CC.translate("&aHas activado el efecto atractor sobre &e" + objetivo.getName()));
    }

    private void liberarJugador(Player lanzador, Player objetivo, IDBCPlayer dbcPlayer) {
        lanzador.playSound(lanzador.getLocation(), Sound.ENDERMAN_TELEPORT, 1.0f, 1.0f);
        objetivo.playSound(objetivo.getLocation(), Sound.EXPLODE, 1.0f, 1.0f);
        objetivo.getWorld().createExplosion(objetivo.getLocation().getX(), objetivo.getLocation().getY(),
                objetivo.getLocation().getZ(), 0.5F, true, false);
        dbcPlayer.setBody((int) (dbcPlayer.getBody() * 0.5));
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player lanzador = event.getPlayer();
        UUID objetivoId = atrapados.get(lanzador.getUniqueId());
        if (objetivoId == null) return;

        Player objetivo = lanzador.getServer().getPlayer(objetivoId);
        if (objetivo == null || !objetivo.isOnline()) {
            atrapados.remove(lanzador.getUniqueId());
            return;
        }

        if (lanzador.getLocation().distanceSquared(objetivo.getLocation()) > 400) {
            IDBCPlayer dbcPlayer = NpcAPI.Instance().getPlayer(objetivo.getName()).getDBCPlayer();
            liberarJugador(lanzador, objetivo, dbcPlayer);
            atrapados.remove(lanzador.getUniqueId());
            return;
        }

        Vector frente = lanzador.getLocation().getDirection().normalize().multiply(2.5);
        frente.setY(0);
        Vector nuevaPos = lanzador.getLocation().toVector().add(frente);
        objetivo.teleport(nuevaPos.toLocation(lanzador.getWorld()));
        objetivo.setFallDistance(0f);
        Location loc = objetivo.getLocation().add(0, 1, 0);
        mostrarParticulas(objetivo, "portal", (float) loc.getX(), (float) loc.getY(), (float) loc.getZ(), 0.3f, 0.5f, 0.3f, 0.01f, 15);
        mostrarParticulas(lanzador, "portal", (float) loc.getX(), (float) loc.getY(), (float) loc.getZ(), 0.3f, 0.5f, 0.3f, 0.01f, 15);
    }

    public void mostrarParticulas(Player jugador, String tipo, float x, float y, float z, float offsetX, float offsetY, float offsetZ, float velocidad, int cantidad) {
        PacketPlayOutWorldParticles packet = new PacketPlayOutWorldParticles(
                tipo, x, y, z, offsetX, offsetY, offsetZ, velocidad, cantidad
        );
        ((CraftPlayer) jugador).getHandle().playerConnection.sendPacket(packet);
    }
}
