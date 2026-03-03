-- =============================================================================
-- 智能座舱主交互系统数据库初始化脚本
-- Intelligent Cockpit Main Interaction System Database Initialization Script
-- =============================================================================
-- 数据库: SQLite 3.36+
-- 版本: V1.0
-- 日期: 2024-06-15
-- 编制: 上海龙旗智能科技有限公司
-- =============================================================================

-- =============================================================================
-- 数据库配置
-- =============================================================================
-- 启用外键约束
PRAGMA foreign_keys = ON;

-- 启用WAL模式，提升并发性能
PRAGMA journal_mode = WAL;

-- 设置同步模式为NORMAL，平衡安全与性能
PRAGMA synchronous = NORMAL;

-- 设置缓存大小为2000页(约8MB)
PRAGMA cache_size = -2000;

-- 设置临时存储为内存
PRAGMA temp_store = MEMORY;

-- 设置页面大小为4096字节
PRAGMA page_size = 4096;

-- 设置编码为UTF-8
PRAGMA encoding = 'UTF-8';

-- 设置时区为本地时间
PRAGMA timezone = 'localtime';

-- =============================================================================
-- 1. 消息中心相关表
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1.1 message_category 消息分类表
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS message_category (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    cat_code VARCHAR(32) NOT NULL UNIQUE,
    cat_name VARCHAR(64) NOT NULL,
    cat_name_en VARCHAR(64),
    parent_id INTEGER NOT NULL DEFAULT 0,
    icon VARCHAR(256),
    sort_order INTEGER NOT NULL DEFAULT 0,
    is_system INTEGER NOT NULL DEFAULT 0 CHECK (is_system IN (0, 1)),
    is_visible INTEGER NOT NULL DEFAULT 1 CHECK (is_visible IN (0, 1)),
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 插入默认消息分类数据
INSERT OR IGNORE INTO message_category (cat_code, cat_name, cat_name_en, parent_id, icon, sort_order, is_system) VALUES
('SYSTEM', '系统消息', 'System', 0, 'ic_cat_system', 1, 1),
('NAVIGATION', '导航消息', 'Navigation', 0, 'ic_cat_nav', 2, 1),
('VEHICLE', '车辆消息', 'Vehicle', 0, 'ic_cat_car', 3, 1),
('PHONE', '通讯消息', 'Phone', 0, 'ic_cat_phone', 4, 1),
('MUSIC', '音乐消息', 'Music', 0, 'ic_cat_music', 5, 1),
('APP', '应用消息', 'Application', 0, 'ic_cat_app', 6, 1),
('SOCIAL', '社交消息', 'Social', 0, 'ic_cat_social', 7, 0),
('PROMOTION', '活动资讯', 'Promotion', 0, 'ic_cat_promo', 8, 0),
('RECOMMEND', '智能推荐', 'Recommendation', 0, 'ic_cat_recommend', 9, 0);

-- -----------------------------------------------------------------------------
-- 1.2 message 消息表
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS message (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    msg_id VARCHAR(64) NOT NULL UNIQUE,
    source_app VARCHAR(64) NOT NULL,
    source_name VARCHAR(128),
    category_id INTEGER,
    priority INTEGER NOT NULL DEFAULT 2 CHECK (priority IN (0, 1, 2, 3)),
    title VARCHAR(256) NOT NULL,
    content TEXT,
    content_type INTEGER NOT NULL DEFAULT 0 CHECK (content_type IN (0, 1, 2, 3)),
    action_type INTEGER NOT NULL DEFAULT 0 CHECK (action_type IN (0, 1, 2, 3)),
    action_data VARCHAR(512),
    icon_url VARCHAR(512),
    user_id INTEGER NOT NULL DEFAULT 0,
    is_read INTEGER NOT NULL DEFAULT 0 CHECK (is_read IN (0, 1)),
    is_deleted INTEGER NOT NULL DEFAULT 0 CHECK (is_deleted IN (0, 1)),
    read_time DATETIME,
    expire_time DATETIME,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (category_id) REFERENCES message_category(id) ON DELETE SET NULL
);

-- 创建消息表触发器：自动更新update_time
CREATE TRIGGER IF NOT EXISTS trg_message_update_time
AFTER UPDATE ON message
FOR EACH ROW
BEGIN
    UPDATE message SET update_time = CURRENT_TIMESTAMP WHERE id = NEW.id;
END;

-- -----------------------------------------------------------------------------
-- 1.3 message_attachment 消息附件表
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS message_attachment (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    msg_id INTEGER NOT NULL,
    file_name VARCHAR(256) NOT NULL,
    file_path VARCHAR(512) NOT NULL,
    file_type INTEGER NOT NULL DEFAULT 0 CHECK (file_type IN (0, 1, 2, 3)),
    file_size INTEGER NOT NULL DEFAULT 0,
    mime_type VARCHAR(64),
    thumbnail VARCHAR(512),
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (msg_id) REFERENCES message(id) ON DELETE CASCADE
);

-- =============================================================================
-- 2. 应用管理相关表
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 2.1 app_category 应用分类表
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS app_category (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    cat_code VARCHAR(32) NOT NULL UNIQUE,
    cat_name VARCHAR(64) NOT NULL,
    cat_name_en VARCHAR(64),
    icon VARCHAR(256),
    parent_id INTEGER NOT NULL DEFAULT 0,
    sort_order INTEGER NOT NULL DEFAULT 0,
    is_visible INTEGER NOT NULL DEFAULT 1 CHECK (is_visible IN (0, 1)),
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 插入默认应用分类数据
INSERT OR IGNORE INTO app_category (cat_code, cat_name, cat_name_en, icon, sort_order) VALUES
('NAV', '导航出行', 'Navigation', 'ic_cat_nav', 1),
('MUSIC', '音乐娱乐', 'Music', 'ic_cat_music', 2),
('PHONE', '通讯联系', 'Phone', 'ic_cat_phone', 3),
('CAR', '车辆控制', 'Vehicle', 'ic_cat_car', 4),
('VIDEO', '视频媒体', 'Video', 'ic_cat_video', 5),
('LIFE', '生活服务', 'Lifestyle', 'ic_cat_life', 6),
('GAME', '游戏娱乐', 'Games', 'ic_cat_game', 7),
('TOOL', '实用工具', 'Tools', 'ic_cat_tool', 8);

-- -----------------------------------------------------------------------------
-- 2.2 application 应用信息表
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS application (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    app_id VARCHAR(64) NOT NULL UNIQUE,
    app_name VARCHAR(128) NOT NULL,
    app_name_en VARCHAR(128),
    category_id INTEGER,
    version_code INTEGER NOT NULL DEFAULT 1,
    version_name VARCHAR(32) NOT NULL DEFAULT '1.0.0',
    icon_path VARCHAR(512),
    apk_path VARCHAR(512),
    package_size INTEGER NOT NULL DEFAULT 0,
    install_time DATETIME,
    last_update DATETIME,
    developer VARCHAR(128),
    description TEXT,
    permissions TEXT,
    min_sdk INTEGER NOT NULL DEFAULT 29,
    target_sdk INTEGER NOT NULL DEFAULT 31,
    signature VARCHAR(512),
    source_type INTEGER NOT NULL DEFAULT 0 CHECK (source_type IN (0, 1, 2, 3)),
    is_system INTEGER NOT NULL DEFAULT 0 CHECK (is_system IN (0, 1)),
    is_enabled INTEGER NOT NULL DEFAULT 1 CHECK (is_enabled IN (0, 1)),
    is_in_whitelist INTEGER NOT NULL DEFAULT 0 CHECK (is_in_whitelist IN (0, 1)),
    sort_order INTEGER NOT NULL DEFAULT 0,
    launch_count INTEGER NOT NULL DEFAULT 0,
    last_launch DATETIME,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (category_id) REFERENCES app_category(id) ON DELETE SET NULL
);

-- 创建应用表触发器：自动更新update_time
CREATE TRIGGER IF NOT EXISTS trg_application_update_time
AFTER UPDATE ON application
FOR EACH ROW
BEGIN
    UPDATE application SET update_time = CURRENT_TIMESTAMP WHERE id = NEW.id;
END;

-- 插入预装应用示例数据
INSERT OR IGNORE INTO application (app_id, app_name, app_name_en, category_id, version_code, version_name, is_system, is_in_whitelist, sort_order, developer) VALUES
('com.autonavi.amapauto', '高德地图', 'AMap', 1, 800, '8.0.0', 1, 1, 1, '高德软件有限公司'),
('com.tencent.qqmusiccar', 'QQ音乐', 'QQ Music', 2, 1000, '10.0.0', 1, 1, 2, '腾讯科技'),
('com.android.dialer', '电话', 'Phone', 3, 100, '1.0.0', 1, 1, 3, 'Google'),
('com.android.settings', '设置', 'Settings', 8, 100, '1.0.0', 1, 1, 4, 'Google'),
('com.longcheer.cockpit.ai', '语音助手', 'Voice Assistant', 4, 100, '1.0.0', 1, 1, 5, '龙旗智能');

-- -----------------------------------------------------------------------------
-- 2.3 app_whitelist 应用白名单表
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS app_whitelist (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    app_id VARCHAR(64) NOT NULL UNIQUE,
    whitelist_type INTEGER NOT NULL DEFAULT 0 CHECK (whitelist_type IN (0, 1, 2)),
    speed_limit INTEGER NOT NULL DEFAULT 0,
    gear_restrict VARCHAR(16),
    allowed_actions TEXT,
    description VARCHAR(256),
    is_active INTEGER NOT NULL DEFAULT 1 CHECK (is_active IN (0, 1)),
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 创建白名单表触发器
CREATE TRIGGER IF NOT EXISTS trg_app_whitelist_update_time
AFTER UPDATE ON app_whitelist
FOR EACH ROW
BEGIN
    UPDATE app_whitelist SET update_time = CURRENT_TIMESTAMP WHERE id = NEW.id;
END;

-- 插入默认白名单配置
INSERT OR IGNORE INTO app_whitelist (app_id, whitelist_type, speed_limit, gear_restrict, allowed_actions, description) VALUES
('com.autonavi.amapauto', 2, 0, NULL, '["navigate","search","voice"]','导航应用，行驶中可用'),
('com.tencent.qqmusiccar', 0, 0, NULL, '["play","pause","next","prev"]','音乐应用，行驶中可用'),
('com.android.dialer', 0, 0, NULL, '["call","answer","hangup"]','电话应用，行驶中可用'),
('com.longcheer.cockpit.ai', 2, 0, NULL, '["all"]','语音助手，全部权限');

-- -----------------------------------------------------------------------------
-- 2.4 app_status 应用状态表
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS app_status (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    app_id VARCHAR(64) NOT NULL UNIQUE,
    process_name VARCHAR(128),
    pid INTEGER,
    status INTEGER NOT NULL DEFAULT 0 CHECK (status IN (0, 1, 2, 3)),
    memory_usage INTEGER NOT NULL DEFAULT 0,
    cpu_usage REAL NOT NULL DEFAULT 0.0,
    start_time DATETIME,
    is_restricted INTEGER NOT NULL DEFAULT 0 CHECK (is_restricted IN (0, 1)),
    restrict_reason VARCHAR(256),
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 创建应用状态表触发器
CREATE TRIGGER IF NOT EXISTS trg_app_status_update_time
AFTER UPDATE ON app_status
FOR EACH ROW
BEGIN
    UPDATE app_status SET update_time = CURRENT_TIMESTAMP WHERE id = NEW.id;
END;

-- =============================================================================
-- 3. 用户设置相关表
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 3.1 sys_user 用户表
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sys_user (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id VARCHAR(64) NOT NULL UNIQUE,
    username VARCHAR(64) NOT NULL,
    nickname VARCHAR(64),
    phone VARCHAR(20),
    email VARCHAR(128),
    avatar VARCHAR(512),
    gender INTEGER CHECK (gender IN (0, 1, 2)),
    birthday DATE,
    user_type INTEGER NOT NULL DEFAULT 0 CHECK (user_type IN (0, 1, 2)),
    voice_model VARCHAR(512),
    is_active INTEGER NOT NULL DEFAULT 1 CHECK (is_active IN (0, 1)),
    last_login DATETIME,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 创建用户表触发器
CREATE TRIGGER IF NOT EXISTS trg_sys_user_update_time
AFTER UPDATE ON sys_user
FOR EACH ROW
BEGIN
    UPDATE sys_user SET update_time = CURRENT_TIMESTAMP WHERE id = NEW.id;
END;

-- 插入默认系统用户
INSERT OR IGNORE INTO sys_user (user_id, username, nickname, user_type) VALUES
('0', 'system', '系统', 0),
('1', 'default_user', '默认用户', 0);

-- -----------------------------------------------------------------------------
-- 3.2 setting_category 设置分类表
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS setting_category (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    cat_code VARCHAR(32) NOT NULL UNIQUE,
    cat_name VARCHAR(64) NOT NULL,
    cat_name_en VARCHAR(64),
    icon VARCHAR(256),
    parent_id INTEGER NOT NULL DEFAULT 0,
    sort_order INTEGER NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 插入设置分类数据
INSERT OR IGNORE INTO setting_category (cat_code, cat_name, cat_name_en, icon, sort_order) VALUES
('display', '显示设置', 'Display', 'ic_setting_display', 1),
('sound', '声音设置', 'Sound', 'ic_setting_sound', 2),
('nav', '导航设置', 'Navigation', 'ic_setting_nav', 3),
('msg', '消息设置', 'Message', 'ic_setting_msg', 4),
('drive', '驾驶设置', 'Driving', 'ic_setting_drive', 5),
('ai', 'AI助手设置', 'AI Assistant', 'ic_setting_ai', 6),
('system', '系统设置', 'System', 'ic_setting_system', 7);

-- -----------------------------------------------------------------------------
-- 3.3 user_settings 用户设置表
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS user_settings (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL,
    setting_key VARCHAR(64) NOT NULL,
    setting_value TEXT,
    setting_type INTEGER NOT NULL DEFAULT 0 CHECK (setting_type IN (0, 1, 2, 3, 4)),
    category VARCHAR(32) NOT NULL DEFAULT 'general',
    description VARCHAR(256),
    is_synced INTEGER NOT NULL DEFAULT 0 CHECK (is_synced IN (0, 1)),
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, setting_key),
    FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE
);

-- 创建用户设置表触发器
CREATE TRIGGER IF NOT EXISTS trg_user_settings_update_time
AFTER UPDATE ON user_settings
FOR EACH ROW
BEGIN
    UPDATE user_settings SET update_time = CURRENT_TIMESTAMP WHERE id = NEW.id;
END;

-- 插入默认用户设置
INSERT OR IGNORE INTO user_settings (user_id, setting_key, setting_value, setting_type, category, description) VALUES
(1, 'display.brightness', '80', 1, 'display', '显示亮度'),
(1, 'display.auto_brightness', 'true', 3, 'display', '自动亮度调节'),
(1, 'display.theme', 'dark', 0, 'display', '主题'),
(1, 'sound.media_volume', '50', 1, 'sound', '媒体音量'),
(1, 'sound.nav_volume', '70', 1, 'sound', '导航音量'),
(1, 'sound.call_volume', '80', 1, 'sound', '通话音量'),
(1, 'nav.avoid_toll', 'false', 3, 'nav', '避开收费路段'),
(1, 'nav.show_traffic', 'true', 3, 'nav', '显示实时路况'),
(1, 'msg.filter_level', '2', 1, 'msg', '消息过滤级别'),
(1, 'drive.auto_restrict', 'true', 3, 'drive', '自动启用行驶限制'),
(1, 'ai.wake_word', '你好小奇', 0, 'ai', '唤醒词'),
(1, 'system.language', 'zh-CN', 0, 'system', '系统语言');

-- =============================================================================
-- 4. 导航相关表
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 4.1 nav_history 导航历史表
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS nav_history (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL,
    start_name VARCHAR(128) NOT NULL,
    start_lat REAL NOT NULL,
    start_lng REAL NOT NULL,
    start_poi VARCHAR(64),
    end_name VARCHAR(128) NOT NULL,
    end_lat REAL NOT NULL,
    end_lng REAL NOT NULL,
    end_poi VARCHAR(64),
    end_address VARCHAR(512),
    distance INTEGER NOT NULL DEFAULT 0,
    duration INTEGER NOT NULL DEFAULT 0,
    route_type INTEGER NOT NULL DEFAULT 0 CHECK (route_type IN (0, 1, 2, 3, 4)),
    nav_strategy INTEGER NOT NULL DEFAULT 0,
    start_time DATETIME NOT NULL,
    end_time DATETIME,
    is_completed INTEGER NOT NULL DEFAULT 0 CHECK (is_completed IN (0, 1)),
    is_favorite INTEGER NOT NULL DEFAULT 0 CHECK (is_favorite IN (0, 1)),
    is_deleted INTEGER NOT NULL DEFAULT 0 CHECK (is_deleted IN (0, 1)),
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE
);

-- -----------------------------------------------------------------------------
-- 4.2 nav_favorite 收藏地点表
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS nav_favorite (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL,
    fav_type INTEGER NOT NULL DEFAULT 3 CHECK (fav_type IN (0, 1, 2, 3)),
    name VARCHAR(128) NOT NULL,
    address VARCHAR(512),
    lat REAL NOT NULL,
    lng REAL NOT NULL,
    poi_id VARCHAR(64),
    phone VARCHAR(32),
    category VARCHAR(64),
    icon VARCHAR(256),
    sort_order INTEGER NOT NULL DEFAULT 0,
    is_synced INTEGER NOT NULL DEFAULT 0 CHECK (is_synced IN (0, 1)),
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, fav_type, name),
    FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE
);

-- 创建收藏地点表触发器
CREATE TRIGGER IF NOT EXISTS trg_nav_favorite_update_time
AFTER UPDATE ON nav_favorite
FOR EACH ROW
BEGIN
    UPDATE nav_favorite SET update_time = CURRENT_TIMESTAMP WHERE id = NEW.id;
END;

-- =============================================================================
-- 5. 系统配置相关表
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 5.1 system_config 系统配置表
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS system_config (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    config_key VARCHAR(128) NOT NULL UNIQUE,
    config_value TEXT,
    config_type INTEGER NOT NULL DEFAULT 0 CHECK (config_type IN (0, 1, 2, 3, 4)),
    category VARCHAR(32) NOT NULL DEFAULT 'system',
    description VARCHAR(512),
    is_editable INTEGER NOT NULL DEFAULT 1 CHECK (is_editable IN (0, 1)),
    is_encrypted INTEGER NOT NULL DEFAULT 0 CHECK (is_encrypted IN (0, 1)),
    default_value TEXT,
    min_value VARCHAR(64),
    max_value VARCHAR(64),
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 创建系统配置表触发器
CREATE TRIGGER IF NOT EXISTS trg_system_config_update_time
AFTER UPDATE ON system_config
FOR EACH ROW
BEGIN
    UPDATE system_config SET update_time = CURRENT_TIMESTAMP WHERE id = NEW.id;
END;

-- 插入默认系统配置
INSERT OR IGNORE INTO system_config (config_key, config_value, config_type, category, description, default_value, is_editable) VALUES
('system.version', '1.0.0', 0, 'system', '系统版本号', '1.0.0', 0),
('system.build_number', '20240601', 0, 'system', '构建编号', '20240601', 0),
('system.max_msg_history', '1000', 1, 'system', '最大消息历史记录数', '1000', 1),
('system.msg_expire_days', '30', 1, 'system', '消息过期天数', '30', 1),
('system.max_nav_history', '200', 1, 'system', '最大导航历史记录数', '200', 1),
('system.auto_clean_days', '7', 1, 'system', '自动清理天数', '7', 1),
('drive.speed_limit_d', '0', 1, 'drive', 'D挡速度限制(km/h,0=无限制)', '0', 1),
('drive.speed_limit_r', '15', 1, 'drive', 'R挡速度限制(km/h)', '15', 1),
('drive.restrict_apps', '["video","game"]', 4, 'drive', '行驶限制应用类型', '["video","game"]', 1),
('ota.check_interval', '86400', 1, 'ota', 'OTA检查间隔(秒)', '86400', 1),
('ai.asr_timeout', '5000', 1, 'ai', '语音识别超时(ms)', '5000', 1),
('ai.nlp_timeout', '2000', 1, 'ai', '语义理解超时(ms)', '2000', 1);

-- -----------------------------------------------------------------------------
-- 5.2 sys_feature_switch 功能开关表
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sys_feature_switch (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    feature_code VARCHAR(64) NOT NULL UNIQUE,
    feature_name VARCHAR(128) NOT NULL,
    is_enabled INTEGER NOT NULL DEFAULT 1 CHECK (is_enabled IN (0, 1)),
    is_runtime INTEGER NOT NULL DEFAULT 0 CHECK (is_runtime IN (0, 1)),
    depends_on TEXT,
    description VARCHAR(512),
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 创建功能开关表触发器
CREATE TRIGGER IF NOT EXISTS trg_sys_feature_switch_update_time
AFTER UPDATE ON sys_feature_switch
FOR EACH ROW
BEGIN
    UPDATE sys_feature_switch SET update_time = CURRENT_TIMESTAMP WHERE id = NEW.id;
END;

-- 插入默认功能开关
INSERT OR IGNORE INTO sys_feature_switch (feature_code, feature_name, is_enabled, is_runtime, depends_on, description) VALUES
('feature_nav_3d', '3D地图导航', 1, 0, NULL, '启用3D地图显示'),
('feature_nav_ar', 'AR实景导航', 1, 0, '["feature_nav_3d"]', '启用AR实景导航'),
('feature_ai_voice', 'AI语音助手', 1, 1, NULL, '启用AI语音交互'),
('feature_msg_smart_filter', '智能消息过滤', 1, 1, NULL, '启用驾驶模式消息过滤'),
('feature_drive_restrict', '行驶限制', 1, 1, NULL, '启用行驶限制功能'),
('feature_ota_auto', '自动OTA检查', 1, 1, NULL, '自动检查OTA更新');

-- =============================================================================
-- 6. 日志审计相关表
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 6.1 operation_log 操作日志表
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS operation_log (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    log_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    user_id INTEGER NOT NULL DEFAULT 0,
    module VARCHAR(32) NOT NULL,
    action VARCHAR(64) NOT NULL,
    target_type VARCHAR(32),
    target_id VARCHAR(64),
    params TEXT,
    result INTEGER NOT NULL DEFAULT 0 CHECK (result IN (0, 1)),
    error_msg VARCHAR(512),
    ip_address VARCHAR(64),
    device_info VARCHAR(256),
    duration_ms INTEGER NOT NULL DEFAULT 0
);

-- -----------------------------------------------------------------------------
-- 6.2 system_event 系统事件表
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS system_event (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    event_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    event_type VARCHAR(32) NOT NULL,
    event_level INTEGER NOT NULL DEFAULT 0 CHECK (event_level IN (0, 1, 2, 3)),
    event_source VARCHAR(64) NOT NULL,
    event_code VARCHAR(32) NOT NULL,
    event_msg TEXT NOT NULL,
    stack_trace TEXT,
    extra_data TEXT,
    is_resolved INTEGER NOT NULL DEFAULT 0 CHECK (is_resolved IN (0, 1)),
    resolved_time DATETIME
);

-- =============================================================================
-- 7. 创建索引
-- =============================================================================

-- 7.1 消息表索引
CREATE INDEX IF NOT EXISTS idx_msg_msg_id ON message(msg_id);
CREATE INDEX IF NOT EXISTS idx_msg_user_query ON message(user_id, is_deleted, create_time DESC);
CREATE INDEX IF NOT EXISTS idx_msg_category ON message(category_id, is_deleted);
CREATE INDEX IF NOT EXISTS idx_msg_priority ON message(priority, is_read);
CREATE INDEX IF NOT EXISTS idx_msg_unread ON message(user_id, is_read, is_deleted) WHERE is_read = 0;
CREATE INDEX IF NOT EXISTS idx_msg_expire ON message(expire_time) WHERE expire_time IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_msg_source ON message(source_app, create_time DESC);
CREATE INDEX IF NOT EXISTS idx_msg_attachment_msg_id ON message_attachment(msg_id);

-- 7.2 应用表索引
CREATE INDEX IF NOT EXISTS idx_app_app_id ON application(app_id);
CREATE INDEX IF NOT EXISTS idx_app_category ON application(category_id, is_enabled);
CREATE INDEX IF NOT EXISTS idx_app_launch ON application(launch_count DESC);
CREATE INDEX IF NOT EXISTS idx_app_whitelist ON application(is_in_whitelist, is_enabled);
CREATE INDEX IF NOT EXISTS idx_app_update ON application(last_update DESC);
CREATE INDEX IF NOT EXISTS idx_app_whitelist_app_id ON app_whitelist(app_id);
CREATE INDEX IF NOT EXISTS idx_app_status_app_id ON app_status(app_id);

-- 7.3 用户表索引
CREATE INDEX IF NOT EXISTS idx_user_user_id ON sys_user(user_id);
CREATE INDEX IF NOT EXISTS idx_setting_user_key ON user_settings(user_id, setting_key);
CREATE INDEX IF NOT EXISTS idx_setting_category ON user_settings(user_id, category);
CREATE INDEX IF NOT EXISTS idx_setting_sync ON user_settings(is_synced) WHERE is_synced = 0;

-- 7.4 导航表索引
CREATE INDEX IF NOT EXISTS idx_nav_user ON nav_history(user_id, is_deleted, start_time DESC);
CREATE INDEX IF NOT EXISTS idx_nav_favorite ON nav_history(user_id, is_favorite) WHERE is_favorite = 1;
CREATE INDEX IF NOT EXISTS idx_nav_end_poi ON nav_history(end_poi, user_id);
CREATE INDEX IF NOT EXISTS idx_nav_time ON nav_history(start_time);
CREATE INDEX IF NOT EXISTS idx_fav_user ON nav_favorite(user_id, fav_type, sort_order);
CREATE INDEX IF NOT EXISTS idx_fav_location ON nav_favorite(lat, lng);
CREATE INDEX IF NOT EXISTS idx_fav_sync ON nav_favorite(is_synced) WHERE is_synced = 0;

-- 7.5 系统配置索引
CREATE INDEX IF NOT EXISTS idx_config_key ON system_config(config_key);
CREATE INDEX IF NOT EXISTS idx_config_category ON system_config(category);
CREATE INDEX IF NOT EXISTS idx_feature_code ON sys_feature_switch(feature_code);

-- 7.6 日志表索引
CREATE INDEX IF NOT EXISTS idx_log_time ON operation_log(log_time DESC);
CREATE INDEX IF NOT EXISTS idx_log_user ON operation_log(user_id, log_time DESC);
CREATE INDEX IF NOT EXISTS idx_log_module ON operation_log(module, action, log_time DESC);
CREATE INDEX IF NOT EXISTS idx_event_query ON system_event(event_time DESC, event_level);
CREATE INDEX IF NOT EXISTS idx_event_type ON system_event(event_type, event_time DESC);

-- =============================================================================
-- 8. 创建视图
-- =============================================================================

-- 8.1 未读消息视图
CREATE VIEW IF NOT EXISTS v_unread_messages AS
SELECT 
    m.*,
    mc.cat_name as category_name,
    mc.icon as category_icon
FROM message m
LEFT JOIN message_category mc ON m.category_id = mc.id
WHERE m.is_read = 0 AND m.is_deleted = 0 AND (m.expire_time IS NULL OR m.expire_time > datetime('now'))
ORDER BY m.priority ASC, m.create_time DESC;

-- 8.2 应用列表视图
CREATE VIEW IF NOT EXISTS v_application_list AS
SELECT 
    a.*,
    ac.cat_name as category_name,
    CASE 
        WHEN aw.app_id IS NOT NULL THEN 1 
        ELSE 0 
    END as is_whitelisted
FROM application a
LEFT JOIN app_category ac ON a.category_id = ac.id
LEFT JOIN app_whitelist aw ON a.app_id = aw.app_id AND aw.is_active = 1
WHERE a.is_enabled = 1
ORDER BY a.sort_order ASC, a.app_name ASC;

-- 8.3 用户设置完整视图
CREATE VIEW IF NOT EXISTS v_user_settings AS
SELECT 
    us.*,
    sc.cat_name as category_name,
    u.username,
    u.nickname
FROM user_settings us
LEFT JOIN setting_category sc ON us.category = sc.cat_code
LEFT JOIN sys_user u ON us.user_id = u.id;

-- 8.4 导航历史摘要视图
CREATE VIEW IF NOT EXISTS v_nav_history_summary AS
SELECT 
    nh.*,
    u.username,
    CASE nh.route_type
        WHEN 0 THEN '推荐'
        WHEN 1 THEN '最快'
        WHEN 2 THEN '最短'
        WHEN 3 THEN '避堵'
        WHEN 4 THEN '省钱'
    END as route_type_name
FROM nav_history nh
LEFT JOIN sys_user u ON nh.user_id = u.id
WHERE nh.is_deleted = 0
ORDER BY nh.start_time DESC;

-- =============================================================================
-- 9. 创建清理过期数据的触发器
-- =============================================================================

-- 消息过期自动标记删除触发器
CREATE TRIGGER IF NOT EXISTS trg_message_expire
AFTER INSERT ON message
BEGIN
    UPDATE message SET is_deleted = 1 
    WHERE expire_time IS NOT NULL 
    AND expire_time < datetime('now') 
    AND is_deleted = 0;
END;

-- =============================================================================
-- 10. 数据库初始化完成
-- =============================================================================

-- 记录初始化版本
CREATE TABLE IF NOT EXISTS db_version (
    version INTEGER PRIMARY KEY,
    description VARCHAR(256),
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

INSERT OR REPLACE INTO db_version (version, description) VALUES 
(1, 'Initial database schema V1.0');

-- 执行VACUUM优化存储
-- VACUUM; -- 注释掉，首次初始化不建议立即执行

-- =============================================================================
-- 脚本结束
-- =============================================================================
