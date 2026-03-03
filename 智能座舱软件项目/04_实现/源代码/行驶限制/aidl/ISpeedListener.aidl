/** ISpeedListener.aidl */
package com.longcheer.cockpit.drv;

import com.longcheer.cockpit.drv.E2EData;

/**
 * 车速变化监听接口
 */
oneway interface ISpeedListener {
    
    /**
     * 车速变化回调
     * @param speed 车速值 (km/h)
     * @param e2eData E2E保护数据
     */
    void onSpeedChanged(int speed, in E2EData e2eData);
}
