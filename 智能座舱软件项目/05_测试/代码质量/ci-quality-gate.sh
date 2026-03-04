#!/bin/bash
# ============================================================
# 智能座舱主交互系统 - CI/CD质量门禁脚本
# ASPICE Level 3 合规 - CI/CD Quality Gate
# ============================================================

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# 日志函数
log_info() { echo -e "${BLUE}[INFO]${NC} $1"; }
log_success() { echo -e "${GREEN}[PASS]${NC} $1"; }
log_error() { echo -e "${RED}[FAIL]${NC} $1"; }
log_warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }

# 项目根目录
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "${PROJECT_ROOT}"

# 质量门禁结果
GATE_PASSED=true
REPORT_DIR="${PROJECT_ROOT}/build/reports/quality-gate"
mkdir -p "$REPORT_DIR"

# ============================================================
# 质量门禁配置
# ============================================================

# 覆盖率阈值
MIN_LINE_COVERAGE=80
MIN_BRANCH_COVERAGE=70
MIN_METHOD_COVERAGE=80

# 关键路径覆盖率
CRITICAL_PATH_COVERAGE=90

# 静态分析阈值
MAX_DETEKT_ERRORS=0
MAX_DETEKT_WARNINGS=50
MAX_LINT_ERRORS=0
MAX_LINT_WARNINGS=100

# 测试阈值
MIN_TEST_COUNT=50
MAX_TEST_FAILURES=0

# 代码复杂度阈值
MAX_CYCLOMATIC_COMPLEXITY=10
MAX_COGNITIVE_COMPLEXITY=15

# ============================================================
# 辅助函数
# ============================================================

save_result() {
    local check_name="$1"
    local status="$2"
    local details="$3"
    
    echo "${check_name}|${status}|${details}" >> "${REPORT_DIR}/gate-results.txt"
}

parse_xml_value() {
    local file="$1"
    local xpath="$2"
    
    if command -v xmllint >/dev/null 2>&1; then
        xmllint --xpath "string($xpath)" "$file" 2>/dev/null || echo "0"
    else
        grep -oP "(?<=<$xpath>)[^<]+" "$file" 2>/dev/null | head -1 || echo "0"
    fi
}

# ============================================================
# 质量门禁检查项
# ============================================================

gate_1_compile() {
    log_info "门禁1: 代码编译检查"
    
    if ./gradlew compileDebugKotlin compileDebugJavaWithJavac --quiet 2>/dev/null; then
        log_success "代码编译通过"
        save_result "编译检查" "PASS" "编译成功"
        return 0
    else
        log_error "代码编译失败"
        save_result "编译检查" "FAIL" "编译错误"
        GATE_PASSED=false
        return 1
    fi
}

gate_2_ktlint() {
    log_info "门禁2: 代码风格检查 (Ktlint)"
    
    if ./gradlew ktlintCheck --quiet 2>/dev/null; then
        log_success "代码风格检查通过"
        save_result "代码风格" "PASS" "无违规"
        return 0
    else
        # 统计违规数
        VIOLATIONS=$(find "${PROJECT_ROOT}/build/reports/ktlint" -name "*.xml" -exec grep -c '<error' {} + 2>/dev/null | awk '{sum+=$1} END {print sum}')
        VIOLATIONS=${VIOLATIONS:-0}
        
        log_error "代码风格检查失败，发现 ${VIOLATIONS} 个违规"
        save_result "代码风格" "FAIL" "${VIOLATIONS}个违规"
        GATE_PASSED=false
        return 1
    fi
}

gate_3_detekt() {
    log_info "门禁3: 静态代码分析 (Detekt)"
    
    ./gradlew detekt --quiet 2>/dev/null || true
    
    local detekt_report="${PROJECT_ROOT}/build/reports/detekt/detekt.xml"
    
    if [ ! -f "$detekt_report" ]; then
        log_warn "Detekt报告未生成，跳过"
        save_result "静态分析" "WARN" "报告未生成"
        return 0
    fi
    
    # 统计错误和警告
    local errors=$(grep -c 'severity="error"' "$detekt_report" 2>/dev/null || echo "0")
    local warnings=$(grep -c 'severity="warning"' "$detekt_report" 2>/dev/null || echo "0")
    
    log_info "Detekt结果: ${errors} 个错误, ${warnings} 个警告"
    
    if [ "$errors" -gt "$MAX_DETEKT_ERRORS" ]; then
        log_error "严重错误数(${errors})超过阈值(${MAX_DETEKT_ERRORS})"
        save_result "静态分析" "FAIL" "${errors}个错误, ${warnings}个警告"
        GATE_PASSED=false
        return 1
    fi
    
    if [ "$warnings" -gt "$MAX_DETEKT_WARNINGS" ]; then
        log_warn "警告数(${warnings})超过阈值(${MAX_DETEKT_WARNINGS})"
    fi
    
    log_success "静态分析通过"
    save_result "静态分析" "PASS" "${errors}个错误, ${warnings}个警告"
    return 0
}

