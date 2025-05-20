package io.github.futurecore.utils.data.KairosDataHandler;

import io.github.futurecore.utils.data.KairosData.ItemKairos;
import io.github.futurecore.utils.data.KairosData.PKairos;

import java.io.Serializable;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class KairosDataHandler implements Serializable {
    public static final CopyOnWriteArrayList<PKairos> pKairos = new CopyOnWriteArrayList<> ( );
    public static final ConcurrentHashMap<Integer,ItemKairos> itemsKairos = new ConcurrentHashMap (  );
    public static PKairos findPKairos ( UUID playerUUID ) {
        return pKairos.stream ( ).filter ( e -> e.getPlayerUUID ( ).equals ( playerUUID ) )
                .findFirst ( ).get ( );
    }

    public static void refreshPKairos ( PKairos pKairos ) {
        KairosDataHandler.pKairos.remove ( pKairos );
        KairosDataHandler.pKairos.add ( pKairos );
    }
}
