package bisq.httpapi.model.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;



import javax.validation.Constraint;
import javax.validation.Payload;

@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = NotNullItemsValidator.class)
@Documented
public @interface NotNullItems {

    String message() default "must not contain null elements";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}
