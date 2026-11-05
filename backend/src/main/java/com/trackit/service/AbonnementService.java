package com.trackit.service;

import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import com.trackit.config.TrackItProperties;
import com.trackit.dto.*;
import com.trackit.entity.*;
import com.trackit.exception.ApiException;
import com.trackit.mapper.DtoMapper;
import com.trackit.repository.AbonnementRepository;
import com.trackit.repository.OrganisationRepository;
import com.trackit.repository.PaiementRepository;
import com.trackit.security.UtilisateurConnecte;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/** Souscription Premium (UC-05) : offre, paiement Stripe, activation et historique. */
@Service
public class AbonnementService {

    private static final Logger LOG = LoggerFactory.getLogger(AbonnementService.class);
    private static final String PREFIXE_SIMULATION = "sim_";
    private static final DateTimeFormatter FORMAT_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final AbonnementRepository abonnementRepository;
    private final PaiementRepository paiementRepository;
    private final OrganisationRepository organisationRepository;
    private final StripeGateway stripe;
    private final TrackItProperties.Stripe config;
    private final String frontendUrl;

    public AbonnementService(AbonnementRepository abonnementRepository, PaiementRepository paiementRepository,
                             OrganisationRepository organisationRepository, StripeGateway stripe,
                             TrackItProperties properties) {
        this.abonnementRepository = abonnementRepository;
        this.paiementRepository = paiementRepository;
        this.organisationRepository = organisationRepository;
        this.stripe = stripe;
        this.config = properties.stripe();
        this.frontendUrl = properties.frontendUrl();
    }

    public List<OffreResponse> offres() {
        return config.offres().stream()
                .map(o -> new OffreResponse(o.code(), o.libelle(), o.montant(), config.currency().toUpperCase(), o.dureeMois()))
                .toList();
    }

    @Transactional(readOnly = true)
    public boolean estPremium(Long organisationId) {
        LocalDate aujourdHui = LocalDate.now();
        return abonnementRepository.findByOrganisationIdAndStatut(organisationId, StatutAbonnement.ACTIF).stream()
                .anyMatch(a -> a.estActifLe(aujourdHui));
    }

    @Transactional
    public EtatPremiumResponse etat(UtilisateurConnecte courant) {
        LocalDate aujourdHui = LocalDate.now();
        List<Abonnement> actifs = abonnementRepository.findByOrganisationIdAndStatut(courant.organisationId(), StatutAbonnement.ACTIF);
        actifs.stream().filter(a -> a.getDateFin() != null && a.getDateFin().isBefore(aujourdHui))
                .forEach(a -> a.setStatut(StatutAbonnement.EXPIRE));
        Abonnement courantActif = actifs.stream().filter(a -> a.estActifLe(aujourdHui))
                .max(Comparator.comparing(Abonnement::getDateDebut)).orElse(null);
        return new EtatPremiumResponse(courantActif != null, DtoMapper.abonnement(courantActif), config.modeSimulation());
    }

    @Transactional(readOnly = true)
    public HistoriqueAbonnementResponse historique(UtilisateurConnecte courant) {
        return new HistoriqueAbonnementResponse(
                abonnementRepository.findByOrganisationIdOrderByDateDebutDescIdDesc(courant.organisationId()).stream()
                        .map(DtoMapper::abonnement).toList(),
                paiementRepository.findByAbonnementOrganisationIdOrderByDatePaiementDesc(courant.organisationId()).stream()
                        .map(DtoMapper::paiement).toList());
    }

    // ------------------------------------------------------------------ Souscription

