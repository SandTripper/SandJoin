package cn.sandtripper.minecraft.sandJoin;

import com.moandjiezana.toml.Toml;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ConfigManager {

    private final SandJoin plugin;
    private final Path configPath;
    private Toml config;

    private Set<String> ignoreSet;
    private int playerJoinLeaveSeconds;
    private int playerJoinLeaveLimit;
    private int playerFirstJoinSeconds;
    private int playerFirstJoinLimit;
    private boolean notWriteFirstJoin;
    private int playerJoinDelay;

    private boolean firstJoinEnabled;
    private List<String> firstJoinOperations;

    private boolean joinEnabled;
    private List<String> joinOperations;

    private boolean leaveEnabled;
    private List<String> leaveOperations;

    public ConfigManager(SandJoin plugin, Path dataDirectory) {
        this.plugin = plugin;
        this.configPath = dataDirectory.resolve("config.toml");
    }

    public void loadConfig() {
        if (!Files.exists(configPath)) {
            saveDefaultConfig();
        }

        config = new Toml().read(configPath.toFile());

        ignoreSet = new HashSet<>(config.getList("other.ignore-players"));
        playerJoinLeaveSeconds = config.getLong("anti-flood.player-join-leave-seconds").intValue();
        playerJoinLeaveLimit = config.getLong("anti-flood.player-join-leave-limit").intValue();
        playerFirstJoinSeconds = config.getLong("anti-flood.player-first-join-seconds").intValue();
        playerFirstJoinLimit = config.getLong("anti-flood.player-first-join-limit").intValue();
        notWriteFirstJoin = config.getBoolean("anti-flood.not-write-first-join");
        playerJoinDelay = config.getLong("anti-flood.player-join-delay").intValue();

        firstJoinEnabled = config.getBoolean("player-first-join.enabled");
        firstJoinOperations = config.getList("player-first-join.operations");

        joinEnabled = config.getBoolean("player-join.enabled");
        joinOperations = config.getList("player-join.operations");

        leaveEnabled = config.getBoolean("player-leave.enabled");
        leaveOperations = config.getList("player-leave.operations");
    }

    private void saveDefaultConfig() {
        try {
            Files.createDirectories(configPath.getParent());
            Files.copy(plugin.getClass().getResourceAsStream("/config.toml"), configPath);
        } catch (IOException e) {
            plugin.getLogger().error("Unable to save default config file", e);
        }
    }

    public void reloadConfig() {
        loadConfig();
    }

    // Getters for all config values

    public Set<String> getIgnoreSet() {
        return ignoreSet;
    }

    public int getPlayerJoinLeaveSeconds() {
        return playerJoinLeaveSeconds;
    }

    public int getPlayerJoinLeaveLimit() {
        return playerJoinLeaveLimit;
    }

    public int getPlayerFirstJoinSeconds() {
        return playerFirstJoinSeconds;
    }

    public int getPlayerFirstJoinLimit() {
        return playerFirstJoinLimit;
    }

    public boolean isNotWriteFirstJoin() {
        return notWriteFirstJoin;
    }

    public int getPlayerJoinDelay() {
        return playerJoinDelay;
    }

    public boolean isFirstJoinEnabled() {
        return firstJoinEnabled;
    }

    public List<String> getFirstJoinOperations() {
        return firstJoinOperations;
    }

    public boolean isJoinEnabled() {
        return joinEnabled;
    }

    public List<String> getJoinOperations() {
        return joinOperations;
    }

    public boolean isLeaveEnabled() {
        return leaveEnabled;
    }

    public List<String> getLeaveOperations() {
        return leaveOperations;
    }
}
