// Point d'entrée : routes, contrôle d'accès par rôle et charpente applicative.
import { session } from './session.js';
import { api } from './api.js';
import { icone } from './icones.js';
import { esc, initiales, activerDeroulants, toast, dateHeure, fermerModal, LIBELLES_ROLES } from './ui.js';
import { naviguer, routeCourante } from './routeur.js';
import { chargerPremium, reinitialiserEtat } from './etat.js';
import { demarrerTraduction, selecteurLangue } from './i18n.js';

demarrerTraduction();

const TOUS = ['UTILISATEUR', 'OPERATEUR', 'RESPONSABLE_TECHNIQUE', 'ANALYSTE', 'ADMINISTRATEUR'];

const ROUTES = [
    { motif: '/', page: 'accueil', public: true },
    { motif: '/connexion', page: 'connexion', public: true },
    { motif: '/inscription', page: 'inscription', public: true },
    { motif: '/tableau-de-bord', page: 'tableau-de-bord', menu: 'tableau-de-bord' },
    { motif: '/anomalies', page: 'anomalies', menu: 'anomalies' },
    { motif: '/anomalies/nouvelle', page: 'anomalie-formulaire', menu: 'anomalies', roles: ['OPERATEUR'] },
    { motif: '/anomalies/:id/modifier', page: 'anomalie-formulaire', menu: 'anomalies', roles: ['OPERATEUR', 'RESPONSABLE_TECHNIQUE'] },
    { motif: '/anomalies/:id', page: 'anomalie-detail', menu: 'anomalies' },
    { motif: '/mes-taches', page: 'anomalies', menu: 'mes-taches', roles: ['OPERATEUR'], mesTaches: true },
    { motif: '/supervision', page: 'supervision', menu: 'supervision', roles: ['RESPONSABLE_TECHNIQUE'] },
    { motif: '/statistiques', page: 'statistiques', menu: 'statistiques', roles: ['ANALYSTE', 'RESPONSABLE_TECHNIQUE'] },
    { motif: '/rapports', page: 'rapports', menu: 'rapports', roles: ['ANALYSTE'] },
    { motif: '/utilisateurs', page: 'utilisateurs', menu: 'utilisateurs', roles: ['ADMINISTRATEUR'] },
    { motif: '/systemes', page: 'systemes', menu: 'systemes' },
    { motif: '/referentiels', page: 'referentiels', menu: 'referentiels', roles: ['ADMINISTRATEUR'] },
    { motif: '/premium', page: 'premium', menu: 'premium' },
    { motif: '/premium/simulation', page: 'premium-simulation', menu: 'premium', roles: ['ADMINISTRATEUR'] },
    { motif: '/premium/retour', page: 'premium-retour', menu: 'premium', roles: ['ADMINISTRATEUR'] },
    { motif: '/profil', page: 'profil', menu: 'parametres' }
];

const MENU = [
    { cle: 'tableau-de-bord', libelle: 'Tableau de bord', icone: 'accueil', lien: '/tableau-de-bord', roles: TOUS },
    { cle: 'anomalies', libelle: 'Anomalies', icone: 'alerte', lien: '/anomalies', roles: TOUS },
    { cle: 'mes-taches', libelle: 'Mes tâches', icone: 'taches', lien: '/mes-taches', roles: ['OPERATEUR'] },
    { cle: 'supervision', libelle: 'Supervision', icone: 'sablier', lien: '/supervision', roles: ['RESPONSABLE_TECHNIQUE'] },
    { cle: 'statistiques', libelle: 'Statistiques', icone: 'statistiques', lien: '/statistiques', roles: ['ANALYSTE', 'RESPONSABLE_TECHNIQUE'] },
    { cle: 'rapports', libelle: 'Rapports', icone: 'rapport', lien: '/rapports', roles: ['ANALYSTE'] },
    { cle: 'utilisateurs', libelle: 'Utilisateurs', icone: 'utilisateurs', lien: '/utilisateurs', roles: ['ADMINISTRATEUR'] },
    { cle: 'systemes', libelle: 'Systèmes IT', icone: 'serveur', lien: '/systemes', roles: TOUS },
    { cle: 'referentiels', libelle: 'Référentiels', icone: 'liste', lien: '/referentiels', roles: ['ADMINISTRATEUR'] },
    { cle: 'parametres', libelle: 'Paramètres', icone: 'parametres', lien: '/profil', roles: TOUS }
];

