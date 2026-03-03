# CAN信号矩阵
## CAN Signal Matrix

**文档版本**: V1.0  
**编制日期**: 2024-06-10  
**编制单位**: 上海龙旗智能科技有限公司  
**CAN协议**: CAN-FD 2.0B  
**波特率**: 500Kbps (仲裁段) / 2Mbps (数据段)  
**符合标准**: ISO 11898, ISO 14229 (UDS)

---

## 目录

1. [概述](#1-概述)
2. [CAN网络拓扑](#2-can网络拓扑)
3. [消息定义](#3-消息定义)
4. [信号详细定义](#4-信号详细定义)
5. [信号矩阵表](#5-信号矩阵表)
6. [周期性消息时序](#6-周期性消息时序)
7. [故障处理机制](#7-故障处理机制)

---

## 1. 概述

### 1.1 文档目的
本文档定义智能座舱主交互系统与车辆其他ECU间的CAN通信信号矩阵，确保车载网络通信的标准化和可靠性。

### 1.2 适用范围
| 网络 | 说明 |
|------|------|
| PT-CAN | 动力CAN，连接动力系统ECU |
| Body-CAN | 车身CAN，连接车身控制ECU |
| Info-CAN | 信息CAN，连接信息娱乐ECU |
| ADAS-CAN | ADAS CAN，连接辅助驾驶ECU |

### 1.3 术语定义
| 术语 | 说明 |
|------|------|
| Message ID | CAN消息标识符 (11bit/29bit) |
| DLC | Data Length Code，数据长度 (0-64 bytes for CAN-FD) |
| Cycle Time | 消息发送周期 |
| Factor | 信号值 = 原始值 × Factor + Offset |
| Offset | 信号偏移量 |
| Byte Order | 字节序：Intel (Little Endian) / Motorola (Big Endian) |

---

## 2. CAN网络拓扑

```
┌─────────────────────────────────────────────────────────────────┐
│                        车辆CAN网络                              │
│                                                                 │
│  ┌─────────┐    ┌─────────┐    ┌─────────┐    ┌─────────┐      │
│  │  ECM    │    │  TCM    │    │   ABS   │    │  EPB    │      │
│  │发动机控制│    │变速箱控制│    │  防抱死 │    │ 电子驻车│      │
│  └────┬────┘    └────┬────┘    └────┬────┘    └────┬────┘      │
│       │              │              │              │            │
│       └──────────────┴──────┬───────┴──────────────┘            │
│                            │                                   │
│                      ┌─────┴─────┐                            │
│                      │  Gateway  │                            │
│                      │  网关ECU  │                            │
│                      └─────┬─────┘                            │
│                            │                                   │
│       ┌──────────────┬─────┴──────────┬──────────────┐        │
│       │              │                │              │        │
│  ┌────┴────┐    ┌────┴────┐      ┌────┴────┐    ┌────┴────┐   │
│  │  BCM    │    │   ACM   │      │  HU(SoC)│    │ Cluster │   │
│  │车身控制 │    │空调控制 │      │ 中控主机│    │  仪表   │   │
│  └─────────┘    └─────────┘      └────┬────┘    └─────────┘   │
│                                       │                        │
│                                  ┌────┴────┐                  │
│                                  │ Display │                  │
│                                  │ 显示屏  │                  │
│                                  └─────────┘                  │
└─────────────────────────────────────────────────────────────────┘
```

### 2.1 网络分配

| 网络名称 | 波特率 | 连接ECU |
|----------|--------|---------|
| PT-CAN | 500Kbps | ECM, TCM, ABS, Gateway |
| Body-CAN | 125Kbps | BCM, ACM, EPB, Gateway |
| Info-CAN | 500Kbps | HU, Cluster, Display, Gateway |
| ADAS-CAN | 500Kbps | ADAS, ABS, Gateway |

---

## 3. 消息定义

### 3.1 消息汇总表

| Message Name | Message ID | DLC | Cycle Time | Sender | Receivers | ASIL |
|--------------|------------|-----|------------|--------|-----------|------|
| VC_VehicleSpeed | 0x130 | 8 | 20ms | ABS | HU, Cluster | ASIL B |
| VC_GearPosition | 0x1F1 | 8 | 50ms | TCM | HU, Cluster | ASIL B |
| VC_EngineStatus | 0x120 | 8 | 50ms | ECM | HU, Cluster | ASIL A |
| VC_ParkingBrake | 0x230 | 8 | 100ms | EPB | HU, Cluster | ASIL B |
| VC_DoorStatus | 0x310 | 8 | 100ms | BCM | HU, Cluster | QM |
| VC_VehiclePower | 0x320 | 8 | 100ms | BCM | HU, Cluster | ASIL B |
| VC_TirePressure | 0x330 | 8 | 1000ms | BCM | HU, Cluster | ASIL A |
| VC_FuelLevel | 0x340 | 8 | 1000ms | BCM | HU, Cluster | QM |
| VC_Odometer | 0x350 | 8 | 1000ms | BCM | HU, Cluster | QM |
| VC_SteeringAngle | 0x140 | 8 | 20ms | ABS | HU, ADAS | ASIL B |
| HU_ControlCmd | 0x500 | 8 | Event | HU | BCM, ACM | QM |
| HU_Request | 0x510 | 8 | Event | HU | Gateway | QM |
| HU_Diagnostics | 0x700 | 8 | Event | HU | All | QM |

### 3.2 消息优先级说明

| 优先级 | 消息类型 | 说明 |
|--------|----------|------|
| 0-3 | 安全关键 | 车速、挡位、制动 |
| 4-7 | 重要功能 | 发动机状态、电源模式 |
| 8-11 | 一般状态 | 车门、空调 |
| 12-15 | 诊断/配置 | 诊断请求、配置参数 |

---

## 4. 信号详细定义

### 4.1 车速信号 (VC_VehicleSpeed)

**Message ID**: 0x130 (320)  
**DLC**: 8  
**Cycle Time**: 20ms  
**Sender**: ABS  
**ASIL**: B

| Signal Name | Start Bit | Length | Byte Order | Factor | Offset | Min | Max | Unit | Description |
|-------------|-----------|--------|------------|--------|--------|-----|-----|------|-------------|
| VehicleSpeed | 0 | 16 | Intel | 0.01 | 0 | 0 | 655.35 | km/h | 实际车速 |
| VehicleSpeedValid | 16 | 1 | Intel | 1 | 0 | 0 | 1 | - | 0=无效, 1=有效 |
| ReverseGearSignal | 17 | 1 | Intel | 1 | 0 | 0 | 1 | - | 倒车信号 |
| ABS_Active | 18 | 1 | Intel | 1 | 0 | 0 | 1 | - | ABS激活状态 |
| ESP_Active | 19 | 1 | Intel | 1 | 0 | 0 | 1 | - | ESP激活状态 |
| RollingCounter | 24 | 4 | Intel | 1 | 0 | 0 | 15 | - | 滚动计数器 |
| Checksum | 28 | 4 | Intel | 1 | 0 | 0 | 15 | - | 校验和 |
| Reserved | 32 | 32 | Intel | - | - | - | - | - | 保留 |

### 4.2 挡位信号 (VC_GearPosition)

**Message ID**: 0x1F1 (497)  
**DLC**: 8  
**Cycle Time**: 50ms  
**Sender**: TCM  
**ASIL**: B

| Signal Name | Start Bit | Length | Byte Order | Factor | Offset | Min | Max | Unit | Description |
|-------------|-----------|--------|------------|--------|--------|-----|-----|------|-------------|
| GearPosition | 0 | 4 | Intel | 1 | 0 | 0 | 15 | - | 挡位显示：0=P,1=R,2=N,3=D,4=M,5=S,6=L,15=Invalid |
| GearShiftMode | 4 | 2 | Intel | 1 | 0 | 0 | 3 | - | 换挡模式：0=自动,1=手动,2=运动,3=经济 |
| CurrentGear | 8 | 4 | Intel | 1 | 0 | 0 | 8 | - | 当前实际挡位 |
| GearPositionValid | 12 | 1 | Intel | 1 | 0 | 0 | 1 | - | 挡位有效性 |
| TransmissionFault | 13 | 1 | Intel | 1 | 0 | 0 | 1 | - | 变速箱故障 |
| SportModeActive | 14 | 1 | Intel | 1 | 0 | 0 | 1 | - | 运动模式激活 |
| ManualModeActive | 15 | 1 | Intel | 1 | 0 | 0 | 1 | - | 手动模式激活 |
| PaddleShiftUp | 16 | 1 | Intel | 1 | 0 | 0 | 1 | - | 拨片升挡 |
| PaddleShiftDown | 17 | 1 | Intel | 1 | 0 | 0 | 1 | - | 拨片降挡 |
| RollingCounter | 24 | 4 | Intel | 1 | 0 | 0 | 15 | - | 滚动计数器 |
| Checksum | 28 | 4 | Intel | 1 | 0 | 0 | 15 | - | 校验和 |
| Reserved | 32 | 32 | Intel | - | - | - | - | - | 保留 |

### 4.3 驻车制动信号 (VC_ParkingBrake)

**Message ID**: 0x230 (560)  
**DLC**: 8  
**Cycle Time**: 100ms  
**Sender**: EPB  
**ASIL**: B

| Signal Name | Start Bit | Length | Byte Order | Factor | Offset | Min | Max | Unit | Description |
|-------------|-----------|--------|------------|--------|--------|-----|-----|------|-------------|
| ParkingBrakeStatus | 0 | 2 | Intel | 1 | 0 | 0 | 3 | - | 0=释放,1=启用,2=过渡,3=故障 |
| ParkingBrakeSwitch | 2 | 2 | Intel | 1 | 0 | 0 | 3 | - | 开关状态 |
| AutoHoldStatus | 4 | 2 | Intel | 1 | 0 | 0 | 3 | - | 自动驻车状态 |
| HillStartAssist | 6 | 1 | Intel | 1 | 0 | 0 | 1 | - | 坡道辅助激活 |
| EPB_Fault | 7 | 1 | Intel | 1 | 0 | 0 | 1 | - | EPB故障 |
| BrakeFluidLevel | 8 | 2 | Intel | 1 | 0 | 0 | 3 | - | 制动液位：0=正常,1=低,2=故障 |
| Reserved | 10 | 54 | Intel | - | - | - | - | - | 保留 |

### 4.4 车门状态信号 (VC_DoorStatus)

**Message ID**: 0x310 (784)  
**DLC**: 8  
**Cycle Time**: 100ms  
**Sender**: BCM  
**ASIL**: QM

| Signal Name | Start Bit | Length | Byte Order | Factor | Offset | Min | Max | Unit | Description |
|-------------|-----------|--------|------------|--------|--------|-----|-----|------|-------------|
| Door_FL_Open | 0 | 1 | Intel | 1 | 0 | 0 | 1 | - | 前左门开启 |
| Door_FR_Open | 1 | 1 | Intel | 1 | 0 | 0 | 1 | - | 前右门开启 |
| Door_RL_Open | 2 | 1 | Intel | 1 | 0 | 0 | 1 | - | 后左门开启 |
| Door_RR_Open | 3 | 1 | Intel | 1 | 0 | 0 | 1 | - | 后右门开启 |
| Trunk_Open | 4 | 1 | Intel | 1 | 0 | 0 | 1 | - | 后备箱开启 |
| Hood_Open | 5 | 1 | Intel | 1 | 0 | 0 | 1 | - | 引擎盖开启 |
| Door_FL_Lock | 8 | 1 | Intel | 1 | 0 | 0 | 1 | - | 前左门锁止 |
| Door_FR_Lock | 9 | 1 | Intel | 1 | 0 | 0 | 1 | - | 前右门锁止 |
| Door_RL_Lock | 10 | 1 | Intel | 1 | 0 | 0 | 1 | - | 后左门锁止 |
| Door_RR_Lock | 11 | 1 | Intel | 1 | 0 | 0 | 1 | - | 后右门锁止 |
| Trunk_Lock | 12 | 1 | Intel | 1 | 0 | 0 | 1 | - | 后备箱锁止 |
| Sunroof_Open | 16 | 1 | Intel | 1 | 0 | 0 | 1 | - | 天窗开启 |
| Window_FL_Open | 17 | 1 | Intel | 1 | 0 | 0 | 1 | - | 前左窗开启 |
| Window_FR_Open | 18 | 1 | Intel | 1 | 0 | 0 | 1 | - | 前右窗开启 |
| Window_RL_Open | 19 | 1 | Intel | 1 | 0 | 0 | 1 | - | 后左窗开启 |
| Window_RR_Open | 20 | 1 | Intel | 1 | 0 | 0 | 1 | - | 后右窗开启 |
| Reserved | 21 | 43 | Intel | - | - | - | - | - | 保留 |

### 4.5 车辆电源信号 (VC_VehiclePower)

**Message ID**: 0x320 (800)  
**DLC**: 8  
**Cycle Time**: 100ms  
**Sender**: BCM  
**ASIL**: B

| Signal Name | Start Bit | Length | Byte Order | Factor | Offset | Min | Max | Unit | Description |
|-------------|-----------|--------|------------|--------|--------|-----|-----|------|-------------|
| PowerMode | 0 | 3 | Intel | 1 | 0 | 0 | 7 | - | 0=Off,1=ACC,2=On,3=Start,4=Crank,7=Invalid |
| IgnitionSwitch | 3 | 2 | Intel | 1 | 0 | 0 | 3 | - | 点火开关状态 |
| BatteryVoltage | 8 | 8 | Intel | 0.1 | 0 | 0 | 25.5 | V | 蓄电池电压 |
| AlternatorFault | 16 | 1 | Intel | 1 | 0 | 0 | 1 | - | 发电机故障 |
| BatteryFault | 17 | 1 | Intel | 1 | 0 | 0 | 1 | - | 蓄电池故障 |
| Reserved | 18 | 46 | Intel | - | - | - | - | - | 保留 |

### 4.6 胎压信号 (VC_TirePressure)

**Message ID**: 0x330 (816)  
**DLC**: 8  
**Cycle Time**: 1000ms  
**Sender**: BCM  
**ASIL**: A

| Signal Name | Start Bit | Length | Byte Order | Factor | Offset | Min | Max | Unit | Description |
|-------------|-----------|--------|------------|--------|--------|-----|-----|------|-------------|
| TirePressure_FL | 0 | 8 | Intel | 1.5 | 0 | 0 | 382.5 | kPa | 前左轮胎压 |
| TirePressure_FR | 8 | 8 | Intel | 1.5 | 0 | 0 | 382.5 | kPa | 前右轮胎压 |
| TirePressure_RL | 16 | 8 | Intel | 1.5 | 0 | 0 | 382.5 | kPa | 后左轮胎压 |
| TirePressure_RR | 24 | 8 | Intel | 1.5 | 0 | 0 | 382.5 | kPa | 后右轮胎压 |
| TPMS_Fault | 32 | 1 | Intel | 1 | 0 | 0 | 1 | - | TPMS系统故障 |
| TireTemp_FL | 33 | 8 | Intel | 1 | -40 | -40 | 215 | ℃ | 前左胎温 |
| TireTemp_FR | 41 | 8 | Intel | 1 | -40 | -40 | 215 | ℃ | 前右胎温 |
| Reserved | 49 | 15 | Intel | - | - | - | - | - | 保留 |

### 4.7 油量信号 (VC_FuelLevel)

**Message ID**: 0x340 (832)  
**DLC**: 8  
**Cycle Time**: 1000ms  
**Sender**: BCM  
**ASIL**: QM

| Signal Name | Start Bit | Length | Byte Order | Factor | Offset | Min | Max | Unit | Description |
|-------------|-----------|--------|------------|--------|--------|-----|-----|------|-------------|
| FuelLevelPercent | 0 | 8 | Intel | 0.5 | 0 | 0 | 127.5 | % | 油量百分比 |
| FuelRange | 8 | 12 | Intel | 1 | 0 | 0 | 4095 | km | 续航里程 |
| LowFuelWarning | 20 | 2 | Intel | 1 | 0 | 0 | 3 | - | 低油量警告级别 |
| FuelSensorFault | 22 | 1 | Intel | 1 | 0 | 0 | 1 | - | 油位传感器故障 |
| Reserved | 23 | 41 | Intel | - | - | - | - | - | 保留 |

### 4.8 里程信号 (VC_Odometer)

**Message ID**: 0x350 (848)  
**DLC**: 8  
**Cycle Time**: 1000ms  
**Sender**: BCM  
**ASIL**: QM

| Signal Name | Start Bit | Length | Byte Order | Factor | Offset | Min | Max | Unit | Description |
|-------------|-----------|--------|------------|--------|--------|-----|-----|------|-------------|
| OdometerTotal | 0 | 24 | Intel | 0.1 | 0 | 0 | 1677721.5 | km | 总里程 |
| TripA | 24 | 16 | Intel | 0.1 | 0 | 0 | 6553.5 | km | 小计里程A |
| TripB | 40 | 16 | Intel | 0.1 | 0 | 0 | 6553.5 | km | 小计里程B |
| Reserved | 56 | 8 | Intel | - | - | - | - | - | 保留 |

### 4.9 方向盘角度信号 (VC_SteeringAngle)

**Message ID**: 0x140 (320)  
**DLC**: 8  
**Cycle Time**: 20ms  
**Sender**: ABS  
**ASIL**: B

| Signal Name | Start Bit | Length | Byte Order | Factor | Offset | Min | Max | Unit | Description |
|-------------|-----------|--------|------------|--------|--------|-----|-----|------|-------------|
| SteeringAngle | 0 | 16 | Intel | 0.1 | -3276.8 | -3276.8 | 3276.7 | ° | 方向盘角度，左负右正 |
| SteeringAngleValid | 16 | 1 | Intel | 1 | 0 | 0 | 1 | - | 角度有效性 |
| SteeringAngleSpeed | 17 | 12 | Intel | 1 | -2048 | -2048 | 2047 | °/s | 转向角速度 |
| SteeringFault | 29 | 1 | Intel | 1 | 0 | 0 | 1 | - | 转向系统故障 |
| Reserved | 30 | 34 | Intel | - | - | - | - | - | 保留 |

### 4.10 发动机状态信号 (VC_EngineStatus)

**Message ID**: 0x120 (288)  
**DLC**: 8  
**Cycle Time**: 50ms  
**Sender**: ECM  
**ASIL**: A

| Signal Name | Start Bit | Length | Byte Order | Factor | Offset | Min | Max | Unit | Description |
|-------------|-----------|--------|------------|--------|--------|-----|-----|------|-------------|
| EngineRPM | 0 | 16 | Intel | 0.25 | 0 | 0 | 16383.75 | rpm | 发动机转速 |
| EngineRunning | 16 | 1 | Intel | 1 | 0 | 0 | 1 | - | 发动机运行状态 |
| EngineFault | 17 | 1 | Intel | 1 | 0 | 0 | 1 | - | 发动机故障 |
| CoolantTemp | 18 | 8 | Intel | 1 | -40 | -40 | 215 | ℃ | 冷却液温度 |
| OilPressureWarning | 26 | 1 | Intel | 1 | 0 | 0 | 1 | - | 机油压力警告 |
| CheckEngineLight | 27 | 1 | Intel | 1 | 0 | 0 | 1 | - | 发动机故障灯 |
| EngineLoad | 28 | 8 | Intel | 0.5 | 0 | 0 | 127.5 | % | 发动机负荷 |
| ThrottlePosition | 36 | 8 | Intel | 0.4 | 0 | 0 | 102 | % | 节气门开度 |
| Reserved | 44 | 20 | Intel | - | - | - | - | - | 保留 |

---

## 5. 信号矩阵表

### 5.1 接收信号矩阵 (HU接收)

| 信号名称 | 信号描述 | 来源 | Message ID | 字节位 | 长度 | 周期 | ASIL |
|----------|----------|------|------------|--------|------|------|------|
| VehicleSpeed | 车速 | ABS | 0x130 | 0-15 | 16bit | 20ms | B |
| VehicleSpeedValid | 车速有效 | ABS | 0x130 | 16 | 1bit | 20ms | B |
| ReverseGearSignal | 倒车信号 | ABS | 0x130 | 17 | 1bit | 20ms | B |
| GearPosition | 挡位 | TCM | 0x1F1 | 0-3 | 4bit | 50ms | B |
| GearPositionValid | 挡位有效 | TCM | 0x1F1 | 12 | 1bit | 50ms | B |
| ParkingBrakeStatus | 驻车制动 | EPB | 0x230 | 0-1 | 2bit | 100ms | B |
| Door_FL_Open | 前左门开 | BCM | 0x310 | 0 | 1bit | 100ms | QM |
| Door_FR_Open | 前右门开 | BCM | 0x310 | 1 | 1bit | 100ms | QM |
| Door_RL_Open | 后左门开 | BCM | 0x310 | 2 | 1bit | 100ms | QM |
| Door_RR_Open | 后右门开 | BCM | 0x310 | 3 | 1bit | 100ms | QM |
| Trunk_Open | 后备箱开 | BCM | 0x310 | 4 | 1bit | 100ms | QM |
| PowerMode | 电源模式 | BCM | 0x320 | 0-2 | 3bit | 100ms | B |
| BatteryVoltage | 电池电压 | BCM | 0x320 | 8-15 | 8bit | 100ms | B |
| TirePressure_FL | 前左胎压 | BCM | 0x330 | 0-7 | 8bit | 1000ms | A |
| TirePressure_FR | 前右胎压 | BCM | 0x330 | 8-15 | 8bit | 1000ms | A |
| TirePressure_RL | 后左胎压 | BCM | 0x330 | 16-23 | 8bit | 1000ms | A |
| TirePressure_RR | 后右胎压 | BCM | 0x330 | 24-31 | 8bit | 1000ms | A |
| FuelLevelPercent | 油量百分比 | BCM | 0x340 | 0-7 | 8bit | 1000ms | QM |
| FuelRange | 续航里程 | BCM | 0x340 | 8-19 | 12bit | 1000ms | QM |
| OdometerTotal | 总里程 | BCM | 0x350 | 0-23 | 24bit | 1000ms | QM |
| SteeringAngle | 方向盘角度 | ABS | 0x140 | 0-15 | 16bit | 20ms | B |
| EngineRPM | 发动机转速 | ECM | 0x120 | 0-15 | 16bit | 50ms | A |
| EngineRunning | 发动机运行 | ECM | 0x120 | 16 | 1bit | 50ms | A |
| CoolantTemp | 冷却液温度 | ECM | 0x120 | 18-25 | 8bit | 50ms | A |

### 5.2 发送信号矩阵 (HU发送)

| 信号名称 | 信号描述 | 目标 | Message ID | 字节位 | 长度 | 类型 | ASIL |
|----------|----------|------|------------|--------|------|------|------|
| HU_DisplayBrightness | 显示亮度 | BCM | 0x500 | 0-6 | 7bit | Event | QM |
| HU_Volume | 系统音量 | BCM | 0x500 | 7-13 | 7bit | Event | QM |
| HU_ScreenState | 屏幕状态 | BCM | 0x500 | 14-15 | 2bit | Event | QM |
| HU_BacklightReq | 背光请求 | BCM | 0x500 | 16 | 1bit | Event | QM |
| HU_DoorLockReq | 门锁请求 | BCM | 0x510 | 0 | 1bit | Event | QM |
| HU_DoorUnlockReq | 解锁请求 | BCM | 0x510 | 1 | 1bit | Event | QM |
| HU_TrunkOpenReq | 后备箱请求 | BCM | 0x510 | 2 | 1bit | Event | QM |
| HU_WindowControl | 车窗控制 | BCM | 0x510 | 3-6 | 4bit | Event | QM |
| HU_ACPowerReq | 空调电源 | ACM | 0x510 | 7 | 1bit | Event | QM |
| HU_DiagnosticReq | 诊断请求 | Gateway | 0x700 | 0-63 | 64bit | Event | QM |

---

## 6. 周期性消息时序

### 6.1 消息时序图

```
时间(ms)    0    20   40   50   60   80   100  120  140  160
            |    |    |    |    |    |    |    |    |    |
VC_VehicleSpeed ●----●----●----●----●----●----●----●----●
(20ms)              |    |    |    |    |    |    |    |
VC_SteeringAngle ●----●----●----●----●----●----●----●----●
(20ms)              |    |    |    |    |    |    |    |
VC_EngineStatus     ●---------●---------●---------●---------
(50ms)              |         |         |         |
VC_GearPosition     ●---------●---------●---------●---------
(50ms)              |         |         |         |
VC_ParkingBrake               ●--------------●--------------
(100ms)                       |              |
VC_DoorStatus                 ●--------------●--------------
(100ms)                       |              |
VC_VehiclePower               ●--------------●--------------
(100ms)                       |              |
```

### 6.2 消息负载分配

| 时间段 | 消息负载 |
|--------|----------|
| 0ms | VehicleSpeed, SteeringAngle |
| 20ms | VehicleSpeed, SteeringAngle |
| 40ms | VehicleSpeed, SteeringAngle |
| 50ms | EngineStatus, GearPosition |
| 60ms | VehicleSpeed, SteeringAngle |
| 80ms | VehicleSpeed, SteeringAngle |
| 100ms | VehicleSpeed, SteeringAngle, ParkingBrake, DoorStatus, VehiclePower |

---

## 7. 故障处理机制

### 7.1 信号有效性检测

| 检测项 | 方法 | 失效处理 |
|--------|------|----------|
| 滚动计数器 | 递增检测 | 标记信号无效 |
| 校验和 | XOR/Checksum | 丢弃消息 |
| 超时检测 | 3倍周期无消息 | 使用默认值/历史值 |
| 范围检测 | 超出Min/Max | 限制在有效范围 |

### 7.2 超时时间定义

| 消息 | 周期 | 超时时间 | 安全状态 |
|------|------|----------|----------|
| VC_VehicleSpeed | 20ms | 100ms | 保持最后有效值 |
| VC_GearPosition | 50ms | 200ms | 默认N挡 |
| VC_ParkingBrake | 100ms | 500ms | 假设启用 |
| VC_DoorStatus | 100ms | 500ms | 保持最后状态 |
| VC_VehiclePower | 100ms | 500ms | 默认Off |
| VC_TirePressure | 1000ms | 5000ms | 显示-- |
| VC_FuelLevel | 1000ms | 5000ms | 显示-- |
| VC_Odometer | 1000ms | 5000ms | 保持最后值 |
| VC_SteeringAngle | 20ms | 100ms | 设为0 |
| VC_EngineStatus | 50ms | 200ms | 假设停止 |

### 7.3 信号有效性矩阵

| 信号名称 | Valid信号 | 超时处理 | ASIL |
|----------|-----------|----------|------|
| VehicleSpeed | VehicleSpeedValid | 冻结 | B |
| GearPosition | GearPositionValid | N挡 | B |
| SteeringAngle | SteeringAngleValid | 0度 | B |
| ParkingBrakeStatus | 无 | 启用 | B |
| PowerMode | 无 | Off | B |
| TirePressure | TPMS_Fault | 显示-- | A |
| FuelLevelPercent | FuelSensorFault | 显示-- | QM |
| EngineRPM | EngineRunning | 0 | A |

---

## 附录 A：DBC文件片段

```
VERSION ""

NS_ :
    NS_DESC_
    CM_
    BA_DEF_
    BA_
    VAL_
    CAT_DEF_
    CAT_
    FILTER
    BA_DEF_DEF_
    EV_DATA_
    ENVVAR_DATA_
    SGTYPE_
    SGTYPE_VAL_
    BA_DEF_SGTYPE_
    BA_SGTYPE_
    SIG_TYPE_REF_
    VAL_TABLE_
    SIG_GROUP_
    SIG_VALTYPE_
    SIGTYPE_VALTYPE_
    BO_TX_BU_
    BA_DEF_REL_
    BA_REL_
    BA_DEF_DEF_REL_
    BU_SG_REL_
    BU_EV_REL_
    BU_BO_REL_
    SG_MUL_VAL_

BS_:

BU_: ABS TCM BCM EPB ECM HU Cluster Gateway

BO_ 304 VC_VehicleSpeed: 8 ABS
 SG_ VehicleSpeed : 0|16@1+ (0.01,0) [0|655.35] "km/h"  HU,Cluster
 SG_ VehicleSpeedValid : 16|1@1+ (1,0) [0|1] ""  HU,Cluster
 SG_ ReverseGearSignal : 17|1@1+ (1,0) [0|1] ""  HU,Cluster
 SG_ ABS_Active : 18|1@1+ (1,0) [0|1] ""  HU,Cluster
 SG_ ESP_Active : 19|1@1+ (1,0) [0|1] ""  HU,Cluster
 SG_ RollingCounter : 24|4@1+ (1,0) [0|15] ""  HU,Cluster
 SG_ Checksum : 28|4@1+ (1,0) [0|15] ""  HU,Cluster

BO_ 497 VC_GearPosition: 8 TCM
 SG_ GearPosition : 0|4@1+ (1,0) [0|15] ""  HU,Cluster
 SG_ GearShiftMode : 4|2@1+ (1,0) [0|3] ""  HU,Cluster
 SG_ CurrentGear : 8|4@1+ (1,0) [0|8] ""  HU,Cluster
 SG_ GearPositionValid : 12|1@1+ (1,0) [0|1] ""  HU,Cluster
 SG_ TransmissionFault : 13|1@1+ (1,0) [0|1] ""  HU,Cluster
 SG_ RollingCounter : 24|4@1+ (1,0) [0|15] ""  HU,Cluster
 SG_ Checksum : 28|4@1+ (1,0) [0|15] ""  HU,Cluster

VAL_ 497 GearPosition 0 "P" 1 "R" 2 "N" 3 "D" 4 "M" 5 "S" 6 "L" 15 "Invalid" ;
VAL_ 800 PowerMode 0 "Off" 1 "ACC" 2 "On" 3 "Start" 4 "Crank" 7 "Invalid" ;
```

---

## 附录 B：信号值转换示例

### B.1 车速信号转换
```
原始值 (Hex): 0x1770
原始值 (Dec): 6000
Factor: 0.01
Offset: 0
物理值 = 6000 × 0.01 + 0 = 60.00 km/h
```

### B.2 方向盘角度转换
```
原始值 (Hex): 0x0064
原始值 (Dec): 100
Factor: 0.1
Offset: -3276.8
物理值 = 100 × 0.1 + (-3276.8) = 10 - 3276.8 = -3266.8° (错误示例)

原始值 (Hex): 0x8000 + 0x0064 = 0x8064
原始值 (Dec): 32868
物理值 = 32868 × 0.1 + (-3276.8) = 3286.8 - 3276.8 = 10.0°
```

---

**文档结束**

*本文档定义了智能座舱主交互系统的CAN信号矩阵，所有CAN通信实现必须遵循本文档定义。*

**编制**: 上海龙旗智能科技有限公司  
**审核**: [待填写]  
**批准**: [待填写]  
**日期**: 2024-06-10
