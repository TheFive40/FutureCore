package io.github.futurecore.commands.player.cmdcore;

import io.github.futurecore.Main;
import io.github.futurecore.utils.CC;
import io.github.futurecore.utils.commands.BaseCommand;
import io.github.futurecore.utils.commands.Command;
import io.github.futurecore.utils.commands.CommandArgs;
import io.github.futurecore.utils.handlers.battle.BattleManager;
import io.github.futurecore.utils.handlers.battle.BattleTask;
import org.bukkit.entity.Player;
import org.delaware.tools.General;

import java.io.IOException;

import static io.github.futurecore.utils.handlers.battle.BattleData.battlePending;
import static io.github.futurecore.utils.handlers.battle.BattleTask.scheduleExpiration;

public class CmdBattle extends BaseCommand {
    @Command(name = "duelo", aliases = {"duelo", "battle"}, permission = "dbzenkai.duelo")
    @Override
    public void onCommand ( CommandArgs command ) throws IOException {
        Player source = command.getPlayer ( );
        if (command.getArgs ( ).length == 0) {
            source.sendMessage ( CC.translate ( "&8[&4Error&8] &cUso: /duelo <jugador|aceptar|rechazar>" ) );
            return;
        }
        Player target = Main.instance.getServer ( ).getPlayer ( command.getArgs ( 0 ) );
        if (target != null) {
            if (target.getName ( ).equalsIgnoreCase ( source.getName ( ) )) {
                source.sendMessage ( CC.translate ( "&8[&4Error&8] &c¡No puedes, desafiarte a tí mismo!" ) );
                return;
            }
        }
        if (command.getArgs ( ).length == 1 && target != null) {
            if (battlePending.containsKey ( target.getUniqueId ( ) )) {
                source.sendMessage ( CC.translate ( "&8[&4Error&8] &cEse jugador ya tiene una solicitud pendiente." ) );
                return;
            }
            if (battlePending.containsValue ( source.getUniqueId ( ) )) {
                source.sendMessage ( CC.translate ( "&8[&4Error&8] &cYa tienes una solicitud de duelo activa." ) );
                return;
            }
            target.sendMessage ( CC.translate ( "&8[&c&lBattle&8] &a¡El jugador &2" + source.getName ( ) + " &a te ha desafiado a un &2Duelo!" ) );
            target.sendMessage ( CC.translate ( "&8[&c&lBattle&8] &aUtiliza el comando /duelo aceptar <jugador> para aceptar el &2Duelo" ) );
            source.sendMessage ( CC.translate ( "&8[&c&lBattle&8] &a¡Haz desafiado al jugador &2" + target.getName ( ) + " &aa un duelo a &2amistoso!" ) );
            battlePending.put ( target.getUniqueId ( ), source.getUniqueId ( ) );
            scheduleExpiration ( source, target );
            return;
        }
        if (command.getArgs ( ).length < 2) {
            source.sendMessage ( CC.translate ( "&8[&4Error&8] &cAsegurate de escribir bien el comando" ) );
            return;
        }
        String args0 = command.getArgs ( 0 );
        switch (args0) {
            case "aceptar":
                target = Main.instance.getServer ( ).getPlayer ( command.getArgs ( 1 ) );
                if (target == null) {
                    source.sendMessage ( CC.translate ( "&8[&4Error&8] &c¡El jugador no se encuentra conectado!" ) );
                    return;
                }
                if (!battlePending.containsKey ( source.getUniqueId ( ) ) ||
                        !battlePending.get ( source.getUniqueId ( ) ).equals ( target.getUniqueId ( ) )) {
                    source.sendMessage ( CC.translate ( "&8[&4Error&8] &cNo tienes ningún duelo pendiente con ese jugador." ) );
                    return;
                }

                String rankTarget = org.delaware.tools.General.getRank ( target );
                String rankSource = General.getRank ( source );
                if (!canBattle ( rankSource, rankTarget )) {
                    source.sendMessage ( CC.translate ( "&8[&4Error&8] &c¡La diferencia de rangos es demasiado alta!" ) );
                    return;
                }
                source.sendMessage ( CC.translate ( "&a¡Has aceptado el duelo de &2" + target.getName ( ) + "&a!" ) );
                target.sendMessage ( CC.translate ( "&a¡" + source.getName ( ) + " ha aceptado tu duelo!" ) );
                battlePending.remove ( source.getUniqueId ( ) );
                BattleTask.cancelExpiration ( target.getUniqueId ( ) );
                BattleManager.teleportToArena ( source, target, rankSource );
                break;
            case "rechazar":
                target = Main.instance.getServer ( ).getPlayer ( command.getArgs ( 1 ) );
                if (target == null) {
                    source.sendMessage ( CC.translate ( "&8[&4Error&8] &c¡El jugador no se encuentra conectado!" ) );
                    return;
                }
                if (battlePending.containsKey ( source.getUniqueId ( ) )
                        && battlePending.get ( source.getUniqueId ( ) ).equals ( target.getUniqueId ( ) )) {
                    battlePending.remove ( source.getUniqueId ( ) );
                    source.sendMessage ( CC.translate ( "&cHas rechazado el duelo de &4" + target.getName ( ) + "&c." ) );
                    target.sendMessage ( CC.translate ( "&4" + source.getName ( ) + " &cha rechazado tu duelo." ) );
                    BattleTask.cancelExpiration ( target.getUniqueId ( ) );
                } else {
                    source.sendMessage ( CC.translate ( "&8[&4Error&8] &cNo tienes una solicitud de ese jugador." ) );
                }
                break;

        }

    }

    public static boolean canBattle ( String rank1, String rank2 ) {
        String[] ranks = {"F", "E", "D", "C", "B", "A", "A+", "S", "S+", "Z", "Z+"};
        int index1 = -1, index2 = -1;

        for (int i = 0; i < ranks.length; i++) {
            if (ranks[i].equalsIgnoreCase ( rank1 )) index1 = i;
            if (ranks[i].equalsIgnoreCase ( rank2 )) index2 = i;
        }

        if (index1 == -1 || index2 == -1) return false;
        return Math.abs ( index1 - index2 ) <= 1;
    }

}
