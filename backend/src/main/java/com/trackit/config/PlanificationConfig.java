package com.trackit.config;

import com.trackit.service.EscaladeDelaisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.LocalDateTime;

/**
 * Tâches planifiées : escalade des délais dépassés, toutes les 15 minutes par défaut
 * (trackit.sla.intervalle-escalade). Désactivable par trackit.sla.escalade-active=false (tests).
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "trackit.sla.escalade-active", havingValue = "true", matchIfMissing = true)
public class PlanificationConfig {

    private static final Logger LOG = LoggerFactory.getLogger(PlanificationConfig.class);

    private final EscaladeDelaisService escaladeDelaisService;

    public PlanificationConfig(EscaladeDelaisService escaladeDelaisService) {
        this.escaladeDelaisService = escaladeDelaisService;
    }

    @Scheduled(initialDelayString = "${trackit.sla.intervalle-escalade:PT15M}",
            fixedDelayString = "${trackit.sla.intervalle-escalade:PT15M}")
    public void escaladerLesDelaisDepasses() {
        int escaladees = escaladeDelaisService.escalader(LocalDateTime.now());
        if (escaladees > 0) {
            LOG.info("Escalade des délais : {} anomalie(s) signalée(s) aux responsables techniques.", escaladees);
        }
    }
}
