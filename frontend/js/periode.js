// Sélecteur de période (dates de début / fin et raccourcis en jours).
import { icone } from './icones.js';
import { isoJour } from './ui.js';

const JOUR = 86400000;
const FORMAT_JOUR = /^\d{4}-\d{2}-\d{2}$/;

/** Lit une date AAAA-MM-JJ dans l'adresse ; toute autre valeur est ignorée (protection XSS). */
function dateValide(valeur) {
    return valeur && FORMAT_JOUR.test(valeur) ? valeur : null;
}

export function periodeParDefaut(query, jours = 30) {
    const fin = dateValide(query.get('fin')) || isoJour(new Date());
    const debut = dateValide(query.get('debut')) || isoJour(new Date(Date.now() - (jours - 1) * JOUR));
    return { debut, fin };
}

export function htmlSelecteurPeriode({ debut, fin }, presets = [7, 30, 90, 180]) {
    const max = isoJour(new Date());
    return `<div class="selecteur-periode" role="group" aria-label="Période">
        ${icone('calendrier')}
        <label class="sr-only" for="periode-debut">Début</label><input class="input" type="date" id="periode-debut" value="${debut}" max="${max}">
        <span>—</span>
        <label class="sr-only" for="periode-fin">Fin</label><input class="input" type="date" id="periode-fin" value="${fin}" max="${max}">
        ${presets.map(j => `<button type="button" data-jours="${j}">${j} j</button>`).join('')}
    </div>`;
}

/** Branche les événements du sélecteur ; `surChangement(periode)` est appelé à chaque modification valide. */
export function activerSelecteurPeriode(racine, periode, surChangement) {
    const debut = racine.querySelector('#periode-debut');
    const fin = racine.querySelector('#periode-fin');
    const marquer = () => racine.querySelectorAll('[data-jours]').forEach(b => {
        const attendu = isoJour(new Date(new Date(periode.fin).getTime() - (Number(b.dataset.jours) - 1) * JOUR));
        b.classList.toggle('actif', attendu === periode.debut);
    });
    const appliquer = () => { marquer(); surChangement(periode); };
    debut.addEventListener('change', () => { if (debut.value && debut.value <= periode.fin) { periode.debut = debut.value; appliquer(); } else debut.value = periode.debut; });
    fin.addEventListener('change', () => { if (fin.value && fin.value >= periode.debut) { periode.fin = fin.value; appliquer(); } else fin.value = periode.fin; });
    racine.querySelectorAll('[data-jours]').forEach(b => b.addEventListener('click', () => {
        periode.fin = isoJour(new Date());
        periode.debut = isoJour(new Date(Date.now() - (Number(b.dataset.jours) - 1) * JOUR));
        debut.value = periode.debut;
        fin.value = periode.fin;
        appliquer();
    }));
    marquer();
}