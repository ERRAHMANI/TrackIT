package com.trackit.service;

import com.trackit.dto.NotificationResponse;
import com.trackit.dto.PageResponse;
import com.trackit.entity.Anomalie;
import com.trackit.entity.Notification;
import com.trackit.entity.Utilisateur;
import com.trackit.exception.ApiException;
import com.trackit.mapper.DtoMapper;
import com.trackit.repository.NotificationRepository;
import com.trackit.security.UtilisateurConnecte;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    /** Notifie chaque destinataire distinct, sauf l'auteur de l'action. */
    public void notifier(Collection<Utilisateur> destinataires, Utilisateur auteurAction, Anomalie anomalie, String message) {
        Set<Long> dejaNotifies = new LinkedHashSet<>();
        destinataires.stream()
                .filter(Objects::nonNull)
                .filter(u -> !u.getId().equals(auteurAction.getId()))
                .filter(u -> dejaNotifies.add(u.getId()))
                .forEach(u -> creer(u, anomalie, message));
    }

    public void creer(Utilisateur destinataire, Anomalie anomalie, String message) {
        Notification n = new Notification();
        n.setUtilisateur(destinataire);
        n.setAnomalie(anomalie);
        n.setMessage(message);
        notificationRepository.save(n);
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> lister(UtilisateurConnecte courant, int page, int taille) {
        return PageResponse.depuis(notificationRepository.findByUtilisateurIdOrderByDateCreationDesc(courant.id(),
                PageRequest.of(page, taille)), DtoMapper::notification);
    }

    @Transactional(readOnly = true)
    public long compterNonLues(UtilisateurConnecte courant) {
        return notificationRepository.countByUtilisateurIdAndLuFalse(courant.id());
    }

    @Transactional
    public NotificationResponse marquerLue(UtilisateurConnecte courant, Long id) {
        Notification n = notificationRepository.findByIdAndUtilisateurId(id, courant.id())
                .orElseThrow(() -> ApiException.introuvable("Notification introuvable."));
        n.setLu(true);
        return DtoMapper.notification(n);
    }

    @Transactional
    public int marquerToutesLues(UtilisateurConnecte courant) {
        return notificationRepository.marquerToutesLues(courant.id());
    }
}
