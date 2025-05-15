/**
 * 管理页面直接访问处理
 * 用于检测管理页面访问并处理token传递，让用户无感知访问
 * 使用POST请求验证权限，避免URL中暴露敏感信息
 */
(function () {
    // 立即检查当前页面路径
    const currentPath = window.location.pathname;

    console.log('[AdminAccess] 当前路径:', currentPath);

    // 检测是否直接访问管理页面
    if (currentPath.startsWith('/admin') &&
        !currentPath.includes('/admin/with-token') &&  // 排除旧的token端点
        !currentPath.includes('/admin/auth-verify') && // 排除验证端点
        !currentPath.includes('/admin-auth')) {        // 排除认证桥接页

        console.log('[AdminAccess] 检测到管理页面直接访问:', currentPath);

        // 阻止其他脚本的默认重定向
        // 在页面完全加载前执行
        window.addEventListener('DOMContentLoaded', function (e) {
            console.log('[AdminAccess] 阻止页面默认跳转');
            e.preventDefault();
            e.stopPropagation();
        });

        // 从localStorage获取token
        const token = localStorage.getItem('jwtToken');

        if (token) {
            console.log('[AdminAccess] 找到本地存储的JWT令牌');

            // 设置cookie以便后续请求使用
            document.cookie = "jwtToken=" + token + "; path=/; max-age=3600";

            // 尝试阻止页面默认加载
            if (document.readyState !== 'complete') {
                window.stop(); // 停止当前页面加载
            }

            // 创建并提交POST表单
            const form = document.createElement('form');
            form.method = 'POST';
            form.action = '/admin/auth-verify';
            form.style.display = 'none';

            // 添加token参数
            const tokenInput = document.createElement('input');
            tokenInput.type = 'hidden';
            tokenInput.name = 'token';
            tokenInput.value = token;
            form.appendChild(tokenInput);

            // 添加目标路径参数
            const targetPathInput = document.createElement('input');
            targetPathInput.type = 'hidden';
            targetPathInput.name = 'targetPath';
            targetPathInput.value = currentPath;
            form.appendChild(targetPathInput);

            // 保存当前URL的查询参数
            const currentQuery = window.location.search;
            if (currentQuery && currentQuery.length > 1) {
                const queryParams = new URLSearchParams(currentQuery);
                queryParams.forEach((value, key) => {
                    if (key !== 'token') { // 避免重复提交token
                        const input = document.createElement('input');
                        input.type = 'hidden';
                        input.name = key;
                        input.value = value;
                        form.appendChild(input);
                    }
                });
            }

            // 添加表单到文档并提交
            document.body.appendChild(form);
            console.log('[AdminAccess] 提交POST表单验证权限');

            // 确保表单被提交
            setTimeout(function () {
                form.submit();
            }, 10);
        } else {
            // 无token则交由拦截器处理重定向
            console.log('[AdminAccess] 未找到JWT令牌，交由后端处理');
        }
    } else if (currentPath === '/login') {
        // 登录页面处理
        const urlParams = new URLSearchParams(window.location.search);
        const redirect = urlParams.get('redirect');

        if (redirect && redirect.startsWith('/admin')) {
            console.log('[AdminAccess] 记录管理页面重定向路径:', redirect);
            localStorage.setItem('adminRedirectPath', redirect);
        }
    }
})(); 