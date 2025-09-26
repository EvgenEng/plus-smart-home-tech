package ru.yandex.practicum.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class AllowedConditionValueValidator implements ConstraintValidator<AllowedConditionValue, Object> {

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        if (value instanceof Boolean) {
            return true;
        }

        if (value instanceof Number) {
            Number number = (Number) value;
            try {
                number.intValue();
                return true;
            } catch (Exception e) {
                return false;
            }
        }

        return false;
    }
}
