package com.drama.common;

import java.util.UUID;

/**
 * ID工具
 */
public class IdUtils {

    public static String randomId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public static String randomId36() {
        return UUID.randomUUID().toString();
    }
}