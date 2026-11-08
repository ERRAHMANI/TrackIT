// Fiche détaillée d'une anomalie (maquette 03, écrans 10 à 16 du prototype).
import { api } from '../api.js';
import { session } from '../session.js';
import { icone } from '../icones.js';
import {
    esc, reference, dateHeure, badgePriorite, badgeStatut, ouvrirModal, fermerModal, confirmer, toast,
    validerFormulaire, bandeau, pendant, activerDeroulants, etatErreur, initiales
} from '../ui.js';
import { naviguer, majQuery } from '../routeur.js';
import { chargerReferentiels } from '../etat.js';

const ONGLETS = [
    ['informations', 'Informations'],
    ['commentaires', 'Commentaires'],
    ['pieces-jointes', 'Pièces jointes'],
    ['historique', 'Historique']
];
const FORMATS = '.pdf,.png,.jpg,.jpeg,.gif,.txt,.log,.csv,.json,.xml,.docx,.xlsx,.zip';

export default async function detailAnomalie(contenu, { params, query }) {
    const id = params.id;
    let onglet = ONGLETS.some(([c]) => c === query.get('onglet')) ? query.get('onglet') : 'informations';
    let a;

    async function recharger() {
        a = await api.get('/anomalies/' + id);
        document.title = `${reference(a.id)} ${a.titre} — TrackIT`;
        afficher();
    }

    function afficher() {
        const retourListe = (sessionStorage.getItem('trackit.retourListe') || '#/anomalies');
        const compteurs = { commentaires: a.nombreCommentaires, 'pieces-jointes': a.nombrePiecesJointes, historique: a.nombreEvenements };
        const menuActions = [
            a.actions.prioriser ? `<button data-action="priorite">${icone('cible')} Changer la priorité</button>` : '',
            `<button data-action="copier">${icone('liste')} Copier le lien</button>`,
            a.actions.supprimer ? `<button class="danger" data-action="supprimer">${icone('poubelle')} Supprimer</button>` : ''
        ].join('');
        contenu.innerHTML = `
            <nav class="fil-ariane" aria-label="Fil d'Ariane"><a href="${esc(retourListe)}">Anomalies</a> › ${reference(a.id)}</nav>
            <div class="entete-fiche">
                <div>
                    <h1>${reference(a.id)} – ${esc(a.titre)}</h1>
                    <div class="meta">${badgePriorite(a.priorite)} ${badgeStatut(a.statut)} <span>Créée le ${dateHeure(a.dateCreation)}</span></div>
                </div>
                <div style="display:flex;gap:.5rem">
                    ${a.actions.modifier ? `<a class="btn btn-secondaire" href="#/anomalies/${a.id}/modifier">${icone('crayon')} Modifier</a>` : ''}
                    <div class="deroulant"><button class="btn-icone" data-basculer aria-label="Autres actions" aria-expanded="false">${icone('points')}</button>
                        <div class="menu-deroulant">${menuActions}</div></div>
                </div>
            </div>
            <div class="onglets" role="tablist">
                ${ONGLETS.map(([cle, libelle]) => `<button role="tab" data-onglet="${cle}" aria-selected="${cle === onglet}" class="${cle === onglet ? 'actif' : ''}">
                    ${libelle}${compteurs[cle] !== undefined ? ` (${compteurs[cle]})` : ''}</button>`).join('')}
            </div>
            <div id="panneau" role="tabpanel"></div>`;
        activerDeroulants(contenu);
        contenu.querySelectorAll('[data-onglet]').forEach(b => b.addEventListener('click', () => changerOnglet(b.dataset.onglet)));
        contenu.querySelector('[data-action=priorite]')?.addEventListener('click', modalPriorite);
        contenu.querySelector('[data-action=supprimer]')?.addEventListener('click', supprimer);
        contenu.querySelector('[data-action=copier]').addEventListener('click', () => {
            navigator.clipboard?.writeText(location.href.split('?')[0]).then(() => toast('Lien copié.'));
        });
        afficherOnglet();
    }

    function changerOnglet(cle, apres) {
        onglet = cle;
        majQuery(cle === 'informations' ? {} : { onglet: cle });
        contenu.querySelectorAll('[data-onglet]').forEach(b => {
            b.classList.toggle('actif', b.dataset.onglet === cle);
            b.setAttribute('aria-selected', String(b.dataset.onglet === cle));
        });
        afficherOnglet().then(apres);
    }

    async function afficherOnglet() {
        const panneau = contenu.querySelector('#panneau');
        try {
            if (onglet === 'informations') return informations(panneau);
            panneau.innerHTML = '<div class="chargement">Chargement…</div>';
            if (onglet === 'commentaires') return await commentaires(panneau);
            if (onglet === 'pieces-jointes') return await piecesJointes(panneau);
            return await historique(panneau);
        } catch (e) {
            etatErreur(panneau, e);
        }
    }

    // ------------------------------------------------------------------ Onglet Informations

    function informations(panneau) {
        const act = a.actions;
        const boutonAffectation = act.affecter
            ? `<button class="btn btn-primaire btn-petit" data-action="affecter">${icone('utilisateur')} ${a.operateur ? 'Réaffecter' : 'Assigner'}</button>`
            : act.prendreEnCharge ? `<button class="btn btn-primaire btn-petit" data-action="prendre">${icone('utilisateur')} Prendre en charge</button>` : '';
        const actionsRapides = [
            act.changerStatut ? `<button class="btn btn-secondaire" data-action="statut">${icone('horloge')} Changer le statut</button>` : '',
            act.commenter ? `<button class="btn btn-secondaire" data-action="commenter">${icone('commentaire')} Ajouter un commentaire</button>` : '',
            act.joindreFichier ? `<button class="btn btn-secondaire" data-action="joindre">${icone('trombone')} Joindre un fichier</button>` : ''
        ].filter(Boolean);
        panneau.innerHTML = `
            <div class="disposition-fiche">
                <section class="carte">
                    <div class="donnee"><span class="libelle">Titre</span><div class="valeur">${esc(a.titre)}</div></div>
                    <div class="donnee"><span class="libelle">Description</span><div class="valeur texte-long">${esc(a.description)}</div></div>
                    <div class="donnee"><span class="libelle">Système concerné</span><div class="valeur">${esc(a.systeme.nom)}</div></div>
                    <div class="donnees-2">
                        <div class="donnee"><span class="libelle">Type d'anomalie</span><div class="valeur">${esc(a.type.nom)}</div></div>
                        <div class="donnee"><span class="libelle">Priorité</span><div class="valeur">${badgePriorite(a.priorite)}</div></div>
                    </div>
                    <div class="donnee"><span class="libelle">Statut</span><div class="valeur">${badgeStatut(a.statut)}</div></div>
                    <div class="donnee"><span class="libelle">Déclaré par</span>
                        <div class="valeur" style="display:flex;gap:.6rem;align-items:center"><span class="avatar">${esc(initialesNom(a.auteur.nomComplet))}</span>
                        ${esc(a.auteur.nomComplet)} <span class="aide">(${esc(a.auteur.role)})</span></div></div>
                    <div class="donnee"><span class="libelle">Assigné à</span>
                        <div class="valeur affectation"><span>${a.operateur ? esc(a.operateur.nomComplet) + ' <span class="aide">(' + esc(a.operateur.role) + ')</span>' : 'Non assigné'}</span>${boutonAffectation}</div></div>
                </section>
                <div class="colonne-laterale">
                    <section class="carte"><h2>Informations complémentaires</h2>
                        <div class="donnee"><span class="libelle">Date de création</span><div class="valeur">${dateHeure(a.dateCreation)}</div></div>
                        <div class="donnee"><span class="libelle">Dernière mise à jour</span><div class="valeur">${dateHeure(a.derniereMiseAJour)}</div></div>
                        ${a.dateResolution ? `<div class="donnee"><span class="libelle">Date de résolution</span><div class="valeur">${dateHeure(a.dateResolution)}</div></div>` : ''}
                        <div class="donnee"><span class="libelle">Organisation</span><div class="valeur">${esc(a.organisation)}</div></div>
                    </section>
                    <section class="carte"><h2>Actions rapides</h2>
                        ${actionsRapides.length ? `<div class="actions-rapides">${actionsRapides.join('')}</div>`
                            : `<p class="aide">Consultation seule : ${a.statut.code === 'FERMEE' ? 'cette anomalie est fermée.' : 'votre rôle ne permet pas de modifier cette anomalie.'}</p>`}
                    </section>
                </div>
            </div>`;
        panneau.querySelector('[data-action=affecter]')?.addEventListener('click', modalAffectation);
        panneau.querySelector('[data-action=prendre]')?.addEventListener('click', prendreEnCharge);
        panneau.querySelector('[data-action=statut]')?.addEventListener('click', modalStatut);
        panneau.querySelector('[data-action=commenter]')?.addEventListener('click', () =>
            changerOnglet('commentaires', () => contenu.querySelector('#nouveau-commentaire')?.focus()));
        panneau.querySelector('[data-action=joindre]')?.addEventListener('click', () =>
            changerOnglet('pieces-jointes', () => contenu.querySelector('#fichier-pj')?.click()));
    }

    // ------------------------------------------------------------------ Onglet Commentaires

    async function commentaires(panneau) {
        const liste = await api.get(`/anomalies/${id}/commentaires`);
        panneau.innerHTML = `
            <section class="carte">
                <h2>Commentaires</h2>
                ${liste.length ? `<ul class="fil">${liste.map(c => `
                    <li><span class="avatar">${esc(initialesNom(c.auteur.nomComplet))}</span>
                        <div><strong>${esc(c.auteur.nomComplet)}</strong> <span class="detail">· ${esc(c.auteur.role)} · ${dateHeure(c.dateCreation)}</span>
                        <div class="contenu-commentaire">${esc(c.contenu)}</div></div></li>`).join('')}</ul>`
                    : '<p class="etat-vide">Aucun commentaire pour le moment.</p>'}
                ${a.actions.commenter ? `
                <form id="form-commentaire" novalidate style="margin-top:1rem">
                    <div class="bandeau-formulaire"></div>
                    <div class="champ"><label for="nouveau-commentaire">Ajouter un commentaire <span class="requis">*</span></label>
                        <textarea class="input" id="nouveau-commentaire" name="contenu" maxlength="5000" data-requis="Le commentaire ne peut pas être vide." placeholder="Votre commentaire (diagnostic, action réalisée…)"></textarea>
                        <span class="erreur-champ"></span></div>
                    <div class="actions-formulaire"><button class="btn btn-primaire" type="submit">Publier le commentaire</button></div>
                </form>` : ''}
            </section>`;
        const form = panneau.querySelector('#form-commentaire');
        form?.addEventListener('submit', async e => {
            e.preventDefault();
            if (validerFormulaire(form)) return;
            try {
                await pendant(form.querySelector('button'), () => api.post(`/anomalies/${id}/commentaires`, { contenu: form.contenu.value.trim() }));
                toast('Commentaire ajouté.', 'succes');
                a.nombreCommentaires++;
                majCompteur('commentaires', a.nombreCommentaires);
                await commentaires(panneau);
            } catch (erreur) {
                bandeau(form, erreur.message);
            }
        });
    }

    // ------------------------------------------------------------------ Onglet Pièces jointes

    async function piecesJointes(panneau) {
        const liste = await api.get(`/anomalies/${id}/pieces-jointes`);
        panneau.innerHTML = `
            <section class="carte">
                <h2>Pièces jointes</h2>
                ${liste.length ? `<ul class="liste-fichiers">${liste.map(p => `
                    <li><span class="nom">${icone('trombone')} ${esc(p.nomFichier)}</span>
                        <span class="aide">Ajouté par ${esc(p.ajoutePar.nomComplet)} le ${dateHeure(p.dateAjout)}</span>
                        <button class="btn btn-secondaire btn-petit" data-pj="${p.id}" data-nom="${esc(p.nomFichier)}">${icone('telecharger')} Télécharger</button></li>`).join('')}</ul>`
                    : '<p class="etat-vide">Aucune pièce jointe.</p>'}
                ${a.actions.joindreFichier ? `
                <form id="form-pj" novalidate>
                    <div class="bandeau-formulaire"></div>
                    <div class="champ"><label for="fichier-pj">Joindre un fichier</label>
                        <input class="input" type="file" id="fichier-pj" name="fichier" accept="${FORMATS}" data-requis="Choisissez un fichier.">
                        <span class="aide">10 Mo maximum · PDF, images, journaux (.log, .txt), CSV, JSON, XML, Office, ZIP</span><span class="erreur-champ"></span></div>
                    <div class="actions-formulaire"><button class="btn btn-primaire" type="submit">${icone('trombone')} Ajouter</button></div>
                </form>` : ''}
            </section>`;
        panneau.querySelectorAll('[data-pj]').forEach(b => b.addEventListener('click', () =>
            api.telecharger(`/pieces-jointes/${b.dataset.pj}/telechargement`, b.dataset.nom).catch(e => toast(e.message, 'erreur'))));
        const form = panneau.querySelector('#form-pj');
        form?.addEventListener('submit', async e => {
            e.preventDefault();
            if (validerFormulaire(form)) return;
            const fichier = form.fichier.files[0];
            if (fichier.size > 10 * 1024 * 1024) return bandeau(form, 'Le fichier dépasse la taille maximale autorisée (10 Mo).');
            try {
                await pendant(form.querySelector('button'), () => api.envoyerFichier(`/anomalies/${id}/pieces-jointes`, fichier), 'Envoi…');
                toast(`Fichier « ${fichier.name} » ajouté.`, 'succes');
                a.nombrePiecesJointes++;
                a.nombreEvenements++;
                majCompteur('pieces-jointes', a.nombrePiecesJointes);
                majCompteur('historique', a.nombreEvenements);
                await piecesJointes(panneau);
            } catch (erreur) {
                bandeau(form, erreur.message);
            }
        });
    }

    // ------------------------------------------------------------------ Onglet Historique

    async function historique(panneau) {
        const liste = await api.get(`/anomalies/${id}/historique`);
        const detail = h => {
            const morceaux = [];
            if (h.ancienStatut || h.nouveauStatut) morceaux.push(h.ancienStatut ? `Statut : ${esc(h.ancienStatut)} → ${esc(h.nouveauStatut)}` : `Statut initial : ${esc(h.nouveauStatut)}`);
            if (h.anciennePriorite || h.nouvellePriorite) morceaux.push(h.anciennePriorite ? `Priorité : ${esc(h.anciennePriorite)} → ${esc(h.nouvellePriorite)}` : `Priorité : ${esc(h.nouvellePriorite)}`);
            return morceaux.join(' · ');
        };
        const pictogramme = h => /statut|Réouverture/i.test(h.action) ? 'horloge' : /ffectation|charge/i.test(h.action) ? 'utilisateur'
            : /priorit/i.test(h.action) ? 'cible' : /pièce/i.test(h.action) ? 'trombone' : /Création/.test(h.action) ? 'plus' : 'crayon';
        panneau.innerHTML = `
            <section class="carte">
                <div class="carte-entete"><h2>Historique des modifications</h2>
                    <button class="btn btn-secondaire btn-petit" id="retour-fiche">${icone('flecheRetour')} Retour à la fiche</button></div>
                <ul class="fil">${liste.map(h => `
                    <li><span class="pastille">${icone(pictogramme(h))}</span>
                        <div><strong>${esc(h.action)}</strong>${detail(h) ? `<div>${detail(h)}</div>` : ''}
                        <div class="detail">${dateHeure(h.dateModification)} · ${esc(h.utilisateur?.nomComplet ?? h.utilisateur)}</div></div></li>`).join('')}</ul>
                <p class="aide">Chaque modification est tracée avec son auteur, sa date et l'état précédent.</p>
            </section>`;
        panneau.querySelector('#retour-fiche').addEventListener('click', () => changerOnglet('informations'));
    }

    // ------------------------------------------------------------------ Actions

    async function modalAffectation() {
        const operateurs = await api.get('/utilisateurs/operateurs');
        const { element, fermer } = ouvrirModal({
            titre: `Affecter l'anomalie ${reference(a.id)}`,
            sousTitre: `${esc(a.titre)} · Système : ${esc(a.systeme.nom)} · Priorité : ${esc(a.priorite.nom)}`,
            pastille: { type: 'info', icone: 'utilisateur' },
            corps: `<form id="form-affectation" novalidate><div class="bandeau-formulaire"></div>
                <div class="champ"><label for="op">Opérateur <span class="requis">*</span></label>
                    <select class="input" id="op" name="idOperateur" data-requis="Choisissez un opérateur.">
                        <option value="">Sélectionner…</option>
                        ${operateurs.filter(o => !a.operateur || o.id !== a.operateur.id).map(o => `<option value="${o.id}">${esc(o.nomComplet)} (${esc(o.role)})</option>`).join('')}
                    </select><span class="erreur-champ"></span></div>
                <div class="champ"><label for="com-aff">Commentaire d'affectation (facultatif)</label>
                    <textarea class="input" id="com-aff" name="commentaire" maxlength="2000" style="min-height:80px" placeholder="Ex. : prise en charge immédiate"></textarea></div>
            </form>`,
            pied: '<button class="btn btn-secondaire" data-fermer>Annuler</button><button class="btn btn-primaire" data-ok>Affecter</button>'
        });
        const form = element.querySelector('form');
        const valider = async () => {
            if (validerFormulaire(form)) return;
            try {
                const ancien = a.statut.nom;
                a = await pendant(element.querySelector('[data-ok]'), () => api.patch(`/anomalies/${id}/affectation`,
                    { idOperateur: Number(form.idOperateur.value), commentaire: form.commentaire.value.trim() || null }));
                fermer();
                afficher();
                confirmationAffectation(ancien);
            } catch (erreur) {
                bandeau(form, erreur.message);
            }
        };
        element.querySelector('[data-ok]').addEventListener('click', valider);
        form.addEventListener('submit', e => { e.preventDefault(); valider(); });
    }

    function confirmationAffectation(statut) {
        const { element } = ouvrirModal({
            titre: 'Anomalie affectée',
            pastille: { type: 'succes', icone: 'coche' },
            corps: `<p>L'anomalie ${reference(a.id)} est désormais affectée à <strong>${esc(a.operateur.nomComplet)}</strong> (Opérateur).</p>
                <p class="aide">Le statut reste « ${esc(statut)} » tant qu'il n'a pas été modifié. L'affectation a été enregistrée dans l'historique et l'opérateur a été notifié.</p>`,
            pied: `<button class="btn btn-secondaire" data-fermer>Retour à la fiche</button>
                   ${a.actions.changerStatut ? '<button class="btn btn-primaire" data-statut>Changer le statut</button>' : ''}`
        });
        element.querySelector('[data-statut]')?.addEventListener('click', modalStatut);
    }

    async function prendreEnCharge() {
        const ok = await confirmer({ titre: `Prendre en charge ${reference(a.id)}`, message: 'Cette anomalie vous sera affectée. Le statut ne change pas tant que vous ne démarrez pas le traitement.', libelle: 'Prendre en charge' });
        if (!ok) return;
        try {
            a = await api.patch(`/anomalies/${id}/affectation`, { idOperateur: session.utilisateur().id });
            toast(`Anomalie ${reference(a.id)} prise en charge.`, 'succes');
            afficher();
        } catch (e) {
            toast(e.message, 'erreur');
        }
    }

    async function modalStatut() {
        const { statuts } = await chargerReferentiels();
        const possibles = new Set(a.actions.statutsPossibles.map(s => s.id));
        const { element, fermer } = ouvrirModal({
            titre: `Changer le statut de ${reference(a.id)}`,
            sousTitre: `Statut actuel : ${esc(a.statut.nom)} · Sélectionnez le nouveau statut.`,
            pastille: { type: 'info', icone: 'horloge' },
            corps: `<form id="form-statut" novalidate><div class="bandeau-formulaire"></div>
                <fieldset class="choix-statut" style="border:0;padding:0;margin:0 0 1rem"><legend class="sr-only">Nouveau statut</legend>
                ${statuts.map(s => {
                    const dispo = possibles.has(s.id);
                    const actuel = s.id === a.statut.id;
                    return `<label class="${dispo ? '' : 'indisponible'}"><input type="radio" name="idStatut" value="${s.id}" ${dispo ? '' : 'disabled'}>
                        <span>${esc(s.nom)}</span>${actuel ? '<span class="aide">(statut actuel)</span>' : dispo ? '' : '<span class="aide">(transition non autorisée)</span>'}</label>`;
                }).join('')}</fieldset>
                <div class="champ"><label for="com-statut">Commentaire (facultatif)</label>
                    <textarea class="input" id="com-statut" name="commentaire" maxlength="2000" style="min-height:80px"></textarea>
                    <span class="aide">Le changement est enregistré dans l'historique avec l'auteur et la date.</span></div>
            </form>`,
            pied: '<button class="btn btn-secondaire" data-fermer>Annuler</button><button class="btn btn-primaire" data-ok>Enregistrer</button>'
        });
        const form = element.querySelector('form');
        element.querySelector('[data-ok]').addEventListener('click', async () => {
            const choix = form.querySelector('[name=idStatut]:checked');
            if (!choix) return bandeau(form, 'Sélectionnez le nouveau statut.');
            try {
                const ancien = a.statut.nom;
                a = await pendant(element.querySelector('[data-ok]'), () => api.patch(`/anomalies/${id}/statut`,
                    { idStatut: Number(choix.value), commentaire: form.commentaire.value.trim() || null }));
                fermer();
                afficher();
                window.dispatchEvent(new Event('trackit:notifications'));
                confirmationStatut(ancien);
            } catch (erreur) {
                bandeau(form, erreur.message);
            }
        });
    }

    function confirmationStatut(ancien) {
        const { element } = ouvrirModal({
            titre: 'Statut mis à jour',
            pastille: { type: 'succes', icone: 'coche' },
            corps: `<p>L'anomalie ${reference(a.id)} est passée du statut « ${esc(ancien)} » au statut « <strong>${esc(a.statut.nom)}</strong> ».</p>
                <p class="aide">Modification enregistrée le ${dateHeure(a.derniereMiseAJour)} par ${esc(session.utilisateur().prenom)} ${esc(session.utilisateur().nom)}.
                Le déclarant a été notifié du changement.</p>`,
            pied: '<button class="btn btn-secondaire" data-fermer>Retour à la fiche</button><button class="btn btn-primaire" data-historique>Voir l\'historique</button>'
        });
        element.querySelector('[data-historique]').addEventListener('click', () => { fermerModal(); changerOnglet('historique'); });
    }

    async function modalPriorite() {
        const { priorites } = await chargerReferentiels();
        const { element, fermer } = ouvrirModal({
            titre: `Changer la priorité de ${reference(a.id)}`,
            sousTitre: `Priorité actuelle : ${esc(a.priorite.nom)}`,
            pastille: { type: 'info', icone: 'cible' },
            corps: `<form novalidate><div class="bandeau-formulaire"></div>
                <div class="champ"><label for="prio">Nouvelle priorité <span class="requis">*</span></label>
                    <select class="input" id="prio" name="idPriorite" data-requis="Choisissez une priorité."><option value="">Sélectionner…</option>
                    ${[...priorites].reverse().filter(p => p.id !== a.priorite.id).map(p => `<option value="${p.id}">${esc(p.nom)}</option>`).join('')}</select>
                    <span class="erreur-champ"></span></div>
                <div class="champ"><label for="com-prio">Justification (facultatif)</label>
                    <textarea class="input" id="com-prio" name="commentaire" maxlength="2000" style="min-height:80px"></textarea></div></form>`,
            pied: '<button class="btn btn-secondaire" data-fermer>Annuler</button><button class="btn btn-primaire" data-ok>Enregistrer</button>'
        });
        const form = element.querySelector('form');
        element.querySelector('[data-ok]').addEventListener('click', async () => {
            if (validerFormulaire(form)) return;
            try {
                a = await pendant(element.querySelector('[data-ok]'), () => api.patch(`/anomalies/${id}/priorite`,
                    { idPriorite: Number(form.idPriorite.value), commentaire: form.commentaire.value.trim() || null }));
                fermer();
                toast(`Priorité passée à « ${a.priorite.nom} ».`, 'succes');
                afficher();
            } catch (erreur) {
                bandeau(form, erreur.message);
            }
        });
    }

    async function supprimer() {
        const ok = await confirmer({
            titre: `Supprimer l'anomalie ${reference(a.id)} ?`,
            message: 'Cette action est définitive : les commentaires, pièces jointes et l\'historique de l\'anomalie seront également supprimés.',
            libelle: 'Supprimer définitivement', danger: true
        });
        if (!ok) return;
        try {
            await api.supprimer('/anomalies/' + id);
            toast(`Anomalie ${reference(a.id)} supprimée.`, 'succes');
            naviguer('/anomalies');
        } catch (e) {
            toast(e.message, 'erreur');
        }
    }

    function majCompteur(cle, valeur) {
        const b = contenu.querySelector(`[data-onglet="${cle}"]`);
        if (b) b.textContent = `${ONGLETS.find(o => o[0] === cle)[1]} (${valeur})`;
    }

    try {
        await recharger();
    } catch (e) {
        contenu.innerHTML = `<nav class="fil-ariane"><a href="#/anomalies">Anomalies</a></nav>`;
        const zone = document.createElement('div');
        contenu.appendChild(zone);
        etatErreur(zone, e);
    }
}

function initialesNom(nomComplet) {
    const morceaux = (nomComplet || '').split(' ');
    return initiales(morceaux[0], morceaux[morceaux.length - 1]);
}