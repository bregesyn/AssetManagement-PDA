package com.ruoyi.asset.pda.core.session;

/**
 * 与后端 PdaApiConstants 逐字一致的 PDA 功能权限。
 */
public final class PdaPermissions {
    public static final String RFID_TAG_ADD = "asset:pda:rfid:tag:add";
    public static final String RFID_BIND = "asset:pda:rfid:bind";
    public static final String RFID_UNBIND = "asset:pda:rfid:unbind";
    public static final String INBOUND_SCAN = "asset:pda:inbound:scan";
    public static final String INBOUND_CONFIRM = "asset:pda:inbound:confirm";
    public static final String INVENTORY_LIST = "asset:pda:inventory:list";
    public static final String INVENTORY_SUBMIT = "asset:pda:inventory:submit";

    private PdaPermissions() {
    }
}