    @Transactional
    public CheckoutResponse demarrerPaiement(UtilisateurConnecte courant, CheckoutRequest requete) {
        TrackItProperties.Offre offre = config.offres().stream()
                .filter(o -> o.code().equals(requete.codeOffre())).findFirst()
                .orElseThrow(() -> ApiException.invalide("Offre inconnue : " + requete.codeOffre()));

        Abonnement abonnement = new Abonnement();
        abonnement.setOffre(offre.libelle());
        abonnement.setDateDebut(LocalDate.now());
        abonnement.setStatut(StatutAbonnement.EN_ATTENTE);
        abonnement.setOrganisation(organisationRepository.getReferenceById(courant.organisationId()));
        abonnementRepository.save(abonnement);

        String reference;
        String url;
        if (config.modeSimulation()) {
            reference = PREFIXE_SIMULATION + UUID.randomUUID().toString().replace("-", "");
            url = frontendUrl + "/index.html#/premium/simulation?session=" + reference;
        } else {
            StripeGateway.SessionPaiement session = stripe.creerSession(abonnement.getId(), offre.libelle(),
                    offre.montant(), courant.email());
            reference = session.id();
            url = session.url();
        }

        Paiement paiement = new Paiement();
        paiement.setAbonnement(abonnement);
        paiement.setMontant(offre.montant());
        paiement.setDevise(config.currency().toUpperCase());
        paiement.setStatut(StatutPaiement.EN_ATTENTE);
        paiement.setReferenceStripe(reference);
        paiementRepository.save(paiement);
        return new CheckoutResponse(url, reference, config.modeSimulation());
    }

    /** Appelée au retour de Stripe : l'état réel est relu auprès de Stripe, jamais déduit de l'URL. */
    @Transactional
    public ResultatPaiementResponse confirmer(UtilisateurConnecte courant, ConfirmationPaiementRequest requete) {
        Paiement paiement = paiementDeLOrganisation(courant, requete.referenceSession());
        if (paiement.getStatut() != StatutPaiement.EN_ATTENTE || estSimulation(paiement)) {
            return resultat(paiement);
        }
        Session session = stripe.recupererSession(paiement.getReferenceStripe());
        if ("paid".equals(session.getPaymentStatus())) {
            activer(paiement);
        } else if ("expired".equals(session.getStatus())) {
            terminer(paiement, StatutPaiement.ANNULE);
        }
        return resultat(paiement);
    }

    @Transactional
    public ResultatPaiementResponse annuler(UtilisateurConnecte courant, ConfirmationPaiementRequest requete) {
        Paiement paiement = paiementDeLOrganisation(courant, requete.referenceSession());
        if (paiement.getStatut() == StatutPaiement.EN_ATTENTE) {
            if (!estSimulation(paiement)) {
                stripe.expirerSession(paiement.getReferenceStripe());
            }
            terminer(paiement, StatutPaiement.ANNULE);
        }
        return resultat(paiement);
    }

    @Transactional
    public ResultatPaiementResponse simuler(UtilisateurConnecte courant, SimulationPaiementRequest requete) {
        if (!config.modeSimulation()) {
            throw ApiException.conflit("Le mode simulation est désactivé : une clé Stripe est configurée.");
        }
        Paiement paiement = paiementDeLOrganisation(courant, requete.referenceSession());
        if (paiement.getStatut() != StatutPaiement.EN_ATTENTE) {
            throw ApiException.conflit("Ce paiement a déjà été traité (" + paiement.getStatut() + ").");
        }
        switch (requete.resultat()) {
            case SUCCES -> activer(paiement);
            case ECHEC -> terminer(paiement, StatutPaiement.ECHOUE);
            case ANNULATION -> terminer(paiement, StatutPaiement.ANNULE);
        }
        return resultat(paiement);
    }

    // ------------------------------------------------------------------ Webhook Stripe

