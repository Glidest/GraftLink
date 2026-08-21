package com.graftlink.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface GraftInfo {
    String id();
    String name();
    String version() default "1.0.0";
    String author() default "";
    String description() default "";
}