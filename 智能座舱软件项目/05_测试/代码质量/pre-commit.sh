#!/bin/bash
# ============================================================
# 智能座舱主交互系统 - 提交前检查脚本
# ASPICE Level 3 合规 - Pre-commit Hook
# ============================================================

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 脚本目录
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"

# 计数器
ERRORS=0
WARNINGS=0

# 打印函数
print_header() {
    echo -e "\n${BLUE}========================================${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}========================================${NC}"
}

print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

print_error() {
    echo -e "${RED}✗ $1${NC}"
    ((ERRORS++))
}

print_warning() {
    echo -e "${YELLOW}⚠ $1${NC}"
    ((WARNINGS++))
}

print_info() {
    echo -e "${BLUE}ℹ $1${NC}"
}

# 检查函数
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

# ============================================================
# 1. 检查Gradle环境
# ============================================================
check_gradle() {
    print_header "1. 检查Gradle环境"
    
    if [ ! -f "${PROJECT_ROOT}/gradlew" ]; then
        print_error "找不到gradlew脚本，请确保在项目根目录执行"
        exit 1
    fi
    
    print_success "Gradle wrapper已找到"
}

# ============================================================
# 2. Kotlin代码风格检查 (Ktlint)
# ============================================================
run_ktlint_check() {
    print_header "2. Kotlin代码风格检查"
    
    cd "${PROJECT_ROOT}"
    
    if ./gradlew ktlintCheck --quiet 2>/dev/null; then
        print_success "代码风格检查通过"
    else
        print_error "代码风格检查失败"
        print_info "运行 ./gradlew ktlintFormat 自动修复格式问题"
        
        # 显示具体问题
        ./gradlew ktlintCheck 2>&1 | grep -E "(ERROR|WARN|Expected)" || true
    fi
}

# ============================================================
# 3. Detekt静态分析
# ============================================================
run_detekt() {
    print_header "3. Detekt静态代码分析"
    
    cd "${PROJECT_ROOT}"
    
    # 运行detekt
    if ./gradlew detekt --quiet 2>/dev/null; then
        print_success "静态分析通过"
    else
        DETEKT_RESULT=$?
        
        # 检查是否有严重错误
        if [ -f "${PROJECT_ROOT}/build/reports/detekt/detekt.xml" ]; then
            SEVERITY_ERRORS=$(grep -c 'severity="error"' "${PROJECT_ROOT}/build/reports/detekt/detekt.xml" 2>/dev/null || echo "0")
            
            if [ "$SEVERITY_ERRORS" -gt 0 ]; then
                print_error "发现 ${SEVERITY_ERRORS} 个严重问题"
                print_info "查看详细报告: build/reports/detekt/detekt.html"
                
                # 显示部分问题
                echo -e "\n${YELLOW}主要问题摘要:${NC}"
                grep -E '<error' "${PROJECT_ROOT}/build/reports/detekt/detekt.xml" 2>/dev/null | head -5 || true
            else
                print_warning "发现警告级别问题"
            fi
        else
            print_error "Detekt执行失败"
        fi
    fi
}

# ============================================================
# 4. Android Lint检查
# ============================================================
run_lint() {
    print_header "4. Android Lint检查"
    
    cd "${PROJECT_ROOT}"
    
    if ./gradlew lintDebug --quiet 2>/dev/null; then
        print_success "Lint检查通过"
    else
        # 检查lint报告
        LINT_REPORT="${PROJECT_ROOT}/build/reports/lint/lint-results.xml"
        if [ -f "$LINT_REPORT" ]; then
            FATAL_ERRORS=$(grep -c 'severity="Fatal"' "$LINT_REPORT" 2>/dev/null || echo "0")
            SEVERITY_ERRORS=$(grep -c 'severity="Error"' "$LINT_REPORT" 2>/dev/null || echo "0")
            
            if [ "$FATAL_ERRORS" -gt 0 ] || [ "$SEVERITY_ERRORS" -gt 0 ]; then
                print_error "发现 ${FATAL_ERRORS} 个致命错误, ${SEVERITY_ERRORS} 个严重错误"
                print_info "查看详细报告: build/reports/lint/lint-results.html"
            else
                print_warning "发现警告级别问题"
            fi
        else
            print_error "Lint执行失败"
        fi
    fi
}

