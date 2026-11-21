package com.trackit.api;

import com.trackit.entity.*;
import com.trackit.repository.*;
import com.trackit.security.JwtService;
import com.trackit.security.UtilisateurConnecte;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.Map;

/**
 * Jeu de données minimal pour les tests d'intégration : deux organisations, un compte par
 * rôle dans l'organisation A, un opérateur et un administrateur dans l'organisation B, les statuts
 * officiels et, pour chaque organisation, ses propres types d'anomalie et priorités.
 */
@Component
public class DonneesTest {

    public static final String MOT_DE_PASSE = "MotDePasse2026";

    private final RoleRepository roleRepository;
    private final StatutRepository statutRepository;
    private final PrioriteRepository prioriteRepository;
    private final TypeAnomalieRepository typeRepository;
    private final OrganisationRepository organisationRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final SystemeItRepository systemeRepository;
    private final AnomalieRepository anomalieRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public final Map<RoleCode, Utilisateur> comptes = new EnumMap<>(RoleCode.class);
    public final Map<StatutCode, Statut> statuts = new EnumMap<>(StatutCode.class);
    public Utilisateur operateur2;
    public Utilisateur operateurAutreOrganisation;
    public Utilisateur administrateurAutreOrganisation;
    public Priorite basse;
    public Priorite critique;
    public TypeAnomalie typeReseau;
    public TypeAnomalie typeApplication;
    public Priorite basseAutreOrganisation;
    public TypeAnomalie typeReseauAutreOrganisation;
    public SystemeIt erp;
    public SystemeIt reseau;
    public SystemeIt systemeAutreOrganisation;

    public DonneesTest(RoleRepository roleRepository, StatutRepository statutRepository,
                       PrioriteRepository prioriteRepository, TypeAnomalieRepository typeRepository,
                       OrganisationRepository organisationRepository, UtilisateurRepository utilisateurRepository,
                       SystemeItRepository systemeRepository, AnomalieRepository anomalieRepository,
                       PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.roleRepository = roleRepository;
        this.statutRepository = statutRepository;
        this.prioriteRepository = prioriteRepository;
        this.typeRepository = typeRepository;
        this.organisationRepository = organisationRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.systemeRepository = systemeRepository;
        this.anomalieRepository = anomalieRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public void initialiser() {
        comptes.clear();
        statuts.clear();
        Map<RoleCode, Role> roles = new EnumMap<>(RoleCode.class);
        for (RoleCode code : RoleCode.values()) {
            Role r = new Role();
            r.setNom(code.getLibelle());
            roles.put(code, roleRepository.save(r));
        }
        for (StatutCode code : StatutCode.values()) {
            Statut s = new Statut();
            s.setNom(code.getLibelle());
            statuts.put(code, statutRepository.save(s));
        }
        Organisation orgA = organisation("Organisation A", "contact@org-a.test");
        Organisation orgB = organisation("Organisation B", "contact@org-b.test");
        basse = priorite("Basse", 1, orgA);
        priorite("Moyenne", 2, orgA);
        priorite("Haute", 3, orgA);
        critique = priorite("Critique", 4, orgA);
        typeReseau = type("Réseau", orgA);
        typeApplication = type("Application", orgA);
        // Mêmes noms dans l'organisation B : l'unicité du nom est propre à chaque organisation.
        basseAutreOrganisation = priorite("Basse", 1, orgB);
        typeReseauAutreOrganisation = type("Réseau", orgB);

        for (RoleCode code : RoleCode.values()) {
            comptes.put(code, utilisateur(code.name().toLowerCase() + "@org-a.test", roles.get(code), orgA, true));
        }
        operateur2 = utilisateur("operateur2@org-a.test", roles.get(RoleCode.OPERATEUR), orgA, true);
        utilisateur("inactif@org-a.test", roles.get(RoleCode.OPERATEUR), orgA, false);
        operateurAutreOrganisation = utilisateur("operateur@org-b.test", roles.get(RoleCode.OPERATEUR), orgB, true);
        administrateurAutreOrganisation = utilisateur("administrateur@org-b.test", roles.get(RoleCode.ADMINISTRATEUR), orgB, true);

        erp = systeme("ERP", orgA);
        reseau = systeme("Réseau", orgA);
        systemeAutreOrganisation = systeme("ERP", orgB);
    }

    public String jeton(RoleCode role) {
        return jeton(comptes.get(role));
    }

    public String jeton(Utilisateur utilisateur) {
        Utilisateur charge = utilisateurRepository.findWithRoleById(utilisateur.getId()).orElseThrow();
        return "Bearer " + jwtService.genererJeton(UtilisateurConnecte.depuis(charge));
    }

    /** Crée directement une anomalie en base (sans passer par l'API). */
    public Anomalie anomalie(String titre, SystemeIt systeme, StatutCode statut, Utilisateur auteur, Utilisateur operateur) {
        Anomalie a = new Anomalie();
        a.setTitre(titre);
        a.setDescription("Description de " + titre);
        a.setSysteme(systeme);
        a.setType(typeReseau);
        a.setPriorite(basse);
        a.setStatut(statuts.get(statut));
        a.setAuteur(auteur);
        a.setOperateur(operateur);
        a.setDateCreation(LocalDateTime.now().minusDays(1));
        if (statut == StatutCode.RESOLUE || statut == StatutCode.FERMEE) {
            a.setDateResolution(LocalDateTime.now().minusHours(2));
        }
        return anomalieRepository.save(a);
    }

    private Priorite priorite(String nom, int niveau, Organisation organisation) {
        Priorite p = new Priorite();
        p.setOrganisation(organisation);
        p.setNom(nom);
        p.setNiveau(niveau);
        return prioriteRepository.save(p);
    }

    private TypeAnomalie type(String nom, Organisation organisation) {
        TypeAnomalie t = new TypeAnomalie();
        t.setOrganisation(organisation);
        t.setNom(nom);
        return typeRepository.save(t);
    }

    private Organisation organisation(String nom, String email) {
        Organisation o = new Organisation();
        o.setNom(nom);
        o.setEmail(email);
        return organisationRepository.save(o);
    }

    private Utilisateur utilisateur(String email, Role role, Organisation organisation, boolean actif) {
        Utilisateur u = new Utilisateur();
        u.setNom(email.substring(0, email.indexOf('@')).toUpperCase());
        u.setPrenom("Test");
        u.setEmail(email);
        u.setMotDePasse(passwordEncoder.encode(MOT_DE_PASSE));
        u.setActif(actif);
        u.setRole(role);
        u.setOrganisation(organisation);
        return utilisateurRepository.save(u);
    }

    private SystemeIt systeme(String nom, Organisation organisation) {
        SystemeIt s = new SystemeIt();
        s.setNom(nom);
        s.setOrganisation(organisation);
        return systemeRepository.save(s);
    }
}
