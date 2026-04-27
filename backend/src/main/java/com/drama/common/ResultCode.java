package com.drama.common;

/**
 * 响应码常量
 */
public class ResultCode {

    public static final int SUCCESS = 200;
    public static final int BAD_REQUEST = 400;
    public static final int UNAUTHORIZED = 401;
    public static final int FORBIDDEN = 403;
    public static final int NOT_FOUND = 404;
    public static final int SERVER_ERROR = 500;

    public static final String MSG_SUCCESS = "success";
    public static final String MSG_BAD_REQUEST = "请求参数错误";
    public static final String MSG_UNAUTHORIZED = "未授权";
    public static final String MSG_FORBIDDEN = "禁止访问";
    public static final String MSG_NOT_FOUND = "资源不存在";
    public static final String MSG_SERVER_ERROR = "服务器内部错误";
}