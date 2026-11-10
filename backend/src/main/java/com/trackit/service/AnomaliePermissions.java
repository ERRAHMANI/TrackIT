package com.trackit.service;

import com.trackit.entity.Anomalie;
import com.trackit.entity.RoleCode;
import com.trackit.entity.StatutCode;
import com.trackit.security.UtilisateurConnecte;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

import static com.trackit.entity.RoleCode.*;

/**
 * Règles d'autorisation sur une anomalie (voir README.md, section 13 « Rôles et permissions »).
 * L'appartenance à l'organisation est vérifiée en amont, au chargement de l'anomalie.
 */
@Component
public class AnomaliePermissions {

    /** L'opérateur est « en charge » s'il a déclaré l'anomalie ou si elle lui est affectée. */
    public boolean estEnCharge(UtilisateurConnecte u, Anomalie a) {
        return a.getAuteur().getId().equals(u.id())
                || (a.getOperateur() != null && a.getOperateur().getId().equals(u.id()));
    }

    public boolean peutDeclarer(UtilisateurConnecte u) {
        return u.role() == OPERATEUR;
    }

    public boolean peutModifier(UtilisateurConnecte u, Anomalie a) {
        if (a.getStatutCode() == StatutCode.FERMEE) {
            return false;
        }
        return u.role() == RESPONSABLE_TECHNIQUE || (u.role() == OPERATEUR && estEnCharge(u, a));
    }

    public boolean peutPrioriser(UtilisateurConnecte u, Anomalie a) {
        return peutModifier(u, a);
    }

    public boolean peutPasserA(UtilisateurConnecte u, Anomalie a, StatutCode cible) {
        StatutCode actuel = a.getStatutCode();
        if (!actuel.peutPasserA(cible)) {
            return false;
        }
        if (cible == StatutCode.FERMEE) {
            return u.aLeRole(RESPONSABLE_TECHNIQUE, ADMINISTRATEUR);
        }
        return switch (u.role()) {
            case RESPONSABLE_TECHNIQUE -> cible != StatutCode.EN_COURS || a.getOperateur() != null;
            case OPERATEUR -> estEnCharge(u, a)
                    || (actuel == StatutCode.NOUVELLE && a.getOperateur() == null);
            default -> false;
        };
    }

    public List<StatutCode> statutsPossibles(UtilisateurConnecte u, Anomalie a) {
        return Arrays.stream(StatutCode.values()).filter(cible -> peutPasserA(u, a, cible)).toList();
    }

    public boolean peutAffecter(UtilisateurConnecte u, Anomalie a) {
        return u.role() == RESPONSABLE_TECHNIQUE && a.getStatutCode().estOuvert();
    }

    public boolean peutPrendreEnCharge(UtilisateurConnecte u, Anomalie a) {
        return u.role() == OPERATEUR && a.getOperateur() == null && a.getStatutCode().estOuvert();
    }

    public boolean peutCommenter(UtilisateurConnecte u, Anomalie a) {
        return u.aLeRole(OPERATEUR, RESPONSABLE_TECHNIQUE) && a.getStatutCode() != StatutCode.FERMEE;
    }

    public boolean peutJoindreFichier(UtilisateurConnecte u, Anomalie a) {
        return peutCommenter(u, a);
    }

    public boolean peutSupprimer(UtilisateurConnecte u) {
        return u.role() == RoleCode.ADMINISTRATEUR;
    }
}
