package com.trackit.dto;

import java.util.List;

public record SupervisionResponse(
        long ouvertes,
        long nonAffectees,
        long enRetard,
        List<DelaiAnomalie> delais,
        List<ChargeOperateur> chargeOperateurs) {
}
