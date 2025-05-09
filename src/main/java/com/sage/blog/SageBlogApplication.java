package com.sage.blog;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@MapperScan("com.sage.blog.mapper")
@EnableTransactionManagement
@ServletComponentScan
public class SageBlogApplication {

    public static void main(String[] args) {
        SpringApplication.run(SageBlogApplication.class, args);
    }
}