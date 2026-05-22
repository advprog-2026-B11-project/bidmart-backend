package com.example.bidmart.common.security;

public final class PermissionNames {
    public static final String LISTING_CREATE = "listing:create";
    public static final String LISTING_READ = "listing:read";
    public static final String LISTING_UPDATE = "listing:update";
    public static final String LISTING_DELETE = "listing:delete";

    public static final String BID_PLACE = "bid:place";
    public static final String BID_READ = "bid:read";

    public static final String ORDER_CREATE = "order:create";
    public static final String ORDER_READ = "order:read";
    public static final String ORDER_UPDATE_STATUS = "order:update-status";
    public static final String ORDER_UPDATE_TRACKING = "order:update-tracking";
    public static final String ORDER_DELETE = "order:delete";

    public static final String WALLET_CREATE = "wallet:create";
    public static final String WALLET_READ = "wallet:read";
    public static final String WALLET_TOP_UP = "wallet:top-up";
    public static final String WALLET_WITHDRAW = "wallet:withdraw";
    public static final String WALLET_TRANSACTIONS_READ = "wallet:transactions:read";
    public static final String WALLET_LIST = "wallet:list";
    public static final String WALLET_HOLD = "wallet:hold";
    public static final String WALLET_RELEASE = "wallet:release";
    public static final String WALLET_SETTLE = "wallet:settle";
    public static final String WALLET_CONFIRM_DELIVERY = "wallet:confirm-delivery";

    public static final String USER_READ = "user:read";
    public static final String USER_UPDATE = "user:update";
    public static final String USER_DELETE = "user:delete";

    public static final String USER_MFA_READ = "user:mfa:read";
    public static final String USER_MFA_SETUP = "user:mfa:setup";
    public static final String USER_MFA_ENABLE = "user:mfa:enable";
    public static final String USER_MFA_ENABLE_EMAIL = "user:mfa:enable-email";
    public static final String USER_MFA_DISABLE = "user:mfa:disable";

    public static final String SESSION_READ = "session:read";
    public static final String SESSION_REVOKE = "session:revoke";

    public static final String ROLE_MANAGE = "role:manage";
    public static final String USER_DEACTIVATE = "user:deactivate";

    private PermissionNames() {
    }
}
