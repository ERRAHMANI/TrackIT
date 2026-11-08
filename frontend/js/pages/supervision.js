// Supervision (UC-02) : anomalies ouvertes, contrôle des délais, charge des opérateurs.
import { api } from '../api.js';
import { icone } from '../icones.js';
import { esc, reference, badgePriorite, badgeStatut, dateHeure, etatErreur } from '../ui.js';
import { barres } from '../graphiques.js';
import { naviguer } from '../routeur.js';

export default async function supervision(contenu) {
    document.title = 'Supervision — TrackIT';
    contenu.innerHTML = `
        <div class="entete-page"><div><h1>Supervision</h1><p>Anomalies ouvertes (Nouvelle, En cours), délais cibles par priorité et charge des opérateurs</p></div>
            <button class="btn btn-secondaire" id="actualiser">${icone('horloge')} Actualiser</button></div>
        <div id="zone"></div>`;
    const zone = contenu.querySelector('#zone');
    let filtre = 'tous';

    async function charger() {
        zone.innerHTML = '<div class="chargement">Chargement…</div>';
        try {
            afficher(await api.get('/supervision'));
        } catch (e) {
            etatErreur(zone, e);
        }
    }

    function afficher(s) {
        const lignes = s.delais.filter(d => filtre === 'tous' || (filtre === 'retard' ? d.enRetard : !d.anomalie.operateur));
        zone.innerHTML = `
            <div class="grille-kpi">
                <button class="kpi" data-filtre="tous"><span class="kpi-icone fond-bleu">${icone('alerte')}</span><span><span class="kpi-libelle">Anomalies ouvertes</span><br><span class="kpi-valeur">${s.ouvertes}</span></span></button>
                <button class="kpi" data-filtre="non-affectees"><span class="kpi-icone fond-orange">${icone('utilisateur')}</span><span><span class="kpi-libelle">Non affectées</span><br><span class="kpi-valeur">${s.nonAffectees}</span></span></button>
                <button class="kpi" data-filtre="retard"><span class="kpi-icone fond-rouge">${icone('sablier')}</span><span><span class="kpi-libelle">Hors délai</span><br><span class="kpi-valeur">${s.enRetard}</span></span></button>
                <div class="kpi kpi-statique"><span class="kpi-icone fond-vert">${icone('utilisateurs')}</span><span><span class="kpi-libelle">Opérateurs mobilisés</span><br><span class="kpi-valeur">${s.chargeOperateurs.length}</span></span></div>
            </div>
                <section class="carte"><h2>Charge des opérateurs</h2>
                    <p class="aide">Anomalies ouvertes affectées (Nouvelle + En cours).</p>
                    ${barres(s.chargeOperateurs.map(c => ({ libelle: c.nomComplet, valeur: c.total })))}
                </section>
                <section class="carte">
                    <div class="carte-entete"><h2>Contrôle des délais${filtre !== 'tous' ? ' · ' + (filtre === 'retard' ? 'hors délai' : 'non affectées') : ''}</h2>
                        ${filtre !== 'tous' ? '<button class="btn-lien" data-filtre="tous">Tout afficher</button>' : ''}</div>
                    ${lignes.length ? `<div class="tableau-conteneur"><table class="tableau">
                        <thead><tr><th>#</th><th>Titre</th><th>Priorité</th><th>Statut</th><th>Opérateur</th><th>Âge</th><th>Délai cible</th><th>Situation</th></tr></thead>
                        <tbody>${lignes.map(d => `<tr class="cliquable" data-id="${d.anomalie.id}" tabindex="0">
                            <td class="num">${reference(d.anomalie.id)}</td><td class="titre-cellule">${esc(d.anomalie.titre)}<br><small class="aide">${esc(d.anomalie.systeme.nom)} · ${dateHeure(d.anomalie.dateCreation)}</small></td>
                            <td>${badgePriorite(d.anomalie.priorite)}</td><td>${badgeStatut(d.anomalie.statut)}</td>
                            <td>${d.anomalie.operateur ? esc(d.anomalie.operateur.nomComplet) : '<span class="badge badge-moyenne">À affecter</span>'}</td>
                            <td>${duree(d.ageHeures)}</td><td>${duree(d.delaiCibleHeures)}</td>
                            <td>${d.enRetard ? `<span class="badge badge-retard">Dépassé de ${duree(d.ageHeures - d.delaiCibleHeures)}</span>` : '<span class="badge badge-ok">Dans les délais</span>'}</td></tr>`).join('')}</tbody>
                    </table></div>` : '<p class="etat-vide">Aucune anomalie dans cette catégorie.</p>'}
                </section>
            `;
        zone.querySelectorAll('[data-filtre]').forEach(b => b.addEventListener('click', () => { filtre = b.dataset.filtre; afficher(s); }));
        zone.querySelectorAll('tr[data-id]').forEach(tr => {
            tr.addEventListener('click', () => naviguer('/anomalies/' + tr.dataset.id));
            tr.addEventListener('keydown', e => { if (e.key === 'Enter') naviguer('/anomalies/' + tr.dataset.id); });
        });
    }

    contenu.querySelector('#actualiser').addEventListener('click', charger);
    await charger();
}

function duree(heures) {
    if (heures < 24) return heures + ' h';
    const jours = Math.floor(heures / 24);
    const reste = heures % 24;
    return jours + ' j' + (reste ? ' ' + reste + ' h' : '');
}
