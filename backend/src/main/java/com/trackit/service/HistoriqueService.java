package com.trackit.service;

import com.trackit.entity.Anomalie;
import com.trackit.entity.Historique;
import com.trackit.entity.Utilisateur;
import com.trackit.repository.HistoriqueRepository;
import org.springframework.stereotype.Service;

/** Trace les modifications d'une anomalie dans la table HISTORIQUE. */
@Service
public class HistoriqueService {

    private static final int LONGUEUR_ACTION = 255;

    private final HistoriqueRepository historiqueRepository;

    public HistoriqueService(HistoriqueRepository historiqueRepository) {
        this.historiqueRepository = historiqueRepository;
    }

    public void journaliser(Anomalie anomalie, Utilisateur auteur, String action) {
        enregistrer(anomalie, auteur, action, null, null, null, null);
    }

    public void journaliserStatut(Anomalie anomalie, Utilisateur auteur, String action, String ancien, String nouveau) {
        enregistrer(anomalie, auteur, action, ancien, nouveau, null, null);
    }

    public void journaliserPriorite(Anomalie anomalie, Utilisateur auteur, String action, String ancienne, String nouvelle) {
        enregistrer(anomalie, auteur, action, null, null, ancienne, nouvelle);
    }

    public void journaliserCreation(Anomalie anomalie, Utilisateur auteur) {
        enregistrer(anomalie, auteur, "Création de l'anomalie (système : " + anomalie.getSysteme().getNom() + ")",
                null, anomalie.getStatut().getNom(), null, anomalie.getPriorite().getNom());
    }

    private void enregistrer(Anomalie anomalie, Utilisateur auteur, String action, String ancienStatut,
                             String nouveauStatut, String anciennePriorite, String nouvellePriorite) {
        Historique h = new Historique();
        h.setAnomalie(anomalie);
        h.setUtilisateur(auteur);
        h.setAction(action.length() > LONGUEUR_ACTION ? action.substring(0, LONGUEUR_ACTION - 1) + "…" : action);
        h.setAncienStatut(ancienStatut);
        h.setNouveauStatut(nouveauStatut);
        h.setAnciennePriorite(anciennePriorite);
        h.setNouvellePriorite(nouvellePriorite);
        historiqueRepository.save(h);
    }
}