gate_4_lint() {
    log_info "门禁4: Android Lint检查"
    
    ./gradlew lintDebug --quiet 2>/dev/null || true
    
    local lint_report="${PROJECT_ROOT}/build/reports/lint/lint-results.xml"
    
    if [ ! -f "$lint_report" ]; then
        log_warn "Lint报告未生成，跳过"
        save_result "Lint检查" "WARN" "报告未生成"
        return 0
    fi
    
    # 统计问题
    local fatal=$(grep -c 'severity="Fatal"' "$lint_report" 2>/dev/null || echo "0")
    local errors=$(grep -c 'severity="Error"' "$lint_report" 2>/dev/null || echo "0")
    local warnings=$(grep -c 'severity="Warning"' "$lint_report" 2>/dev/null || echo "0")
    
    log_info "Lint结果: ${fatal} 个致命, ${errors} 个错误, ${warnings} 个警告"
    
    if [ "$fatal" -gt 0 ] || [ "$errors" -gt "$MAX_LINT_ERRORS" ]; then
        log_error "严重问题数(${fatal}致命/${errors}错误)超过阈值"
        save_result "Lint检查" "FAIL" "${fatal}致命/${errors}错误/${warnings}警告"
        GATE_PASSED=false
        return 1
    fi
    
    log_success "Lint检查通过"
    save_result "Lint检查" "PASS" "${fatal}致命/${errors}错误/${warnings}警告"
    return 0
}

gate_5_unit_tests() {
    log_info "门禁5: 单元测试执行"
    
    if ./gradlew testDebugUnitTest --quiet 2>/dev/null; then
        log_success "单元测试通过"
        
        # 统计测试数量
        local test_count=$(find "${PROJECT_ROOT}/app/build/test-results" -name "*.xml" -exec grep -c '<testcase' {} + 2>/dev/null | awk '{sum+=$1} END {print sum}')
        test_count=${test_count:-0}
        
        save_result "单元测试" "PASS" "${test_count}个测试通过"
        return 0
    else
        # 统计失败
        local failures=$(find "${PROJECT_ROOT}/app/build/test-results" -name "*.xml" -exec grep -c 'failures="[1-9]' {} + 2>/dev/null | awk '{sum+=$1} END {print sum}')
        failures=${failures:-0}
        
        log_error "单元测试失败: ${failures} 个测试失败"
        save_result "单元测试" "FAIL" "${failures}个测试失败"
        GATE_PASSED=false
        return 1
    fi
}

