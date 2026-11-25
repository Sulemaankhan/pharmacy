package com.pharmacy.drugstore.security;

import com.pharmacy.drugstore.entity.User;

public final class CurrentUserHolder {
    private static final ThreadLocal<User> CURRENT = new ThreadLocal<>();

    private CurrentUserHolder() {}

    public static void set(User user) {
        CURRENT.set(user);
    }

    public static User get() {
        return CURRENT.get();
    }

    public static void clear() {
        CURRENT.remove();
    }
}
