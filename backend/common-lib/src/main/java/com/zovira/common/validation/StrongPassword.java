package com.zovira.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 8 to 72 characters (bcrypt's input limit) with at least one letter and one digit.
 */
@NotBlank(message = "Enter a password")
@Size(min = 8, max = 72, message = "Password must be 8 to 72 characters")
@Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "Password must include a letter and a number")
@Constraint(validatedBy = {})
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface StrongPassword {

    String message() default "Password does not meet requirements";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
