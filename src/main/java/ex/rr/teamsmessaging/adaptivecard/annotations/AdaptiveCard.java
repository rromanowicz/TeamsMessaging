package ex.rr.teamsmessaging.adaptivecard.annotations;

import ex.rr.teamsmessaging.adaptivecard.enums.TemplateType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks the class as a valid AdaptiveCard output.
 *
 * @param   type        TemplateType (DEFAULT/CUSTOM)
 * @param   template    Overrides the default template. (Required for TemplateType.CUSTOM)
 * @param   name        Name to be displayed in the output.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface AdaptiveCard {
    TemplateType type() default TemplateType.DEFAULT;
    String template() default "";

    String title() default "";
}


