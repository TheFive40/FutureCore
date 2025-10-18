package io.github.futurecore.commands.player.cmdcore.cmdItemsAbilities;

import io.github.futurecore.events.customitems.SpellEvent;
import io.github.futurecore.utils.CC;
import io.github.futurecore.utils.commands.BaseCommand;
import io.github.futurecore.utils.commands.Command;
import io.github.futurecore.utils.commands.CommandArgs;
import org.bukkit.Bukkit;

import java.io.IOException;

public class CmdGogetta extends BaseCommand {
    @Command(name = "kitgogetta", aliases = "gogetta", permission = "dbfuture.gogetta"
    , inGameOnly = false)
    @Override
    public void onCommand ( CommandArgs command ) throws IOException {
        String name = command.getArgs ( 0 );
        SpellEvent.kitGogetta.add ( Bukkit.getPlayer ( name ).getUniqueId ( ) );
        command.getSender ( ).sendMessage ( CC.translate ( "&aJugador registrado correctamente. " ) );
    }
}