    @Transactional
    public void traiterWebhook(String charge, String signature) {
        Event evenement = stripe.verifierEvenement(charge, signature);
        StripeObject objet = evenement.getDataObjectDeserializer().getObject().orElse(null);
        if (objet == null) {
            LOG.warn("Événement Stripe {} non désérialisable (version d'API différente)", evenement.getId());
            return;
        }
        switch (evenement.getType()) {
            case "checkout.session.completed", "checkout.session.async_payment_succeeded" -> {
                Session session = (Session) objet;
                if ("paid".equals(session.getPaymentStatus())) {
                    paiementRepository.findByReferenceStripe(session.getId()).ifPresent(this::activer);
                }
            }
            case "checkout.session.async_payment_failed" -> paiementRepository.findByReferenceStripe(((Session) objet).getId())
                    .ifPresent(p -> terminer(p, StatutPaiement.ECHOUE));
            case "checkout.session.expired" -> paiementRepository.findByReferenceStripe(((Session) objet).getId())
                    .filter(p -> p.getStatut() == StatutPaiement.EN_ATTENTE)
                    .ifPresent(p -> terminer(p, StatutPaiement.ANNULE));
            case "payment_intent.payment_failed" -> LOG.info("Échec de paiement Stripe pour l'abonnement {}",
                    ((PaymentIntent) objet).getMetadata().get("id_abonnement"));
            default -> LOG.debug("Événement Stripe ignoré : {}", evenement.getType());
        }
    }

    // ------------------------------------------------------------------ Règles

    /** Active l'abonnement payé ; il prolonge un abonnement encore actif plutôt que de le chevaucher. */
    private void activer(Paiement paiement) {
        Abonnement abonnement = paiement.getAbonnement();
        paiement.setStatut(StatutPaiement.REUSSI);
        if (abonnement.getStatut() == StatutAbonnement.ACTIF) {
            return;
        }
        LocalDate aujourdHui = LocalDate.now();
        LocalDate debut = abonnementRepository.findByOrganisationIdAndStatut(abonnement.getOrganisation().getId(),
                        StatutAbonnement.ACTIF).stream()
                .filter(a -> a.estActifLe(aujourdHui) && a.getDateFin() != null)
                .map(a -> a.getDateFin().plusDays(1))
                .max(LocalDate::compareTo)
                .orElse(aujourdHui);
        int duree = config.offres().stream().filter(o -> o.libelle().equals(abonnement.getOffre()))
                .mapToInt(TrackItProperties.Offre::dureeMois).findFirst().orElse(1);
        abonnement.setDateDebut(debut);
        abonnement.setDateFin(debut.plusMonths(duree).minusDays(1));
        abonnement.setStatut(StatutAbonnement.ACTIF);
    }

    private static void terminer(Paiement paiement, StatutPaiement statut) {
        paiement.setStatut(statut);
        if (paiement.getAbonnement().getStatut() == StatutAbonnement.EN_ATTENTE) {
            paiement.getAbonnement().setStatut(StatutAbonnement.ANNULE);
        }
    }

    private Paiement paiementDeLOrganisation(UtilisateurConnecte courant, String reference) {
        return paiementRepository.findByReferenceStripe(reference)
                .filter(p -> p.getAbonnement().getOrganisation().getId().equals(courant.organisationId()))
                .orElseThrow(() -> ApiException.introuvable("Paiement introuvable."));
    }

    private static boolean estSimulation(Paiement paiement) {
        return paiement.getReferenceStripe().startsWith(PREFIXE_SIMULATION);
    }

    private static ResultatPaiementResponse resultat(Paiement paiement) {
        String message = switch (paiement.getStatut()) {
            case REUSSI -> "Paiement confirmé : l'abonnement « " + paiement.getAbonnement().getOffre()
                    + " » est actif jusqu'au " + FORMAT_DATE.format(paiement.getAbonnement().getDateFin()) + ".";
            case ECHOUE -> "Le paiement a échoué. Aucun montant n'a été débité ; vous pouvez réessayer.";
            case ANNULE -> "Le paiement a été annulé. Votre abonnement n'a pas été modifié.";
            case EN_ATTENTE -> "Le paiement est en cours de traitement par Stripe.";
        };
        return new ResultatPaiementResponse(paiement.getStatut().name(), paiement.getAbonnement().getStatut().name(), message);
    }
}
