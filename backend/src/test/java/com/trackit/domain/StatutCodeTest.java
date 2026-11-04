package com.trackit.domain;

import com.trackit.entity.StatutCode;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class StatutCodeTest {

    @Test
    void lesQuatreStatutsOfficielsUniquement() {
        assertThat(Arrays.stream(StatutCode.values()).map(StatutCode::getLibelle))
                .containsExactly("Nouvelle", "En cours", "Résolue", "Fermée");
    }

    @Test
    void transitionsDuCycleDeVie() {
        assertThat(StatutCode.NOUVELLE.transitionsAutorisees()).containsExactly(StatutCode.EN_COURS);
        assertThat(StatutCode.EN_COURS.transitionsAutorisees()).containsExactly(StatutCode.RESOLUE);
        assertThat(StatutCode.RESOLUE.transitionsAutorisees()).containsExactlyInAnyOrder(StatutCode.EN_COURS, StatutCode.FERMEE);
        assertThat(StatutCode.FERMEE.transitionsAutorisees()).isEmpty();
    }

    @Test
    void aucunRetourEnArriereVersNouvelle() {
        assertThat(Arrays.stream(StatutCode.values()).filter(s -> s.peutPasserA(StatutCode.NOUVELLE))).isEmpty();
    }
}
