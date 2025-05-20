package io.github.futurecore.utils.data.KairosData;

import io.github.futurecore.Main;
import io.github.futurecore.utils.CC;
import io.github.futurecore.utils.General;
import io.github.futurecore.utils.data.KairosDataHandler.KairosDataHandler;
import io.github.futurecore.utils.handlers.nbt.NBTPhandler;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Player;
import org.delaware.events.PlayerConsumeTP;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.Objects;
import java.util.UUID;

@Getter
@Setter
public class PKairos implements Serializable {
    private double amount;
    private UUID playerUUID;
    private String username;
    private double boosterMultiplier = General.BOOSTER_MULTIPLIER;
    private double loss = General.LOSS_KAIROS;

    public PKairos ( UUID playerUUID ) {
            this.playerUUID = playerUUID;
        try {
            this.amount = KairosDataHandler.findPKairos ( playerUUID ).getAmount ( );
        } catch (Exception exception) {
            this.amount = 0;
        }
        this.username = toMCPlayer ( ).getName ( );
        if (!KairosDataHandler.pKairos.contains ( this )){
            KairosDataHandler.pKairos.add ( this );
        }
    }

    @Override
    public boolean equals ( Object object ) {
        if (this == object) return true;
        if (!(object instanceof PKairos)) return false;
        PKairos pKairos = (PKairos) object;
        return Objects.equals ( getPlayerUUID ( ), pKairos.getPlayerUUID ( ) );
    }

    @Override
    public int hashCode () {
        return Objects.hashCode ( getPlayerUUID ( ) );
    }

    public void pay(double amount) {
        this.amount += amount;
        Player player = toMCPlayer();
        if (player == null) return;

        DecimalFormat formatter = new DecimalFormat("#,###");
        if (amount >= 0) {
            player.sendMessage(CC.translate("&d¡Has recibido &5" + formatter.format(amount) + " &dKairos en tu cuenta!"));
        } else {
            player.sendMessage(CC.translate("&c¡Se han descontado &4" + formatter.format(-amount) + " &cKairos de tu cuenta!"));
        }

        KairosDataHandler.refreshPKairos(this);
    }

    public void pay ( double amount, UUID toPlayerUUID ) {
        PKairos pKairos = new PKairos ( toPlayerUUID );
        this.amount -= amount;
        pKairos.pay ( amount );
        KairosDataHandler.refreshPKairos ( this );
    }

    public void balance () {
        Player player = toMCPlayer ( );
        if (player == null) return;
        DecimalFormat formatter = new DecimalFormat("#,###");
        player.sendMessage ( CC.translate ( "&dBalance: &5" + formatter.format ( amount ) + "&d Kairos" ) );
    }

    public void lossKairos () {
        Player player = toMCPlayer ( );
        if (player == null) return;
        this.amount -= this.loss;
        player.sendMessage ( CC.translate ( "&c¡Haz perdido &4" + loss + "&cKairos!" ) );
        KairosDataHandler.refreshPKairos ( this );
    }

    public Player toMCPlayer () {
        return Main.instance.getServer ( ).getPlayer ( playerUUID );
    }



}
