// Liste des anomalies (maquette 02) et « Mes tâches » : recherche, filtres cumulables, tri, pagination.
// Les filtres sont conservés dans l'URL pour être retrouvés au retour de la fiche.
import { api } from '../api.js';
import { session } from '../session.js';
import { icone } from '../icones.js';
import { esc, reference, date, badgePriorite, badgeStatut, pagination, activerDeroulants, etatErreur } from '../ui.js';
import { naviguer, majQuery } from '../routeur.js';
import { chargerReferentiels } from '../etat.js';
import { preferences } from '../preferences.js';
import { t, langue } from '../i18n.js';

const COLONNES = [
    ['id', '#'], ['titre', 'Titre'], ['systeme', 'Système'], ['type', 'Type'],
    ['priorite', 'Priorité'], ['statut', 'Statut'], ['dateCreation', 'Date création']
];
const FILTRES = ['q', 'idStatut', 'idPriorite', 'idType', 'idSysteme', 'idOperateur', 'nonAffectee', 'mesDeclarations', 'dateDebut', 'dateFin'];

export default async function anomalies(contenu, { query, route }) {
    const mesTaches = !!route.mesTaches;
    document.title = (mesTaches ? 'Mes tâches' : 'Anomalies') + ' — TrackIT';
    const ref = await chargerReferentiels();
    const supervision = session.aLeRole('RESPONSABLE_TECHNIQUE', 'OPERATEUR');
    const operateurs = supervision ? await api.get('/utilisateurs/operateurs').catch(() => []) : [];

    const etat = {
        page: Number(query.get('page') || 0),
        tri: query.get('tri') || 'dateCreation',
        sens: query.get('sens') || 'desc',
        filtres: Object.fromEntries(FILTRES.map(f => [f, query.get(f) || '']))
    };

    const options = (liste, valeur, libelle = x => x.nom) =>
        liste.map(x => `<option value="${x.id}" ${String(x.id) === valeur ? 'selected' : ''}>${esc(libelle(x))}</option>`).join('');

    contenu.innerHTML = `
        <div class="entete-page">
            <div><h1>${mesTaches ? 'Mes tâches' : 'Anomalies'}</h1>
                <p>${mesTaches ? 'Anomalies qui vous sont affectées' : 'Consultez, recherchez et filtrez les anomalies de votre organisation'}</p></div>
            ${session.aLeRole('OPERATEUR') ? `<a class="btn btn-primaire" href="#/anomalies/nouvelle">${icone('plus')} Nouvelle anomalie</a>` : ''}
        </div>
        <section class="carte">
            <form class="barre-filtres" id="filtres" role="search">
                <div class="input-icone recherche">${icone('recherche')}
                    <label class="sr-only" for="f-q">Rechercher</label>
                    <input class="input" id="f-q" name="q" placeholder="Rechercher une anomalie…" title="Recherche sur le titre, la description, le système ou le numéro (#012)" value="${esc(etat.filtres.q)}">
                </div>
                <label class="sr-only" for="f-statut">Statut</label>
                <select class="input" id="f-statut" name="idStatut"><option value="">Tous les statuts</option>${options(ref.statuts, etat.filtres.idStatut)}</select>
                <label class="sr-only" for="f-priorite">Priorité</label>
                <select class="input" id="f-priorite" name="idPriorite"><option value="">Toutes les priorités</option>${options([...ref.priorites].reverse(), etat.filtres.idPriorite)}</select>
                <label class="sr-only" for="f-type">Type</label>
                <select class="input" id="f-type" name="idType"><option value="">Tous les types</option>${options(ref.types, etat.filtres.idType)}</select>
                <button class="btn btn-secondaire" type="button" id="plus-filtres" aria-expanded="false">Plus de filtres</button>
                <button class="btn btn-secondaire" type="button" id="reinitialiser">Réinitialiser</button>
            </form>
            <div class="filtres-avances" id="filtres-avances" hidden>
                <label>Système <select class="input" name="idSysteme" form="filtres"><option value="">Tous</option>${options(ref.systemes, etat.filtres.idSysteme)}</select></label>
                ${supervision && !mesTaches ? `<label>Opérateur <select class="input" name="idOperateur" form="filtres"><option value="">Tous</option>${options(operateurs, etat.filtres.idOperateur, o => o.nomComplet)}</select></label>
                    <label class="case"><input type="checkbox" name="nonAffectee" form="filtres" value="true" ${etat.filtres.nonAffectee ? 'checked' : ''}> Non affectées</label>` : ''}
                ${session.aLeRole('OPERATEUR') && !mesTaches ? `<label class="case"><input type="checkbox" name="mesDeclarations" form="filtres" value="true" ${etat.filtres.mesDeclarations ? 'checked' : ''}> Mes déclarations</label>` : ''}
                <label>Du <input class="input" type="date" name="dateDebut" form="filtres" value="${esc(etat.filtres.dateDebut)}"></label>
                <label>au <input class="input" type="date" name="dateFin" form="filtres" value="${esc(etat.filtres.dateFin)}"></label>
            </div>
            <div class="resume-resultats" id="resume" aria-live="polite"></div>
            <div id="resultats"></div>
        </section>`;

    const form = contenu.querySelector('#filtres');
    const avances = contenu.querySelector('#filtres-avances');
    const resultats = contenu.querySelector('#resultats');
    const resume = contenu.querySelector('#resume');
    if (['idSysteme', 'idOperateur', 'nonAffectee', 'mesDeclarations', 'dateDebut', 'dateFin'].some(f => etat.filtres[f])) avances.hidden = false;

    contenu.querySelector('#plus-filtres').addEventListener('click', e => {
        avances.hidden = !avances.hidden;
        e.currentTarget.setAttribute('aria-expanded', String(!avances.hidden));
    });
    contenu.querySelector('#reinitialiser').addEventListener('click', () => naviguer(mesTaches ? '/mes-taches' : '/anomalies'));

    const lireFiltres = () => {
        const f = {};
        FILTRES.forEach(nom => {
            const el = contenu.querySelector(`[name="${nom}"]`);
            f[nom] = !el ? '' : el.type === 'checkbox' ? (el.checked ? 'true' : '') : el.value.trim();
        });
        return f;
    };
    let minuterie;
    const surFiltre = immediat => {
        clearTimeout(minuterie);
        minuterie = setTimeout(() => { etat.filtres = lireFiltres(); etat.page = 0; charger(); }, immediat ? 0 : 350);
    };
    form.addEventListener('submit', e => { e.preventDefault(); surFiltre(true); });
    contenu.querySelector('#f-q').addEventListener('input', () => surFiltre(false));
    contenu.querySelectorAll('select[name], input[type=checkbox][name], input[type=date][name]').forEach(el => el.addEventListener('change', () => surFiltre(true)));

    async function charger() {
        majQuery({ ...etat.filtres, page: etat.page || '', tri: etat.tri !== 'dateCreation' ? etat.tri : '', sens: etat.sens !== 'desc' ? etat.sens : '' });
        sessionStorage.setItem('trackit.retourListe', location.hash);
        resultats.innerHTML = '<div class="chargement">Recherche…</div>';
        try {
            const page = await api.get('/anomalies', {
                ...etat.filtres, affecteesAMoi: mesTaches ? 'true' : '',
                page: etat.page, taille: preferences.get().taille, tri: etat.tri, sens: etat.sens
            });
            if (page.contenu.length === 0 && etat.page > 0 && page.totalPages > 0) {
                etat.page = page.totalPages - 1;
                return charger();
            }
            afficher(page);
        } catch (e) {
            etatErreur(resultats, e);
        }
    }

    function decrireFiltres() {
        const actifs = [];
        const f = etat.filtres;
        const nom = (liste, id) => liste.find(x => String(x.id) === id)?.nom ?? '';
        if (f.idStatut) actifs.push(t('statut') + ' ' + t(nom(ref.statuts, f.idStatut)));
        if (f.idPriorite) actifs.push(t('priorité') + ' ' + t(nom(ref.priorites, f.idPriorite)));
        if (f.idType) actifs.push(t('type') + ' ' + t(nom(ref.types, f.idType)));
        if (f.idSysteme) actifs.push(t('système') + ' ' + nom(ref.systemes, f.idSysteme));
        if (f.idOperateur) actifs.push(t('opérateur') + ' ' + (operateurs.find(o => String(o.id) === f.idOperateur)?.nomComplet || ''));
        if (f.nonAffectee) actifs.push(t('non affectées'));
        if (f.mesDeclarations) actifs.push(t('mes déclarations'));
        if (f.dateDebut || f.dateFin) actifs.push(t('période') + ' ' + (f.dateDebut ? t('du') + ' ' + date(f.dateDebut) : '') + (f.dateFin ? ' ' + t('au') + ' ' + date(f.dateFin) : ''));
        return actifs;
    }

    function afficher(page) {
        const actifs = decrireFiltres();
        const q = etat.filtres.q;
        resume.innerHTML = (q || actifs.length)
            ? `<strong>${page.totalElements}</strong> ${t(page.totalElements > 1 ? 'résultats' : 'résultat')}${q ? ` ${t('pour')} « ${esc(q)} »` : ''}${actifs.length ? ' · ' + t(actifs.length > 1 ? 'filtres actifs' : 'filtre actif') + (langue() === 'fr' ? ' : ' : ': ') + esc(actifs.join(', ')) : ''}`
            : '';
        if (!page.contenu.length) {
            resultats.innerHTML = `<div class="etat-vide">${icone('recherche')}<p>Aucune anomalie ne correspond à ces critères.</p>
                ${q || actifs.length ? '<button class="btn btn-secondaire" id="vider">Réinitialiser les filtres</button>' : ''}</div>`;
            resultats.querySelector('#vider')?.addEventListener('click', () => naviguer(mesTaches ? '/mes-taches' : '/anomalies'));
            return;
        }
        const enTete = COLONNES.map(([cle, libelle]) => {
            const actif = etat.tri === cle;
            const aria = actif ? (etat.sens === 'asc' ? 'ascending' : 'descending') : 'none';
            return `<th aria-sort="${aria}"><button type="button" data-tri="${cle}">${libelle}${actif ? (etat.sens === 'asc' ? ' ▲' : ' ▼') : ''}</button></th>`;
        }).join('');
        const debut = page.page * page.taille + 1;
        const fin = debut + page.contenu.length - 1;
        resultats.innerHTML = `
            <div class="tableau-conteneur adaptatif"><table class="tableau">
                <thead><tr>${enTete}<th>Actions</th></tr></thead>
                <tbody>${page.contenu.map(a => `
                    <tr class="cliquable" data-id="${a.id}" tabindex="0">
                        <td class="num">${reference(a.id)}</td>
                        <td class="titre-cellule">${esc(a.titre)}${a.operateur ? `<br><small class="aide">Affectée à ${esc(a.operateur.nomComplet)}</small>` : ''}</td>
                        <td>${esc(a.systeme.nom)}</td><td>${esc(a.type.nom)}</td>
                        <td>${badgePriorite(a.priorite)}</td><td>${badgeStatut(a.statut)}</td><td>${date(a.dateCreation)}</td>
                        <td><div class="deroulant"><button class="btn-icone" data-basculer aria-label="Actions sur ${reference(a.id)}" aria-expanded="false">${icone('points')}</button>
                            <div class="menu-deroulant">
                                <a href="#/anomalies/${a.id}">${icone('oeil')} Voir la fiche</a>
                                <a href="#/anomalies/${a.id}?onglet=commentaires">${icone('commentaire')} Commentaires</a>
                                <a href="#/anomalies/${a.id}?onglet=historique">${icone('horloge')} Historique</a>
                            </div></div></td>
                    </tr>`).join('')}</tbody>
            </table></div>
            <div class="cartes-mobiles">${page.contenu.map(a => `
                <a class="carte-anomalie" href="#/anomalies/${a.id}">
                    <strong>${reference(a.id)} – ${esc(a.titre)}</strong>
                    <div class="ligne">${badgePriorite(a.priorite)} ${badgeStatut(a.statut)}</div>
                    <div class="ligne">${esc(a.systeme.nom)} · ${esc(a.type.nom)} · ${date(a.dateCreation)}</div>
                </a>`).join('')}</div>
            <div class="pied-tableau">
                <span class="aide">Affichage de ${debut} à ${fin} sur ${page.totalElements} anomalie${page.totalElements > 1 ? 's' : ''}</span>
                <div id="pagination"></div>
            </div>`;
        resultats.querySelector('#pagination').replaceWith(pagination(page.page, page.totalPages, p => { etat.page = p; charger(); }));
        activerDeroulants(resultats);
        resultats.querySelectorAll('tr[data-id]').forEach(tr => {
            const ouvrir = () => naviguer('/anomalies/' + tr.dataset.id);
            tr.addEventListener('click', e => { if (!e.target.closest('.deroulant')) ouvrir(); });
            tr.addEventListener('keydown', e => { if (e.key === 'Enter' && e.target === tr) ouvrir(); });
        });
        resultats.querySelectorAll('[data-tri]').forEach(b => b.addEventListener('click', () => {
            if (etat.tri === b.dataset.tri) etat.sens = etat.sens === 'asc' ? 'desc' : 'asc';
            else { etat.tri = b.dataset.tri; etat.sens = b.dataset.tri === 'titre' ? 'asc' : 'desc'; }
            etat.page = 0;
            charger();
        }));
    }

    await charger();
}
