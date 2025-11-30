// PlayerStats.java
package cn.hycer.advancedscoreboard;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerStats {
    private final UUID playerUUID;
    private final String playerName;

    private long blocksMined = 0;
    private int deaths = 0;
    private long onlineTime = 0; // 毫秒
    private double elytraDistance = 0.0; // 米
    private long joinTime = 0;

    public PlayerStats(UUID playerUUID, String playerName) {
        this.playerUUID = playerUUID;
        this.playerName = playerName;
    }

    // Getter methods
    public UUID getPlayerUUID() { return playerUUID; }
    public String getPlayerName() { return playerName; }
    public long getBlocksMined() { return blocksMined; }
    public int getDeaths() { return deaths; }
    public long getOnlineTime() { return onlineTime; }
    public double getElytraDistance() { return elytraDistance; }

    // 增加统计值的方法
    public void incrementBlocksMined() { blocksMined++; }
    public void incrementDeaths() { deaths++; }
    public void addElytraDistance(double distance) { elytraDistance += distance; }

    public void setJoinTime() { this.joinTime = System.currentTimeMillis(); }
    public void updateOnlineTime() {
        if (joinTime > 0) {
            onlineTime += (System.currentTimeMillis() - joinTime);
            joinTime = System.currentTimeMillis();
        }
    }

    // 获取格式化数据
    public String getFormattedStat(StatType type) {
        switch (type) {
            case BLOCKS_MINED:
                return String.format("%,d", blocksMined);
            case DEATHS:
                return String.format("%,d", deaths);
            case ONLINE_TIME:
                double hours = onlineTime / 3600000.0;
                return String.format("%.1f 小时", hours);
            case ELYTRA_DISTANCE:
                double km = elytraDistance / 1000.0;
                return String.format("%.1f km", km);
            default:
                return "0";
        }
    }

    // 序列化为Map
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("playerName", playerName);
        map.put("blocksMined", blocksMined);
        map.put("deaths", deaths);
        map.put("onlineTime", onlineTime);
        map.put("elytraDistance", elytraDistance);
        return map;
    }

    // 从Map加载
    public void fromMap(Map<String, Object> map) {
        blocksMined = ((Number) map.getOrDefault("blocksMined", 0)).longValue();
        deaths = ((Number) map.getOrDefault("deaths", 0)).intValue();
        onlineTime = ((Number) map.getOrDefault("onlineTime", 0)).longValue();
        elytraDistance = ((Number) map.getOrDefault("elytraDistance", 0.0)).doubleValue();
    }
}