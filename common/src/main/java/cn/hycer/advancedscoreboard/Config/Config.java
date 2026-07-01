package cn.hycer.advancedscoreboard.Config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import static cn.hycer.advancedscoreboard.Global.Global.logger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Config {
    public static final String CONFIG_FILE_NAME = "advanced_scoreboard.json";

    public static final String MINE_COUNT_INTERNAL_NAME = "mine_count";
    public static final String PLACE_COUNT_INTERNAL_NAME = "place_count";
    public static final String ONLINE_TIME_INTERNAL_NAME = "online_time";
    public static final String ELYTRA_DISTANCE_INTERNAL_NAME = "elytra_dist";
    public static final String DAMAGE_TAKEN_INTERNAL_NAME = "damage_taken";
    public static final String DEATHS_INTERNAL_NAME = "deaths";
    public static final String MOB_KILLS_INTERNAL_NAME = "mob_kills";
    public static final String LATENCY_INTERNAL_NAME = "latency";

    @JsonIgnore
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    private String border;
    private int switchInterval;
    private int saveInterval;
    private int maxDisplayNum;
    private Set<String> hiddenScoreboards = new HashSet<>();
    private List<ScoreboardItem> scoreboards = new ArrayList<>();

    @JsonIgnore
    private final File configFile;

    public Config() {
        this.configFile = null;
    }

    public Config(String configDirPath) {
        this.configFile = new File(configDirPath + File.separator + CONFIG_FILE_NAME);
        if (!this.configFile.exists()) {
            initDefaultConfig();
            saveConfig();
        } else {
            loadConfig();
        }
    }

    private void initDefaultConfig() {
        border = "===";
        switchInterval = 5;
        saveInterval = 5;
        maxDisplayNum = 15;

        ScoreboardItem mineCountBoard = new ScoreboardItem();
        mineCountBoard.setInternalName(MINE_COUNT_INTERNAL_NAME);
        mineCountBoard.setDisplayName("挖掘量");

        ScoreboardItem placeCountBoard = new ScoreboardItem();
        placeCountBoard.setInternalName(PLACE_COUNT_INTERNAL_NAME);
        placeCountBoard.setDisplayName("放置量");

        ScoreboardItem onlineTimeBoard = new ScoreboardItem();
        onlineTimeBoard.setInternalName(ONLINE_TIME_INTERNAL_NAME);
        onlineTimeBoard.setDisplayName("在线时长(h)");

        ScoreboardItem elytronBoard = new ScoreboardItem();
        elytronBoard.setInternalName(ELYTRA_DISTANCE_INTERNAL_NAME);
        elytronBoard.setDisplayName("飞行距离(km)");

        ScoreboardItem damageTakenBoard = new ScoreboardItem();
        damageTakenBoard.setInternalName(DAMAGE_TAKEN_INTERNAL_NAME);
        damageTakenBoard.setDisplayName("受到伤害");

        ScoreboardItem deathsBoard = new ScoreboardItem();
        deathsBoard.setInternalName(DEATHS_INTERNAL_NAME);
        deathsBoard.setDisplayName("死亡次数");

        ScoreboardItem mobKillsBoard = new ScoreboardItem();
        mobKillsBoard.setInternalName(MOB_KILLS_INTERNAL_NAME);
        mobKillsBoard.setDisplayName("击杀生物数");

        this.scoreboards.add(mineCountBoard);
        this.scoreboards.add(placeCountBoard);
        this.scoreboards.add(onlineTimeBoard);
        this.scoreboards.add(elytronBoard);
        this.scoreboards.add(damageTakenBoard);
        this.scoreboards.add(deathsBoard);
        this.scoreboards.add(mobKillsBoard);

        ScoreboardItem latencyBoard = new ScoreboardItem();
        latencyBoard.setInternalName(LATENCY_INTERNAL_NAME);
        latencyBoard.setDisplayName("延迟(ms)");

        this.scoreboards.add(latencyBoard);

        logger.info("default config initialization completed");
    }

    private void addMissingDefaultScoreboards() {
        List<String[]> defaults = List.of(
            new String[]{MINE_COUNT_INTERNAL_NAME, "挖掘量"},
            new String[]{PLACE_COUNT_INTERNAL_NAME, "放置量"},
            new String[]{ONLINE_TIME_INTERNAL_NAME, "在线时长(h)"},
            new String[]{ELYTRA_DISTANCE_INTERNAL_NAME, "飞行距离(km)"},
            new String[]{DAMAGE_TAKEN_INTERNAL_NAME, "受到伤害"},
            new String[]{DEATHS_INTERNAL_NAME, "死亡次数"},
            new String[]{MOB_KILLS_INTERNAL_NAME, "击杀生物数"},
            new String[]{LATENCY_INTERNAL_NAME, "延迟(ms)"}
        );

        for (String[] def : defaults) {
            if (getScoreboardByInternalName(def[0]) == null) {
                ScoreboardItem item = new ScoreboardItem();
                item.setInternalName(def[0]);
                item.setDisplayName(def[1]);
                this.scoreboards.add(item);
                logger.info("added missing default scoreboard: {} ({})", def[1], def[0]);
            }
        }
    }

    public void loadConfig() {
        if (this.configFile == null || !this.configFile.exists()) {
            logger.warn("the config file does not exist and cannot be loaded");
            return;
        }

        try {
            Config loadedConfig = OBJECT_MAPPER.readValue(
                    this.configFile,
                    Config.class
            );
            this.border = loadedConfig.getBorder() != null ? loadedConfig.getBorder() : "===";
            this.switchInterval = loadedConfig.getSwitchInterval();
            this.saveInterval = loadedConfig.getSaveInterval();
            this.maxDisplayNum = loadedConfig.getMaxDisplayNum() > 0 ? loadedConfig.getMaxDisplayNum() : 15;
            this.hiddenScoreboards = loadedConfig.getHiddenScoreboards() != null ? loadedConfig.getHiddenScoreboards() : new HashSet<>();
            this.scoreboards = loadedConfig.getScoreboards() != null ? loadedConfig.getScoreboards() : new ArrayList<>();
            addMissingDefaultScoreboards();
            for (ScoreboardItem item : this.scoreboards) {
                if ("elytron_distance".equals(item.getInternalName())) {
                    item.setInternalName(ELYTRA_DISTANCE_INTERNAL_NAME);
                    logger.info("migrated old internalName 'elytron_distance' to '{}'", ELYTRA_DISTANCE_INTERNAL_NAME);
                }
            }
            logger.info("config file loaded successfully: {}", this.configFile.getAbsolutePath());
        } catch (IOException e) {
            logger.error("load config failed, using default config: {}", e);
            initDefaultConfig();
        }
    }

    public void saveConfig() {
        if (this.configFile == null) {
            logger.warn("the config file path has not been initialized and cannot be saved");
            return;
        }

        try {
            File parentDir = this.configFile.getParentFile();
            if (!parentDir.exists()) {
                parentDir.mkdirs();
            }

            OBJECT_MAPPER.writeValue(this.configFile, this);
            logger.debug("config file saved: {}", this.configFile.getAbsolutePath());
        } catch (IOException e) {
            logger.error("save config failed: {}", e);
        }
    }

    public ScoreboardItem getScoreboardByInternalName(String internalName) {
        return this.scoreboards.stream()
                .filter(item -> internalName.equals(item.getInternalName()))
                .findFirst()
                .orElse(null);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("AdvancedScoreboardConfig {\n");
        sb.append("  border = ").append(border).append("\n");
        sb.append("  switchInterval = ").append(switchInterval).append("\n");
        sb.append("  saveInterval = ").append(saveInterval).append("\n");
        sb.append("  maxDisplayNum = ").append(maxDisplayNum).append("\n");
        sb.append("  scoreboards (").append(scoreboards.size()).append("个)：\n");

        for (int i = 0; i < scoreboards.size(); i++) {
            ScoreboardItem item = scoreboards.get(i);
            sb.append("    ").append(i + 1).append(". {\n");
            sb.append("      internalName = ").append(item.getInternalName()).append("\n");
            sb.append("      displayName  = ").append(item.getDisplayName()).append("\n");
            sb.append("      data         = ").append(item.getData()).append("\n");
            sb.append("    }\n");
        }

        sb.append("  configFile = ").append(configFile != null ? configFile.getAbsolutePath() : "null").append("\n");
        sb.append("}");
        return sb.toString();
    }

    public String getBorder() {
        return border;
    }

    public void setBorder(String border) {
        this.border = border != null ? border : "===";
    }

    public String getFormattedDisplayName(ScoreboardItem item) {
        return border + item.getDisplayName() + border;
    }

    public int getSwitchInterval() {
        return switchInterval;
    }

    public void setSwitchInterval(int switchInterval) {
        this.switchInterval = Math.max(1, switchInterval);
    }

    public int getSaveInterval() {
        return saveInterval;
    }

    public void setSaveInterval(int saveInterval) {
        this.saveInterval = Math.max(1, saveInterval);
    }

    public int getMaxDisplayNum() {
        return maxDisplayNum;
    }

    public void setMaxDisplayNum(int maxDisplayNum) {
        this.maxDisplayNum = Math.max(1, maxDisplayNum);
    }

    public List<ScoreboardItem> getScoreboards() {
        return scoreboards;
    }

    public void setScoreboards(List<ScoreboardItem> scoreboards) {
        this.scoreboards = scoreboards != null ? scoreboards : new ArrayList<>();
    }

    @JsonIgnore
    public File getConfigFile() {
        return configFile;
    }

    public Set<String> getHiddenScoreboards() {
        return hiddenScoreboards;
    }

    public void setHiddenScoreboards(Set<String> hiddenScoreboards) {
        this.hiddenScoreboards = hiddenScoreboards != null ? hiddenScoreboards : new HashSet<>();
    }

    public boolean toggleScoreboardVisibility(String internalName) {
        if (hiddenScoreboards.contains(internalName)) {
            hiddenScoreboards.remove(internalName);
            return false;
        } else {
            hiddenScoreboards.add(internalName);
            return true;
        }
    }
}
