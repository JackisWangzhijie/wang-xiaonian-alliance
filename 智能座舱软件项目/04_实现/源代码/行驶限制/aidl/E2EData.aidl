/** E2EData.aidl */
package com.longcheer.cockpit.drv;

/**
 * E2E保护数据
 * 包含CRC校验、序列号和时间戳
 */
parcelable E2EData {
    /** E2E序列号 (0-15) */
    byte counter;
    /** CRC8校验值 */
    byte crc;
    /** 信号时间戳 */
    long timestamp;
    /** 数据ID */
    int dataId;
    /** 原始数据 */
    byte[] rawData;
}
