package com.longcheer.cockpit.common.presentation.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Android Automotive OS (AAOS) 字体规范
 * 
 * 设计原则：
 * - 驾驶场景可读性优先
 * - 最小字号 24sp（静态内容）
 * - 驾驶中操作最小 32sp
 * - 字重清晰，避免过细字体
 * - 行高充足，确保远距离阅读
 */

/**
 * 车载专用字体族
 * 建议使用系统无衬线字体或专为屏幕阅读优化的字体
 */
val CarFontFamily = FontFamily.Default

/**
 * AAOS 字体规范
 * 
 * 层级定义：
 * - Display: 仪表盘大数字、速度显示（最大）
 * - Headline: 页面标题、重要信息
 * - Title: 卡片标题、区块标题
 * - Body: 正文内容、描述
 * - Label: 按钮文字、标签、小提示
 */
val CarTypography = Typography(
    
    // ============================================
    // Display - 最大字号，用于关键信息展示
    // ============================================
    
    displayLarge = TextStyle(
        fontFamily = CarFontFamily,
        fontWeight = FontWeight.Light,      // 轻量，大数字不压迫
        fontSize = 96.sp,                   // 超大显示（如车速）
        lineHeight = 112.sp,
        letterSpacing = (-1.5).sp
    ),
    displayMedium = TextStyle(
        fontFamily = CarFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 72.sp,                   // 大显示（如转速）
        lineHeight = 88.sp,
        letterSpacing = (-0.5).sp
    ),
    displaySmall = TextStyle(
        fontFamily = CarFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 56.sp,                   // 中等显示
        lineHeight = 68.sp,
        letterSpacing = 0.sp
    ),
    
    // ============================================
    // Headline - 页面标题
    // ============================================
    
    headlineLarge = TextStyle(
        fontFamily = CarFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 40.sp,                   // 大标题
        lineHeight = 52.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = CarFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 36.sp,                   // 中标题
        lineHeight = 48.sp,
        letterSpacing = 0.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = CarFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 32.sp,                   // 小标题（驾驶中可操作最小字号）
        lineHeight = 44.sp,
        letterSpacing = 0.sp
    ),
    
    // ============================================
    // Title - 卡片、区块标题
    // ============================================
    
    titleLarge = TextStyle(
        fontFamily = CarFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 28.sp,                   // 卡片标题
        lineHeight = 38.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = CarFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 24.sp,                   // 列表项标题（最小静态字号）
        lineHeight = 34.sp,
        letterSpacing = 0.sp
    ),
    titleSmall = TextStyle(
        fontFamily = CarFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 22.sp,                   // 小标题（谨慎使用，仅静态）
        lineHeight = 30.sp,
        letterSpacing = 0.sp
    ),
    
    // ============================================
    // Body - 正文内容
    // ============================================
    
    bodyLarge = TextStyle(
        fontFamily = CarFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 24.sp,                   // 大正文（推荐）
        lineHeight = 36.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = CarFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp,                   // 中正文（谨慎使用）
        lineHeight = 32.sp,
        letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontFamily = CarFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 20.sp,                   // 小正文（仅非驾驶场景）
        lineHeight = 28.sp,
        letterSpacing = 0.4.sp
    ),
    
    // ============================================
    // Label - 按钮、标签、小提示
    // ============================================
    
    labelLarge = TextStyle(
        fontFamily = CarFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 24.sp,                   // 大按钮文字（推荐）
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    ),
    labelMedium = TextStyle(
        fontFamily = CarFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 20.sp,                   // 中按钮文字
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    labelSmall = TextStyle(
        fontFamily = CarFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,                   // 小标签（谨慎使用）
        lineHeight = 24.sp,
        letterSpacing = 0.sp
    )
)

// ============================================
// 驾驶场景专用字体扩展
// ============================================

/**
 * 驾驶中可操作组件的字体样式
 * 确保在颠簸环境下仍可清晰阅读
 */
object DrivingTypography {
    
    /**
     * 驾驶中按钮文字
     * 最小 32sp，确保快速扫视可识别
     */
    val ButtonLarge = TextStyle(
        fontFamily = CarFontFamily,
        fontWeight = FontWeight.Bold,       // 加粗提高可读性
        fontSize = 32.sp,
        lineHeight = 42.sp,
        letterSpacing = 0.sp
    )
    
    val ButtonMedium = TextStyle(
        fontFamily = CarFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 38.sp,
        letterSpacing = 0.sp
    )
    
    /**
     * 驾驶中列表项文字
     */
    val ListItemTitle = TextStyle(
        fontFamily = CarFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp
    )
    
    val ListItemSubtitle = TextStyle(
        fontFamily = CarFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 24.sp,
        lineHeight = 34.sp,
        letterSpacing = 0.25.sp
    )
    
    /**
     * 关键驾驶信息（如速度、限速）
     */
    val CriticalInfo = TextStyle(
        fontFamily = CarFontFamily,
        fontWeight = FontWeight.Light,
        fontSize = 80.sp,
        lineHeight = 96.sp,
        letterSpacing = (-1).sp
    )
    
    /**
     * 警告/提示信息
     */
    val WarningText = TextStyle(
        fontFamily = CarFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 48.sp,
        letterSpacing = 0.sp
    )
    
    /**
     * 状态标签
     */
    val StatusLabel = TextStyle(
        fontFamily = CarFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.5.sp
    )
}

// ============================================
// 字体使用场景指南（代码注释形式）
// ============================================

/*
 * 使用场景指南：
 * 
 * 【静态内容】（车辆静止时显示）
 * - 正文描述: bodyLarge (24sp)
 * - 详细设置: bodyMedium (22sp)
 * - 次要说明: bodySmall (20sp) - 谨慎使用
 * 
 * 【驾驶中可操作】（行驶中可交互）
 * - 主要按钮: DrivingTypography.ButtonLarge (32sp)
 * - 次要按钮: DrivingTypography.ButtonMedium (28sp)
 * - 列表标题: DrivingTypography.ListItemTitle (28sp)
 * - 列表描述: DrivingTypography.ListItemSubtitle (24sp)
 * 
 * 【关键信息】（需要立即注意）
 * - 车速显示: displayLarge (96sp) 或 DrivingTypography.CriticalInfo (80sp)
 * - 警告信息: DrivingTypography.WarningText (36sp)
 * - 状态指示: DrivingTypography.StatusLabel (24sp)
 * 
 * 【导航/标题】
 * - 页面标题: headlineLarge (40sp)
 * - 卡片标题: titleLarge (28sp)
 * - 区块标签: labelLarge (24sp)
 */
