package dcapture.io;

import java.lang.annotation.*;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface HttpPath {
    String value() default "";

    boolean secured() default true;
}
