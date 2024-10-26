package cn.sandtripper.minecraft.sandJoin;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bstats.velocity.Metrics;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;

@Plugin(id = "sandjoin", name = "SandJoin", version = "1.0", authors = {"沙酱紫漏"})
public class SandJoin {

    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;
    private final Metrics.Factory metricsFactory;
    private ConfigManager configManager;
    private PlayerDataManager playerDataManager;
    private Queue<Long> joinLeaveQueue;
    private Queue<Long> firstJoinQueue;
    private boolean isStopped;

    @Inject
    public SandJoin(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory, Metrics.Factory metricsFactory) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
        this.metricsFactory = metricsFactory;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        int pluginId = 23722;
        Metrics metrics = metricsFactory.make(this, pluginId);

        // Plugin startup logic
        this.configManager = new ConfigManager(this, dataDirectory);
        this.playerDataManager = new PlayerDataManager(this, dataDirectory);

        loadConfigurations();

        this.joinLeaveQueue = new ArrayDeque<>();
        this.firstJoinQueue = new ArrayDeque<>();
        this.isStopped = false;

        // Register event listeners
        server.getEventManager().register(this, new EventsHandler(this));

        // Register command
        server.getCommandManager().register("sandjoin", new CommandHandler(this));

        // Display startup message
        logger.info("SandJoin has been enabled!");
    }

    private void loadConfigurations() {
        configManager.loadConfig();
    }

    public void playerLogin(Player player) {
        if (isStopped || configManager.getIgnoreSet().contains(player.getUsername())) {
            return;
        }

        CompletableFuture<Boolean> isNewPlayerFuture = playerDataManager.isNewPlayer(player.getUniqueId());
        isNewPlayerFuture.thenAccept(isNewPlayer -> {
            if (isNewPlayer) {
                handleFirstJoin(player);
            } else {
                handleRegularJoin(player);
            }
        });
    }

    private void handleFirstJoin(Player player) {
        boolean check = checkUpdateFirstJoinLimit();
        if (check && configManager.isFirstJoinEnabled()) {
            CompletableFuture.runAsync(() -> executeOperations(configManager.getFirstJoinOperations(), player));
        }

        if (check || !configManager.isNotWriteFirstJoin()) {
            playerDataManager.addPlayer(player.getUniqueId());
        }
    }

    private void handleRegularJoin(Player player) {
        boolean check = checkUpdateJoinLeaveLimit();
        if (check && configManager.isJoinEnabled()) {
            CompletableFuture.runAsync(() -> executeOperations(configManager.getJoinOperations(), player));
        }
    }

    public void playerDisconnect(Player player) {
        if (isStopped || configManager.getIgnoreSet().contains(player.getUsername())) {
            return;
        }

        boolean check = checkUpdateJoinLeaveLimit();
        if (check && configManager.isLeaveEnabled()) {
            CompletableFuture.runAsync(() -> executeOperations(configManager.getLeaveOperations(), player));
        }
    }

    private boolean checkUpdateJoinLeaveLimit() {
        long currentTime = System.currentTimeMillis() / 1000;
        joinLeaveQueue.offer(currentTime);
        while (!joinLeaveQueue.isEmpty() && currentTime - joinLeaveQueue.peek() > configManager.getPlayerJoinLeaveSeconds()) {
            joinLeaveQueue.poll();
        }
        return joinLeaveQueue.size() <= configManager.getPlayerJoinLeaveLimit();
    }

    private boolean checkUpdateFirstJoinLimit() {
        long currentTime = System.currentTimeMillis() / 1000;
        firstJoinQueue.offer(currentTime);
        while (!firstJoinQueue.isEmpty() && currentTime - firstJoinQueue.peek() > configManager.getPlayerFirstJoinSeconds()) {
            firstJoinQueue.poll();
        }
        return firstJoinQueue.size() <= configManager.getPlayerFirstJoinLimit();
    }

    private void executeOperations(List<String> operations, Player player) {
        for (String operation : operations) {
            String[] parts = operation.split(" ", 2);
            if (parts.length != 2) {
                logger.warn("Invalid operation format: " + operation);
                continue;
            }

            String type = parts[0].toUpperCase().replace("[", "").replace("]", "");
            String content = parts[1].replace("{PLAYER}", player.getUsername());

            switch (type) {
                case "TEXT":
                    sendMessage(player, content, "TEXT");
                    break;
                case "MINIMESSAGE":
                    sendMessage(player, content, "MINIMESSAGE");
                    break;
                case "JSON":
                    sendMessage(player, content, "JSON");
                    break;
                case "TITLE":
                    sendTitle(player, content);
                    break;
                case "BOSSBAR":
                    sendBossBar(player, content);
                    break;
                case "ACTIONBAR":
                    sendActionBar(player, content);
                    break;
                case "BC_TEXT":
                case "BC_MINIMESSAGE":
                case "BC_JSON":
                    broadcastMessage(content, type.substring(3));
                    break;
                case "BC_TITLE":
                    broadcastTitle(content);
                    break;
                case "BC_BOSSBAR":
                    broadcastBossBar(content);
                    break;
                case "BC_ACTIONBAR":
                    broadcastActionBar(content);
                    break;
                case "DELAY":
                    delay(Integer.parseInt(content));
                    break;
//                case "COMMAND":
//                    executeCommand(player, content);
//                    break;
                default:
                    logger.warn("Unknown operation type: " + type);
            }
        }
    }

    private void sendMessage(Player player, String content, String type) {
        Component component = parseComponent(content, type);
        player.sendMessage(component);
    }

    private void sendTitle(Player player, String content) {
        try {
            JsonObject jsonContent = new Gson().fromJson(content, JsonObject.class);

            Component title = parseComponent(jsonContent.get("title").getAsString(), "TEXT");
            Component subtitle = parseComponent(jsonContent.get("sub-title").getAsString(), "TEXT");

            int fadeIn = jsonContent.get("fade-in").getAsInt();
            int stay = jsonContent.get("stay").getAsInt();
            int fadeOut = jsonContent.get("fade-out").getAsInt();

            Title.Times times = Title.Times.times(
                    Duration.ofMillis(fadeIn),
                    Duration.ofMillis(stay),
                    Duration.ofMillis(fadeOut)
            );

            Title titleObj = Title.title(title, subtitle, times);
            player.showTitle(titleObj);
        } catch (Exception e) {
            logger.error("Error parsing or sending title: " + content, e);
        }
    }

    private void sendBossBar(Player player, String content) {
        try {
            JsonObject jsonContent = new Gson().fromJson(content, JsonObject.class);

            Component message = parseComponent(jsonContent.get("content").getAsString(), "TEXT");
            int stayMillis = jsonContent.get("stay").getAsInt();
            String colorString = jsonContent.get("color").getAsString().toUpperCase();

            BossBar.Color color = BossBar.Color.valueOf(colorString);
            if (color == null) {
                color = BossBar.Color.WHITE; // 默认颜色
            }

            BossBar bossBar = BossBar.bossBar(message, 1.0f, color, BossBar.Overlay.PROGRESS);
            player.showBossBar(bossBar);

            // 安排在指定时间后隐藏 boss bar
            server.getScheduler()
                    .buildTask(this, () -> player.hideBossBar(bossBar))
                    .delay(Duration.ofMillis(stayMillis))
                    .schedule();
        } catch (Exception e) {
            logger.error("Error parsing or sending boss bar: " + content, e);
        }
    }

    private void sendActionBar(Player player, String content) {
        Component component = parseComponent(content, "TEXT");
        player.sendActionBar(component);
    }

    private void broadcastMessage(String content, String type) {
        Component component = parseComponent(content, type);
        server.getAllPlayers().forEach(p -> p.sendMessage(component));
    }

    private void broadcastTitle(String content) {
        server.getAllPlayers().forEach(p -> sendTitle(p, content));
    }

    private void broadcastBossBar(String content) {
        server.getAllPlayers().forEach(p -> sendBossBar(p, content));
    }

    private void broadcastActionBar(String content) {
        Component component = parseComponent(content, "TEXT");
        server.getAllPlayers().forEach(p -> p.sendActionBar(component));
    }

    private void delay(int milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            logger.error("Delay interrupted", e);
        }
    }

//    private void executeCommand(Player player, String command) {
//        server.getCommandManager().executeAsync(player, command);
//    }

    private Component parseComponent(String content, String type) {
        switch (type.toUpperCase()) {
            case "TEXT":
                return LegacyComponentSerializer.legacyAmpersand().deserialize(content);
            case "MINIMESSAGE":
                return MiniMessage.miniMessage().deserialize(content);
            case "JSON":
                return GsonComponentSerializer.gson().deserialize(content);
            default:
                logger.warn("Invalid content type: " + type + ". Defaulting to TEXT.");
                return LegacyComponentSerializer.legacyAmpersand().deserialize(content);
        }
    }

    public void stop() {
        isStopped = true;
    }

    public void start() {
        isStopped = false;
    }

    public void reload() {
        CompletableFuture.runAsync(() -> {
            configManager.loadConfig();
            playerDataManager.reloadDatabase();
        });
    }

    public ProxyServer getServer() {
        return server;
    }

    public Logger getLogger() {
        return logger;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public Path getDataDirectory() {
        return dataDirectory;
    }
}
