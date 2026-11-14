// Référentiels (UC-04 : configurer types d'anomalie, priorités et statuts).
import { api } from '../api.js';
import { icone } from '../icones.js';
import { esc, champ, ouvrirModal, confirmer, toast, validerFormulaire, lireFormulaire, erreurFormulaire, pendant, etatErreur, badgePriorite } from '../ui.js';
import { chargerReferentiels } from '../etat.js';

const ONGLETS = { types: 'Types d\'anomalie', priorites: 'Priorités', statuts: 'Statuts' };

export default async function referentiels(contenu) {
    document.title = 'Référentiels — TrackIT';
    let onglet = 'types';
    contenu.innerHTML = `
        <div class="entete-page"><div><h1>Référentiels</h1><p>Valeurs utilisées pour classer les anomalies (communes à la plateforme)</p></div></div>
        <div class="onglets" role="tablist">${Object.entries(ONGLETS).map(([c, l]) => `<button role="tab" data-onglet="${c}" class="${c === onglet ? 'actif' : ''}">${l}</button>`).join('')}</div>
        <section class="carte" id="panneau"></section>`;
    const panneau = contenu.querySelector('#panneau');
    contenu.querySelectorAll('[data-onglet]').forEach(b => b.addEventListener('click', () => {
        onglet = b.dataset.onglet;
        contenu.querySelectorAll('[data-onglet]').forEach(x => x.classList.toggle('actif', x === b));
        charger();
    }));

    async function charger() {
        panneau.innerHTML = '<div class="chargement">Chargement…</div>';
        try {
            const elements = await api.get('/' + onglet);
            const estStatut = onglet === 'statuts';
            const estPriorite = onglet === 'priorites';
            panneau.innerHTML = `
                <div class="carte-entete"><h2>${ONGLETS[onglet]}</h2>
                    ${estStatut ? '' : `<button class="btn btn-primaire btn-petit" id="ajouter">${icone('plus')} Ajouter</button>`}</div>
                ${estStatut ? `<div class="bandeau bandeau-info">${icone('info')}<span>Les quatre statuts officiels (Nouvelle, En cours, Résolue, Fermée) sont fixes : seule leur description est modifiable.</span></div>` : ''}
                <div class="tableau-conteneur"><table class="tableau">
                    <thead><tr><th>Nom</th>${estPriorite ? '<th>Niveau</th>' : ''}<th>Description</th><th>Actions</th></tr></thead>
                    <tbody>${elements.map(x => `<tr><td class="titre-cellule">${estPriorite ? badgePriorite(x) : esc(x.nom)}</td>
                        ${estPriorite ? `<td>${x.niveau}</td>` : ''}<td>${esc(x.description || '—')}</td>
                        <td style="white-space:nowrap"><button class="btn btn-secondaire btn-petit" data-editer="${x.id}">${icone('crayon')} Modifier</button>
                        ${estStatut ? '' : `<button class="btn btn-secondaire btn-petit" data-supprimer="${x.id}" aria-label="Supprimer ${esc(x.nom)}">${icone('poubelle')}</button>`}</td></tr>`).join('')}</tbody>
                </table></div>`;
            panneau.querySelector('#ajouter')?.addEventListener('click', () => editer(null));
            panneau.querySelectorAll('[data-editer]').forEach(b => b.addEventListener('click', () => editer(elements.find(x => String(x.id) === b.dataset.editer))));
            panneau.querySelectorAll('[data-supprimer]').forEach(b => b.addEventListener('click', () => supprimer(elements.find(x => String(x.id) === b.dataset.supprimer))));
        } catch (e) {
            etatErreur(panneau, e);
        }
    }

    function editer(x) {
        const estStatut = onglet === 'statuts';
        const estPriorite = onglet === 'priorites';
        const { element, fermer } = ouvrirModal({
            titre: x ? `Modifier « ${esc(x.nom)} »` : 'Ajouter — ' + ONGLETS[onglet],
            corps: `<form novalidate><div class="bandeau-formulaire"></div>
                ${champ({ nom: 'nom', libelle: 'Nom', requis: !estStatut, valeur: x?.nom || '', attributs: (estStatut ? 'readonly ' : '') + `maxlength="${estPriorite ? 50 : 100}"` })}
                ${estPriorite ? champ({ nom: 'niveau', libelle: 'Niveau (1 = Basse … 4 = Critique)', type: 'number', requis: 'Le niveau est obligatoire.', valeur: x?.niveau || '', attributs: 'min="1" max="4"' }) : ''}
                ${champ({ nom: 'description', libelle: 'Description', valeur: x?.description || '', attributs: 'maxlength="255"' })}
            </form>`,
            pied: '<button class="btn btn-secondaire" data-fermer>Annuler</button><button class="btn btn-primaire" data-ok>Enregistrer</button>'
        });
        const form = element.querySelector('form');
        element.querySelector('[data-ok]').addEventListener('click', async () => {
            if (validerFormulaire(form)) return;
            const v = lireFormulaire(form);
            if (estPriorite && !(Number(v.niveau) >= 1 && Number(v.niveau) <= 4)) {
                return erreurFormulaire(form, { message: 'Niveau invalide.', champs: { niveau: 'Le niveau doit être compris entre 1 et 4.' } });
            }
            const corps = estStatut ? { description: v.description } : estPriorite ? { nom: v.nom, niveau: Number(v.niveau), description: v.description } : { nom: v.nom, description: v.description };
            try {
                await pendant(element.querySelector('[data-ok]'), () => x ? api.put(`/${onglet}/${x.id}`, corps) : api.post('/' + onglet, corps));
                fermer();
                toast('Référentiel mis à jour.', 'succes');
                await chargerReferentiels(true);
                charger();
            } catch (erreur) {
                erreurFormulaire(form, erreur);
            }
        });
    }

    async function supprimer(x) {
        if (!(await confirmer({ titre: `Supprimer « ${esc(x.nom)} » ?`, message: 'La suppression est refusée si des anomalies utilisent cette valeur.', libelle: 'Supprimer', danger: true }))) return;
        try {
            await api.supprimer(`/${onglet}/${x.id}`);
            toast('Valeur supprimée.', 'succes');
            await chargerReferentiels(true);
            charger();
        } catch (e) {
            toast(e.message, 'erreur');
        }
    }

    await charger();
}
