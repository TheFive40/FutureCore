package io.github.futurecore.utils.handlers.battle;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class BattleData {

    public static final HashMap<UUID, UUID> battlePending = new HashMap<>();

    private static final Set<String> activeBattles = new HashSet<>();

    public static void startBattle(UUID p1, UUID p2) {
        activeBattles.add(pairKey(p1, p2));
    }

    public static void endBattle(UUID p1, UUID p2) {
        activeBattles.remove(pairKey(p1, p2));
    }

    public static boolean inBattle(UUID p1, UUID p2) {
        return activeBattles.contains(pairKey(p1, p2));
    }
    public static UUID getOpponent(UUID playerId) {
        for (String key : activeBattles) {
            String[] parts = key.split(":");
            UUID p1 = UUID.fromString(parts[0]);
            UUID p2 = UUID.fromString(parts[1]);
            if (p1.equals(playerId)) return p2;
            if (p2.equals(playerId)) return p1;
        }
        return null;
    }
    // Genera una clave única para la pareja de jugadores (sin importar el orden)
    private static String pairKey(UUID p1, UUID p2) {
        return p1.compareTo(p2) < 0
                ? p1.toString() + ":" + p2.toString()
                : p2.toString() + ":" + p1.toString();
    }
}
