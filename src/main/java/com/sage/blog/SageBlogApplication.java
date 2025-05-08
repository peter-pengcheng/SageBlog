package com.sage.blog;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.sage.blog.mapper")
public class SageBlogApplication {

    public static void main(String[] args) {
        SpringApplication.run(SageBlogApplication.class, args);
    }
}