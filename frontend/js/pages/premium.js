// Abonnement Premium (UC-05) : état, offres, souscription via Stripe Checkout et historique.
import { api } from '../api.js';
import { session } from '../session.js';
import { icone } from '../icones.js';
import { esc, date, dateHeure, toast, etatErreur } from '../ui.js';
import { chargerPremium } from '../etat.js';
import { decimal } from '../i18n.js';

const LIBELLES = {
    ACTIF: ['Actif', 'ok'], EXPIRE: ['Expiré', 'fermee'], ANNULE: ['Annulé', 'fermee'], EN_ATTENTE: ['En attente', 'moyenne'],
    REUSSI: ['Réussi', 'ok'], ECHOUE: ['Échoué', 'retard']
};
const badge = statut => `<span class="badge badge-${(LIBELLES[statut] || [])[1] || 'neutre'}">${esc((LIBELLES[statut] || [statut])[0])}</span>`;

export default async function premium(contenu) {
    document.title = 'Abonnement Premium — TrackIT';
    const admin = session.aLeRole('ADMINISTRATEUR');
    const [etat, offres] = await Promise.all([chargerPremium(true), api.get('/abonnements/offres')]);
    const a = etat.abonnementActif;

    contenu.innerHTML = `
        <div class="entete-page"><div><h1>Abonnement Premium</h1><p>Fonctionnalités Premium : export des rapports PDF et Excel</p></div></div>
        <section class="carte">
            <div style="display:flex;gap:1rem;align-items:center;flex-wrap:wrap">
                <span class="kpi-icone ${etat.premiumActif ? 'fond-vert' : 'fond-gris'}">${icone('bouclier')}</span>
                <div><h2 style="margin:0">${etat.premiumActif ? 'Premium actif' : 'Offre gratuite'}</h2>
                    <p class="aide" style="margin:0">${etat.premiumActif ? `${esc(a.offre)} · du ${date(a.dateDebut)} au ${date(a.dateFin)}` : 'Les exports de rapports PDF / Excel ne sont pas disponibles.'}</p></div>
            </div>
        </section>
        ${admin ? `
        ${etat.modeSimulation ? `<div class="bandeau bandeau-info" style="margin-top:1rem">${icone('info')}<span><strong>Mode démonstration :</strong> aucune clé Stripe n'est configurée,
            le paiement est simulé (succès, échec ou annulation). Renseignez STRIPE_SECRET_KEY (clé de test) pour utiliser Stripe Checkout.</span></div>` : ''}
        <section class="carte"><h2>${etat.premiumActif ? 'Prolonger l\'abonnement' : 'Choisir une offre'}</h2>
            <div class="offres">${offres.map((o, i) => `<div class="offre ${i === 1 ? 'mise-en-avant' : ''}">
                <h3>${esc(o.libelle)}</h3>
                <div class="prix">${esc(decimal(o.montant))} € <small class="aide">/ ${o.dureeMois === 1 ? 'mois' : o.dureeMois + ' mois'}</small></div>
                <ul><li>Export des rapports PDF et Excel</li><li>Historique des rapports générés</li><li>Paiement sécurisé par Stripe</li></ul>
                <button class="btn btn-primaire" data-offre="${esc(o.code)}">${etat.premiumActif ? 'Prolonger' : 'Souscrire'}</button></div>`).join('')}</div>
            ${etat.premiumActif ? '<p class="aide">Une nouvelle souscription prolonge l\'abonnement en cours à partir de sa date de fin.</p>' : ''}
        </section>
        <section class="carte"><h2>Historique des abonnements et paiements</h2><div id="historique"><div class="chargement">Chargement…</div></div></section>`
        : `<div class="bandeau bandeau-info" style="margin-top:1rem">${icone('info')}<span>La souscription est gérée par l'administrateur de votre organisation.</span></div>`}`;

    contenu.querySelectorAll('[data-offre]').forEach(b => b.addEventListener('click', async () => {
        const texte = b.textContent;
        b.disabled = true;
        b.textContent = 'Redirection vers le paiement…';
        try {
            const reponse = await api.post('/abonnements/paiement', { codeOffre: b.dataset.offre });
            sessionStorage.setItem('trackit.paiement', reponse.referenceSession);
            window.location.href = reponse.urlPaiement;
        } catch (e) {
            toast(e.message, 'erreur');
            b.disabled = false;
            b.textContent = texte;
        }
    }));

    if (admin) {
        const zone = contenu.querySelector('#historique');
        try {
            const h = await api.get('/abonnements/historique');
            zone.innerHTML = `<div class="grille-2-cartes" style="margin:0">
                <div class="tableau-conteneur"><table class="tableau"><thead><tr><th>Offre</th><th>Début</th><th>Fin</th><th>État</th></tr></thead>
                    <tbody>${h.abonnements.map(x => `<tr><td>${esc(x.offre)}</td><td>${date(x.dateDebut)}</td><td>${date(x.dateFin)}</td><td>${badge(x.statut)}</td></tr>`).join('')
                        || '<tr><td colspan="4" class="vide">Aucun abonnement.</td></tr>'}</tbody></table></div>
                <div class="tableau-conteneur"><table class="tableau"><thead><tr><th>Date</th><th>Montant</th><th>État</th><th>Référence Stripe</th></tr></thead>
                    <tbody>${h.paiements.map(p => `<tr><td>${dateHeure(p.datePaiement)}</td><td>${esc(decimal(p.montant))} ${esc(p.devise)}</td>
                        <td>${badge(p.statut)}</td><td><code title="${esc(p.referenceStripe)}">${esc((p.referenceStripe || '').slice(0, 18))}…</code></td></tr>`).join('')
                        || '<tr><td colspan="4" class="vide">Aucun paiement.</td></tr>'}</tbody></table></div></div>`;
        } catch (e) {
            etatErreur(zone, e);
        }
    }
}