# ============================================================
# 5. 单元测试
# ============================================================
run_unit_tests() {
    print_header "5. 单元测试执行"
    
    cd "${PROJECT_ROOT}"
    
    # 运行单元测试
    if ./gradlew testDebugUnitTest --quiet 2>/dev/null; then
        print_success "单元测试通过"
        
        # 显示测试统计
        TEST_REPORT="${PROJECT_ROOT}/app/build/test-results/testDebugUnitTest"
        if [ -d "$TEST_REPORT" ]; then
            TEST_COUNT=$(find "$TEST_REPORT" -name "*.xml" -exec grep -c '<testcase' {} + 2>/dev/null | awk '{sum+=$1} END {print sum}')
            print_info "执行测试用例: ${TEST_COUNT} 个"
        fi
    else
        print_error "单元测试失败"
        print_info "查看详细报告: app/build/reports/tests/testDebugUnitTest/index.html"
        
        # 显示失败的测试
        echo -e "\n${YELLOW}失败的测试:${NC}"
        find "${PROJECT_ROOT}/app/build/test-results" -name "*.xml" -exec grep -l 'failures="[1-9]' {} \; 2>/dev/null | head -3 || true
    fi
}

# ============================================================
# 6. 检查提交信息规范
# ============================================================
check_commit_message() {
    print_header "6. 提交信息规范检查"
    
    # 获取最新的提交信息（用于git hook场景）
    if [ -n "$1" ]; then
        COMMIT_MSG="$1"
    elif [ -f ".git/COMMIT_EDITMSG" ]; then
        COMMIT_MSG=$(head -1 .git/COMMIT_EDITMSG)
    else
        # 检查最近一次提交
        COMMIT_MSG=$(git log -1 --pretty=%B 2>/dev/null | head -1 || echo "")
    fi
    
    if [ -z "$COMMIT_MSG" ]; then
        print_warning "无法获取提交信息"
        return
    fi
    
    # 检查提交信息格式: 类型(范围): 描述
    if echo "$COMMIT_MSG" | grep -qE '^(feat|fix|docs|style|refactor|test|chore|ci|build)(\([^)]+\))?: .+'; then
        print_success "提交信息格式正确"
    else
        print_error "提交信息格式不符合规范"
        print_info "正确格式: <type>(<scope>): <description>"
        print_info "类型: feat/fix/docs/style/refactor/test/chore/ci/build"
        print_info "示例: feat(MSG): 添加驾驶安全控制器单元测试"
    fi
}

# ============================================================
# 7. 检查敏感信息
# ============================================================
check_sensitive_data() {
    print_header "7. 敏感信息检查"
    
    cd "${PROJECT_ROOT}"
    
    # 检查是否有密钥、密码等敏感信息
    PATTERNS=(
        "password\s*="
        "secret\s*="
        "api[_-]?key\s*="
        "private[_-]?key\s*="
        "AKIA[0-9A-Z]{16}"
        "ghp_[a-zA-Z0-9]{36}"
    )
    
    FOUND=0
    for pattern in "${PATTERNS[@]}"; do
        if git diff --cached --name-only 2>/dev/null | xargs grep -l -E "$pattern" 2>/dev/null; then
            FOUND=1
            print_error "发现潜在敏感信息匹配模式: $pattern"
        fi
    done
    
    if [ $FOUND -eq 0 ]; then
        print_success "未发现明显敏感信息"
    else
        print_warning "请检查上述文件是否包含敏感信息"
    fi
}

# ============================================================
# 8. 检查文件大小
# ============================================================
check_file_size() {
    print_header "8. 大文件检查"
    
    cd "${PROJECT_ROOT}"
    
    # 检查大于10MB的文件
    LARGE_FILES=$(git diff --cached --name-only --diff-filter=A 2>/dev/null | while read file; do
        if [ -f "$file" ]; then
            SIZE=$(stat -f%z "$file" 2>/dev/null || stat -c%s "$file" 2>/dev/null || echo "0")
            if [ "$SIZE" -gt 10485760 ]; then
                echo "$file ($((SIZE / 1048576))MB)"
            fi
        fi
    done)
    
    if [ -n "$LARGE_FILES" ]; then
        print_warning "发现大文件:"
        echo "$LARGE_FILES"
        print_info "建议使用Git LFS管理大文件"
    else
        print_success "未发现大文件"
    fi
}

