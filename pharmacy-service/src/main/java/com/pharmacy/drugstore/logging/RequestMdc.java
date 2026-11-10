package com.pharmacy.drugstore.logging;

import com.pharmacy.drugstore.entity.User;
import org.slf4j.MDC;

public final class RequestMdc {
    public static final String REQUEST_ID = "requestId";
    public static final String USER_ID = "userId";
    public static final String USER_EMAIL = "userEmail";
    public static final String USER_NAME = "userName";
    public static final String USER_ROLE = "userRole";
    public static final String HEADER = "X-Request-Id";

    private RequestMdc() {}

    public static void setRequestId(String id) {
        MDC.put(REQUEST_ID, id == null || id.isBlank() ? "-" : id);
    }

    public static void setAnonymous() {
        MDC.put(USER_ID, "anonymous");
        MDC.put(USER_EMAIL, "-");
        MDC.put(USER_NAME, "-");
        MDC.put(USER_ROLE, "-");
    }

    public static void setUser(User user) {
        if (user == null) {
            setAnonymous();
            return;
        }
        MDC.put(USER_ID, String.valueOf(user.getId()));
        MDC.put(USER_EMAIL, dash(user.getEmail()));
        MDC.put(USER_NAME, dash(user.getName()));
        MDC.put(USER_ROLE, dash(user.getRole()));
    }

    public static String describe(User user) {
        if (user == null) return "userId=anonymous";
        return "userId=" + user.getId()
                + " email=" + user.getEmail()
                + " name=" + user.getName()
                + " role=" + user.getRole();
    }

    public static void clear() {
        MDC.clear();
    }

    private static String dash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }
}
