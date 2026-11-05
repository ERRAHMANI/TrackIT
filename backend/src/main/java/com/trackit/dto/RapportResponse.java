package com.trackit.dto;

import java.time.LocalDateTime;

public record RapportResponse(Long id, String titre, String type, String format, LocalDateTime dateGeneration,
                              boolean fichierDisponible, UtilisateurResume analyste) {
}
