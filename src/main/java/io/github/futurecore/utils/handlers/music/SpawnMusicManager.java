package io.github.futurecore.utils.handlers.music;

import io.github.futurecore.utils.RegionUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.delaware.Main;

import java.time.Instant;
import java.util.*;

public class SpawnMusicManager {

    private static final String REGION_NAME = "spawn";

    private static final List<Song> PLAYLIST = Arrays.asList(
            new Song("dbctools:dandan", 109),  // 1:49
            new Song("dbctools:kai", 91),    // 1:31
            new Song("dbctools:angeles", 229),  // 3:49
            new Song("dbctools:power", 236)    // 3:56
    );

    private static final Map<String, Instant> musicStartTime = new HashMap<>();
    private static final Map<String, Integer> playlistIndex = new HashMap<>();

    public static void startMusicTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player :Main.instance.getServer ().getOnlinePlayers ()) {
                    if (RegionUtils.isLocationInRegion(player.getLocation(), REGION_NAME)) {
                        Instant start = musicStartTime.get(player.getName());
                        Integer index = playlistIndex.getOrDefault(player.getName(), 0);
                        if (start == null || Instant.now().isAfter(start.plusSeconds(PLAYLIST.get(index).duration))) {
                            playNextSong(player);
                        }
                    } else {
                        musicStartTime.remove(player.getName());
                        playlistIndex.remove(player.getName());
                    }
                }
            }
        }.runTaskTimerAsynchronously(Main.instance, 20L, 20L); // 1 segundo
    }
    private static void playNextSong(Player player) {
        int index = playlistIndex.getOrDefault(player.getName(), 0);
        Song song = PLAYLIST.get(index);

        player.playSound(player.getLocation (), song.soundName, 1.0f, 1.0f);

        Instant now = Instant.now();
        musicStartTime.put(player.getName(), now);

        playlistIndex.put(player.getName(), (index + 1) % PLAYLIST.size());
    }

    private static class Song {
        String soundName;
        int duration;

        Song(String soundName, int duration) {
            this.soundName = soundName;
            this.duration = duration;
        }
    }
}
