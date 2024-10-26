package cn.sandtripper.minecraft.sandJoin;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;

import java.util.HashSet;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class EventsHandler {

    private final SandJoin plugin;
    private HashSet<UUID> delayingPlayer = new HashSet<>();

    public EventsHandler(SandJoin plugin) {
        this.plugin = plugin;
    }

    @Subscribe
    public void onPostLogin(PostLoginEvent event) {
        if (plugin.getConfigManager().getPlayerJoinDelay() == 0) {
            plugin.playerLogin(event.getPlayer());
        } else {
            // 正确的延迟执行
            plugin.getServer().getScheduler().buildTask(plugin, () -> {
                        if (delayingPlayer.contains(event.getPlayer().getUniqueId())) {
                            plugin.playerLogin(event.getPlayer());
                            delayingPlayer.remove(event.getPlayer().getUniqueId());
                        }
                    }).delay(plugin.getConfigManager().getPlayerJoinDelay(), TimeUnit.MILLISECONDS)
                    .schedule();
            delayingPlayer.add(event.getPlayer().getUniqueId());
        }
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        if (delayingPlayer.contains(event.getPlayer().getUniqueId())) {
            delayingPlayer.remove(event.getPlayer().getUniqueId());
        } else {
            plugin.playerDisconnect(event.getPlayer());
        }
    }
}
