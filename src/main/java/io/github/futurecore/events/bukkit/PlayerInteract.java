package io.github.futurecore.events.bukkit;
import io.github.futurecore.utils.data.KairosData.PKairos;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.delaware.tools.NbtHandler.NbtHandler;

public class PlayerInteract implements Listener {
    @EventHandler
    public void onPlayerInteractEvent(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_AIR) {
            ItemStack item = event.getPlayer().getItemInHand();

            if (item == null || item.getType() == Material.AIR) return;

            NbtHandler nbtHandler = new NbtHandler(item);
            if (nbtHandler.containsCompound("Kairos")) {
                int kairosAmount = nbtHandler.getInteger("kairosAmount");

                if (kairosAmount > 0) {
                    int amount = kairosAmount * item.getAmount();

                    PKairos pKairos = new PKairos(event.getPlayer().getUniqueId());
                    pKairos.pay(amount);

                    event.getPlayer().setItemInHand(null);
                    event.getPlayer().playSound(
                            event.getPlayer().getLocation(),
                            "random.orb",
                            1.0f,
                            1.0f
                    );
                }
            }
        }
    }

}
