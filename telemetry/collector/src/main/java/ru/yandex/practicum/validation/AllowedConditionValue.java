package ru.yandex.practicum.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = AllowedConditionValueValidator.class)
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface AllowedConditionValue {
    String message() default "Value must be null, Boolean or a number within int range";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
