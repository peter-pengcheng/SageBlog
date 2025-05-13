/**
 * SageBlog 认证工具类
 * 处理JWT令牌以及请求拦截
 */

// 存储JWT令牌的键名
const JWT_TOKEN_KEY = 'jwtToken';
const CURRENT_USER_KEY = 'currentUser';

// 获取当前应用的上下文路径
function getContextPath() {
    // 首先尝试从页面变量获取
    if (typeof contextPath !== 'undefined' && contextPath !== null) {
        return contextPath;
    }

    // 默认使用根路径
    return '/';
}

/**
 * 认证工具类
 */
const AuthUtil = {
    /**
     * 获取JWT令牌
     */
    getToken: function () {
        return localStorage.getItem(JWT_TOKEN_KEY);
    },

    /**
     * 设置JWT令牌
     */
    setToken: function (token) {
        if (token) {
            localStorage.setItem(JWT_TOKEN_KEY, token);
        }
    },

    /**
     * 移除JWT令牌
     */
    removeToken: function () {
        localStorage.removeItem(JWT_TOKEN_KEY);
    },

    /**
     * 获取当前用户信息
     */
    getCurrentUser: function () {
        try {
            const userStr = localStorage.getItem(CURRENT_USER_KEY);
            return userStr ? JSON.parse(userStr) : null;
        } catch (e) {
            console.error('解析用户信息时出错:', e);
            // 发生错误时清除可能损坏的数据
            this.removeCurrentUser();
            return null;
        }
    },

    /**
     * 设置当前用户信息
     */
    setCurrentUser: function (user) {
        if (user) {
            try {
                localStorage.setItem(CURRENT_USER_KEY, JSON.stringify(user));
            } catch (e) {
                console.error('保存用户信息时出错:', e);
            }
        }
    },

    /**
     * 移除当前用户信息
     */
    removeCurrentUser: function () {
        localStorage.removeItem(CURRENT_USER_KEY);
    },

    /**
     * 清除所有认证相关数据
     * 在登录前或退出后调用
     */
    clearAllAuthData: function () {
        // 清除令牌
        this.removeToken();
        // 清除用户信息
        this.removeCurrentUser();
        // 清除其他可能的认证数据
        localStorage.removeItem('loginTime');
        // 清除cookie
        document.cookie = "isLoggedIn=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/;";

        console.log('所有认证数据已清除');
    },

    /**
     * 检查JWT令牌是否过期
     */
    checkTokenExpiration: function () {
        const token = this.getToken();
        if (!token) return true;

        try {
            // 解析JWT令牌（不验证签名）
            const base64Url = token.split('.')[1];
            const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
            const jsonPayload = decodeURIComponent(atob(base64).split('').map(function (c) {
                return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2);
            }).join(''));

            const payload = JSON.parse(jsonPayload);
            const expiration = payload.exp * 1000; // 转为毫秒
            return expiration < Date.now();
        } catch (e) {
            console.log('Token解析失败:', e);
            return true; // 解析失败视为过期
        }
    },

    /**
     * 检查是否已经登录
     */
    isLoggedIn: function () {
        const token = this.getToken();
        const user = this.getCurrentUser();

        if (!token || !user) return false;

        // 检查令牌是否已过期
        if (this.checkTokenExpiration()) {
            this.clearAllAuthData();
            return false;
        }

        return true;
    },

    /**
     * 退出登录
     */
    logout: function () {
        // 先发送请求通知服务器，然后清除本地存储
        try {
            const token = this.getToken();
            if (token) {
                // 调用登出API
                $.ajax({
                    url: getContextPath() + 'api/auth/logout',
                    type: 'POST',
                    headers: {
                        'Authorization': 'Bearer ' + token
                    },
                    async: false  // 同步请求，确保在页面跳转前完成
                });
            }
        } catch (e) {
            console.error('退出请求发送失败:', e);
        }

        // 清除所有认证数据
        this.clearAllAuthData();

        // 获取当前上下文路径并跳转到首页
        window.location.href = getContextPath();
    },

    /**
     * 快速检查认证状态并更新UI
     */
    updateAuthUI: function () {
        if (this.isLoggedIn()) {
            const user = this.getCurrentUser();
            $('.auth-logged-in').show();
            $('.auth-not-logged-in').hide();

            if (user && user.nickname) {
                $('#currentUsername').text(user.nickname || user.username || '用户');
            } else {
                // 如果用户信息不完整，尝试从服务器重新获取
                this.refreshUserInfo();
            }
        } else {
            $('.auth-logged-in').hide();
            $('.auth-not-logged-in').show();
        }
    },

    /**
     * 从服务器重新获取用户信息
     */
    refreshUserInfo: function () {
        const token = this.getToken();
        if (!token) return;

        $.ajax({
            url: getContextPath() + 'api/users/info',
            type: 'GET',
            headers: {
                'Authorization': 'Bearer ' + token
            },
            success: (response) => {
                if (response.code === 200 && response.data) {
                    console.log('成功获取最新用户信息');
                    this.setCurrentUser(response.data);

                    // 更新UI显示
                    const user = response.data;
                    const displayName = user.nickname || user.username || '用户';
                    $('#currentUsername').text(displayName);
                }
            },
            error: (xhr) => {
                console.error('获取用户信息失败:', xhr.status);
                if (xhr.status === 401) {
                    // 认证失败，清除过期信息
                    this.clearAllAuthData();
                    // 更新UI
                    $('.auth-logged-in').hide();
                    $('.auth-not-logged-in').show();
                }
            }
        });
    }
};

