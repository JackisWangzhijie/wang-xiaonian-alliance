# 接口需求规格说明书 (IRS)
## Interface Requirements Specification

**文档版本**: V1.0  
**编制日期**: 2024-06-01

---

## 1. 硬件接口需求

### 1.1 显示接口
**REQ-INT-HW-001**: 主显示屏 MIPI DSI 4-lane, 2560×1600@60Hz  
**REQ-INT-HW-002**: 支持EDID读取和分辨率自适应  
**REQ-INT-HW-003**: 支持背光亮度控制（0-100%）

### 1.2 车辆网络接口
**REQ-INT-HW-004**: CAN-FD 2.0B, 最高2Mbps, ≥2路  
**REQ-INT-HW-005**: LIN 2.x, 19.2kbps, ≥4路  
**REQ-INT-HW-006**: 车载以太网 100BASE-T1, ≥2路

### 1.3 传感器接口
**REQ-INT-HW-007**: GPS/GNSS 双频接收  
**REQ-INT-HW-008**: 加速度计/陀螺仪 I2C/SPI  
**REQ-INT-HW-009**: 环境光传感器 ADC

---

## 2. 软件接口需求

### 2.1 操作系统接口
**REQ-INT-SW-001**: Android Automotive 12 API Level 31  
**REQ-INT-SW-002**: Linux Kernel 5.10+  
**REQ-INT-SW-003**: HAL层符合Android Automotive规范

### 2.2 中间件接口
**REQ-INT-SW-004**: 车辆网络服务 (Vehicle HAL)  
**REQ-INT-SW-005**: 音频管理 (Audio HAL)  
**REQ-INT-SW-006**: 显示管理 (Display HAL)

### 2.3 应用接口
**REQ-INT-SW-007**: Car App Library  
**REQ-INT-SW-008**: Media Browser Service  
**REQ-INT-SW-009**: Navigation Service

---

## 3. 通信协议需求

### 3.1 CAN协议
**REQ-INT-PRO-001**: 支持标准帧和扩展帧  
**REQ-INT-PRO-002**: 支持周期性消息和事件触发消息  
**REQ-INT-PRO-003**: 支持DBC文件解析

### 3.2 诊断协议
**REQ-INT-PRO-004**: UDS (ISO 14229)  
**REQ-INT-PRO-005**: OBD-II (ISO 15031)  
**REQ-INT-PRO-006**: 支持DTC读取和清除

### 3.3 网络协议
**REQ-INT-PRO-007**: HTTP/HTTPS (REST API)  
**REQ-INT-PRO-008**: WebSocket (实时通信)  
**REQ-INT-PRO-009**: MQTT (物联网通信)

---

## 4. 外部系统接口

### 4.1 TSP平台接口
**REQ-INT-EXT-001**: 车辆状态上报  
**REQ-INT-EXT-002**: 远程控制指令接收  
**REQ-INT-EXT-003**: OTA升级管理

### 4.2 内容服务接口
**REQ-INT-EXT-004**: 地图数据服务 (高德/百度)  
**REQ-INT-EXT-005**: 音乐流媒体服务 (QQ音乐/网易云)  
**REQ-INT-EXT-006**: 语音云服务 (讯飞/百度)

### 4.3 手机互联接口
**REQ-INT-EXT-007**: Android Auto  
**REQ-INT-EXT-008**: CarPlay (如适用)  
**REQ-INT-EXT-009**: 蓝牙电话和音乐
