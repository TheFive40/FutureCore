package io.github.futurecore.events.bukkit;

import io.github.futurecore.Main;
import io.github.futurecore.utils.CC;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.UUID;

import static io.github.futurecore.commands.player.cmdcore.CmdQuests.questTitles;
import static io.github.futurecore.commands.player.cmdcore.CmdQuests.waitingForTitle;

public class PlayerChat implements Listener {
    @EventHandler
    public void onPlayerChat ( AsyncPlayerChatEvent event ) {
        Player player = event.getPlayer ( );
        UUID uuid = player.getUniqueId ( );
        if (waitingForTitle.containsKey ( uuid )) {
            event.setCancelled ( true );
            String id = waitingForTitle.remove ( uuid );
            String title = event.getMessage ( );
            questTitles.put ( id, CC.translate ( title ) );
            Bukkit.getScheduler ( ).runTask ( Main.instance, () -> {
                player.sendMessage ( "§aTítulo guardado para la misión §e" + id + "§a: §f" + title );
            } );
        }
    }
}