// 直接初始化认证状态，无需等待DOM完全加载
(function () {
    console.log('初始化认证状态...');

    // 从URL参数中提取token
    try {
        const urlParams = new URLSearchParams(window.location.search);
        const tokenParam = urlParams.get('token');

        // 如果URL中有token参数，保存到localStorage
        if (tokenParam) {
            console.log('从URL参数获取到token');
            localStorage.setItem(JWT_TOKEN_KEY, tokenParam);

            // 清除URL中的token参数
            urlParams.delete('token');
            const newParams = urlParams.toString();
            const newUrl = window.location.pathname + (newParams ? '?' + newParams : '');
            history.replaceState(null, '', newUrl);
        }
    } catch (e) {
        console.error('Token参数处理错误:', e);
    }
})();

// 处理表单提交和按钮点击
$(document).ready(function () {
    console.log('认证模块DOM加载完成，初始化UI...');

    // 初始化认证状态UI
    try {
        AuthUtil.updateAuthUI();
    } catch (e) {
        console.error('初始化UI时出错:', e);
    }

    // 注册退出登录事件
    $(document).on('click', '#logoutBtn', function (e) {
        e.preventDefault();
        console.log('退出登录');
        AuthUtil.logout();
        return false;
    });

    // 简化AJAX拦截处理
    $.ajaxSetup({
        beforeSend: function (xhr, settings) {
            // 只为API请求添加认证头
            if (settings.url &&
                (settings.url.includes('/api/') &&
                    !settings.url.includes('/api/auth/login') &&
                    !settings.url.includes('/api/auth/register'))) {

                const token = AuthUtil.getToken();
                if (token) {
                    xhr.setRequestHeader('Authorization', 'Bearer ' + token);
                }
            }
        },
        complete: function (xhr, status) {
            // 处理401错误
            if (xhr.status === 401) {
                // 只在API请求未认证时处理
                if (xhr.responseURL && xhr.responseURL.includes('/api/') &&
                    !xhr.responseURL.includes('/api/auth/login') &&
                    !xhr.responseURL.includes('/api/auth/register')) {

                    console.log('API请求返回401，清除认证数据');
                    AuthUtil.clearAllAuthData();

                    // 尝试更新UI
                    try {
                        $('.auth-logged-in').hide();
                        $('.auth-not-logged-in').show();
                    } catch (e) {
                        console.error('更新UI时出错:', e);
                    }
                }
            }
        }
    });
}); 