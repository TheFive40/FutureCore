package io.github.futurecore.commands.player.cmdRaids;

import io.github.futurecore.Main;
import io.github.futurecore.utils.commands.BaseCommand;
import io.github.futurecore.utils.commands.Command;
import io.github.futurecore.utils.commands.CommandArgs;
import io.github.futurecore.utils.menus.RaidMenu;

import java.io.IOException;

public class CmdRaids extends BaseCommand {
    @Command(name = "cmdRaids", aliases = "cmdRaids", inGameOnly = false)
    @Override
    public void onCommand ( CommandArgs command ) throws IOException {
        RaidMenu.open ( Main.instance.getServer ( ).getPlayer ( command.getArgs ( 0 ) ) );
    }
}