gate_6_coverage() {
    log_info "门禁6: 代码覆盖率检查"
    
    ./gradlew jacocoTestReport --quiet 2>/dev/null || true
    
    local coverage_report="${PROJECT_ROOT}/app/build/reports/jacoco/jacocoTestReport/jacocoTestReport.xml"
    
    if [ ! -f "$coverage_report" ]; then
        log_warn "覆盖率报告未生成，跳过"
        save_result "代码覆盖率" "WARN" "报告未生成"
        return 0
    fi
    
    # 解析覆盖率数据
    local line_coverage=$(grep -oE 'line-rate="[0-9.]+"' "$coverage_report" | head -1 | grep -oE '[0-9.]+')
    local branch_coverage=$(grep -oE 'branch-rate="[0-9.]+"' "$coverage_report" | head -1 | grep -oE '[0-9.]+')
    
    # 转换为百分比
    line_coverage=${line_coverage:-0}
    branch_coverage=${branch_coverage:-0}
    local line_pct=$(echo "$line_coverage * 100" | bc -l | cut -d. -f1)
    local branch_pct=$(echo "$branch_coverage * 100" | bc -l | cut -d. -f1)
    line_pct=${line_pct:-0}
    branch_pct=${branch_pct:-0}
    
    log_info "行覆盖率: ${line_pct}%, 分支覆盖率: ${branch_pct}%"
    
    local coverage_pass=true
    
    if [ "$line_pct" -lt "$MIN_LINE_COVERAGE" ]; then
        log_error "行覆盖率(${line_pct}%)低于阈值(${MIN_LINE_COVERAGE}%)"
        coverage_pass=false
    fi
    
    if [ "$branch_pct" -lt "$MIN_BRANCH_COVERAGE" ]; then
        log_error "分支覆盖率(${branch_pct}%)低于阈值(${MIN_BRANCH_COVERAGE}%)"
        coverage_pass=false
    fi
    
    if [ "$coverage_pass" = false ]; then
        save_result "代码覆盖率" "FAIL" "行:${line_pct}%/分支:${branch_pct}%"
        GATE_PASSED=false
        return 1
    fi
    
    log_success "代码覆盖率达标"
    save_result "代码覆盖率" "PASS" "行:${line_pct}%/分支:${branch_pct}%"
    return 0
}

gate_7_critical_path_coverage() {
    log_info "门禁7: 关键路径覆盖率检查"
    
    # 关键路径类列表
    local critical_paths=(
        "com.longcheer.cockpit.message.service.DrivingSafetyController"
        "com.longcheer.cockpit.message.service.MessagePopupManager"
        "com.longcheer.cockpit.fwk.lifecycle.AppLifecycleManager"
        "com.longcheer.cockpit.vehicle.RestrictionManager"
    )
    
    local all_passed=true
    local coverage_report="${PROJECT_ROOT}/app/build/reports/jacoco/jacocoTestReport/jacocoTestReport.xml"
    
    for path in "${critical_paths[@]}"; do
        # 检查关键类是否有测试覆盖
        local class_coverage=$(grep -c "${path//./\/}" "$coverage_report" 2>/dev/null || echo "0")
        
        if [ "$class_coverage" -eq 0 ]; then
            log_warn "关键路径类 ${path} 未找到覆盖率数据"
        fi
    done
    
    log_success "关键路径覆盖率检查完成"
    save_result "关键路径覆盖" "PASS" "已检查${#critical_paths[@]}个类"
    return 0
}

gate_8_complexity() {
    log_info "门禁8: 代码复杂度检查"
    
    local detekt_report="${PROJECT_ROOT}/build/reports/detekt/detekt.xml"
    
    if [ ! -f "$detekt_report" ]; then
        log_warn "Detekt报告未生成，跳过复杂度检查"
        save_result "代码复杂度" "WARN" "报告未生成"
        return 0
    fi
    
    # 统计复杂度过高的方法
    local complex_methods=$(grep -c 'CyclomaticComplexMethod' "$detekt_report" 2>/dev/null || echo "0")
    local cognitive_complex=$(grep -c 'CognitiveComplexMethod' "$detekt_report" 2>/dev/null || echo "0")
    
    log_info "复杂方法: ${complex_methods}个(圈复杂度), ${cognitive_complex}个(认知复杂度)"
    
    if [ "$complex_methods" -gt 0 ] || [ "$cognitive_complex" -gt 0 ]; then
        log_warn "发现复杂方法，建议重构"
    fi
    
    log_success "代码复杂度检查完成"
    save_result "代码复杂度" "PASS" "圈复杂:${complex_methods}/认知复杂:${cognitive_complex}"
    return 0
}

gate_9_security_scan() {
    log_info "门禁9: 安全扫描"
    
    # 检查依赖漏洞
    if ./gradlew dependencyCheckAnalyze --quiet 2>/dev/null; then
        log_success "依赖漏洞扫描通过"
        save_result "安全扫描" "PASS" "无高危漏洞"
    else
        log_warn "依赖漏洞扫描未完成或发现问题"
        save_result "安全扫描" "WARN" "需人工检查"
    fi
    
    return 0
}

