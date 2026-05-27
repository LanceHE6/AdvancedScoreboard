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

/**
 * 根配置类
 */
public class Config {
    // JSON 配置文件名称
    public static final String CONFIG_FILE_NAME = "advanced_scoreboard.json";

    public static final String MINE_COUNT_INTERNAL_NAME = "mine_count"; // 挖掘量
    public static final String PLACE_COUNT_INTERNAL_NAME = "place_count"; // 放置量
    public static final String ONLINE_TIME_INTERNAL_NAME = "online_time"; // 在线时长
    public static final String ELYTRA_DISTANCE_INTERNAL_NAME = "elytra_dist"; // 鞘翅飞行距离
    public static final String DAMAGE_TAKEN_INTERNAL_NAME = "damage_taken"; // 受到的伤害
    public static final String DEATHS_INTERNAL_NAME = "deaths"; // 死亡次数
    public static final String MOB_KILLS_INTERNAL_NAME = "mob_kills"; // 击杀生物数
    // JSON 解析器（格式化输出）
    @JsonIgnore
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);
    // JSON 配置项
    private String border; // 榜单显示名边框
    private int switchInterval; // 轮播切换周期s
    private int saveInterval; // 数据保存周期s
    private int maxDisplayNum; // 榜单最大显示玩家数量
    private Set<String> hiddenScoreboards = new HashSet<>(); // 全局隐藏的榜单 internalName 集合
    private List<ScoreboardItem> scoreboards = new ArrayList<>(); // 计分板列表

    // 非JSON字段
    @JsonIgnore
    private final File configFile; // 配置文件对象

    /**
     * 空构造（Jackson 反序列化必需）
     */
    public Config() {
        this.configFile = null;
    }

    /**
     * 带路径构造，加载配置
     * @param configDirPath 配置目录路径
     */
    public Config(String configDirPath) {
        this.configFile = new File(configDirPath + File.separator + CONFIG_FILE_NAME);
        // 初始化默认配置（首次启动）
        if (!this.configFile.exists()) {
            initDefaultConfig();
            saveConfig(); // 保存默认配置到文件
        } else {
            loadConfig(); // 加载已有配置
        }
    }

    /**
     * 初始化默认配置（首次启动）
     */
    private void initDefaultConfig() {
        // 初始化切换周期和保存周期，默认5s
        border = "===";
        switchInterval = 5;
        saveInterval = 5;
        maxDisplayNum = 15;

        // 初始化挖掘榜
        ScoreboardItem mineCountBoard = new ScoreboardItem();
        mineCountBoard.setInternalName(MINE_COUNT_INTERNAL_NAME);
        mineCountBoard.setDisplayName("挖掘量");

        // 初始化放置榜
        ScoreboardItem placeCountBoard = new ScoreboardItem();
        placeCountBoard.setInternalName(PLACE_COUNT_INTERNAL_NAME);
        placeCountBoard.setDisplayName("放置量");

        // 初始化在线时长榜
        ScoreboardItem onlineTimeBoard = new ScoreboardItem();
        onlineTimeBoard.setInternalName(ONLINE_TIME_INTERNAL_NAME);
        onlineTimeBoard.setDisplayName("在线时长(h)");

        // 初始化鞘翅飞行距离榜
        ScoreboardItem elytronBoard = new ScoreboardItem();
        elytronBoard.setInternalName(ELYTRA_DISTANCE_INTERNAL_NAME);
        elytronBoard.setDisplayName("飞行距离(km)");

        // 初始化受到伤害榜
        ScoreboardItem damageTakenBoard = new ScoreboardItem();
        damageTakenBoard.setInternalName(DAMAGE_TAKEN_INTERNAL_NAME);
        damageTakenBoard.setDisplayName("受到伤害");

        // 初始化死亡榜
        ScoreboardItem deathsBoard = new ScoreboardItem();
        deathsBoard.setInternalName(DEATHS_INTERNAL_NAME);
        deathsBoard.setDisplayName("死亡次数");

        // 初始化击杀生物榜
        ScoreboardItem mobKillsBoard = new ScoreboardItem();
        mobKillsBoard.setInternalName(MOB_KILLS_INTERNAL_NAME);
        mobKillsBoard.setDisplayName("击杀生物数");

        // 添加到计分板列表
        this.scoreboards.add(mineCountBoard);
        this.scoreboards.add(placeCountBoard);
        this.scoreboards.add(onlineTimeBoard);
        this.scoreboards.add(elytronBoard);
        this.scoreboards.add(damageTakenBoard);
        this.scoreboards.add(deathsBoard);
        this.scoreboards.add(mobKillsBoard);

        logger.info("default config initialization completed");
    }

    /**
     * 从文件加载配置
     */
    public void loadConfig() {
        if (this.configFile == null || !this.configFile.exists()) {
            logger.warn("the config file does not exist and cannot be loaded");
            return;
        }

        try {
            // 读取JSON文件并映射到当前对象
            Config loadedConfig = OBJECT_MAPPER.readValue(
                    this.configFile,
                    Config.class
            );
            // 覆盖当前对象的配置项
            this.border = loadedConfig.getBorder() != null ? loadedConfig.getBorder() : "===";
            this.switchInterval = loadedConfig.getSwitchInterval();
            this.saveInterval = loadedConfig.getSaveInterval();
            this.maxDisplayNum = loadedConfig.getMaxDisplayNum() > 0 ? loadedConfig.getMaxDisplayNum() : 15;
            this.hiddenScoreboards = loadedConfig.getHiddenScoreboards() != null ? loadedConfig.getHiddenScoreboards() : new HashSet<>();
            this.scoreboards = loadedConfig.getScoreboards();
            // 迁移旧版配置：elytron_distance → elytra_dist
            for (ScoreboardItem item : this.scoreboards) {
                if ("elytron_distance".equals(item.getInternalName())) {
                    item.setInternalName(ELYTRA_DISTANCE_INTERNAL_NAME);
                    logger.info("migrated old internalName 'elytron_distance' to '{}'", ELYTRA_DISTANCE_INTERNAL_NAME);
                }
            }
            logger.info("config file loaded successfully: {}", this.configFile.getAbsolutePath());
        } catch (IOException e) {
            logger.error("load config failed, using default config: {}", e);
            initDefaultConfig(); // 加载失败时回退到默认配置
        }
    }

    /**
     * 保存当前配置到 JSON 文件
     */
    public void saveConfig() {
        if (this.configFile == null) {
            logger.warn("the config file path has not been initialized and cannot be saved");
            return;
        }

        try {
            // 确保配置目录存在
            File parentDir = this.configFile.getParentFile();
            if (!parentDir.exists()) {
                parentDir.mkdirs();
            }

            // 写入JSON文件
            OBJECT_MAPPER.writeValue(this.configFile, this);
            logger.debug("config file saved: {}", this.configFile.getAbsolutePath());
        } catch (IOException e) {
            logger.error("save config failed: {}", e);
        }
    }

    /**
     * 根据内部名快速获取计分板配置
     * @param internalName 计分板内部名
     * @return 计分板配置（null = 未找到）
     */
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

    /**
     * 获取带边框的显示名（border + displayName + border）
     */
    public String getFormattedDisplayName(ScoreboardItem item) {
        return border + item.getDisplayName() + border;
    }

    public int getSwitchInterval() {
        return switchInterval;
    }

    public void setSwitchInterval(int switchInterval) {
        // 最小间隔1秒
        this.switchInterval = Math.max(1, switchInterval);
    }

    public int getSaveInterval() {
        return saveInterval;
    }

    public void setSaveInterval(int saveInterval) {
        // 最小间隔1秒
        this.saveInterval = Math.max(1, saveInterval);
    }

    public int getMaxDisplayNum() {
        return maxDisplayNum;
    }

    public void setMaxDisplayNum(int maxDisplayNum) {
        // 最少显示1名玩家
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

    /**
     * 全局切换某计分板的显示/隐藏状态
     * @return true = 现在已隐藏，false = 现在已显示
     */
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
