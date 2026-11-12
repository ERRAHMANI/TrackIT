package com.trackit.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Politique de mot de passe de TrackIT, inspirée de la recommandation NIST SP 800-63B :
 * au moins 12 caractères, au plus 72 octets (limite de BCrypt), pas de mot de passe courant ni de
 * motif trivial. Aucune règle de composition imposée (majuscule, chiffre…) : la longueur protège mieux.
 * Une valeur vide est laissée à {@code @NotBlank}.
 */
@Documented
@Constraint(validatedBy = MotDePasseRobusteValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface MotDePasseRobuste {

    String message() default "Ce mot de passe n'est pas assez robuste.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
