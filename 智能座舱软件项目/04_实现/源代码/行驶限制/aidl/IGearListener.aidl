/** IGearListener.aidl */
package com.longcheer.cockpit.drv;

import com.longcheer.cockpit.drv.E2EData;

/**
 * 挡位变化监听接口
 */
oneway interface IGearListener {
    
    /**
     * 挡位变化回调
     * @param gear 挡位值 (0=P, 1=R, 2=N, 3=D)
     * @param e2eData E2E保护数据
     */
    void onGearChanged(int gear, in E2EData e2eData);
}
