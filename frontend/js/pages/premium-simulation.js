// Page de paiement simulée, utilisée uniquement lorsque aucune clé Stripe n'est configurée.
import { api } from '../api.js';
import { icone } from '../icones.js';
import { esc } from '../ui.js';
import { naviguer } from '../routeur.js';

export default function simulation(contenu, { query }) {
    document.title = 'Paiement (simulation) — TrackIT';
    const reference = query.get('session') || '';
    contenu.innerHTML = `
        <div class="entete-page"><div><h1>Paiement de l'abonnement</h1><p>Environnement de démonstration — aucun paiement réel n'est effectué</p></div></div>
        <section class="carte" style="max-width:560px">
            <div class="bandeau bandeau-info">${icone('info')}<span>Cette page remplace Stripe Checkout lorsque STRIPE_SECRET_KEY n'est pas renseignée.
                Choisissez le résultat à simuler.</span></div>
            <p class="aide">Référence de session : <code>${esc(reference)}</code></p>
            <div class="actions-rapides">
                <button class="btn btn-primaire" data-resultat="SUCCES">${icone('coche')} Payer — paiement accepté</button>
                <button class="btn btn-secondaire" data-resultat="ECHEC">${icone('croix')} Simuler un refus de carte</button>
                <button class="btn btn-secondaire" data-resultat="ANNULATION">${icone('flecheRetour')} Annuler et revenir</button>
            </div>
        </section>`;
    contenu.querySelectorAll('[data-resultat]').forEach(b => b.addEventListener('click', async () => {
        contenu.querySelectorAll('[data-resultat]').forEach(x => { x.disabled = true; });
        try {
            const r = await api.post('/abonnements/paiement/simulation', { referenceSession: reference, resultat: b.dataset.resultat });
            sessionStorage.setItem('trackit.resultatPaiement', JSON.stringify(r));
        } catch (e) {
            sessionStorage.setItem('trackit.resultatPaiement', JSON.stringify({ statutPaiement: 'ERREUR', message: e.message }));
        }
        naviguer('/premium/retour?resultat=simulation&session=' + encodeURIComponent(reference));
    }));
}
