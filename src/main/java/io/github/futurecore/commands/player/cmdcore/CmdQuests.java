package io.github.futurecore.commands.player.cmdcore;
import io.github.futurecore.utils.commands.BaseCommand;
import io.github.futurecore.utils.commands.Command;
import io.github.futurecore.utils.commands.CommandArgs;
import io.github.futurecore.utils.handlers.quests.QuestMenu;
import java.io.IOException;
import java.util.HashMap;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import static io.github.futurecore.Main.npcLocations;

public class CmdQuests extends BaseCommand {

    @Command(name = "misiones", aliases = "misiones")
    @Override
    public void onCommand(CommandArgs command) throws IOException {
        Player player = command.getPlayer();
        String[] args = command.getArgs();

        if (args.length > 0 && args[0].equalsIgnoreCase("agregar")) {
            if (args.length < 2) {
                player.sendMessage("§cUso correcto: /misiones agregar <id>");
                return;
            }

            String id = args[1];
            Location location = player.getLocation();
            npcLocations.put(id, location);
            player.sendMessage("§aUbicación guardada para la misión con ID §e" + id);
        } else {
            QuestMenu.open(player);
        }
    }
}

