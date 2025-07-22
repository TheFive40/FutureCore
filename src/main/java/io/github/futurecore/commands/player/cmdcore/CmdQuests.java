package io.github.futurecore.commands.player.cmdcore;

import io.github.futurecore.Main;
import io.github.futurecore.utils.commands.BaseCommand;
import io.github.futurecore.utils.commands.Command;
import io.github.futurecore.utils.commands.CommandArgs;
import io.github.futurecore.utils.handlers.quests.QuestMenu;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.delaware.tools.Pastebin.PastebinReader;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import static io.github.futurecore.Main.npcLocations;

public class CmdQuests extends BaseCommand {
    public static HashMap<UUID, String> waitingForTitle = new HashMap<> ( );
    public static HashMap<String, List<String>> questDescriptions = new HashMap<> ( );
    public static HashMap<String, String> questTitles = new HashMap<> ( );

    @Command(name = "misiones", aliases = "misiones", inGameOnly = false)
    @Override
    public void onCommand ( CommandArgs command ) throws IOException {
        String[] args = command.getArgs ( );
        Player player = null;
        if (command.getSender ( ) instanceof Player) {
            player = command.getPlayer ( );
        } else {
            String arg0 = command.getArgs ( 0 );
            player = Main.instance.getServer ( ).getPlayer ( arg0 );
        }
        if (args.length > 0) {
            if (args[0].equalsIgnoreCase ( "agregar" )) {
                player = command.getPlayer ( );
                if (args.length < 2) {
                    player.sendMessage ( "§cUso correcto: /misiones agregar <id>" );
                    return;
                }
                String id = args[1];
                if (npcLocations.containsKey ( id )) {
                    player.sendMessage ( "§c¡Ya existe una misión registrada con ese ID! Usa otro diferente." );
                    return;
                }
                Location location = player.getLocation ( );
                npcLocations.put ( id, location );
                waitingForTitle.put ( player.getUniqueId ( ), id );
                player.sendMessage ( "§aUbicación guardada para la misión con ID §e" + id + "§a." );
                player.sendMessage ( "§bEscribe el §ltítulo§r§b de la misión por el chat." );
                return;
            }

            if (args[0].equalsIgnoreCase ( "desc" )) {
                player = command.getPlayer ( );
                if (args.length < 3) {
                    player.sendMessage ( "§cUso correcto: /misiones desc <id> <url>" );
                    return;
                }
                String id = args[1];
                String url = args[2];
                Player finalPlayer = player;
                Bukkit.getScheduler ( ).runTaskAsynchronously ( Main.instance, () -> {
                    List<String> descLines = PastebinReader.getFromPastebin ( url );
                    if (descLines == null) {
                        finalPlayer.sendMessage ( "§cURL de Pastebin inválida o no se pudo obtener la descripción." );
                        return;
                    }
                    questDescriptions.put ( id, descLines );
                    finalPlayer.sendMessage ( "§aDescripción guardada correctamente para la misión §e" + id );
                } );
                return;
            }
        }
        QuestMenu.open ( player );
    }


}
