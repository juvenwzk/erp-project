package com.kangcode.utils;

import jakarta.servlet.http.HttpServletRequest;

public final class RequestContextUtils {

    private RequestContextUtils() {
    }

    public static Integer resolveUserId(HttpServletRequest request, Integer fromBody) {
        if (fromBody != null) {
            return fromBody;
        }
        Object userId = request.getAttribute("userId");
        if (userId == null) {
            return null;
        }
        return Integer.parseInt(userId.toString());
    }
}
