package com.drama.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.io.Serializable;

/**
 * 统一响应结构
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class R<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    private int code;
    private String message;
    private T data;
    private long timestamp;

    public R() {
        this.timestamp = System.currentTimeMillis();
    }

    public R(int code, String message) {
        this.code = code;
        this.message = message;
        this.timestamp = System.currentTimeMillis();
    }

    public R(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }

    public static <T> R<T> ok() {
        return new R<>(200, "success");
    }

    public static <T> R<T> ok(T data) {
        return new R<>(200, "success", data);
    }

    public static <T> R<T> ok(String message, T data) {
        return new R<>(200, message, data);
    }

    public static <T> R<T> fail() {
        return new R<>(500, "error");
    }

    public static <T> R<T> fail(String message) {
        return new R<>(500, message);
    }

    public static <T> R<T> fail(int code, String message) {
        return new R<>(code, message);
    }

    public static <T> R<T> of(int code, String message, T data) {
        return new R<>(code, message, data);
    }

    public boolean isSuccess() {
        return this.code == 200;
    }
}