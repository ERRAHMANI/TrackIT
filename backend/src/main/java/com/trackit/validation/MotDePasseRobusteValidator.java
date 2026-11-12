package com.trackit.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/** Vérifie un mot de passe selon {@link PolitiqueMotDePasse} et renvoie un message précis par champ. */
public class MotDePasseRobusteValidator implements ConstraintValidator<MotDePasseRobuste, String> {

    @Override
    public boolean isValid(String motDePasse, ConstraintValidatorContext contexte) {
        if (motDePasse == null || motDePasse.isEmpty()) {
            return true;
        }
        String erreur = PolitiqueMotDePasse.verifier(motDePasse);
        if (erreur == null) {
            return true;
        }
        contexte.disableDefaultConstraintViolation();
        contexte.buildConstraintViolationWithTemplate(erreur).addConstraintViolation();
        return false;
    }
}
