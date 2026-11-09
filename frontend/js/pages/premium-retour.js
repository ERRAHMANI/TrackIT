// Résultat du paiement : retour de Stripe Checkout (succès / annulation) ou de la simulation.
import { api } from '../api.js';
import { icone } from '../icones.js';
import { esc } from '../ui.js';

const PRESENTATION = {
    REUSSI: ['succes', 'coche', 'Paiement confirmé'],
    ECHOUE: ['erreur', 'croix', 'Paiement refusé'],
    ANNULE: ['attention', 'flecheRetour', 'Paiement annulé'],
    EN_ATTENTE: ['info', 'horloge', 'Paiement en cours de traitement'],
    ERREUR: ['erreur', 'alerte', 'Erreur']
};

export default async function retour(contenu, { query }) {
    document.title = 'Résultat du paiement — TrackIT';
    const reference = query.get('session') || sessionStorage.getItem('trackit.paiement') || '';
    const resultat = query.get('resultat');
    contenu.innerHTML = '<div class="chargement">Vérification du paiement auprès de Stripe…</div>';

    let r;
    try {
        if (resultat === 'simulation') {
            r = JSON.parse(sessionStorage.getItem('trackit.resultatPaiement') || 'null');
        } else if (!reference) {
            r = { statutPaiement: 'ERREUR', message: 'Référence de paiement introuvable.' };
        } else if (resultat === 'annule') {
            r = await api.post('/abonnements/paiement/annulation', { referenceSession: reference });
        } else {
            r = await api.post('/abonnements/paiement/confirmation', { referenceSession: reference });
        }
    } catch (e) {
        r = { statutPaiement: 'ERREUR', message: e.message };
    }
    sessionStorage.removeItem('trackit.paiement');
    sessionStorage.removeItem('trackit.resultatPaiement');
    window.dispatchEvent(new Event('trackit:premium-modifie'));

    const [type, pictogramme, titre] = PRESENTATION[r?.statutPaiement] || PRESENTATION.ERREUR;
    contenu.innerHTML = `
        <section class="carte" style="max-width:600px;margin:2rem auto;text-align:center">
            <div class="pastille-modal pastille-${type === 'succes' ? 'succes' : type === 'info' ? 'info' : 'danger'}" style="margin:0 auto 1rem;width:56px;height:56px">${icone(pictogramme)}</div>
            <h1>${titre}</h1>
            <p>${esc(r?.message || '')}</p>
            <div class="actions-formulaire" style="justify-content:center">
                <a class="btn btn-secondaire" href="#/tableau-de-bord">Tableau de bord</a>
                <a class="btn btn-primaire" href="#/premium">Voir mon abonnement</a>
            </div>
        </section>`;
}
