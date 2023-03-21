package ex.rr.teamsmessaging.adaptivecard.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotates the the field should be mapped in the AdaptiveTemplete output.
 *
 * @param name  Optional. Attribute name to be shown in the output.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CardField {
    String name() default "";
}


