/**
 * SageBlog 认证工具类
 * 处理JWT令牌以及请求拦截
 */

// 存储JWT令牌的键名
const JWT_TOKEN_KEY = 'jwtToken';
const CURRENT_USER_KEY = 'currentUser';

/**
 * 认证工具类
 */
const AuthUtil = {
    /**
     * 获取JWT令牌
     */
    getToken: function () {
        try {
            return localStorage.getItem(JWT_TOKEN_KEY);
        } catch (e) {
            console.error('获取令牌出错:', e);
            return null;
        }
    },

    /**
     * 设置JWT令牌
     */
    setToken: function (token) {
        try {
            if (!token) {
                console.warn('尝试设置空令牌');
                return;
            }
            localStorage.setItem(JWT_TOKEN_KEY, token);
            console.log('令牌已保存到localStorage');
        } catch (e) {
            console.error('保存令牌出错:', e);
        }
    },

    /**
     * 移除JWT令牌
     */
    removeToken: function () {
        try {
            localStorage.removeItem(JWT_TOKEN_KEY);
            console.log('令牌已从localStorage移除');
        } catch (e) {
            console.error('移除令牌出错:', e);
        }
    },

    /**
     * 获取当前用户信息
     */
    getCurrentUser: function () {
        try {
            const userStr = localStorage.getItem(CURRENT_USER_KEY);
            if (!userStr) return null;
            return JSON.parse(userStr);
        } catch (e) {
            console.error('获取用户信息出错:', e);
            return null;
        }
    },

    /**
     * 设置当前用户信息
     */
    setCurrentUser: function (user) {
        try {
            if (!user) {
                console.warn('尝试设置空用户信息');
                return;
            }
            localStorage.setItem(CURRENT_USER_KEY, JSON.stringify(user));
            console.log('用户信息已保存到localStorage');
        } catch (e) {
            console.error('保存用户信息出错:', e);
        }
    },

    /**
     * 移除当前用户信息
     */
    removeCurrentUser: function () {
        try {
            localStorage.removeItem(CURRENT_USER_KEY);
            console.log('用户信息已从localStorage移除');
        } catch (e) {
            console.error('移除用户信息出错:', e);
        }
    },

    /**
     * 检查是否已经登录
     */
    isLoggedIn: function () {
        try {
            const token = this.getToken();
            return token != null && token.trim() !== '';
        } catch (e) {
            console.error('检查登录状态出错:', e);
            return false;
        }
    },

    /**
     * 退出登录
     */
    logout: function () {
        console.log('执行退出登录操作');
        this.removeToken();
        this.removeCurrentUser();
        // 使用相对于根路径的URL，避免硬编码路径
        window.location.href = window.location.pathname.split('/')[1] === 'sageblog'
            ? '/sageblog/login'
            : '/login';
    }
};

// 注册全局AJAX请求拦截器
$(document).ready(function () {
    console.log('初始化AJAX拦截器');

    $.ajaxSetup({
        beforeSend: function (xhr) {
            // 获取JWT令牌
            const token = AuthUtil.getToken();

            // 如果令牌存在，添加到请求头
            if (token) {
                console.log('请求添加Authorization头');
                xhr.setRequestHeader('Authorization', 'Bearer ' + token);
            }
        },
        complete: function (xhr) {
            // 处理401未授权响应
            if (xhr.status === 401) {
                console.warn('收到401未授权响应，清除令牌并重定向到登录页面');
                // 清除令牌并重定向到登录页面
                AuthUtil.removeToken();
                AuthUtil.removeCurrentUser();

                // 存储当前URL作为登录后的重定向目标
                const currentUrl = window.location.pathname + window.location.search;
                if (currentUrl !== '/sageblog/login') {
                    console.log('重定向到登录页面，并设置回调URL:', currentUrl);
                    window.location.href = window.location.pathname.split('/')[1] === 'sageblog'
                        ? '/sageblog/login?redirect=' + encodeURIComponent(currentUrl)
                        : '/login?redirect=' + encodeURIComponent(currentUrl);
                }
            }
        }
    });
}); 