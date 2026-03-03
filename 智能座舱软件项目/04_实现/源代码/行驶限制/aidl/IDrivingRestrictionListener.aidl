/** IDrivingRestrictionListener.aidl */
package com.longcheer.cockpit.drv;

/**
 * 行驶限制状态监听接口
 * 用于应用层监听限制状态变化
 */
oneway interface IDrivingRestrictionListener {
    
    /**
     * 限制状态变化回调
     * @param restricted 是否受限
     * @param state 当前状态 (0=NORMAL, 1=RESTRICTED, 2=RECOVERING, 3=FAULT)
     */
    void onRestrictionChanged(boolean restricted, int state);
    
    /**
     * 应用被限制回调
     * @param appId 应用包名
     * @param restrictionType 限制类型
     */
    void onAppRestricted(in String appId, int restrictionType);
    
    /**
     * 应用限制解除回调
     * @param appId 应用包名
     */
    void onAppUnrestricted(in String appId);
    
    /**
     * 进入安全状态回调
     * @param reason 原因代码
     */
    void onEnterSafeState(int reason);
}
