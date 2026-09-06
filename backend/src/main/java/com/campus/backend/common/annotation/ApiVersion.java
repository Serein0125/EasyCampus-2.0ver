package com.campus.backend.common.annotation;

import java.lang.annotation.*;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ApiVersion {
    
    String value() default "v1";
    
    String[] supportedVersions() default {"v1"};
    
    boolean deprecated() default false;
    
    String deprecatedMessage() default "此API版本已弃用，请使用最新版本";
    
    String since() default "";
    
    String description() default "";
}