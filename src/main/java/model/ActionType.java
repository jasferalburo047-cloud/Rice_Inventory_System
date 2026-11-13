package model;

/** The action types that are stored in the activity_logs table. */
public final class ActionType {

    public static final String USER_LOGIN = "USER_LOGIN";
    public static final String USER_LOGOUT = "USER_LOGOUT";
    public static final String USER_REGISTERED = "USER_REGISTERED";
    public static final String USER_APPROVED = "USER_APPROVED";
    public static final String USER_UPDATED = "USER_UPDATED";
    public static final String SALE_PROCESSED = "SALE_PROCESSED";
    public static final String STOCK_IN = "STOCK_IN";
    public static final String STOCK_OUT = "STOCK_OUT";
    public static final String STOCK_ADJUSTED = "STOCK_ADJUSTED";
    public static final String PRODUCT_ADDED = "PRODUCT_ADDED";
    public static final String PRODUCT_UPDATED = "PRODUCT_UPDATED";
    public static final String PRODUCT_REMOVED = "PRODUCT_REMOVED";
    public static final String SUPPLIER_ADDED = "SUPPLIER_ADDED";
    public static final String LOGS_CLEARED = "LOGS_CLEARED";

    private ActionType() { }
}