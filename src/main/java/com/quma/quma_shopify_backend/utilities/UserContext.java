package com.quma.quma_shopify_backend.utilities;

public class UserContext {
    private static final ThreadLocal<UserContext> context = new ThreadLocal<>();

    private String username;
    private String ipAddress;
    private String macAddress;

    private UserContext() {
    }

    public static void set(String username, String ipAddress, String macAddress) {
        UserContext ctx = new UserContext();
        ctx.username = username;
        ctx.ipAddress = ipAddress;
        ctx.macAddress = macAddress;
        context.set(ctx);
    }

    public static UserContext get() {
        return context.get();
    }

    public static void clear() {
        context.remove();
    }

    public String getUsername() {
        return username;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getMacAddress() {
        return macAddress;
    }
}