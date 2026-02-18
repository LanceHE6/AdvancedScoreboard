package cn.hycer.advancedscoreboard.Config;

import cn.hycer.advancedscoreboard.AdvancedScoreboard;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Properties;

public class Config {

    File file;
    Logger LOGGER = AdvancedScoreboard.LOGGER;

    public static final String ConfigFileName = "advanced_scoreboard.properties";//配置文件的名称
    public Properties prop;

    // 挖掘榜配置键
    public static String MiningDisplayNameConfigKey = "MineCountDisplayName";
    public static String MiningInternalNameConfigKey = "MineCountInternalName";

    // 在线时长榜配置键
    public static String OnlineTimeDisplayNameConfigKey = "OnlineTimeDisplayName";
    public static String OnlineTimeInternalNameConfigKey = "OnlineTimeInternalName";

    // 轮播切换时间（秒）
    public static String SwitchIntervalConfigKey = "ScoreboardSwitchInterval";

    private void createDefaultConfigFile(File file) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file)))//创建新的文件写入对象
        {
            String DEFAULT_CONFIG_DATA =
                    "MineCountDisplayName = 挖掘榜\n" +
                            "MineCountInternalName = mine_count\n" +
                            "OnlineTimeDisplayName = 在线时长\n" +
                            "OnlineTimeInternalName = online_time\n" +
                            "ScoreboardSwitchInterval = 10"; // 默认10秒切换一次
            writer.write(DEFAULT_CONFIG_DATA);//写入默认的配置文件信息
        } catch (Exception e) {
            LOGGER.warn("AdvancedScoreboard Config file write error.");
        }
    }//创建默认的配置文件

    public Config(final String filePath) throws IOException {//构造函数,传入配置文件的地址
        file = new File(filePath + "\\" + ConfigFileName);//拼接成正确的配置文件路径
        if (!file.exists())//判断配置文件是否存在
        {
            System.out.println("create config file");
            createDefaultConfigFile(file);//如果配置文件不存在,则直接创建默认的配置文件
        }

        this.prop = new Properties();//创建新的properties文件读取对象

        try (Reader reader = new InputStreamReader(Files.newInputStream(file.toPath()),
                StandardCharsets.UTF_8)) {//创建输入流,使用utf-8的字符集
            this.prop.load(reader);//加载配置文件
        } catch (Exception e) {
            throw e;
        }
    }

    public String getValue(final String key) { //获取配置文件对应的值
        return this.prop.getProperty(key); //返回值
    }

    public int getIntValue(final String key, int defaultValue) {
        try {
            return Integer.parseInt(getValue(key));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    //更新配置文件的值
    public void updateValue(final String key, final String val) throws IOException {
        this.prop.setProperty(key, val);
        this.prop.store(Files.newOutputStream(this.file.toPath()), null);
    }
}