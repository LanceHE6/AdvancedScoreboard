package cn.hycer.advancedscoreboard.Config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import cn.hycer.advancedscoreboard.AdvancedScoreboard;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 计分板根配置类（对应 JSON 根节点）
 */
public class Config {
    // 日志对象
    private static final Logger LOGGER = AdvancedScoreboard.LOGGER;
    // JSON 配置文件名称
    public static final String CONFIG_FILE_NAME = "advanced_scoreboard.json";

    public static final String MINE_COUNT_INTERNAL_NAME = "mine_count";
    public static final String ONLINE_TIME_INTERNAL_NAME = "online_time";
    // Jackson JSON 解析器（格式化输出）
    @JsonIgnore
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);
    // JSON 配置项
    private int switchInterval = 10; // 轮播切换间隔（秒）
    private List<ScoreboardItem> scoreboards = new ArrayList<>(); // 计分板列表

    // 非JSON字段
    @JsonIgnore
    private final File configFile; // 配置文件对象

    // ========== 构造方法 ==========
    /**
     * 空构造（Jackson 反序列化必需）
     */
    public Config() {
        this.configFile = null;
    }

    /**
     * 带路径构造（加载/保存配置）
     * @param configDirPath 配置目录路径
     */
    public Config(String configDirPath) {
        // 拼接配置文件完整路径
        this.configFile = new File(configDirPath + File.separator + CONFIG_FILE_NAME);
        // 初始化默认配置（首次启动）
        if (!this.configFile.exists()) {
            initDefaultConfig();
            saveConfig(); // 保存默认配置到文件
        } else {
            loadConfig(); // 加载已有配置
        }
    }

    // ========== 核心方法 ==========
    /**
     * 初始化默认配置（首次启动）
     */
    private void initDefaultConfig() {
        this.switchInterval = 10;

        // 初始化挖掘榜
        ScoreboardItem mineCountBoard = new ScoreboardItem();
        mineCountBoard.setInternalName(MINE_COUNT_INTERNAL_NAME);
        mineCountBoard.setDisplayName("挖掘榜");

        // 初始化在线时长榜
        ScoreboardItem onlineTimeBoard = new ScoreboardItem();
        onlineTimeBoard.setInternalName(ONLINE_TIME_INTERNAL_NAME);
        onlineTimeBoard.setDisplayName("在线时长");

        // 添加到计分板列表
        this.scoreboards.add(mineCountBoard);
        this.scoreboards.add(onlineTimeBoard);

        LOGGER.info("default config initialization completed");
    }

    /**
     * 从文件加载配置（映射到当前对象）
     */
    public void loadConfig() {
        if (this.configFile == null || !this.configFile.exists()) {
            LOGGER.warn("the config file does not exist and cannot be loaded");
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
            this.scoreboards = loadedConfig.getScoreboards();
            LOGGER.info("config file loaded successfully：{}", this.configFile.getAbsolutePath());
        } catch (IOException e) {
            LOGGER.error("load config failed, using default config: ", e);
            initDefaultConfig(); // 加载失败时回退到默认配置
        }
    }

    /**
     * 保存当前配置到 JSON 文件（核心方法）
     */
    public void saveConfig() {
        if (this.configFile == null) {
            LOGGER.warn("the config file path has not been initialized and cannot be saved");
            return;
        }

        try {
            // 确保配置目录存在
            File parentDir = this.configFile.getParentFile();
            if (!parentDir.exists()) {
                parentDir.mkdirs();
            }

            // 写入JSON文件（UTF-8 编码）
            OBJECT_MAPPER.writeValue(this.configFile, this);
            LOGGER.info("config file saved successfully：{}", this.configFile.getAbsolutePath());
        } catch (IOException e) {
            LOGGER.error("save config failed", e);
        }
    }

    // ========== 辅助方法（快速获取计分板） ==========
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

    public void print() {
        try {
            // 将对象转为格式化的JSON字符串
            String configJson = OBJECT_MAPPER.writeValueAsString(this);
            System.out.println("===== Scoreboard =====");
            System.out.println(configJson);
        } catch (Exception e) {
            System.err.println("print config failed：" + e.getMessage());
        }
    }

    // ========== Getter & Setter ==========
    public int getSwitchInterval() {
        return switchInterval;
    }

    public void setSwitchInterval(int switchInterval) {
        // 最小间隔1秒
        this.switchInterval = Math.max(1, switchInterval);
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
