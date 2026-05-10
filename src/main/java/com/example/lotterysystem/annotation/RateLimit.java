package com.example.lotterysystem.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {

    // 限流key
    String key() default "";

    // 时间窗口（秒）
    int time() default 60;

    // 允许的次数
    int count() default 10;

    // 提示消息
    String message() default "请求过于频繁，请稍后再试";
}

