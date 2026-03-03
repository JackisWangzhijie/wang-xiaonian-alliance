/** RestrictionStatus.aidl */
package com.longcheer.cockpit.drv;

/**
 * 限制状态数据类
 * 用于跨进程传递当前限制状态
 */
parcelable RestrictionStatus {
    /** 当前状态: 0=NORMAL, 1=RESTRICTED, 2=RECOVERING, 3=FAULT */
    int state;
    /** 是否处于限制状态 */
    boolean isRestricted;
    /** 状态时间戳 */
    long timestamp;
    /** 触发限制的原因 */
    String triggerReason;
    /** 车速 (km/h) */
    int vehicleSpeed;
    /** 当前挡位 */
    int gearPosition;
    /** 驻车制动状态 */
    boolean parkingBrakeOn;
}
