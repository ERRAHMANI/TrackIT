// Statistiques détaillées (UC-03 « Consulter statistiques », « Identifier incidents récurrents »).
import { api } from '../api.js';
import { session } from '../session.js';
import { icone } from '../icones.js';
import { esc, etatErreur } from '../ui.js';
import { anneau, barres } from '../graphiques.js';
import { majQuery } from '../routeur.js';
import { periodeParDefaut, htmlSelecteurPeriode, activerSelecteurPeriode } from '../periode.js';

export default async function statistiques(contenu, { query }) {
    document.title = 'Statistiques — TrackIT';
    const periode = periodeParDefaut(query, 90);
    contenu.innerHTML = `
        <div class="entete-page">
            <div><h1>Statistiques</h1><p>Indicateurs calculés en base sur les anomalies déclarées pendant la période</p></div>
            ${htmlSelecteurPeriode(periode)}
        </div>
        <div id="zone"></div>`;
    const zone = contenu.querySelector('#zone');

    async function charger() {
        majQuery(periode);
        zone.innerHTML = '<div class="chargement">Calcul des statistiques…</div>';
        try {
            const s = await api.get('/statistiques', periode);
            zone.innerHTML = rendu(s);
        } catch (e) {
            etatErreur(zone, e);
        }
    }
    activerSelecteurPeriode(contenu, periode, charger);
    await charger();
}

function carteKpi(libelle, valeur, iconeNom, fond) {
    return `<div class="kpi kpi-statique"><span class="kpi-icone ${fond}">${icone(iconeNom)}</span>
        <span><span class="kpi-libelle">${libelle}</span><br><span class="kpi-valeur">${valeur}</span></span></div>`;
}

function rendu(s) {
    const lienRapports = session.aLeRole('ANALYSTE')
        ? `<a class="btn btn-secondaire" href="#/rapports">${icone('rapport')} Générer un rapport PDF / Excel</a>` : '';
    return `
        <div class="grille-kpi">
            ${carteKpi('Anomalies déclarées', s.totalDeclarees, 'alerte', 'fond-bleu')}
            ${carteKpi('Résolues sur la période', s.totalResolues, 'coche', 'fond-vert')}
            ${carteKpi('Taux de résolution', s.tauxResolutionPourcent == null ? '—' : String(s.tauxResolutionPourcent).replace('.', ',') + ' %', 'cible', 'fond-violet')}
            ${carteKpi('Durée moyenne de résolution', s.dureeMoyenneResolutionHeures == null ? '—' : String(s.dureeMoyenneResolutionHeures).replace('.', ',') + ' h', 'horloge', 'fond-orange')}
        </div>
        <div class="grille-2-cartes">
            <section class="carte"><h2>Répartition par statut</h2>${anneau(s.parStatut.filter(v => v.valeur > 0))}</section>
            <section class="carte"><h2>Répartition par priorité</h2>${barres(s.parPriorite, { couleur: '#e05260' })}</section>
        </div>
        <div class="grille-2-cartes">
            <section class="carte"><h2>Répartition par type</h2>${barres(s.parType)}</section>
            <section class="carte"><h2>Répartition par système</h2>${barres(s.parSysteme.slice(0, 12), { couleur: '#7c4ddb' })}</section>
        </div>
        <div class="grille-2-cartes">
            <section class="carte"><h2>Durée moyenne de résolution par priorité</h2>
                ${barres(s.dureeMoyenneParPriorite.map(d => ({ libelle: d.priorite, valeur: d.heures })), { suffixe: ' h', couleur: '#f0a020' })}</section>
            <section class="carte"><div class="carte-entete"><h2>Incidents récurrents</h2>${lienRapports}</div>
                <p class="aide">Couples système / type apparus au moins 3 fois sur la période.</p>
                ${s.incidentsRecurrents.length ? `<div class="tableau-conteneur"><table class="tableau">
                    <thead><tr><th>Système</th><th>Type</th><th>Occurrences</th></tr></thead>
                    <tbody>${s.incidentsRecurrents.map(i => `<tr><td>${esc(i.systeme)}</td><td>${esc(i.type)}</td><td><strong>${i.nombre}</strong></td></tr>`).join('')}</tbody>
                </table></div>` : '<p class="etat-vide">Aucun incident récurrent sur la période.</p>'}
            </section>
        </div>`;
}
