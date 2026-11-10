// Profil utilisateur (maquette 06) : informations, mot de passe, préférences, notifications.
import { api } from '../api.js';
import { session } from '../session.js';
import { icone } from '../icones.js';
import {
    esc, champ, initiales, validerFormulaire, lireFormulaire, erreurFormulaire, bandeau, pendant, toast, dateHeure, pagination, etatErreur
} from '../ui.js';
import { majQuery } from '../routeur.js';
import { preferences } from '../preferences.js';
import { LANGUES, langue, changerLangue } from '../i18n.js';

const SECTIONS = [
    ['informations', 'Informations personnelles', 'utilisateur'],
    ['mot-de-passe', 'Changer le mot de passe', 'cadenas'],
    ['preferences', 'Préférences', 'parametres'],
    ['notifications', 'Notifications', 'cloche']
];

export default async function profil(contenu, { query }) {
    document.title = 'Mon profil — TrackIT';
    let section = SECTIONS.some(s => s[0] === query.get('onglet')) ? query.get('onglet') : 'informations';
    const roles = await api.get('/roles');
    contenu.innerHTML = `
        <div class="entete-page"><div><h1>Mon profil</h1><p>Gérez vos informations personnelles</p></div></div>
        <div class="disposition-profil">
            <nav class="menu-secondaire" aria-label="Sections du profil">
                ${SECTIONS.map(([c, l, i]) => `<button data-section="${c}" class="${c === section ? 'actif' : ''}">${icone(i)} ${l}</button>`).join('')}
            </nav>
            <section class="carte" id="panneau" style="margin:0"></section>
        </div>`;
    const panneau = contenu.querySelector('#panneau');
    contenu.querySelectorAll('[data-section]').forEach(b => b.addEventListener('click', () => {
        section = b.dataset.section;
        majQuery(section === 'informations' ? {} : { onglet: section });
        contenu.querySelectorAll('[data-section]').forEach(x => x.classList.toggle('actif', x === b));
        afficher();
    }));

    async function afficher() {
        try {
            if (section === 'informations') await informations();
            else if (section === 'mot-de-passe') motDePasse();
            else if (section === 'preferences') reglages();
            else await notifications(0);
        } catch (e) {
            etatErreur(panneau, e);
        }
    }

    async function informations() {
        const u = await api.get('/profil');
        panneau.innerHTML = `
            <h2>Informations personnelles</h2>
            <div class="bloc-photo"><span class="avatar avatar-grand">${esc(initiales(u.prenom, u.nom))}</span>
                <span class="aide">L'avatar est généré à partir de vos initiales.</span></div>
            <form id="form-profil" novalidate>
                <div class="bandeau-formulaire"></div>
                <div class="grille-2">${champ({ nom: 'nom', libelle: 'Nom', requis: true, valeur: u.nom, attributs: 'maxlength="100" autocomplete="family-name"' })}
                    ${champ({ nom: 'prenom', libelle: 'Prénom', requis: true, valeur: u.prenom, attributs: 'maxlength="100" autocomplete="given-name"' })}</div>
                ${champ({ nom: 'email', libelle: 'Email', type: 'email', requis: true, valeur: u.email, attributs: 'maxlength="150" autocomplete="email"' })}
                <div class="champ"><label for="role">Rôle</label>
                    <select class="input" id="role" disabled>${roles.map(r => `<option ${r.id === u.role.id ? 'selected' : ''}>${esc(r.nom)}</option>`).join('')}</select>
                    <span class="aide">Le rôle est attribué par l'administrateur de votre organisation.</span></div>
                <div class="champ"><label for="organisation">Organisation</label><input class="input" id="organisation" value="${esc(u.organisation.nom)}" readonly></div>
                <div class="actions-formulaire"><button class="btn btn-primaire" type="submit">Enregistrer les modifications</button></div>
            </form>`;
        const form = panneau.querySelector('form');
        form.addEventListener('submit', async e => {
            e.preventDefault();
            if (validerFormulaire(form)) return;
            try {
                const maj = await pendant(form.querySelector('button'), () => api.put('/profil', lireFormulaire(form)));
                session.majUtilisateur(maj);
                document.querySelector('.identite strong').textContent = `${maj.prenom} ${maj.nom}`;
                document.querySelector('.identite .avatar').textContent = initiales(maj.prenom, maj.nom);
                bandeau(form, 'Vos informations ont été enregistrées.', 'succes');
                toast('Profil mis à jour.', 'succes');
            } catch (erreur) {
                erreurFormulaire(form, erreur);
            }
        });
    }

    function motDePasse() {
        panneau.innerHTML = `
            <h2>Changer le mot de passe</h2>
            <form id="form-mdp" novalidate style="max-width:460px">
                <div class="bandeau-formulaire"></div>
                ${champ({ nom: 'motDePasseActuel', libelle: 'Mot de passe actuel', type: 'password', requis: true, attributs: 'autocomplete="current-password"' })}
                ${champ({ nom: 'nouveauMotDePasse', libelle: 'Nouveau mot de passe', type: 'password', requis: true, aide: '8 caractères minimum', attributs: 'data-min="8" autocomplete="new-password"' })}
                ${champ({ nom: 'confirmation', libelle: 'Confirmer le nouveau mot de passe', type: 'password', requis: true, attributs: 'data-egal="nouveauMotDePasse" autocomplete="new-password"' })}
                <div class="actions-formulaire"><button class="btn btn-primaire" type="submit">Mettre à jour le mot de passe</button></div>
            </form>`;
        const form = panneau.querySelector('form');
        form.addEventListener('submit', async e => {
            e.preventDefault();
            if (validerFormulaire(form)) return;
            const { confirmation, ...corps } = lireFormulaire(form);
            try {
                await pendant(form.querySelector('button'), () => api.put('/profil/mot-de-passe', corps));
                form.reset();
                bandeau(form, 'Votre mot de passe a été modifié.', 'succes');
            } catch (erreur) {
                erreurFormulaire(form, erreur);
            }
        });
    }

    function reglages() {
        const p = preferences.get();
        const accueils = [['/tableau-de-bord', 'Tableau de bord'], ['/anomalies', 'Liste des anomalies']];
        if (session.aLeRole('OPERATEUR')) accueils.push(['/mes-taches', 'Mes tâches']);
        if (session.aLeRole('RESPONSABLE_TECHNIQUE')) accueils.push(['/supervision', 'Supervision']);
        panneau.innerHTML = `
            <h2>Préférences</h2>
            <p class="aide">Préférences d'affichage enregistrées sur ce navigateur.</p>
            <form id="form-pref" style="max-width:460px">
                <div class="bandeau-formulaire"></div>
                <div class="champ"><label for="langue">Langue de l'interface</label>
                    <select class="input" id="langue" name="langue" data-sans-traduction>${LANGUES.map(l => `<option value="${l.code}" ${l.code === langue() ? 'selected' : ''}>${l.libelle}</option>`).join('')}</select></div>
                <div class="champ"><label for="taille">Nombre d'anomalies par page</label>
                    <select class="input" id="taille" name="taille">${[8, 10, 20, 50].map(n => `<option ${n === Number(p.taille) ? 'selected' : ''}>${n}</option>`).join('')}</select></div>
                <div class="champ"><label for="accueil">Page affichée après la connexion</label>
                    <select class="input" id="accueil" name="accueil">${accueils.map(([v, l]) => `<option value="${v}" ${v === p.accueil ? 'selected' : ''}>${l}</option>`).join('')}</select></div>
                <div class="actions-formulaire"><button class="btn btn-primaire" type="submit">Enregistrer les préférences</button></div>
            </form>`;
        const form = panneau.querySelector('form');
        form.addEventListener('submit', e => {
            e.preventDefault();
            preferences.enregistrer({ taille: Number(form.taille.value), accueil: form.accueil.value });
            if (form.langue.value !== langue()) return changerLangue(form.langue.value);
            bandeau(form, 'Préférences enregistrées.', 'succes');
        });
    }

    async function notifications(page) {
        const p = await api.get('/notifications', { page, taille: 10 });
        panneau.innerHTML = `
            <div class="carte-entete"><h2>Notifications</h2><button class="btn btn-secondaire btn-petit" id="tout-lu">${icone('coche')} Tout marquer comme lu</button></div>
            ${p.contenu.length ? `<ul class="fil">${p.contenu.map(n => `<li><span class="pastille" style="${n.lu ? 'background:#eef1f5;color:#6b7a90' : ''}">${icone('cloche')}</span>
                <div><div style="${n.lu ? '' : 'font-weight:600'}">${esc(n.message)}</div>
                <div class="detail">${dateHeure(n.dateCreation)} ${n.lu ? '· lue' : '· <button class="btn-lien" data-lue="' + n.id + '">Marquer comme lue</button>'}
                ${n.idAnomalie ? ` · <a href="#/anomalies/${n.idAnomalie}">Voir l'anomalie</a>` : ''}</div></div></li>`).join('')}</ul>`
                : '<p class="etat-vide">Aucune notification.</p>'}
            <div class="pied-tableau"><span class="aide">${p.totalElements} notification(s)</span><div id="pagination-n"></div></div>`;
        panneau.querySelector('#pagination-n').replaceWith(pagination(p.page, p.totalPages, notifications));
        panneau.querySelector('#tout-lu').addEventListener('click', async () => {
            await api.patch('/notifications/lues');
            window.dispatchEvent(new Event('trackit:notifications'));
            notifications(page);
        });
        panneau.querySelectorAll('[data-lue]').forEach(b => b.addEventListener('click', async () => {
            await api.patch(`/notifications/${b.dataset.lue}/lue`);
            window.dispatchEvent(new Event('trackit:notifications'));
            notifications(page);
        }));
    }

    await afficher();
}