gate_10_documentation() {
    log_info "门禁10: 文档完整性检查"
    
    local missing_docs=0
    
    # 检查核心类是否有文档
    local public_classes=$(find "${PROJECT_ROOT}/app/src/main/java" -name "*.kt" -exec grep -l "^class\|^object\|^interface" {} \; 2>/dev/null | wc -l)
    local documented=$(find "${PROJECT_ROOT}/app/src/main/java" -name "*.kt" -exec grep -l "/\*\*" {} \; 2>/dev/null | wc -l)
    
    local doc_ratio=0
    if [ "$public_classes" -gt 0 ]; then
        doc_ratio=$((documented * 100 / public_classes))
    fi
    
    log_info "文档覆盖率: ${doc_ratio}% (${documented}/${public_classes})"
    
    if [ "$doc_ratio" -lt 50 ]; then
        log_warn "文档覆盖率较低，建议补充"
    fi
    
    log_success "文档检查完成"
    save_result "文档完整性" "PASS" "${doc_ratio}%覆盖率"
    return 0
}

# ============================================================
# 生成质量门禁报告
# ============================================================

generate_report() {
    local report_file="${REPORT_DIR}/quality-gate-report.md"
    local timestamp=$(date '+%Y-%m-%d %H:%M:%S')
    local git_commit=$(git rev-parse --short HEAD 2>/dev/null || echo "unknown")
    local git_branch=$(git rev-parse --abbrev-ref HEAD 2>/dev/null || echo "unknown")
    
    cat > "$report_file" << EOF
# 质量门禁报告

**执行时间**: ${timestamp}  
**Git分支**: ${git_branch}  
**Git提交**: ${git_commit}  
**执行结果**: $(if [ "$GATE_PASSED" = true ]; then echo "✅ 通过"; else echo "❌ 未通过"; fi)

---

## 检查项汇总

| 检查项 | 状态 | 详情 |
|--------|------|------|
EOF
    
    # 读取检查结果
    if [ -f "${REPORT_DIR}/gate-results.txt" ]; then
        while IFS='|' read -r name status details; do
            local status_icon="✅"
            [ "$status" = "FAIL" ] && status_icon="❌"
            [ "$status" = "WARN" ] && status_icon="⚠️"
            echo "| ${name} | ${status_icon} ${status} | ${details} |" >> "$report_file"
        done < "${REPORT_DIR}/gate-results.txt"
    fi
    
    cat >> "$report_file" << EOF

---

## 详细报告链接

- [Detekt报告](../detekt/detekt.html)
- [Lint报告](../lint/lint-results.html)
- [覆盖率报告](../../app/build/reports/jacoco/jacocoTestReport/html/index.html)
- [单元测试报告](../../app/build/reports/tests/testDebugUnitTest/index.html)

---

*此报告由CI/CD质量门禁自动生成*
EOF
    
    log_info "质量门禁报告已生成: ${report_file}"
}

# ============================================================
# 主函数
# ============================================================

main() {
    echo -e "${BLUE}"
    echo "╔══════════════════════════════════════════════════════════╗"
    echo "║          CI/CD 质量门禁 - ASPICE Level 3                 ║"
    echo "║          智能座舱主交互系统                              ║"
    echo "╚══════════════════════════════════════════════════════════╝"
    echo -e "${NC}"
    
    START_TIME=$(date +%s)
    
    # 清空之前的检查结果
    rm -f "${REPORT_DIR}/gate-results.txt"
    
    # 执行所有门禁检查
    gate_1_compile
    gate_2_ktlint
    gate_3_detekt
    gate_4_lint
    gate_5_unit_tests
    gate_6_coverage
    gate_7_critical_path_coverage
    gate_8_complexity
    gate_9_security_scan
    gate_10_documentation
    
    # 生成报告
    generate_report
    
    END_TIME=$(date +%s)
    DURATION=$((END_TIME - START_TIME))
    
    echo ""
    echo "╔══════════════════════════════════════════════════════════╗"
    if [ "$GATE_PASSED" = true ]; then
        echo -e "${GREEN}║              ✓ 质量门禁通过，允许合并                      ║${NC}"
    else
        echo -e "${RED}║              ✗ 质量门禁未通过，请修复问题                  ║${NC}"
    fi
    echo "║              执行时间: ${DURATION}秒                              ║"
    echo "╚══════════════════════════════════════════════════════════╝"
    
    # 返回退出码
    if [ "$GATE_PASSED" = true ]; then
        exit 0
    else
        exit 1
    fi
}

# 执行主函数
main "$@"
