package com.trackit.service;

import com.trackit.dto.HistoriqueResponse;
import com.trackit.mapper.DtoMapper;
import com.trackit.repository.HistoriqueRepository;
import com.trackit.security.UtilisateurConnecte;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class HistoriqueConsultationService {

    private final HistoriqueRepository historiqueRepository;
    private final AnomalieService anomalieService;

    public HistoriqueConsultationService(HistoriqueRepository historiqueRepository, AnomalieService anomalieService) {
        this.historiqueRepository = historiqueRepository;
        this.anomalieService = anomalieService;
    }

    @Transactional(readOnly = true)
    public List<HistoriqueResponse> lister(UtilisateurConnecte courant, Long idAnomalie) {
        anomalieService.charger(courant, idAnomalie);
        return historiqueRepository.findByAnomalieIdOrderByDateModificationAscIdAsc(idAnomalie).stream()
                .map(DtoMapper::historique).toList();
    }
}
