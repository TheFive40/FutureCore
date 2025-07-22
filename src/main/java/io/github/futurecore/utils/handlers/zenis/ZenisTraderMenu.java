package io.github.futurecore.utils.handlers.zenis;

import com.earth2me.essentials.Essentials;
import com.earth2me.essentials.IEssentials;
import com.earth2me.essentials.User;
import com.earth2me.essentials.api.Economy;
import fr.minuskube.inv.ClickableItem;
import fr.minuskube.inv.SmartInventory;
import fr.minuskube.inv.content.InventoryContents;
import fr.minuskube.inv.content.InventoryProvider;
import io.github.futurecore.utils.data.KairosData.PKairos;
import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.math.BigDecimal;

public class ZenisTraderMenu implements InventoryProvider {

    public static SmartInventory getInventory () {
        return SmartInventory.builder ( )
                .id ( "zenis_trader" )
                .provider ( new ZenisTraderMenu ( ) )
                .size ( 3, 9 )
                .title ( "§a☼ Intercambiador ☼" )
                .build ( );
    }

    @Override
    public void init ( Player player, InventoryContents contents ) {
        addZenisButton ( contents, player, 1, 1, 100 );
        addZenisButton ( contents, player, 1, 3, 500 );
        addZenisButton ( contents, player, 1, 5, 1000 );
        addZenisButton ( contents, player, 1, 7, 10_000 );
        ItemStack border = new ItemStack ( Material.STAINED_GLASS_PANE, 1, DyeColor.LIME.getData ( ) );
        for (int col = 0; col < 9; col++) {
            contents.set ( 0, col, ClickableItem.empty ( border ) );
        }
        for (int col = 0; col < 9; col++) {
            contents.set ( 2, col, ClickableItem.empty ( border ) );
        }
        contents.set ( 1, 0, ClickableItem.empty ( border ) );
        contents.set ( 1, 8, ClickableItem.empty ( border ) );
        contents.set ( 1, 2, ClickableItem.empty ( border ) );
        contents.set ( 1, 4, ClickableItem.empty ( border ) );
        contents.set ( 1, 6, ClickableItem.empty ( border ) );
    }


    private void addZenisButton ( InventoryContents contents, Player player, int row, int col, int amount ) {
        ItemStack zenisItem = ZenisTraderManager.getZenisItem ( amount );
        contents.set ( row, col, ClickableItem.of ( zenisItem, e -> {
            double balance = obtenerBalance ( player );
            if (balance < amount) {
                player.sendMessage ( "§cNo tienes suficientes Zenis para este intercambio." );
                return;
            }

            descontarDinero ( player, amount );
            player.updateInventory ( );
            player.getInventory ( ).addItem ( zenisItem );
            player.sendMessage ( "§aIntercambiaste §e" + amount + " Zenis §apor ítems físicos." );
            player.playSound ( player.getLocation ( ), Sound.LEVEL_UP, 1f, 1f );
        } ) );
    }

    private static Economy econ;

    public static double obtenerBalance ( Player jugador ) {
        IEssentials essentials = (IEssentials) Bukkit.getServer ( ).getPluginManager ( ).getPlugin ( "Essentials" );
        if (essentials != null) {
            User user = essentials.getUser ( jugador );
            return user.getMoney ( ).doubleValue ( );

        }
        return 0.0;
    }

    public void descontarDinero ( Player jugador, double cantidad ) {
        Essentials essentials = (Essentials) Bukkit.getPluginManager ( ).getPlugin ( "Essentials" );
        if (essentials == null) return;

        User user = essentials.getUser ( jugador );

        try {
            BigDecimal saldoActual = user.getMoney ( );
            if (saldoActual.compareTo ( BigDecimal.valueOf ( cantidad ) ) >= 0) {
                user.setMoney ( saldoActual.subtract ( BigDecimal.valueOf ( cantidad ) ) );
                jugador.sendMessage ( "§aSe te ha descontado §c$" + cantidad );
            } else {
                jugador.sendMessage ( "§cNo tienes suficiente dinero." );
            }
        } catch (Exception e) {
            jugador.sendMessage ( "§cError al modificar tu saldo." );
            e.printStackTrace ( );
        }
    }

    @Override
    public void update ( Player player, InventoryContents contents ) {
    }
}
