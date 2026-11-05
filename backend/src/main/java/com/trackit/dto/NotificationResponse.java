package com.trackit.dto;

import java.time.LocalDateTime;

public record NotificationResponse(Long id, String message, LocalDateTime dateCreation, boolean lu, Long idAnomalie) {
}
