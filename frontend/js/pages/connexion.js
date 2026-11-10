// Écran de connexion (maquette 04 et écran d'erreur du prototype).
import { api } from '../api.js';
import { session } from '../session.js';
import { icone } from '../icones.js';
import { validerFormulaire, bandeau, pendant, ouvrirModal } from '../ui.js';
import { naviguer } from '../routeur.js';
import { preferences } from '../preferences.js';
import { selecteurLangue } from '../i18n.js';

// Comptes de la base de démonstration : l'encadré n'est affiché qu'en local (config.js → demo).
const COMPTES_DEMO = [
    ['Utilisateur', 'lucas.janssens@trackit.demo'],
    ['Opérateur', 'younes.errahmani@trackit.demo'],
    ['Responsable technique', 'sophie.peeters@trackit.demo'],
    ['Analyste', 'nadia.lambert@trackit.demo'],
    ['Administrateur', 'julien.dubois@trackit.demo']
];

export default function connexion(app, { query }) {
    document.title = 'Connexion — TrackIT';
    const demo = !!(window.TRACKIT_CONFIG && window.TRACKIT_CONFIG.demo);
    app.innerHTML = `
    <div class="page-centree">
        <div class="langue-flottante">${selecteurLangue()}</div>
        <div class="carte-connexion">
            <a class="marque" href="#/"><img src="assets/logo.svg" alt="">TrackIT</a>
            <h1>Connectez-vous à votre compte</h1>
            <p class="sous-titre">Accédez à TrackIT pour gérer vos anomalies IT.</p>
            <form id="form-connexion" novalidate>
                <div class="bandeau-formulaire"></div>
                <div class="champ">
                    <label for="email">Email</label>
                    <div class="input-icone">${icone('courriel')}
                        <input class="input" id="email" name="email" type="email" autocomplete="username" placeholder="votre@email.com" data-requis="L'adresse e-mail est obligatoire.">
                    </div>
                    <span class="erreur-champ"></span>
                </div>
                <div class="champ">
                    <label for="motDePasse">Mot de passe</label>
                    <div class="input-icone">${icone('cadenas')}
                        <input class="input" id="motDePasse" name="motDePasse" type="password" autocomplete="current-password" placeholder="Votre mot de passe" data-requis="Le mot de passe est obligatoire.">
                        <button type="button" class="basculer-mdp" aria-label="Afficher le mot de passe">${icone('oeil')}</button>
                    </div>
                    <span class="erreur-champ"></span>
                </div>
                <div class="ligne-options">
                    <label class="case"><input type="checkbox" name="seSouvenir" checked> Se souvenir de moi</label>
                    <a href="#" id="mdp-oublie">Mot de passe oublié ?</a>
                </div>
                <button class="btn btn-primaire btn-large" type="submit">Se connecter</button>
            </form>
            <p class="bas-carte">Vous n'avez pas de compte ? <a href="#/inscription">S'inscrire</a></p>
            ${demo ? `<details class="comptes-demo" ${query.get('demo') ? 'open' : ''}>
                <summary>Comptes de démonstration</summary>
                <p class="aide">Cliquez sur un compte pour pré-remplir le formulaire (environnement local uniquement).</p>
                <ul>${COMPTES_DEMO.map(([r, e]) => `<li><button type="button" data-email="${e}"><strong>${r}</strong> — ${e}</button></li>`).join('')}</ul>
            </details>` : ''}
        </div>
    </div>`;

    const form = app.querySelector('#form-connexion');
    const mdp = form.querySelector('#motDePasse');
    form.querySelector('.basculer-mdp').addEventListener('click', e => {
        const visible = mdp.type === 'text';
        mdp.type = visible ? 'password' : 'text';
        e.currentTarget.innerHTML = icone(visible ? 'oeil' : 'oeilBarre');
        e.currentTarget.setAttribute('aria-label', visible ? 'Afficher le mot de passe' : 'Masquer le mot de passe');
    });
    app.querySelectorAll('[data-email]').forEach(b => b.addEventListener('click', () => {
        form.email.value = b.dataset.email;
        mdp.value = 'TrackIT@2026';
        form.querySelector('button[type=submit]').focus();
    }));
    app.querySelector('#mdp-oublie').addEventListener('click', e => {
        e.preventDefault();
        ouvrirModal({
            titre: 'Mot de passe oublié',
            pastille: { type: 'info', icone: 'cadenas' },
            corps: `<p>Pour des raisons de sécurité, la réinitialisation est effectuée par l'administrateur de votre organisation
                    depuis l'écran <strong>Utilisateurs</strong>. Contactez-le en indiquant votre adresse e-mail professionnelle.</p>`,
            pied: '<button class="btn btn-primaire" data-fermer>J\'ai compris</button>'
        });
    });

    form.addEventListener('submit', async e => {
        e.preventDefault();
        if (validerFormulaire(form)) return;
        try {
            const reponse = await pendant(form.querySelector('button[type=submit]'),
                () => api.post('/auth/connexion', { email: form.email.value.trim(), motDePasse: mdp.value }), 'Connexion…');
            session.ouvrir(reponse, form.seSouvenir.checked);
            const retour = query.get('retour');
            naviguer(retour && retour.startsWith('/') && !retour.startsWith('/connexion') ? retour : preferences.get().accueil);
        } catch (erreur) {
            bandeau(form, erreur.message);
            if (erreur.status === 401) {
                form.querySelectorAll('.champ').forEach(c => c.classList.add('invalide'));
                mdp.value = '';
                mdp.focus();
            }
        }
    });
    form.email.focus();
}