# ============================================================
# 9. 检查TODO标记
# ============================================================
check_todo_markers() {
    print_header "9. TODO标记检查"
    
    cd "${PROJECT_ROOT}"
    
    # 检查新增的TODO项
    TODO_LIST=$(git diff --cached --name-only 2>/dev/null | xargs grep -n "TODO" 2>/dev/null || true)
    
    if [ -n "$TODO_LIST" ]; then
        TODO_COUNT=$(echo "$TODO_LIST" | wc -l)
        print_warning "发现 ${TODO_COUNT} 个TODO标记"
        
        # 检查TODO格式
        INVALID_TODOS=$(echo "$TODO_LIST" | grep -v "TODO(.*):" || true)
        if [ -n "$INVALID_TODOS" ]; then
            print_error "发现格式不规范的TODO，应使用: TODO(姓名): REQ-XXX - 描述"
            echo "$INVALID_TODOS" | head -5
        fi
    else
        print_success "未发现新的TODO标记"
    fi
}

# ============================================================
# 10. 检查代码覆盖率（如果配置了JaCoCo）
# ============================================================
check_coverage() {
    print_header "10. 代码覆盖率检查"
    
    cd "${PROJECT_ROOT}"
    
    # 检查是否有覆盖率报告
    COVERAGE_REPORT="${PROJECT_ROOT}/app/build/reports/jacoco/jacocoTestReport/html/index.html"
    
    if [ -f "$COVERAGE_REPORT" ]; then
        # 提取覆盖率百分比（简化处理）
        COVERAGE=$(grep -oE '[0-9]+%' "${PROJECT_ROOT}/app/build/reports/jacoco/index.xml" 2>/dev/null | head -1 || echo "N/A")
        
        if [ "$COVERAGE" != "N/A" ]; then
            print_info "当前代码覆盖率: $COVERAGE"
            
            # 提取数字
            COV_NUM=$(echo "$COVERAGE" | tr -d '%')
            if [ "$COV_NUM" -lt 80 ]; then
                print_warning "覆盖率低于80%目标"
            else
                print_success "覆盖率达标"
            fi
        fi
    else
        print_info "跳过覆盖率检查（未生成报告）"
    fi
}

# ============================================================
# 汇总报告
# ============================================================
print_summary() {
    print_header "检查汇总"
    
    echo -e "错误数: ${RED}${ERRORS}${NC}"
    echo -e "警告数: ${YELLOW}${WARNINGS}${NC}"
    
    if [ $ERRORS -eq 0 ]; then
        echo -e "\n${GREEN}========================================${NC}"
        echo -e "${GREEN}  ✓ 所有检查通过，可以提交${NC}"
        echo -e "${GREEN}========================================${NC}"
        return 0
    else
        echo -e "\n${RED}========================================${NC}"
        echo -e "${RED}  ✗ 检查未通过，请修复后重试${NC}"
        echo -e "${RED}========================================${NC}"
        return 1
    fi
}

# ============================================================
# 主函数
# ============================================================
main() {
    echo -e "${BLUE}"
    echo "╔══════════════════════════════════════════════════════════╗"
    echo "║     智能座舱主交互系统 - 提交前质量检查                    ║"
    echo "║     ASPICE Level 3 合规检查                              ║"
    echo "╚══════════════════════════════════════════════════════════╝"
    echo -e "${NC}"
    
    # 记录开始时间
    START_TIME=$(date +%s)
    
    # 执行检查
    check_gradle
    run_ktlint_check
    run_detekt
    run_lint
    run_unit_tests
    check_commit_message "$1"
    check_sensitive_data
    check_file_size
    check_todo_markers
    check_coverage
    
    # 记录结束时间
    END_TIME=$(date +%s)
    DURATION=$((END_TIME - START_TIME))
    
    print_info "检查耗时: ${DURATION}秒"
    
    # 输出汇总
    print_summary
    exit $?
}

# 快速检查模式（跳过耗时测试）
quick_check() {
    echo -e "${BLUE}快速检查模式（跳过单元测试）${NC}"
    
    check_gradle
    run_ktlint_check
    run_detekt
    check_commit_message
    check_sensitive_data
    
    print_summary
}

# 根据参数执行不同模式
case "${1:-}" in
    --quick|-q)
        quick_check
        ;;
    --help|-h)
        echo "使用方法: $0 [选项]"
        echo ""
        echo "选项:"
        echo "  --quick, -q    快速检查模式（跳过单元测试）"
        echo "  --help, -h     显示帮助信息"
        echo ""
        echo "此脚本执行ASPICE Level 3合规的提交前质量检查："
        echo "  1. Kotlin代码风格检查"
        echo "  2. Detekt静态分析"
        echo "  3. Android Lint检查"
        echo "  4. 单元测试执行"
        echo "  5. 提交信息规范检查"
        echo "  6. 敏感信息检查"
        echo "  7. 大文件检查"
        echo "  8. TODO标记检查"
        echo "  9. 代码覆盖率检查"
        ;;
    *)
        main "$@"
        ;;
esac
