# REST API接口规范
## REST API Interface Specification

**文档版本**: V1.0  
**编制日期**: 2024-06-10  
**编制单位**: 上海龙旗智能科技有限公司  
**API版本**: v1  
**符合标准**: RESTful API Design, OpenAPI 3.0

---

## 目录

1. [概述](#1-概述)
2. [通用规范](#2-通用规范)
3. [认证授权](#3-认证授权)
4. [API接口定义](#4-api接口定义)
5. [错误码定义](#5-错误码定义)
6. [事件定义](#6-事件定义)

---

## 1. 概述

### 1.1 文档目的
本文档定义智能座舱主交互系统与TSP平台、内容服务、手机互联等外部系统间的REST API接口规范。

### 1.2 接口范围
本文档涵盖以下外部接口：
- TSP平台接口（车辆状态上报、远程控制、OTA）
- 内容服务接口（地图、音乐、语音云）
- 用户服务接口（账号、偏好设置）
- 诊断服务接口（日志上传、故障上报）

### 1.3 基础信息
| 项目 | 说明 |
|------|------|
| 协议 | HTTPS |
| 数据格式 | JSON |
| 字符编码 | UTF-8 |
| 时间格式 | ISO 8601 (2024-06-10T14:30:00Z) |
| 日期格式 | YYYY-MM-DD |
| 时区 | UTC+8 (Asia/Shanghai) |

---

## 2. 通用规范

### 2.1 请求格式

#### 请求头 (Request Headers)
| 字段 | 必填 | 说明 |
|------|------|------|
| Content-Type | 是 | `application/json` |
| Authorization | 是 | `Bearer {access_token}` |
| X-Request-ID | 是 | 请求唯一标识 (UUID) |
| X-Device-ID | 是 | 设备唯一标识 |
| X-VIN | 是 | 车辆识别码 |
| X-Timestamp | 是 | 请求时间戳 (Unix毫秒) |
| X-API-Version | 否 | API版本号，默认v1 |

#### 请求示例
```http
POST /api/v1/vehicle/status HTTP/1.1
Host: api.longcheer.com
Content-Type: application/json
Authorization: Bearer eyJhbGciOiJSUzI1NiIs...
X-Request-ID: 550e8400-e29b-41d4-a716-446655440000
X-Device-ID: DEV123456789
X-VIN: LSVNV2182E2100001
X-Timestamp: 1718003400000
X-API-Version: v1

{
  "speed": 60,
  "gear": "D",
  "odometer": 12580
}
```

### 2.2 响应格式

#### 成功响应
```json
{
  "code": 200,
  "message": "success",
  "data": {},
  "timestamp": 1718003400123,
  "requestId": "550e8400-e29b-41d4-a716-446655440000"
}
```

#### 错误响应
```json
{
  "code": 40001,
  "message": "Invalid parameter: speed must be >= 0",
  "data": null,
  "timestamp": 1718003400123,
  "requestId": "550e8400-e29b-41d4-a716-446655440000"
}
```

#### 响应字段说明
| 字段 | 类型 | 说明 |
|------|------|------|
| code | Integer | 状态码 (200成功，其他见错误码定义) |
| message | String | 响应消息 |
| data | Object | 响应数据 (可能为null) |
| timestamp | Long | 服务器响应时间戳 |
| requestId | String | 请求ID (用于追踪) |

### 2.3 HTTP方法规范
| 方法 | 用途 |
|------|------|
| GET | 获取资源 |
| POST | 创建资源 / 执行操作 |
| PUT | 更新资源 (完整替换) |
| PATCH | 部分更新资源 |
| DELETE | 删除资源 |

### 2.4 URL设计规范
```
https://{host}/api/{version}/{resource}/{action}
```

| 部分 | 说明 | 示例 |
|------|------|------|
| host | API服务器域名 | api.longcheer.com |
| version | API版本 | v1, v2 |
| resource | 资源名称 | vehicle, user, message |
| action | 操作 (可选) | status, command, upload |

---

## 3. 认证授权

### 3.1 认证方式
采用OAuth 2.0 + JWT Token认证机制

### 3.2 Token获取接口

#### POST /api/v1/auth/token
获取访问令牌

**请求参数：**
| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| grantType | String | 是 | 授权类型：`client_credentials` |
| clientId | String | 是 | 客户端ID |
| clientSecret | String | 是 | 客户端密钥 |
| scope | String | 否 | 权限范围 |

**请求示例：**
```json
{
  "grantType": "client_credentials",
  "clientId": "vehicle_client_001",
  "clientSecret": "xxxxxxxxxxxxxxxx",
  "scope": "vehicle:read vehicle:write"
}
```

**响应示例：**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "accessToken": "eyJhbGciOiJSUzI1NiIs...",
    "tokenType": "Bearer",
    "expiresIn": 3600,
    "scope": "vehicle:read vehicle:write"
  },
  "timestamp": 1718003400123,
  "requestId": "550e8400-e29b-41d4-a716-446655440000"
}
```

**Token数据结构：**
```json
{
  "accessToken": "eyJhbGciOiJSUzI1NiIs...",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "scope": "vehicle:read vehicle:write"
}
```

### 3.3 Token刷新接口

#### POST /api/v1/auth/refresh
刷新访问令牌

**请求参数：**
| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| refreshToken | String | 是 | 刷新令牌 |

**响应示例：**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "accessToken": "eyJhbGciOiJSUzI1NiIs...",
    "refreshToken": "eyJhbGciOiJSUzI1NiIs...",
    "expiresIn": 3600
  }
}
```

---

## 4. API接口定义

### 4.1 车辆状态接口

#### 4.1.1 上报车辆状态

**接口信息：**
| 项目 | 内容 |
|------|------|
| 接口地址 | POST /api/v1/vehicle/status |
| 安全等级 | ASIL B |
| 频率限制 | 1次/秒 |

**请求参数：**
| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| timestamp | Long | 是 | 数据产生时间戳 |
| speed | Integer | 是 | 车速 (km/h) |
| gear | String | 是 | 挡位：P/R/N/D/M/S/L |
| parkingBrake | Boolean | 是 | 驻车制动状态 |
| odometer | Long | 是 | 总里程 (km) |
| fuelLevel | Integer | 否 | 油量百分比 (0-100) |
| batteryLevel | Integer | 否 | 电量百分比 (0-100) |
| powerMode | String | 是 | 电源模式：OFF/ACC/ON/START |
| engineRpm | Integer | 否 | 发动机转速 |
| steeringAngle | Float | 否 | 方向盘角度 |
| location | Object | 否 | 位置信息 |

**location对象：**
| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| latitude | Double | 是 | 纬度 |
| longitude | Double | 是 | 经度 |
| altitude | Double | 否 | 海拔 |
| accuracy | Float | 否 | 精度 |
| heading | Float | 否 | 方向 |

**请求示例：**
```json
{
  "timestamp": 1718003400000,
  "speed": 60,
  "gear": "D",
  "parkingBrake": false,
  "odometer": 12580,
  "fuelLevel": 65,
  "batteryLevel": 0,
  "powerMode": "ON",
  "engineRpm": 2500,
  "steeringAngle": 5.5,
  "location": {
    "latitude": 31.2304,
    "longitude": 121.4737,
    "altitude": 10.0,
    "accuracy": 5.0,
    "heading": 90.0
  }
}
```

**响应示例：**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "recordId": "REC123456789",
    "receivedAt": 1718003400100
  },
  "timestamp": 1718003400123,
  "requestId": "550e8400-e29b-41d4-a716-446655440000"
}
```

#### 4.1.2 批量上报车辆状态

**接口信息：**
| 项目 | 内容 |
|------|------|
| 接口地址 | POST /api/v1/vehicle/status/batch |
| 安全等级 | ASIL B |
| 批量限制 | 最多100条/次 |

**请求参数：**
| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| records | Array | 是 | 状态记录数组 |

**请求示例：**
```json
{
  "records": [
    {
      "timestamp": 1718003400000,
      "speed": 60,
      "gear": "D",
      "parkingBrake": false,
      "odometer": 12580
    },
    {
      "timestamp": 1718003401000,
      "speed": 62,
      "gear": "D",
      "parkingBrake": false,
      "odometer": 12580
    }
  ]
}
```

#### 4.1.3 查询车辆状态

**接口信息：**
| 项目 | 内容 |
|------|------|
| 接口地址 | GET /api/v1/vehicle/status |
| 安全等级 | QM |

**请求参数：**
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| vin | String | 是 | 车辆VIN码 |
| fields | String | 否 | 指定字段，逗号分隔 |

**响应示例：**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "vin": "LSVNV2182E2100001",
    "timestamp": 1718003400000,
    "speed": 60,
    "gear": "D",
    "parkingBrake": false,
    "odometer": 12580,
    "fuelLevel": 65,
    "location": {
      "latitude": 31.2304,
      "longitude": 121.4737
    }
  },
  "timestamp": 1718003400123,
  "requestId": "550e8400-e29b-41d4-a716-446655440000"
}
```

### 4.2 远程控制接口

#### 4.2.1 接收远程控制指令

**接口说明：** 此接口由车辆端长连接接收，服务端主动推送

**WebSocket连接：**
```
wss://api.longcheer.com/ws/v1/vehicle/control?vin={vin}&token={token}
```

**指令格式：**
| 字段 | 类型 | 说明 |
|------|------|------|
| commandId | String | 指令唯一ID |
| commandType | String | 指令类型 |
| parameters | Object | 指令参数 |
| timestamp | Long | 指令下发时间 |
| timeout | Integer | 超时时间(秒) |

**支持的指令类型：**
| 指令类型 | 说明 | 参数 |
|----------|------|------|
| DOOR_LOCK | 车门上锁 | - |
| DOOR_UNLOCK | 车门解锁 | - |
| ENGINE_START | 远程启动 | - |
| ENGINE_STOP | 远程熄火 | - |
| AC_ON | 空调开启 | temperature: 温度 |
| AC_OFF | 空调关闭 | - |
| HORN | 鸣笛 | duration: 时长 |
| FLASH | 闪灯 | duration: 时长, times: 次数 |
| TRUNK_OPEN | 开启后备箱 | - |
| WINDOWS_OPEN | 降窗 | - |
| WINDOWS_CLOSE | 升窗 | - |
| FIND_VEHICLE | 寻车 | - |

**指令示例：**
```json
{
  "commandId": "CMD123456789",
  "commandType": "AC_ON",
  "parameters": {
    "temperature": 24,
    "mode": "AUTO"
  },
  "timestamp": 1718003400000,
  "timeout": 30
}
```

#### 4.2.2 上报指令执行结果

**接口信息：**
| 项目 | 内容 |
|------|------|
| 接口地址 | POST /api/v1/vehicle/command/result |
| 安全等级 | ASIL B |

**请求参数：**
| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| commandId | String | 是 | 指令ID |
| vin | String | 是 | 车辆VIN |
| status | String | 是 | 状态：SUCCESS/FAILED/TIMEOUT |
| resultCode | Integer | 否 | 结果码 |
| resultMessage | String | 否 | 结果描述 |
| executeTime | Long | 否 | 执行耗时(ms) |
| timestamp | Long | 是 | 结果上报时间 |

**请求示例：**
```json
{
  "commandId": "CMD123456789",
  "vin": "LSVNV2182E2100001",
  "status": "SUCCESS",
  "resultCode": 0,
  "resultMessage": "空调已开启，温度24℃",
  "executeTime": 1200,
  "timestamp": 1718003401200
}
```

### 4.3 OTA升级接口

#### 4.3.1 查询OTA更新

**接口信息：**
| 项目 | 内容 |
|------|------|
 接口地址 | GET /api/v1/ota/check |
| 安全等级 | QM |

**请求参数：**
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| vin | String | 是 | 车辆VIN |
| deviceType | String | 是 | 设备类型：MCU/SOC/AMP |
| currentVersion | String | 是 | 当前版本号 |
| currentBuild | String | 是 | 当前构建号 |

**响应示例：**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "hasUpdate": true,
    "updateInfo": {
      "version": "2.1.0",
      "buildNumber": "20240610",
      "fileSize": 524288000,
      "downloadUrl": "https://ota.longcheer.com/packages/...",
      "releaseNotes": "1. 优化导航体验\n2. 修复已知问题",
      "md5": "d41d8cd98f00b204e9800998ecf8427e",
      "isForce": false,
      "releaseDate": "2024-06-10"
    }
  }
}
```

#### 4.3.2 上报升级进度

**接口信息：**
| 项目 | 内容 |
|------|------|
| 接口地址 | POST /api/v1/ota/progress |
| 安全等级 | QM |

**请求参数：**
| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| vin | String | 是 | 车辆VIN |
| taskId | String | 是 | 升级任务ID |
| status | String | 是 | 状态：DOWNLOADING/VERIFYING/INSTALLING/REBOOTING/COMPLETED/FAILED |
| progress | Integer | 是 | 进度百分比 (0-100) |
| phase | String | 否 | 当前阶段描述 |
| timestamp | Long | 是 | 上报时间 |

**请求示例：**
```json
{
  "vin": "LSVNV2182E2100001",
  "taskId": "OTA123456789",
  "status": "DOWNLOADING",
  "progress": 45,
  "phase": "正在下载升级包",
  "timestamp": 1718003400000
}
```

#### 4.3.3 上报升级结果

**接口信息：**
| 项目 | 内容 |
|------|------|
| 接口地址 | POST /api/v1/ota/result |
| 安全等级 | QM |

**请求参数：**
| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| vin | String | 是 | 车辆VIN |
| taskId | String | 是 | 升级任务ID |
| success | Boolean | 是 | 是否成功 |
| fromVersion | String | 是 | 原版本 |
| toVersion | String | 是 | 目标版本 |
| duration | Integer | 是 | 升级耗时(秒) |
| errorCode | String | 否 | 错误码 |
| errorMessage | String | 否 | 错误信息 |
| timestamp | Long | 是 | 上报时间 |

### 4.4 消息服务接口

#### 4.4.1 拉取消息

**接口信息：**
| 项目 | 内容 |
|------|------|
| 接口地址 | GET /api/v1/messages |
| 安全等级 | QM |

**请求参数：**
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| vin | String | 是 | 车辆VIN |
| category | String | 否 | 消息分类 |
| unreadOnly | Boolean | 否 | 仅未读 |
| page | Integer | 否 | 页码，默认1 |
| pageSize | Integer | 否 | 每页数量，默认20 |

**响应示例：**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "list": [
      {
        "id": "MSG123456789",
        "category": "SYSTEM",
        "priority": "HIGH",
        "title": "系统更新",
        "content": "有新版本系统可供更新",
        "timestamp": 1718003400000,
        "isRead": false,
        "actions": [
          {
            "id": "UPDATE_NOW",
            "label": "立即更新",
            "type": "DEEP_LINK",
            "url": "settings://ota"
          }
        ]
      }
    ],
    "total": 100,
    "page": 1,
    "pageSize": 20,
    "hasMore": true
  }
}
```

