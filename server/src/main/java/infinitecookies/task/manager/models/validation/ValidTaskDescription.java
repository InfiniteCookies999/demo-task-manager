package infinitecookies.task.manager.models.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = TaskDescriptionValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidTaskDescription {

    String message() default "Invalid task description";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}
