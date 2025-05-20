package io.github.futurecore.commands.player.cmdBosses;

import io.github.futurecore.utils.CC;
import io.github.futurecore.utils.commands.BaseCommand;
import io.github.futurecore.utils.commands.Command;
import io.github.futurecore.utils.commands.CommandArgs;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.HashMap;

public class CmdBosses extends BaseCommand {
    public static CopyOnWriteArrayList<String> npcNames = new CopyOnWriteArrayList<>();
    public static HashMap<String, Integer> npcRarity = new HashMap<>();

    @Command(name = "bosses", aliases = "bosses")
    @Override
    public void onCommand(CommandArgs command) throws IOException {
        CommandSender sender = command.getSender();

        if (!(sender instanceof Player)) {
            sender.sendMessage("Este comando solo puede ser usado por jugadores.");
            return;
        }

        String[] args = command.getArgs();

        if (args.length < 1) {
            sender.sendMessage(CC.translate("§cUso correcto:"));
            sender.sendMessage(CC.translate("§7 - /bosses <npc> [rareza]"));
            sender.sendMessage(CC.translate("§7 - /bosses remove <npc>"));
            sender.sendMessage(CC.translate("§7 - /bosses setrarity <npc> <nueva_rareza>"));
            return;
        }

        String sub = args[0].toLowerCase();
        if (sub.equals("remove")) {
            if (args.length < 2) {
                sender.sendMessage(CC.translate("§cUso: /bosses remove <npc>"));
                return;
            }
            String target = args[1];
            if (npcNames.remove(target)) {
                npcRarity.remove(target);
                sender.sendMessage(CC.translate("§cNPC eliminado: §f" + target));
            } else {
                sender.sendMessage(CC.translate("§eEse NPC no está registrado."));
            }
            return;
        }
        if (sub.equals("setrarity")) {
            if (args.length < 3) {
                sender.sendMessage(CC.translate("§cUso: /bosses setrarity <npc> <nueva_rareza>"));
                return;
            }

            String target = args[1];
            if (!npcNames.contains(target)) {
                sender.sendMessage(CC.translate("§eEse NPC no está registrado."));
                return;
            }

            try {
                int newRarity = Integer.parseInt(args[2]);
                if (newRarity < 1 || newRarity > 100) {
                    sender.sendMessage(CC.translate("§cLa rareza debe ser un número entre 1 y 100."));
                    return;
                }

                npcRarity.put(target, newRarity);
                sender.sendMessage(CC.translate("§aRareza del NPC §f" + target + " §aactualizada a §e" + newRarity + "%"));
            } catch (NumberFormatException e) {
                sender.sendMessage(CC.translate("§cEl valor de rareza debe ser un número."));
            }
            return;
        }
        String npcName = args[0];

        if (npcNames.contains(npcName)) {
            sender.sendMessage(CC.translate("§eEl NPC ya está registrado."));
            return;
        }

        if (args.length > 1) {
            try {
                int rarity = Integer.parseInt(args[1]);
                if (rarity < 1 || rarity > 100) {
                    sender.sendMessage(CC.translate("§cLa rareza debe ser un número entre 1 y 100."));
                    return;
                }
                npcRarity.put(npcName, rarity);
                sender.sendMessage(CC.translate("§aNPC registrado: §f" + npcName + " con rareza del " + rarity + "%"));
            } catch (NumberFormatException e) {
                sender.sendMessage(CC.translate("§cEl valor de rareza debe ser un número."));
            }
        } else {
            npcRarity.put(npcName, 100);
            sender.sendMessage(CC.translate("§aNPC registrado: §f" + npcName + " con rareza del 100%"));
        }

        npcNames.add(npcName);
    }

}
