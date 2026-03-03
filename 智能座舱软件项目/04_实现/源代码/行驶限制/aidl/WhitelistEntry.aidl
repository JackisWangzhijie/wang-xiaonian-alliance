/** WhitelistEntry.aidl */
package com.longcheer.cockpit.drv;

/**
 * 白名单条目数据类
 */
parcelable WhitelistEntry {
    /** 应用包名 */
    String packageName;
    /** 应用名称 */
    String appName;
    /** 应用类别 */
    String category;
    /** 添加时间 */
    long addedTime;
    /** 添加原因 */
    String reason;
    /** 签名哈希 (防篡改) */
    String signatureHash;
}
