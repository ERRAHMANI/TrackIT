package com.trackit.security;

import com.trackit.repository.UtilisateurRepository;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UtilisateurDetailsService implements UserDetailsService {

    private final UtilisateurRepository utilisateurRepository;

    public UtilisateurDetailsService(UtilisateurRepository utilisateurRepository) {
        this.utilisateurRepository = utilisateurRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UtilisateurConnecte loadUserByUsername(String email) {
        return utilisateurRepository.findByEmailIgnoreCase(email)
                .map(UtilisateurConnecte::depuis)
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur introuvable"));
    }
}
