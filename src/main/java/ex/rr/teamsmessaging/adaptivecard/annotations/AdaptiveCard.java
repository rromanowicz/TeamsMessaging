package ex.rr.teamsmessaging.adaptivecard.annotations;

import ex.rr.teamsmessaging.adaptivecard.enums.TemplateType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface AdaptiveCard {
    TemplateType type() default TemplateType.DEFAULT;
    String template() default "";

    String title() default "";
}


