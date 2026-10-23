package dao;

/** Unchecked wrapper so the UI layer never has to deal with SQL exceptions. */
public class DataAccessException extends RuntimeException {
    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}