function trouverRoute(chemin) {
    for (const route of ROUTES) {
        const morceauxMotif = route.motif.split('/');
        const morceaux = chemin.split('/');
        if (morceauxMotif.length !== morceaux.length) continue;
        const params = {};
        const ok = morceauxMotif.every((m, i) => {
            if (m.startsWith(':')) { params[m.slice(1)] = decodeURIComponent(morceaux[i]); return morceaux[i] !== ''; }
            return m === morceaux[i];
        });
        if (ok) return { route, params };
    }
    return null;
}

// ------------------------------------------------------------------ Charpente applicative

let charpenteAffichee = false;

function afficherCharpente() {
    const u = session.utilisateur();
    const role = session.role();
    document.getElementById('app').innerHTML = `
    <div class="app">
        <aside class="menu-lateral" aria-label="Navigation principale">
            <a class="marque" href="#/tableau-de-bord"><img src="assets/logo.svg" alt="">TrackIT</a>
            <nav class="nav">
                ${MENU.filter(m => m.roles.includes(role)).map(m =>
                    `<a href="#${m.lien}" data-menu="${m.cle}">${icone(m.icone)}<span>${m.libelle}</span></a>`).join('')}
            </nav>
            <a class="carte-premium" href="#/premium" id="carte-premium">${icone('bouclier')}<div><strong>Premium</strong><small>…</small></div></a>
        </aside>
        <div class="principal">
            <header class="barre-superieure">
                <button class="btn-icone bouton-menu-mobile" id="bouton-menu" aria-label="Ouvrir le menu">${icone('menu')}</button>
                <form class="recherche-globale input-icone" id="recherche-globale" role="search">
                    ${icone('recherche')}
                    <label class="sr-only" for="q-global">Rechercher une anomalie</label>
                    <input class="input" id="q-global" name="q" placeholder="Rechercher…" autocomplete="off">
                </form>
                <div class="barre-droite">
                    ${selecteurLangue()}
                    <div class="deroulant" id="zone-notifications">
                        <button class="cloche" data-basculer aria-label="Notifications" aria-expanded="false">${icone('cloche')}<span class="compteur" hidden></span></button>
                        <div class="menu-deroulant panneau-notifications"></div>
                    </div>
                    <div class="deroulant">
                        <button class="identite" data-basculer aria-expanded="false">
                            <span class="avatar">${esc(initiales(u.prenom, u.nom))}</span>
                            <span class="texte-identite"><strong>${esc(u.prenom)} ${esc(u.nom)}</strong><small>${esc(LIBELLES_ROLES[role])}</small></span>
                            ${icone('chevronBas')}
                        </button>
                        <div class="menu-deroulant">
                            <a href="#/profil">${icone('utilisateur')} Mon profil</a>
                            <a href="#/profil?onglet=notifications">${icone('cloche')} Mes notifications</a>
                            <button class="danger" id="deconnexion">${icone('deconnexion')} Se déconnecter</button>
                        </div>
                    </div>
                </div>
            </header>
            <main class="contenu" id="contenu" tabindex="-1"></main>
        </div>
    </div>`;
    charpenteAffichee = true;
    const racine = document.querySelector('.app');
    activerDeroulants(racine);
    document.getElementById('bouton-menu').addEventListener('click', () => racine.classList.toggle('menu-ouvert'));
    racine.addEventListener('click', e => {
        if (racine.classList.contains('menu-ouvert') && (e.target.closest('.nav a') || e.target === racine)) racine.classList.remove('menu-ouvert');
    });
    document.getElementById('deconnexion').addEventListener('click', () => {
        session.fermer();
        reinitialiserEtat();
        charpenteAffichee = false;
        toast('Vous êtes déconnecté.');
        naviguer('/connexion');
    });
    document.getElementById('recherche-globale').addEventListener('submit', e => {
        e.preventDefault();
        const q = e.target.q.value.trim();
        naviguer('/anomalies' + (q ? '?q=' + encodeURIComponent(q) : ''));
    });
    document.querySelector('#zone-notifications [data-basculer]').addEventListener('click', chargerNotifications);
    rafraichirCompteurNotifications();
    afficherPremium();
}

async function afficherPremium() {
    const carte = document.getElementById('carte-premium');
    try {
        const etat = await chargerPremium();
        carte.querySelector('small').textContent = etat.premiumActif ? 'Plan actif' : 'Passer à Premium';
        carte.querySelector('strong').textContent = etat.premiumActif ? 'Premium' : 'Offre gratuite';
    } catch {
        carte.querySelector('small').textContent = '';
    }
}

async function rafraichirCompteurNotifications() {
    const compteur = document.querySelector('.cloche .compteur');
    if (!compteur) return;
    try {
        const { nombre } = await api.get('/notifications/non-lues');
        compteur.hidden = nombre === 0;
        compteur.textContent = nombre > 99 ? '99+' : nombre;
    } catch { /* compteur non bloquant */ }
}

