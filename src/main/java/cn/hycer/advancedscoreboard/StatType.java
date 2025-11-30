// StatType.java
package cn.hycer.advancedscoreboard;

public enum StatType {
    BLOCKS_MINED("blocks_mined", "挖掘量", "§6挖掘量"),
    DEATHS("deaths", "死亡次数", "§c死亡次数"),
    ONLINE_TIME("online_time", "在线时长", "§a在线时长"),
    ELYTRA_DISTANCE("elytra_distance", "飞行距离", "§b飞行距离");

    private final String id;
    private final String displayName;
    private final String formattedName;

    StatType(String id, String displayName, String formattedName) {
        this.id = id;
        this.displayName = displayName;
        this.formattedName = formattedName;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public String getFormattedName() { return formattedName; }

    public static StatType fromId(String id) {
        for (StatType type : values()) {
            if (type.id.equals(id)) {
                return type;
            }
        }
        return null;
    }
}