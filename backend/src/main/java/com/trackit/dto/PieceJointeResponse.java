package com.trackit.dto;

import java.time.LocalDateTime;

public record PieceJointeResponse(Long id, String nomFichier, String typeFichier, LocalDateTime dateAjout,
                                  UtilisateurResume ajoutePar) {
}
