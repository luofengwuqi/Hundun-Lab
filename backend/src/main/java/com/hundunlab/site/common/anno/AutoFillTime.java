package com.hundunlab.site.common.anno;

import com.hundunlab.site.common.enums.AutoFillTimeType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AutoFillTime {
    AutoFillTimeType value();
}
