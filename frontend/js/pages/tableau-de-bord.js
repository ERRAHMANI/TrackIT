// Tableau de bord (maquette 05) : indicateurs calculés par l'API sur la période choisie.
import { api } from '../api.js';
import { session } from '../session.js';
import { icone } from '../icones.js';
import { esc, reference, date, badgePriorite, badgeStatut, etatErreur } from '../ui.js';
import { courbes, anneau } from '../graphiques.js';
import { naviguer, majQuery } from '../routeur.js';
import { chargerReferentiels } from '../etat.js';
import { periodeParDefaut, htmlSelecteurPeriode, activerSelecteurPeriode } from '../periode.js';
import { decimal } from '../i18n.js';

export default async function tableauDeBord(contenu, { query }) {
    document.title = 'Tableau de bord — TrackIT';
    const periode = periodeParDefaut(query);
    const { statuts } = await chargerReferentiels();
    const idStatut = code => statuts.find(s => s.code === code)?.id;

    contenu.innerHTML = `
        <div class="entete-page">
            <div><h1>Tableau de bord</h1><p>Vue d'ensemble des anomalies de votre organisation</p></div>
            ${htmlSelecteurPeriode(periode, [7, 30, 90])}
        </div>
        <div id="zone"></div>`;
    const zone = contenu.querySelector('#zone');

    async function charger() {
        const { debut, fin } = periode;
        zone.innerHTML = '<div class="chargement">Calcul des indicateurs…</div>';
        majQuery({ debut, fin });
        try {
            const d = await api.get('/statistiques/tableau-de-bord', { debut, fin });
            zone.innerHTML = rendu(d);
            zone.querySelectorAll('[data-kpi]').forEach(b => b.addEventListener('click', () => {
                const statut = b.dataset.kpi ? '&idStatut=' + idStatut(b.dataset.kpi) : '';
                naviguer(`/anomalies?dateDebut=${debut}&dateFin=${fin}${statut}`);
            }));
            zone.querySelectorAll('tr[data-id]').forEach(tr => {
                tr.addEventListener('click', () => naviguer('/anomalies/' + tr.dataset.id));
                tr.addEventListener('keydown', e => { if (e.key === 'Enter') naviguer('/anomalies/' + tr.dataset.id); });
            });
        } catch (e) {
            etatErreur(zone, e);
        }
    }

    activerSelecteurPeriode(contenu, periode, charger);
    await charger();
}

function kpi(libelle, indicateur, iconeNom, fond, code) {
    const v = indicateur.variationPourcent;
    const texte = v == null ? '' : decimal(Math.abs(v)) + ' %';
    const variation = v == null ? '<span class="kpi-variation aide">— vs période préc.</span>'
        : v === 0 ? `<span class="kpi-variation aide" title="Par rapport à la période précédente">= ${texte}</span>`
        : `<span class="kpi-variation ${v > 0 ? 'hausse' : 'baisse'}" title="Par rapport à la période précédente">${v > 0 ? '↑' : '↓'} ${texte}</span>`;
    return `<button class="kpi" data-kpi="${code}" title="Voir la liste filtrée">
        <span class="kpi-icone ${fond}">${icone(iconeNom)}</span>
        <span><span class="kpi-libelle">${libelle}</span><br><span class="kpi-valeur">${indicateur.valeur}</span><br>${variation}</span>
    </button>`;
}

function rendu(d) {
    const recentes = d.anomaliesRecentes;
    const declarer = session.aLeRole('OPERATEUR')
        ? '<a class="btn btn-primaire" href="#/anomalies/nouvelle">' + icone('plus') + ' Déclarer une anomalie</a>' : '';
    return `
        <div class="grille-kpi">
            ${kpi('Total anomalies', d.total, 'alerte', 'fond-bleu', '')}
            ${kpi('Nouvelles', d.nouvelles, 'plus', 'fond-vert', 'NOUVELLE')}
            ${kpi('En cours', d.enCours, 'horloge', 'fond-orange', 'EN_COURS')}
            ${kpi('Résolues', d.resolues, 'coche', 'fond-violet', 'RESOLUE')}
        </div>
        <div class="grille-graphiques">
            <section class="carte"><div class="carte-entete"><h2>Évolution des anomalies</h2></div>
                ${courbes(d.evolution, [
                    { cle: 'declarees', libelle: 'Nouvelles (déclarées)', couleur: '#1872E6' },
                    { cle: 'nonResolues', libelle: 'En cours (non résolues)', couleur: '#f0a020' },
                    { cle: 'resolues', libelle: 'Résolues', couleur: '#7c4ddb' }
                ])}
            </section>
            <section class="carte"><div class="carte-entete"><h2>Répartition par type</h2></div>${anneau(d.repartitionParType)}</section>
        </div>
        <section class="carte">
            <div class="carte-entete"><h2>Anomalies récentes</h2>
                <div style="display:flex;gap:.8rem;align-items:center;flex-wrap:wrap">${declarer}<a href="#/anomalies">Voir toutes les anomalies ${icone('fleche')}</a></div></div>
            <div class="tableau-conteneur"><table class="tableau">
                <thead><tr><th>#</th><th>Titre</th><th>Système</th><th>Priorité</th><th>Statut</th><th>Date</th></tr></thead>
                <tbody>${recentes.length ? recentes.map(a => `
                    <tr class="cliquable" data-id="${a.id}" tabindex="0">
                        <td class="num">${reference(a.id)}</td><td class="titre-cellule">${esc(a.titre)}</td><td>${esc(a.systeme.nom)}</td>
                        <td>${badgePriorite(a.priorite)}</td><td>${badgeStatut(a.statut)}</td><td>${date(a.dateCreation)}</td></tr>`).join('')
                    : '<tr><td colspan="6" class="vide">Aucune anomalie déclarée.</td></tr>'}</tbody>
            </table></div>
        </section>`;
}
