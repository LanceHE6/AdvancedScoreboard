package cn.hycer.advancedscoreboard.Config;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 单个计分板配置项（对应 JSON 中 scoreboards 数组的元素）
 */
public class ScoreboardItem {
    // ========== JSON 配置项 ==========
    private String internalName; // 内部名
    private String displayName;  // 显示名
    private Map<String, Integer> data = new LinkedHashMap<>(); // 计分板数据（UUID->数值）

    // ========== Getter & Setter ==========
    public String getInternalName() {
        return internalName;
    }

    public void setInternalName(String internalName) {
        this.internalName = internalName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public Map<String, Integer> getData() {
        return data;
    }

    public void setData(Map<String, Integer> data) {
        this.data = data != null ? data : new LinkedHashMap<>();
    }

    // ========== 辅助方法 ==========
    /**
     * 更新计分板数据（玩家->数值）
     * @param playerName 玩家名称
     * @param value 数值
     */
    public void updateData(String playerName, int value) {
        this.data.put(playerName, value);
    }

    /**
     * 获取指定玩家的计分板数值
     * @param playerName 玩家名称
     * @param defaultValue 默认值（未找到时返回）
     * @return 数值
     */
    public int getDataValue(String playerName, int defaultValue) {
        return this.data.getOrDefault(playerName, defaultValue);
    }
}
