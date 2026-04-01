package cn.hycer.advancedscoreboard.Config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import static cn.hycer.advancedscoreboard.Global.Global.logger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 根配置类
 */
public class Config {
    // JSON 配置文件名称
    public static final String CONFIG_FILE_NAME = "advanced_scoreboard.json";

    public static final String MINE_COUNT_INTERNAL_NAME = "mine_count"; // 挖掘量
    public static final String ONLINE_TIME_INTERNAL_NAME = "online_time"; // 在线时长
    public static final String ELYTRON_DISTANCE_INTERNAL_NAME = "elytron_distance"; // 鞘翅飞行距离
    public static final String DAMAGE_TAKEN_INTERNAL_NAME = "damage_taken"; // 受到的伤害
    // JSON 解析器（格式化输出）
    @JsonIgnore
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);
    // JSON 配置项
    private int switchInterval; // 轮播切换周期s
    private int saveInterval; // 数据保存周期s
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
        switchInterval = 5;
        saveInterval = 5;

        // 初始化挖掘榜
        ScoreboardItem mineCountBoard = new ScoreboardItem();
        mineCountBoard.setInternalName(MINE_COUNT_INTERNAL_NAME);
        mineCountBoard.setDisplayName("===挖掘量===");

        // 初始化在线时长榜
        ScoreboardItem onlineTimeBoard = new ScoreboardItem();
        onlineTimeBoard.setInternalName(ONLINE_TIME_INTERNAL_NAME);
        onlineTimeBoard.setDisplayName("===在线时长(h)===");

        // 初始化鞘翅飞行距离榜
        ScoreboardItem elytronBoard = new ScoreboardItem();
        elytronBoard.setInternalName(ELYTRON_DISTANCE_INTERNAL_NAME);
        elytronBoard.setDisplayName("===飞行距离(km)===");

        // 初始化鞘翅飞行距离榜
        ScoreboardItem damageTakenBoard = new ScoreboardItem();
        damageTakenBoard.setInternalName(DAMAGE_TAKEN_INTERNAL_NAME);
        damageTakenBoard.setDisplayName("===受到伤害===");

        // 添加到计分板列表
        this.scoreboards.add(mineCountBoard);
        this.scoreboards.add(onlineTimeBoard);
        this.scoreboards.add(elytronBoard);
        this.scoreboards.add(damageTakenBoard);

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
            this.switchInterval = loadedConfig.getSwitchInterval();
            this.saveInterval = loadedConfig.getSaveInterval();
            this.scoreboards = loadedConfig.getScoreboards();
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
        sb.append("  switchInterval = ").append(switchInterval).append("\n");
        sb.append("  saveInterval = ").append(saveInterval).append("\n");
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
}
