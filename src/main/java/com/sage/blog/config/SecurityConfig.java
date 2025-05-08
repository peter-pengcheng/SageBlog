package com.sage.blog.config;

import com.sage.blog.security.JwtAuthenticationEntryPoint;
import com.sage.blog.security.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.rememberme.JdbcTokenRepositoryImpl;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;

import javax.sql.DataSource;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private JwtAuthenticationEntryPoint unauthorizedHandler;

    @Autowired
    private DataSource dataSource;

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter();
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(userDetailsService).passwordEncoder(passwordEncoder());
    }

    @Bean
    @Override
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public PersistentTokenRepository persistentTokenRepository() {
        JdbcTokenRepositoryImpl tokenRepository = new JdbcTokenRepositoryImpl();
        tokenRepository.setDataSource(dataSource);
        return tokenRepository;
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                // 禁用CSRF
                .csrf().disable()
                // 设置认证异常处理
                .exceptionHandling().authenticationEntryPoint(unauthorizedHandler).and()
                // 设置会话管理，使用无状态会话
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS).and()
                // 授权请求配置
                .authorizeRequests()
                // 允许访问静态资源
                .antMatchers(
                        "/css/**", "/js/**", "/img/**", "/favicon.ico",
                        "/sageblog/css/**", "/sageblog/js/**", "/sageblog/img/**",
                        "/sageblog/favicon.ico", "/sageblog/resources/**",
                        "/static/**", "/sageblog/static/**")
                .permitAll()
                // 允许访问认证相关接口
                .antMatchers("/api/auth/**").permitAll()
                // 允许访问登录、注册等页面
                .antMatchers("/", "/login", "/register", "/register-success", "/forgot-password", "/reset-password")
                .permitAll()
                // 需要认证才能访问的页面
                .antMatchers("/profile").authenticated()
                // 其他请求需要认证
                .anyRequest().authenticated()
                .and()
                // 表单登录配置
                .formLogin()
                .loginPage("/login")
                .defaultSuccessUrl("/")
                .and()
                // 记住我配置
                .rememberMe()
                .tokenRepository(persistentTokenRepository())
                .tokenValiditySeconds(86400) // 记住我的有效期：1天
                .userDetailsService(userDetailsService)
                .and()
                // 登出配置
                .logout()
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .deleteCookies("JSESSIONID", "remember-me");

        // 添加JWT过滤器
        http.addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);
    }
}