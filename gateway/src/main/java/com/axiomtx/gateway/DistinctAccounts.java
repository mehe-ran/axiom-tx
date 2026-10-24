package com.axiomtx.gateway;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// reported against receiver_id, same as the pydantic validator
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = DistinctAccounts.Validator.class)
public @interface DistinctAccounts {

    String message() default "sender and receiver cannot be the same";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class Validator implements ConstraintValidator<DistinctAccounts, TransferRequest> {

        @Override
        public boolean isValid(TransferRequest request, ConstraintValidatorContext context) {
            if (request.senderId() == null || !request.senderId().equals(request.receiverId())) {
                return true;
            }
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                    .addPropertyNode("receiverId")
                    .addConstraintViolation();
            return false;
        }
    }
}
