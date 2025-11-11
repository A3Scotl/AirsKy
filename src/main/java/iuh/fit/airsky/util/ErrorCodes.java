package iuh.fit.airsky.util;

public final class ErrorCodes {
    private ErrorCodes() {}

    public static final String USER_ALREADY_EXISTS = "USER_ALREADY_EXISTS";
    public static final String INVALID_CREDENTIALS = "INVALID_CREDENTIALS";
    public static final String EMAIL_NOT_VERIFIED = "EMAIL_NOT_VERIFIED";
    public static final String INVALID_OTP = "INVALID_OTP";
    public static final String RESOURCE_NOT_FOUND = "RESOURCE_NOT_FOUND";
    public static final String ROUTE_NOT_FOUND = "ROUTE_NOT_FOUND";
    public static final String INVALID_TOKEN = "INVALID_TOKEN_OR_NOT_LOGGED_IN";
    public static final String FORBIDDEN = "FORBIDDEN";
    public static final String INTERNAL_ERROR = "INTERNAL_SERVER_ERROR";
    public static final String BAD_REQUEST = "BAD_REQUEST";
    public static final String METHOD_NOT_ALLOWED = "METHOD_NOT_ALLOWED";
    public static final String TYPE_MISMATCH = "TYPE_MISMATCH";
    public static final String DB_CONSTRAINT_VIOLATION = "DB_CONSTRAINT_VIOLATION";
}
