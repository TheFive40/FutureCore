package io.github.futurecore.events.customitems;

import io.github.futurecore.utils.CC;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.InheritanceNode;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.delaware.tools.NbtHandler.NbtHandler;

import java.util.Comparator;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class StaffRaidItem implements Listener {

    private final LuckPerms luckPerms;

    public StaffRaidItem () {
        RegisteredServiceProvider<LuckPerms> provider = Bukkit.getServicesManager ( ).getRegistration ( LuckPerms.class );
        if (provider == null) throw new IllegalStateException ( "LuckPerms no encontrado" );
        this.luckPerms = provider.getProvider ( );
    }

    @EventHandler
    public void onInteract ( PlayerInteractEvent event ) {
        Player player = event.getPlayer ( );
        ItemStack item = player.getItemInHand ( );
        if (item == null || item.getTypeId ( ) != 6145) return;
        NbtHandler nbt = new NbtHandler ( item );
        if (event.getAction ( ).name ( ).contains ( "RIGHT_CLICK" )) {
            User user = luckPerms.getUserManager ( ).getUser ( player.getUniqueId ( ) );
            if (user == null) {
                player.sendMessage ( CC.translate ( "&cError: No se pudo cargar el usuario desde LuckPerms." ) );
                return;
            }

            Set<String> inheritedGroups = user.getNodes ( ).stream ( )
                    .filter ( NodeType.INHERITANCE::matches )
                    .map ( node -> ((InheritanceNode) node).getGroupName ( ) )
                    .filter ( groupName -> !groupName.equalsIgnoreCase ( "default" ) )
                    .collect ( Collectors.toSet ( ) );

            if (inheritedGroups.isEmpty ( )) {
                player.sendMessage ( CC.translate ( "&eNo tienes rangos adicionales que guardar." ) );
                return;
            }

            Optional<Group> highest = inheritedGroups.stream ( )
                    .map ( groupName -> luckPerms.getGroupManager ( ).getGroup ( groupName ) )
                    .filter ( g -> g != null )
                    .max ( Comparator.comparingInt ( g -> g.getWeight ( ).orElse ( 0 ) ) );
            if (!highest.isPresent ( )) {
                player.sendMessage ( CC.translate ( "&cNo se pudo determinar tu rango más alto." ) );
                return;
            }
            String highestRank = highest.get ( ).getName ( );
            nbt.setString ( "rank", highestRank );
            user.data ( ).clear ( NodeType.INHERITANCE.predicate ( n -> !n.getGroupName ( ).equalsIgnoreCase ( "default" ) ) );
            luckPerms.getUserManager ( ).saveUser ( user );
            player.sendMessage ( CC.translate ( "&aSe ha guardado tu rango &f" + highestRank + " &ay removido temporalmente." ) );
            player.playSound ( player.getLocation ( ), Sound.LEVEL_UP, 1.0f, 1.0f );
            player.setItemInHand ( nbt.getItemStack () );
        } else if (event.getAction ( ).name ( ).contains ( "LEFT_CLICK" )) {
            String rankStored = nbt.getString("rank");
            if (rankStored != null && !rankStored.isEmpty ( )) {
                User user = luckPerms.getUserManager ( ).getUser ( player.getUniqueId ( ) );
                if (user == null) {
                    player.sendMessage ( CC.translate ( "&cError: No se pudo cargar el usuario desde LuckPerms." ) );
                    return;
                }

                InheritanceNode rankNode = InheritanceNode.builder ( rankStored ).build ( );
                user.data ( ).add ( rankNode );
                luckPerms.getUserManager ( ).saveUser ( user );

                player.sendMessage ( CC.translate ( "&aTu rango &f" + rankStored + " &aha sido restaurado." ) );
                player.playSound ( player.getLocation ( ), Sound.LEVEL_UP, 1.0f, 2.0f );

                nbt.setString ( "rank", "" );
            }
        }
    }
}
