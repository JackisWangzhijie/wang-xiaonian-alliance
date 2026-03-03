/** IVehicleService.aidl */
package com.longcheer.cockpit.drv;

import com.longcheer.cockpit.drv.ISpeedListener;
import com.longcheer.cockpit.drv.IGearListener;
import com.longcheer.cockpit.drv.IParkingBrakeListener;
import com.longcheer.cockpit.drv.E2EData;

/**
 * 车辆服务接口
 * 与车辆HAL层交互获取车辆信号
 */
interface IVehicleService {
    
    /**
     * 获取当前车速
     * @return 车速值 (km/h)
     */
    int getVehicleSpeed();
    
    /**
     * 获取当前挡位
     * @return 挡位值 (0=P, 1=R, 2=N, 3=D)
     */
    int getGearPosition();
    
    /**
     * 获取驻车制动状态
     * @return true=启用, false=释放
     */
    boolean isParkingBrakeOn();
    
    /**
     * 注册车速监听
     * @param listener 监听回调
     * @param intervalMs 回调间隔(ms)
     */
    void registerSpeedListener(in ISpeedListener listener, int intervalMs);
    
    /**
     * 注销车速监听
     * @param listener 监听回调
     */
    void unregisterSpeedListener(in ISpeedListener listener);
    
    /**
     * 注册挡位监听
     * @param listener 监听回调
     */
    void registerGearListener(in IGearListener listener);
    
    /**
     * 注销挡位监听
     * @param listener 监听回调
     */
    void unregisterGearListener(in IGearListener listener);
    
    /**
     * 注册驻车制动监听
     * @param listener 监听回调
     */
    void registerParkingBrakeListener(in IParkingBrakeListener listener);
    
    /**
     * 注销驻车制动监听
     * @param listener 监听回调
     */
    void unregisterParkingBrakeListener(in IParkingBrakeListener listener);
}
