/** IDrivingRestrictionService.aidl */
package com.longcheer.cockpit.drv;

import com.longcheer.cockpit.drv.IDrivingRestrictionListener;
import com.longcheer.cockpit.drv.RestrictionStatus;
import com.longcheer.cockpit.drv.WhitelistEntry;

/**
 * 行驶限制服务接口
 * ASIL等级: ASIL B
 * 
 * 提供行驶限制功能的跨进程访问能力
 * 所有接口调用需要权限: com.longcheer.permission.CONTROL_DRIVING_RESTRICTION
 */
interface IDrivingRestrictionService {
    
    /**
     * 获取当前限制状态
     * @return 限制状态信息
     */
    RestrictionStatus getRestrictionStatus();
    
    /**
     * 获取应用限制类型
     * @param appId 应用包名
     * @return 限制类型 (0=NONE, 1=VIDEO_BLOCKED, 2=GAME_BLOCKED, 3=FULL_RESTRICTED)
     */
    int getAppRestrictionType(in String appId);
    
    /**
     * 控制应用行为
     * @param appId 应用包名
     * @param control 控制类型 (0=PAUSE, 1=RESUME, 2=RETURN_HOME, 3=LIMIT_INTERACTION, 4=GRAYSCALE)
     */
    void controlAppBehavior(in String appId, int control);
    
    /**
     * 注册状态监听
     * @param listener 监听者接口
     */
    void registerListener(in IDrivingRestrictionListener listener);
    
    /**
     * 注销状态监听
     * @param listener 监听者接口
     */
    void unregisterListener(in IDrivingRestrictionListener listener);
    
    /**
     * 添加到白名单 (需要系统权限)
     * @param appId 应用包名
     * @param reason 添加原因
     */
    void addToWhitelist(in String appId, in String reason);
    
    /**
     * 从白名单移除 (需要系统权限)
     * @param appId 应用包名
     */
    void removeFromWhitelist(in String appId);
    
    /**
     * 获取白名单列表
     * @return 白名单条目列表
     */
    List<WhitelistEntry> getWhitelist();
    
    /**
     * 获取E2E状态信息 (调试用)
     * @return E2E状态信息JSON
     */
    String getE2EStatus();
    
    /**
     * 强制进入安全状态 (紧急调试用)
     */
    void forceEnterSafeState();
}
