/**
 * SageBlog 主脚本
 * 负责页面元素初始化和通用功能
 */

// 页面加载完成后执行
$(document).ready(function () {
    console.log("页面初始化...");

    // 初始化导航栏认证状态
    initAuthUI();

    // 绑定事件处理
    bindEvents();

    // 初始化页面特定功能
    initPageSpecific();
});

/**
 * 初始化认证状态UI
 */
function initAuthUI() {
    console.log("初始化认证状态UI...");

    // 检查是否通过AuthUtil初始化过
    if (typeof AuthUtil !== 'undefined') {
        // 使用AuthUtil更新UI
        AuthUtil.updateAuthUI();
    } else {
        // 兜底显示默认状态 - 未登录
        $('.auth-not-logged-in').show();
        $('.auth-logged-in').hide();
    }
}

/**
 * 绑定全局事件处理
 */
function bindEvents() {
    // 确保导航链接正确处理
    $('.nav-link').on('click', function (e) {
        var href = $(this).attr('href');
        if (href && href !== '#' && href !== 'javascript:void(0);') {
            console.log("导航点击: ", href);
        }
    });

    // 处理TokenNavigation操作
    $('[onclick^="navigateWithToken"]').on('click', function (e) {
        e.preventDefault();
        var target = $(this).attr('onclick').split("'")[1];
        if (target) {
            navigateWithToken(target);
        }
    });
}

/**
 * 带令牌导航
 */
function navigateWithToken(path) {
    console.log("导航到: ", path);

    // 获取令牌
    var token = '';
    if (typeof AuthUtil !== 'undefined') {
        token = AuthUtil.getToken();
    } else {
        token = localStorage.getItem('jwtToken');
    }

    // 构建URL
    var baseUrl = window.location.pathname.includes('/sageblog') ? '/sageblog/' : '/';
    var targetUrl = baseUrl + path;

    // 添加token参数
    if (token) {
        targetUrl += (targetUrl.includes('?') ? '&' : '?') + 'token=' + token;
    }

    // 跳转
    window.location.href = targetUrl;
}

/**
 * 初始化页面特定功能
 */
function initPageSpecific() {
    // 根据页面路径执行特定初始化
    var path = window.location.pathname;

    if (path.endsWith('/') || path.endsWith('/index')) {
        // 首页特定初始化
        initHomePage();
    } else if (path.includes('/profile')) {
        // 个人资料页初始化
        initProfilePage();
    } else if (path.includes('/dashboard')) {
        // 控制面板初始化
        initDashboardPage();
    }
}

/**
 * 初始化首页
 */
function initHomePage() {
    console.log("初始化首页...");

    // 确保认证链接可见
    $('.auth-not-logged-in').show();
}

/**
 * 初始化个人资料页
 */
function initProfilePage() {
    console.log("初始化个人资料页...");
}

/**
 * 初始化控制面板
 */
function initDashboardPage() {
    console.log("初始化控制面板...");
} 