package io.github.futurecore.commands.player.cmdUsages;

import io.github.futurecore.Main;
import io.github.futurecore.utils.CC;
import io.github.futurecore.utils.commands.BaseCommand;
import io.github.futurecore.utils.commands.Command;
import io.github.futurecore.utils.commands.CommandArgs;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.util.Arrays;

public class CmdItemRepairUsages extends BaseCommand {
  private static Integer noRepairItems[] = {6259, 6251, 6247, 6258, 6244, 6260, 6239, 6249};

  @Command(aliases = "reparar", permission = "dbfuture.reparar", inGameOnly = false, name = "reparar")
  @Override
  public void onCommand ( CommandArgs command ) throws IOException {
    String name = command.getArgs ( 0 );
    int amount = Integer.parseInt ( command.getArgs ( 1 ) );
    Player player = Main.instance.getServer ( ).getPlayer ( name );
    if (!Arrays.asList ( noRepairItems ).contains ( player.getItemInHand ( ).getType ( ).getId ( ) )) {
      CmdItemUsage.repairUses ( player.getItemInHand ( ), amount, player );
      player.playSound ( player.getLocation ( ), Sound.ANVIL_USE, 1.0F, 1.0F );
      return;
    }
    player.sendMessage ( CC.translate ( "&cEl item no es reparable" ) );
    player.playSound ( player.getLocation ( ), Sound.ANVIL_BREAK, 1.0F, 1.0F );
  }
}
