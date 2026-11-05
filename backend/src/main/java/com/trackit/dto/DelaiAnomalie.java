package com.trackit.dto;

public record DelaiAnomalie(AnomalieResume anomalie, long ageHeures, int delaiCibleHeures, boolean enRetard) {
}
