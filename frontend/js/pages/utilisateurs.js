// Gestion des comptes (UC-04 : créer, modifier, supprimer un utilisateur, attribuer un rôle).
import { api } from '../api.js';
import { session } from '../session.js';
import { icone } from '../icones.js';
import {
    esc, date, champ, ouvrirModal, confirmer, toast, validerFormulaire, lireFormulaire, erreurFormulaire, pendant,
    pagination, initiales, activerDeroulants, etatErreur
} from '../ui.js';

export default async function utilisateurs(contenu) {
    document.title = 'Utilisateurs — TrackIT';
    const roles = await api.get('/roles');
    const etat = { q: '', idRole: '', actif: '', page: 0 };

    contenu.innerHTML = `
        <div class="entete-page"><div><h1>Utilisateurs</h1><p>Comptes et rôles de votre organisation</p></div>
            <button class="btn btn-primaire" id="nouvel-utilisateur">${icone('plus')} Nouvel utilisateur</button></div>
        <section class="carte">
            <form class="barre-filtres" id="filtres" role="search">
                <div class="input-icone recherche">${icone('recherche')}<label class="sr-only" for="u-q">Rechercher</label>
                    <input class="input" id="u-q" name="q" placeholder="Nom, prénom ou e-mail…"></div>
                <label class="sr-only" for="u-role">Rôle</label>
                <select class="input" id="u-role" name="idRole"><option value="">Tous les rôles</option>${roles.map(r => `<option value="${r.id}">${esc(r.nom)}</option>`).join('')}</select>
                <label class="sr-only" for="u-actif">État</label>
                <select class="input" id="u-actif" name="actif"><option value="">Tous les états</option><option value="true">Actifs</option><option value="false">Désactivés</option></select>
            </form>
            <div id="liste"></div>
        </section>`;

    const liste = contenu.querySelector('#liste');
    const form = contenu.querySelector('#filtres');
    let minuterie;
    form.addEventListener('input', () => {
        clearTimeout(minuterie);
        minuterie = setTimeout(() => { Object.assign(etat, lireFormulaire(form), { page: 0 }); charger(); }, 300);
    });
    form.addEventListener('submit', e => e.preventDefault());
    contenu.querySelector('#nouvel-utilisateur').addEventListener('click', () => editer(null));

    async function charger() {
        try {
            const p = await api.get('/utilisateurs', { ...etat, taille: 15 });
            const moi = session.utilisateur().id;
            liste.innerHTML = p.contenu.length ? `<div class="tableau-conteneur"><table class="tableau">
                <thead><tr><th>Utilisateur</th><th>E-mail</th><th>Rôle</th><th>État</th><th>Créé le</th><th>Actions</th></tr></thead>
                <tbody>${p.contenu.map(u => `<tr>
                    <td><span style="display:inline-flex;gap:.6rem;align-items:center"><span class="avatar">${esc(initiales(u.prenom, u.nom))}</span>
                        <strong>${esc(u.prenom)} ${esc(u.nom)}</strong>${u.id === moi ? ' <span class="badge badge-neutre">vous</span>' : ''}</span></td>
                    <td>${esc(u.email)}</td><td>${esc(u.role.nom)}</td>
                    <td>${u.actif ? '<span class="badge badge-ok">Actif</span>' : '<span class="badge badge-fermee">Désactivé</span>'}</td>
                    <td>${date(u.dateCreation)}</td>
                    <td><div class="deroulant"><button class="btn-icone" data-basculer aria-label="Actions pour ${esc(u.email)}" aria-expanded="false">${icone('points')}</button>
                        <div class="menu-deroulant">
                            <button data-editer="${u.id}">${icone('crayon')} Modifier / rôle</button>
                            <button data-mdp="${u.id}">${icone('cadenas')} Réinitialiser le mot de passe</button>
                            ${u.id !== moi ? `<button data-basculer-actif="${u.id}">${icone(u.actif ? 'croix' : 'coche')} ${u.actif ? 'Désactiver' : 'Réactiver'}</button>
                            <button class="danger" data-supprimer="${u.id}">${icone('poubelle')} Supprimer</button>` : ''}
                        </div></div></td></tr>`).join('')}</tbody>
            </table></div>
            <div class="pied-tableau"><span class="aide">${p.totalElements} utilisateur(s)</span><div id="pagination-u"></div></div>`
                : '<p class="etat-vide">Aucun utilisateur ne correspond à la recherche.</p>';
            liste.querySelector('#pagination-u')?.replaceWith(pagination(p.page, p.totalPages, n => { etat.page = n; charger(); }));
            activerDeroulants(liste);
            const parId = id => p.contenu.find(u => String(u.id) === id);
            liste.querySelectorAll('[data-editer]').forEach(b => b.addEventListener('click', () => editer(parId(b.dataset.editer))));
            liste.querySelectorAll('[data-mdp]').forEach(b => b.addEventListener('click', () => reinitialiser(parId(b.dataset.mdp))));
            liste.querySelectorAll('[data-basculer-actif]').forEach(b => b.addEventListener('click', () => basculerActif(parId(b.dataset.basculerActif))));
            liste.querySelectorAll('[data-supprimer]').forEach(b => b.addEventListener('click', () => supprimer(parId(b.dataset.supprimer))));
        } catch (e) {
            etatErreur(liste, e);
        }
    }

    function editer(u) {
        const creation = !u;
        const { element, fermer } = ouvrirModal({
            titre: creation ? 'Nouvel utilisateur' : `Modifier ${esc(u.prenom)} ${esc(u.nom)}`,
            sousTitre: 'Les champs suivis d\'un astérisque (*) sont obligatoires.',
            corps: `<form novalidate><div class="bandeau-formulaire"></div>
                <div class="grille-2">${champ({ nom: 'nom', libelle: 'Nom', requis: true, valeur: u?.nom || '', attributs: 'maxlength="100"' })}
                ${champ({ nom: 'prenom', libelle: 'Prénom', requis: true, valeur: u?.prenom || '', attributs: 'maxlength="100"' })}</div>
                ${champ({ nom: 'email', libelle: 'Adresse e-mail', type: 'email', requis: true, valeur: u?.email || '', attributs: 'maxlength="150"' })}
                ${champ({ nom: 'idRole', libelle: 'Rôle', requis: 'Le rôle est obligatoire.', valeur: u?.role.id || '', options: roles.map(r => ({ valeur: r.id, libelle: r.nom })) })}
                ${creation ? champ({ nom: 'motDePasse', libelle: 'Mot de passe initial', type: 'password', requis: true, aide: '8 caractères minimum, à communiquer à l\'utilisateur', attributs: 'data-min="8" autocomplete="new-password"' }) : ''}
                <label class="case"><input type="checkbox" name="actif" ${!u || u.actif ? 'checked' : ''} ${u && u.id === session.utilisateur().id ? 'disabled' : ''}> Compte actif</label>
            </form>`,
            pied: `<button class="btn btn-secondaire" data-fermer>Annuler</button><button class="btn btn-primaire" data-ok>${creation ? 'Créer le compte' : 'Enregistrer'}</button>`
        });
        const form = element.querySelector('form');
        element.querySelector('[data-ok]').addEventListener('click', async () => {
            if (validerFormulaire(form)) return;
            const v = lireFormulaire(form);
            const corps = { nom: v.nom, prenom: v.prenom, email: v.email, idRole: Number(v.idRole), actif: form.actif.checked };
            if (creation) corps.motDePasse = v.motDePasse;
            try {
                await pendant(element.querySelector('[data-ok]'), () => creation ? api.post('/utilisateurs', corps) : api.put('/utilisateurs/' + u.id, corps));
                fermer();
                toast(creation ? `Compte de ${corps.prenom} ${corps.nom} créé.` : 'Utilisateur mis à jour.', 'succes');
                charger();
            } catch (erreur) {
                erreurFormulaire(form, erreur);
            }
        });
    }

    function reinitialiser(u) {
        const { element, fermer } = ouvrirModal({
            titre: 'Réinitialiser le mot de passe',
            sousTitre: `${esc(u.prenom)} ${esc(u.nom)} · ${esc(u.email)}`,
            pastille: { type: 'info', icone: 'cadenas' },
            corps: `<form novalidate><div class="bandeau-formulaire"></div>
                ${champ({ nom: 'nouveauMotDePasse', libelle: 'Nouveau mot de passe', type: 'password', requis: true, aide: '8 caractères minimum', attributs: 'data-min="8" autocomplete="new-password"' })}</form>`,
            pied: '<button class="btn btn-secondaire" data-fermer>Annuler</button><button class="btn btn-primaire" data-ok>Réinitialiser</button>'
        });
        const form = element.querySelector('form');
        element.querySelector('[data-ok]').addEventListener('click', async () => {
            if (validerFormulaire(form)) return;
            try {
                await api.put(`/utilisateurs/${u.id}/mot-de-passe`, lireFormulaire(form));
                fermer();
                toast('Mot de passe réinitialisé.', 'succes');
            } catch (erreur) {
                erreurFormulaire(form, erreur);
            }
        });
    }

    async function basculerActif(u) {
        if (u.actif && !(await confirmer({ titre: 'Désactiver ce compte ?', message: `${esc(u.prenom)} ${esc(u.nom)} ne pourra plus se connecter. Ses anomalies et son historique sont conservés.`, libelle: 'Désactiver', danger: true }))) return;
        try {
            await api.put('/utilisateurs/' + u.id, { nom: u.nom, prenom: u.prenom, email: u.email, idRole: u.role.id, actif: !u.actif });
            toast(u.actif ? 'Compte désactivé.' : 'Compte réactivé.', 'succes');
            charger();
        } catch (e) {
            toast(e.message, 'erreur');
        }
    }

    async function supprimer(u) {
        if (!(await confirmer({ titre: 'Supprimer ce compte ?', message: `Le compte de ${esc(u.prenom)} ${esc(u.nom)} sera supprimé. S'il est lié à des anomalies, il sera anonymisé : ses données personnelles seront effacées et son historique conservé.`, libelle: 'Supprimer', danger: true }))) return;
        try {
            await api.supprimer('/utilisateurs/' + u.id);
            toast('Compte supprimé.', 'succes');
            charger();
        } catch (e) {
            toast(e.message, 'erreur');
        }
    }

    await charger();
}
