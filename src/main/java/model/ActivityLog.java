package model;

public class ActivityLog {
    private final int id;
    private final int userId;
    private final String actionType;
    private final String description;
    private final String timestamp;

    public ActivityLog(int id, int userId, String actionType, String description, String timestamp) {
        this.id = id;
        this.userId = userId;
        this.actionType = actionType;
        this.description = description;
        this.timestamp = timestamp;
    }

    public int getId() { return id; }
    public int getUserId() { return userId; }
    public String getActionType() { return actionType; }
    public String getDescription() { return description; }
    public String getTimestamp() { return timestamp; }
}