package com.fitness.common;

/**
 * 统一错误码
 */
public final class ResultCode {

    private ResultCode() {
    }

    public static final int SUCCESS = 0;
    public static final int BAD_REQUEST = 400;
    public static final int UNAUTHORIZED = 401;
    public static final int FORBIDDEN = 403;
    public static final int NOT_FOUND = 404;
    public static final int SERVER_ERROR = 500;

    public static final int PHONE_EXISTS = 1001;
    public static final int LOGIN_FAILED = 1002;
    public static final int ACCOUNT_DISABLED = 1003;
    public static final int DUPLICATE_OPERATION = 1004;

    public static final int CONTENT_PENDING = 2001;
    public static final int ALREADY_REVIEWED = 2002;
    public static final int WX_LOGIN_FAILED = 2003;
}