async function chargerNotifications() {
    const panneau = document.querySelector('.panneau-notifications');
    panneau.innerHTML = '<div class="chargement">Chargement…</div>';
    try {
        const page = await api.get('/notifications', { taille: 6 });
        panneau.innerHTML = `
            <header><strong>Notifications</strong><button class="btn-lien" id="tout-lu">Tout marquer comme lu</button></header>
            <ul>${page.contenu.length ? page.contenu.map(n => `
                <li class="${n.lu ? '' : 'non-lue'}"><a href="${n.idAnomalie ? '#/anomalies/' + n.idAnomalie : '#/profil?onglet=notifications'}" data-id="${n.id}">
                    ${esc(n.message)}<br><small class="aide">${dateHeure(n.dateCreation)}</small></a></li>`).join('')
                : '<li class="etat-vide">Aucune notification.</li>'}</ul>
            <footer><a href="#/profil?onglet=notifications">Voir toutes les notifications</a></footer>`;
        panneau.querySelectorAll('a[data-id]').forEach(a => a.addEventListener('click', () => {
            api.patch(`/notifications/${a.dataset.id}/lue`).then(rafraichirCompteurNotifications).catch(() => {});
        }));
        panneau.querySelector('#tout-lu').addEventListener('click', async e => {
            e.stopPropagation();
            await api.patch('/notifications/lues');
            panneau.querySelectorAll('.non-lue').forEach(li => li.classList.remove('non-lue'));
            rafraichirCompteurNotifications();
        });
    } catch (e) {
        panneau.innerHTML = `<div class="chargement">${esc(e.message)}</div>`;
    }
}

// ------------------------------------------------------------------ Routage

async function router() {
    fermerModal();
    const { chemin, query } = routeCourante();
    const trouve = trouverRoute(chemin);
    if (!trouve) return naviguer(session.estConnecte() ? '/tableau-de-bord' : '/', { remplacer: true });
    const { route, params } = trouve;

    if (route.public) {
        if (session.estConnecte() && route.page !== 'accueil') return naviguer('/tableau-de-bord', { remplacer: true });
        charpenteAffichee = false;
        const module = await import(`./pages/${route.page}.js`);
        return module.default(document.getElementById('app'), { params, query });
    }
    if (!session.estConnecte()) {
        session.fermer();
        const retour = location.hash.replace(/^#/, '');
        return naviguer('/connexion?retour=' + encodeURIComponent(retour), { remplacer: true });
    }
    if (route.roles && !session.aLeRole(...route.roles)) {
        toast("Votre rôle ne permet pas d'accéder à cette page.", 'erreur');
        return naviguer('/tableau-de-bord', { remplacer: true });
    }
    if (!charpenteAffichee || !document.getElementById('contenu')) afficherCharpente();
    document.querySelectorAll('.nav a').forEach(a => a.classList.toggle('actif', a.dataset.menu === route.menu));
    document.querySelectorAll('.deroulant.ouvert').forEach(d => d.classList.remove('ouvert'));
    const contenu = document.getElementById('contenu');
    contenu.innerHTML = '<div class="chargement">Chargement…</div>';
    window.scrollTo(0, 0);
    try {
        const module = await import(`./pages/${route.page}.js`);
        await module.default(contenu, { params, query, route });
    } catch (e) {
        console.error(e);
        contenu.innerHTML = `<div class="bandeau bandeau-erreur" role="alert">${icone('alerte')}<span>${esc(e.message || 'Erreur de chargement de la page.')}</span></div>`;
    }
}

window.addEventListener('trackit:session-expiree', () => {
    charpenteAffichee = false;
    reinitialiserEtat();
    toast('Votre session a expiré. Veuillez vous reconnecter.', 'erreur');
    naviguer('/connexion');
});
window.addEventListener('trackit:premium-modifie', () => chargerPremium(true).then(afficherPremium).catch(() => {}));
window.addEventListener('trackit:notifications', rafraichirCompteurNotifications);
window.addEventListener('hashchange', router);

// Retour depuis Stripe Checkout (index.html?paiement=…) : on bascule vers la page de résultat.
const retourPaiement = new URLSearchParams(location.search);
if (retourPaiement.has('paiement')) {
    const sessionId = retourPaiement.get('session_id') || sessionStorage.getItem('trackit.paiement') || '';
    const cible = `/premium/retour?resultat=${retourPaiement.get('paiement')}&session=${encodeURIComponent(sessionId)}`;
    history.replaceState(null, '', location.pathname + '#' + cible);
}
router();