#### 4.4.2 同步消息状态

**接口信息：**
| 项目 | 内容 |
|------|------|
| 接口地址 | POST /api/v1/messages/sync |
| 安全等级 | QM |

**请求参数：**
| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| vin | String | 是 | 车辆VIN |
| syncType | String | 是 | 同步类型：READ/DELETE/ALL_READ |
| messageIds | Array | 条件 | 消息ID列表 |
| timestamp | Long | 是 | 同步时间 |

### 4.5 内容服务接口

#### 4.5.1 搜索POI

**接口信息：**
| 项目 | 内容 |
|------|------|
| 接口地址 | GET /api/v1/poi/search |
| 安全等级 | QM |

**请求参数：**
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| keyword | String | 是 | 搜索关键词 |
| longitude | Double | 是 | 当前经度 |
| latitude | Double | 是 | 当前纬度 |
| radius | Integer | 否 | 搜索半径(m)，默认5000 |
| category | String | 否 | POI分类 |
| page | Integer | 否 | 页码，默认1 |
| pageSize | Integer | 否 | 每页数量，默认20 |

**响应示例：**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "list": [
      {
        "id": "POI123456789",
        "name": "上海龙旗科技园",
        "address": "上海市浦东新区XX路XX号",
        "longitude": 121.4737,
        "latitude": 31.2304,
        "category": "COMPANY",
        "phone": "021-XXXXXXXX",
        "distance": 1200,
        "rating": 4.5
      }
    ],
    "total": 50
  }
}
```

#### 4.5.2 获取推荐内容

**接口信息：**
| 项目 | 内容 |
|------|------|
| 接口地址 | GET /api/v1/content/recommend |
| 安全等级 | QM |

**请求参数：**
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| vin | String | 是 | 车辆VIN |
| type | String | 是 | 推荐类型：MUSIC/NEWS/VIDEO/APP |
| count | Integer | 否 | 推荐数量，默认10 |

### 4.6 诊断服务接口

#### 4.6.1 上报故障码

**接口信息：**
| 项目 | 内容 |
|------|------|
| 接口地址 | POST /api/v1/diag/dtc |
| 安全等级 | ASIL A |

**请求参数：**
| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| vin | String | 是 | 车辆VIN |
| timestamp | Long | 是 | 故障发生时间 |
| dtcs | Array | 是 | 故障码列表 |

**dtc对象：**
| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| code | String | 是 | 故障码 |
| ecu | String | 是 | ECU名称 |
| status | String | 是 | 状态：ACTIVE/PENDING/HISTORY |
| severity | Integer | 是 | 严重程度 (1-4) |
| description | String | 否 | 故障描述 |

**请求示例：**
```json
{
  "vin": "LSVNV2182E2100001",
  "timestamp": 1718003400000,
  "dtcs": [
    {
      "code": "P0101",
      "ecu": "ECM",
      "status": "ACTIVE",
      "severity": 2,
      "description": "Mass Air Flow Circuit Range/Performance"
    }
  ]
}
```

#### 4.6.2 上传日志

**接口信息：**
| 项目 | 内容 |
|------|------|
| 接口地址 | POST /api/v1/diag/log/upload |
| 安全等级 | QM |
| 传输方式 | 分片上传 |

**请求参数：**
| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| vin | String | 是 | 车辆VIN |
| logType | String | 是 | 日志类型 |
| fileName | String | 是 | 文件名 |
| fileSize | Long | 是 | 文件大小 |
| chunkIndex | Integer | 是 | 分片序号 |
| chunkCount | Integer | 是 | 总分片数 |
| chunkData | String | 是 | Base64编码的分片数据 |
| checksum | String | 是 | 分片MD5 |

---

## 5. 错误码定义

### 5.1 错误码分类

| 范围 | 类别 |
|------|------|
| 00000-00999 | 通用错误 |
| 10000-19999 | 认证授权错误 |
| 20000-29999 | 车辆相关错误 |
| 30000-39999 | 消息相关错误 |
| 40000-49999 | OTA相关错误 |
| 50000-59999 | 诊断相关错误 |
| 90000-99999 | 系统错误 |

### 5.2 通用错误码 (00000-00999)

| 错误码 | 错误信息 | 说明 |
|--------|----------|------|
| 200 | success | 成功 |
| 400 | bad request | 请求参数错误 |
| 401 | unauthorized | 未授权 |
| 403 | forbidden | 禁止访问 |
| 404 | not found | 资源不存在 |
| 429 | too many requests | 请求过于频繁 |
| 500 | internal server error | 服务器内部错误 |
| 503 | service unavailable | 服务不可用 |

### 5.3 认证授权错误码 (10000-19999)

| 错误码 | 错误信息 | 说明 |
|--------|----------|------|
| 10001 | invalid client credentials | 客户端凭证无效 |
| 10002 | token expired | Token已过期 |
| 10003 | invalid token | Token无效 |
| 10004 | insufficient scope | 权限不足 |
| 10005 | token refresh failed | Token刷新失败 |
| 10006 | account disabled | 账号已禁用 |
| 10007 | device not authorized | 设备未授权 |

### 5.4 车辆相关错误码 (20000-29999)

| 错误码 | 错误信息 | 说明 |
|--------|----------|------|
| 20001 | invalid vin | VIN码无效 |
| 20002 | vehicle not found | 车辆不存在 |
| 20003 | vehicle offline | 车辆离线 |
| 20004 | command not supported | 指令不支持 |
| 20005 | command execution failed | 指令执行失败 |
| 20006 | command timeout | 指令执行超时 |
| 20007 | vehicle busy | 车辆忙 |
| 20008 | invalid parameter value | 参数值无效 |
| 20009 | safety check failed | 安全检查失败 |

### 5.5 消息相关错误码 (30000-39999)

| 错误码 | 错误信息 | 说明 |
|--------|----------|------|
| 30001 | message not found | 消息不存在 |
| 30002 | invalid message category | 无效的消息分类 |
| 30003 | message queue full | 消息队列已满 |
| 30004 | notification disabled | 通知已禁用 |

### 5.6 OTA相关错误码 (40000-49999)

| 错误码 | 错误信息 | 说明 |
|--------|----------|------|
| 40001 | no update available | 无可用更新 |
| 40002 | download failed | 下载失败 |
| 40003 | verification failed | 校验失败 |
| 40004 | installation failed | 安装失败 |
| 40005 | battery too low | 电量过低 |
| 40006 | insufficient storage | 存储空间不足 |
| 40007 | update in progress | 正在升级中 |
| 40008 | version not found | 版本不存在 |
| 40009 | package corrupted | 升级包损坏 |

### 5.7 诊断相关错误码 (50000-59999)

| 错误码 | 错误信息 | 说明 |
|--------|----------|------|
| 50001 | dtc read failed | 故障码读取失败 |
| 50002 | log upload failed | 日志上传失败 |
| 50003 | diagnostic session failed | 诊断会话失败 |
| 50004 | ecu not responding | ECU无响应 |

### 5.8 系统错误码 (90000-99999)

| 错误码 | 错误信息 | 说明 |
|--------|----------|------|
| 90001 | database error | 数据库错误 |
| 90002 | cache error | 缓存错误 |
| 90003 | network error | 网络错误 |
| 90004 | timeout | 超时 |
| 99999 | unknown error | 未知错误 |

---

## 6. 事件定义

### 6.1 事件类型

| 事件类别 | 说明 | 触发场景 |
|----------|------|----------|
| VEHICLE | 车辆事件 | 车辆状态变化 |
| SYSTEM | 系统事件 | 系统级操作 |
| USER | 用户事件 | 用户操作 |
| ALERT | 告警事件 | 异常/告警 |
| OTA | 升级事件 | OTA相关 |

### 6.2 车辆事件 (VEHICLE)

| 事件码 | 事件名称 | 说明 | 数据字段 |
|--------|----------|------|----------|
| V001 | GEAR_CHANGED | 挡位变化 | oldGear, newGear |
| V002 | SPEED_LIMIT_EXCEEDED | 超速 | speed, limit |
| V003 | PARKING_BRAKE_CHANGED | 驻车制动变化 | isOn |
| V004 | ENGINE_STARTED | 发动机启动 | - |
| V005 | ENGINE_STOPPED | 发动机关闭 | - |
| V006 | DOOR_OPENED | 车门开启 | doorPosition |
| V007 | DOOR_CLOSED | 车门关闭 | doorPosition |
| V008 | LOW_FUEL | 油量低 | level |
| V009 | LOW_BATTERY | 电量低 | level |
| V010 | TIRE_PRESSURE_ALERT | 胎压告警 | position, pressure |
| V011 | COLLISION_DETECTED | 碰撞检测 | severity |
| V012 | TOWING_ALERT | 拖吊告警 | - |

### 6.3 系统事件 (SYSTEM)

| 事件码 | 事件名称 | 说明 | 数据字段 |
|--------|----------|------|----------|
| S001 | SYSTEM_STARTUP | 系统启动 | version |
| S002 | SYSTEM_SHUTDOWN | 系统关闭 | reason |
| S003 | APP_INSTALLED | 应用安装 | packageName, version |
| S004 | APP_UNINSTALLED | 应用卸载 | packageName |
| S005 | APP_UPDATED | 应用更新 | packageName, oldVer, newVer |
| S006 | APP_CRASHED | 应用崩溃 | packageName, stackTrace |
| S007 | MEMORY_WARNING | 内存警告 | usagePercent |
| S008 | STORAGE_WARNING | 存储警告 | usagePercent |
| S009 | NETWORK_CHANGED | 网络变化 | type, connected |
| S010 | BLUETOOTH_CONNECTED | 蓝牙连接 | deviceName |
| S011 | BLUETOOTH_DISCONNECTED | 蓝牙断开 | - |

### 6.4 用户事件 (USER)

| 事件码 | 事件名称 | 说明 | 数据字段 |
|--------|----------|------|----------|
| U001 | USER_LOGIN | 用户登录 | userId |
| U002 | USER_LOGOUT | 用户登出 | userId |
| U003 | NAVIGATION_STARTED | 导航开始 | destination |
| U004 | NAVIGATION_ENDED | 导航结束 | destination |
| U005 | MEDIA_PLAYBACK_STARTED | 媒体播放 | mediaType, title |
| U006 | MEDIA_PLAYBACK_PAUSED | 媒体暂停 | - |
| U007 | CALL_STARTED | 通话开始 | number, type |
| U008 | CALL_ENDED | 通话结束 | duration |
| U009 | VOICE_COMMAND | 语音命令 | command, intent |
| U010 | SETTINGS_CHANGED | 设置变更 | key, oldValue, newValue |

### 6.5 告警事件 (ALERT)

| 事件码 | 事件名称 | 级别 | 说明 | 数据字段 |
|--------|----------|------|------|----------|
| A001 | SECURITY_ALERT | HIGH | 安全告警 | type, description |
| A002 | FAULT_DETECTED | HIGH | 故障检测 | dtcCode, ecu |
| A003 | UNAUTHORIZED_ACCESS | CRITICAL | 未授权访问 | source |
| A004 | ABNORMAL_BEHAVIOR | MEDIUM | 异常行为 | type |
| A005 | COMMUNICATION_LOST | HIGH | 通信中断 | system |

### 6.6 OTA事件 (OTA)

| 事件码 | 事件名称 | 说明 | 数据字段 |
|--------|----------|------|----------|
| O001 | UPDATE_AVAILABLE | 更新可用 | version, size |
| O002 | DOWNLOAD_STARTED | 下载开始 | version |
| O003 | DOWNLOAD_PROGRESS | 下载进度 | progress |
| O004 | DOWNLOAD_COMPLETED | 下载完成 | version |
| O005 | DOWNLOAD_FAILED | 下载失败 | errorCode |
| O006 | INSTALL_STARTED | 安装开始 | version |
| O007 | INSTALL_PROGRESS | 安装进度 | progress |
| O008 | INSTALL_COMPLETED | 安装完成 | version |
| O009 | INSTALL_FAILED | 安装失败 | errorCode |
| O010 | ROLLBACK_TRIGGERED | 回滚触发 | reason |

### 6.7 事件上报格式

```json
{
  "eventId": "EVT123456789",
  "eventCode": "V001",
  "eventName": "GEAR_CHANGED",
  "category": "VEHICLE",
  "level": "INFO",
  "vin": "LSVNV2182E2100001",
  "timestamp": 1718003400000,
  "data": {
    "oldGear": "P",
    "newGear": "D"
  },
  "location": {
    "longitude": 121.4737,
    "latitude": 31.2304
  }
}
```

### 6.8 事件级别定义

| 级别 | 说明 | 处理方式 |
|------|------|----------|
| DEBUG | 调试信息 | 本地记录 |
| INFO | 普通信息 | 正常上报 |
| WARNING | 警告 | 上报+本地提示 |
| ERROR | 错误 | 上报+告警通知 |
| CRITICAL | 严重 | 立即上报+紧急通知 |

---

## 附录 A：完整错误码表

```json
{
  "errorCodes": {
    "common": {
      "200": "success",
      "400": "bad request",
      "401": "unauthorized",
      "403": "forbidden",
      "404": "not found",
      "429": "too many requests",
      "500": "internal server error",
      "503": "service unavailable"
    },
    "auth": {
      "10001": "invalid client credentials",
      "10002": "token expired",
      "10003": "invalid token",
      "10004": "insufficient scope",
      "10005": "token refresh failed",
      "10006": "account disabled",
      "10007": "device not authorized"
    },
    "vehicle": {
      "20001": "invalid vin",
      "20002": "vehicle not found",
      "20003": "vehicle offline",
      "20004": "command not supported",
      "20005": "command execution failed",
      "20006": "command timeout",
      "20007": "vehicle busy",
      "20008": "invalid parameter value",
      "20009": "safety check failed"
    },
    "message": {
      "30001": "message not found",
      "30002": "invalid message category",
      "30003": "message queue full",
      "30004": "notification disabled"
    },
    "ota": {
      "40001": "no update available",
      "40002": "download failed",
      "40003": "verification failed",
      "40004": "installation failed",
      "40005": "battery too low",
      "40006": "insufficient storage",
      "40007": "update in progress",
      "40008": "version not found",
      "40009": "package corrupted"
    },
    "diagnostic": {
      "50001": "dtc read failed",
      "50002": "log upload failed",
      "50003": "diagnostic session failed",
      "50004": "ecu not responding"
    },
    "system": {
      "90001": "database error",
      "90002": "cache error",
      "90003": "network error",
      "90004": "timeout",
      "99999": "unknown error"
    }
  }
}
```

---

## 附录 B：Postman测试集合

### B.1 环境变量
| 变量名 | 说明 | 示例值 |
|--------|------|--------|
| baseUrl | API基础地址 | https://api.longcheer.com |
| accessToken | 访问令牌 | eyJhbGciOiJSUzI1NiIs... |
| vin | 测试车辆VIN | LSVNV2182E2100001 |

### B.2 测试脚本示例

```javascript
// 前置脚本：生成请求ID
pm.environment.set("requestId", uuid.v4());
pm.environment.set("timestamp", Date.now().toString());

// 后置脚本：验证响应
pm.test("Status code is 200", function () {
    pm.response.to.have.status(200);
});

pm.test("Response has correct structure", function () {
    var jsonData = pm.response.json();
    pm.expect(jsonData).to.have.property("code");
    pm.expect(jsonData).to.have.property("message");
    pm.expect(jsonData).to.have.property("data");
});
```

---

**文档结束**

*本文档定义了智能座舱主交互系统的REST API接口规范，所有接口调用必须遵循本文档定义。*

**编制**: 上海龙旗智能科技有限公司  
**审核**: [待填写]  
**批准**: [待填写]  
**日期**: 2024-06-10
