package model;

public class ActivityLog {
    private final int id;
    private final int userId;
    private final String username;
    private final String action;
    private final String date;

    public ActivityLog(int id, int userId, String username, String action, String date) {
        this.id = id;
        this.userId = userId;
        this.username = username;
        this.action = action;
        this.date = date;
    }

    public int getId() { return id; }
    public int getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getAction() { return action; }
    public String getDate() { return date; }
}