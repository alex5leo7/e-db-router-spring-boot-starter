package cn.electric.middleware.db.router.annotation;

import java.lang.annotation.*;

/**
 * 路由注解
 *
 * @author alex5leo7
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface DBRouter {

    /** 分库分表字段 */
    String key() default "";

}
