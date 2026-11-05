package com.trackit.repository;

import com.trackit.dto.AnomalieCritere;
import com.trackit.entity.Anomalie;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Construit la requête de recherche des anomalies : le filtrage est réalisé par MySQL. */
public final class AnomalieSpecifications {

    private static final Pattern REFERENCE = Pattern.compile("^#?0*(\\d{1,18})$");

    private AnomalieSpecifications() {
    }

    public static Specification<Anomalie> rechercher(Long organisationId, Long utilisateurId, AnomalieCritere c) {
        return (root, query, cb) -> {
            List<Predicate> predicats = new ArrayList<>();
            predicats.add(cb.equal(root.get("systeme").get("organisation").get("id"), organisationId));

            if (c.q() != null && !c.q().isBlank()) {
                String texte = c.q().trim();
                String motif = "%" + texte.toLowerCase().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
                List<Predicate> ou = new ArrayList<>();
                ou.add(cb.like(cb.lower(root.get("titre")), motif, '\\'));
                ou.add(cb.like(cb.lower(root.get("description")), motif, '\\'));
                ou.add(cb.like(cb.lower(root.get("systeme").get("nom")), motif, '\\'));
                Matcher reference = REFERENCE.matcher(texte);
                if (reference.matches()) {
                    ou.add(cb.equal(root.get("id"), Long.parseLong(reference.group(1))));
                }
                predicats.add(cb.or(ou.toArray(Predicate[]::new)));
            }
            if (c.idStatut() != null) {
                predicats.add(cb.equal(root.get("statut").get("id"), c.idStatut()));
            }
            if (c.idPriorite() != null) {
                predicats.add(cb.equal(root.get("priorite").get("id"), c.idPriorite()));
            }
            if (c.idType() != null) {
                predicats.add(cb.equal(root.get("type").get("id"), c.idType()));
            }
            if (c.idSysteme() != null) {
                predicats.add(cb.equal(root.get("systeme").get("id"), c.idSysteme()));
            }
            if (c.idOperateur() != null) {
                predicats.add(cb.equal(root.get("operateur").get("id"), c.idOperateur()));
            }
            if (Boolean.TRUE.equals(c.nonAffectee())) {
                predicats.add(cb.isNull(root.get("operateur")));
            }
            if (Boolean.TRUE.equals(c.mesDeclarations())) {
                predicats.add(cb.equal(root.get("auteur").get("id"), utilisateurId));
            }
            if (Boolean.TRUE.equals(c.affecteesAMoi())) {
                predicats.add(cb.equal(root.get("operateur").get("id"), utilisateurId));
            }
            if (c.dateDebut() != null) {
                predicats.add(cb.greaterThanOrEqualTo(root.get("dateCreation"), c.dateDebut().atStartOfDay()));
            }
            if (c.dateFin() != null) {
                predicats.add(cb.lessThan(root.get("dateCreation"), c.dateFin().plusDays(1).atStartOfDay()));
            }
            return cb.and(predicats.toArray(Predicate[]::new));
        };
    }
}
