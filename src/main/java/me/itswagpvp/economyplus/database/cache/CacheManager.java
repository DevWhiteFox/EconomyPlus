package me.itswagpvp.economyplus.database.cache;

import me.itswagpvp.economyplus.EconomyPlus;
import me.itswagpvp.economyplus.database.misc.DatabaseType;
import org.bukkit.Bukkit;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static me.itswagpvp.economyplus.EconomyPlus.plugin;

public class CacheManager {

    private static final ConcurrentHashMap<String, Double> cachedPlayersMoneys = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Double> cachedPlayersBanks = new ConcurrentHashMap<>();

    // For MySQL create a separated thread.
    public static Thread dbUpdater;

    /**
     * @return ConcurrentHashMap for moneys and banks
     *
     * @param selector (1 = moneys, 2 = bank)
     **/
    public static ConcurrentHashMap<String, Double> getCache(int selector) {
        if (selector == 1) {
            return cachedPlayersMoneys;
        } else if (selector == 2) {
            return cachedPlayersBanks;
        }

        return new ConcurrentHashMap<>();
    }

    public int cacheDatabase() {
        AtomicInteger i = new AtomicInteger();
        if (EconomyPlus.getDBType() == DatabaseType.H2 || EconomyPlus.getDBType() == DatabaseType.YAML) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                for (String player : EconomyPlus.getDBType().getList()) {
                    cachedPlayersMoneys.put(player, EconomyPlus.getDBType().getToken(player));
                    i.getAndIncrement();
                }

                for (String player : EconomyPlus.getDBType().getList()) {
                    cachedPlayersBanks.put(player, EconomyPlus.getDBType().getBank(player));
                }
            });
        } else {
            Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
                dbUpdater = new Thread(() -> {
                    for (String player : EconomyPlus.getDBType().getList()) {
                        cachedPlayersMoneys.put(player, EconomyPlus.getDBType().getToken(player));
                        i.getAndIncrement();
                    }

                    for (String player : EconomyPlus.getDBType().getList()) {
                        cachedPlayersBanks.put(player, EconomyPlus.getDBType().getBank(player));
                    }
                });

                dbUpdater.start();
            }, 0, 100);
        }
        return i.get();
    }

    public void startAutoSave() {
        long refreshRate = plugin.getConfig().getLong("Database.Cache.Auto-Save", 300) * 20L;

        if (EconomyPlus.getDBType() != DatabaseType.MySQL) {
            Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
                int savedAccounts = cacheDatabase();
                if (EconomyPlus.debugMode) Bukkit.getConsoleSender().sendMessage(
                        "[EconomyPlus-Debug] Cached §6%accounts% §7accounts..."
                                .replace("%accounts%", "" + savedAccounts));
            }, 0L, refreshRate);
        }
    }

}
