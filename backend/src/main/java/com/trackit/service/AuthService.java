package com.trackit.service;

import com.trackit.dto.AuthResponse;
import com.trackit.dto.ConnexionRequest;
import com.trackit.dto.InscriptionRequest;
import com.trackit.entity.Organisation;
import com.trackit.entity.RoleCode;
import com.trackit.entity.Utilisateur;
import com.trackit.exception.ApiException;
import com.trackit.mapper.DtoMapper;
import com.trackit.repository.OrganisationRepository;
import com.trackit.repository.RoleRepository;
import com.trackit.repository.UtilisateurRepository;
import com.trackit.security.JwtService;
import com.trackit.security.ProtectionConnexion;
import com.trackit.security.UtilisateurConnecte;
import com.trackit.validation.PolitiqueMotDePasse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UtilisateurRepository utilisateurRepository;
    private final OrganisationRepository organisationRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final ProtectionConnexion protectionConnexion;

    public AuthService(AuthenticationManager authenticationManager, JwtService jwtService,
                       UtilisateurRepository utilisateurRepository, OrganisationRepository organisationRepository,
                       RoleRepository roleRepository, PasswordEncoder passwordEncoder,
                       ProtectionConnexion protectionConnexion) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.utilisateurRepository = utilisateurRepository;
        this.organisationRepository = organisationRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.protectionConnexion = protectionConnexion;
    }

    /**
     * Connexion par e-mail et mot de passe. Un compte ou une adresse IP bloqué après trop d'échecs est
     * refusé (429) avant même la vérification du mot de passe, y compris si ce dernier est correct.
     */
    @Transactional(readOnly = true)
    public AuthResponse connecter(ConnexionRequest requete, String adresseIp) {
        String email = requete.email().trim();
        protectionConnexion.verifier(email, adresseIp);
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, requete.motDePasse()));
        } catch (AuthenticationException e) {
            protectionConnexion.enregistrerEchec(email, adresseIp);
            throw e;
        }
        protectionConnexion.enregistrerSucces(email);
        UtilisateurConnecte principal = (UtilisateurConnecte) authentication.getPrincipal();
        return reponse(principal);
    }

    /** Crée une organisation cliente et son premier compte administrateur. */
    @Transactional
    public AuthResponse inscrire(InscriptionRequest requete) {
        if (organisationRepository.existsByEmailIgnoreCase(requete.emailOrganisation().trim())) {
            throw ApiException.conflit("Une organisation utilise déjà cette adresse e-mail.");
        }
        if (utilisateurRepository.existsByEmailIgnoreCase(requete.email().trim())) {
            throw ApiException.conflit("Un compte existe déjà avec cette adresse e-mail.");
        }
        if (PolitiqueMotDePasse.contientInformationPersonnelle(requete.motDePasse(), requete.email(), requete.nom(), requete.prenom())) {
            throw ApiException.motDePassePersonnel();
        }
        Organisation organisation = new Organisation();
        organisation.setNom(requete.nomOrganisation().trim());
        organisation.setEmail(requete.emailOrganisation().trim().toLowerCase());
        organisationRepository.save(organisation);

        Utilisateur administrateur = new Utilisateur();
        administrateur.setNom(requete.nom().trim().toUpperCase());
        administrateur.setPrenom(requete.prenom().trim());
        administrateur.setEmail(requete.email().trim().toLowerCase());
        administrateur.setMotDePasse(passwordEncoder.encode(requete.motDePasse()));
        administrateur.setActif(true);
        administrateur.setOrganisation(organisation);
        administrateur.setRole(roleRepository.findByNom(RoleCode.ADMINISTRATEUR.getLibelle())
                .orElseThrow(() -> new IllegalStateException("Rôle Administrateur absent du référentiel")));
        utilisateurRepository.save(administrateur);
        return reponse(UtilisateurConnecte.depuis(administrateur));
    }

    private AuthResponse reponse(UtilisateurConnecte principal) {
        Utilisateur utilisateur = utilisateurRepository.findWithRoleById(principal.id())
                .orElseThrow(() -> ApiException.introuvable("Utilisateur introuvable."));
        return new AuthResponse(jwtService.genererJeton(principal), "Bearer", jwtService.getExpirationSecondes(),
                DtoMapper.utilisateur(utilisateur));
    }
}
