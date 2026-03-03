/** IParkingBrakeListener.aidl */
package com.longcheer.cockpit.drv;

import com.longcheer.cockpit.drv.E2EData;

/**
 * 驻车制动变化监听接口
 */
oneway interface IParkingBrakeListener {
    
    /**
     * 驻车制动变化回调
     * @param isOn 是否启用
     * @param e2eData E2E保护数据
     */
    void onParkingBrakeChanged(boolean isOn, in E2EData e2eData);
}
