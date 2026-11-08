// Déclaration (écrans 6 à 9 du prototype) et modification d'une anomalie.
import { api } from '../api.js';
import { icone } from '../icones.js';
import { esc, reference, champ, validerFormulaire, lireFormulaire, afficherErreurs, bandeau, pendant, ouvrirModal, toast } from '../ui.js';
import { naviguer } from '../routeur.js';
import { chargerReferentiels } from '../etat.js';

const FORMATS = '.pdf,.png,.jpg,.jpeg,.gif,.txt,.log,.csv,.json,.xml,.docx,.xlsx,.zip';
const TAILLE_MAX = 10 * 1024 * 1024;

export default async function formulaireAnomalie(contenu, { params }) {
    const edition = !!params.id;
    const ref = await chargerReferentiels(true);
    const anomalie = edition ? await api.get('/anomalies/' + params.id) : null;
    if (edition && !anomalie.actions.modifier) {
        toast("Vous n'êtes pas autorisé à modifier cette anomalie.", 'erreur');
        return naviguer('/anomalies/' + params.id, { remplacer: true });
    }
    document.title = (edition ? 'Modifier ' + reference(anomalie.id) : 'Nouvelle anomalie') + ' — TrackIT';

    const systemes = ref.systemes.filter(s => s.actif || (anomalie && s.id === anomalie.systeme.id));
    const opt = (liste, libelle = x => x.nom) => liste.map(x => ({ valeur: x.id, libelle: libelle(x) }));

    contenu.innerHTML = `
        <nav class="fil-ariane" aria-label="Fil d'Ariane"><a href="#/anomalies">Anomalies</a> ›
            ${edition ? `<a href="#/anomalies/${anomalie.id}">${reference(anomalie.id)}</a> › Modifier` : 'Nouvelle anomalie'}</nav>
        <div class="entete-page"><div>
            <h1>${edition ? 'Modifier l\'anomalie ' + reference(anomalie.id) : 'Nouvelle anomalie'}</h1>
            <p>Les champs suivis d'un astérisque (*) sont obligatoires.</p></div></div>
        <form class="carte" id="form-anomalie" novalidate style="max-width:860px">
            <div class="bandeau-formulaire"></div>
            ${champ({ nom: 'titre', libelle: 'Titre', requis: 'Le titre est obligatoire.', valeur: anomalie?.titre || '', placeholder: 'Ex. : Application indisponible', attributs: 'maxlength="200"' })}
            ${champ({ nom: 'description', libelle: 'Description', type: 'textarea', requis: 'La description est obligatoire.', valeur: anomalie?.description || '', placeholder: 'Décrivez le problème constaté', attributs: 'maxlength="10000"' })}
            <div class="grille-2">
                ${champ({ nom: 'idSysteme', libelle: 'Système concerné', requis: 'Le système concerné est obligatoire.', options: opt(systemes), valeur: anomalie?.systeme.id || '' })}
                ${champ({ nom: 'idType', libelle: "Type d'anomalie", requis: "Le type d'anomalie est obligatoire.", options: opt(ref.types), valeur: anomalie?.type.id || '' })}
            </div>
            <div class="grille-2">
                ${champ({ nom: 'idPriorite', libelle: 'Priorité', requis: 'La priorité est obligatoire.', options: opt([...ref.priorites].reverse()), valeur: anomalie?.priorite.id || '' })}
                ${edition ? '' : `<div class="champ"><label for="champ-fichier">Pièce jointe</label>
                    <input class="input" id="champ-fichier" name="fichier" type="file" accept="${FORMATS}">
                    <span class="aide">Facultatif · 10 Mo maximum · PDF, images, journaux, CSV, Office, ZIP</span><span class="erreur-champ"></span></div>`}
            </div>
            ${systemes.length ? '' : '<div class="bandeau bandeau-attention">Aucun système IT actif : un administrateur doit d\'abord en créer un.</div>'}
            <div class="actions-formulaire">
                <a class="btn btn-secondaire" href="#${edition ? '/anomalies/' + anomalie.id : '/anomalies'}">Annuler</a>
                <button class="btn btn-primaire" type="submit">${edition ? 'Enregistrer les modifications' : 'Enregistrer'}</button>
            </div>
        </form>`;

    const form = contenu.querySelector('#form-anomalie');
    const fichierInput = form.querySelector('[name=fichier]');

    form.addEventListener('submit', async e => {
        e.preventDefault();
        const nbErreurs = validerFormulaire(form);
        const fichier = fichierInput?.files[0];
        if (fichier && fichier.size > TAILLE_MAX) {
            afficherErreurs(form, { fichier: 'Le fichier dépasse 10 Mo.' });
            bandeau(form, 'La pièce jointe est trop volumineuse.');
            return;
        }
        if (nbErreurs) {
            bandeau(form, `${nbErreurs} champ${nbErreurs > 1 ? 's' : ''} obligatoire${nbErreurs > 1 ? 's ne sont' : ' n\'est'} pas renseigné${nbErreurs > 1 ? 's' : ''}. L'anomalie n'a pas été enregistrée.`);
            return;
        }
        const v = lireFormulaire(form);
        const corps = { titre: v.titre, description: v.description, idSysteme: Number(v.idSysteme), idType: Number(v.idType), idPriorite: Number(v.idPriorite) };
        const bouton = form.querySelector('button[type=submit]');
        try {
            if (edition) {
                await pendant(bouton, () => api.put('/anomalies/' + anomalie.id, corps));
                toast('Modifications enregistrées et tracées dans l\'historique.', 'succes');
                return naviguer('/anomalies/' + anomalie.id);
            }
            const cree = await pendant(bouton, () => api.post('/anomalies', corps));
            let avertissement = '';
            if (fichier) {
                try {
                    await api.envoyerFichier(`/anomalies/${cree.id}/pieces-jointes`, fichier);
                } catch (erreurFichier) {
                    avertissement = `<div class="bandeau bandeau-attention">La pièce jointe n'a pas pu être ajoutée : ${esc(erreurFichier.message)}</div>`;
                }
            }
            window.dispatchEvent(new Event('trackit:notifications'));
            confirmation(cree, avertissement);
        } catch (erreur) {
            afficherErreurs(form, erreur.champs);
            bandeau(form, erreur.message);
        }
    });
}

function confirmation(a, avertissement) {
    const { element } = ouvrirModal({
        titre: 'Anomalie créée',
        pastille: { type: 'succes', icone: 'coche' },
        corps: `<p>L'anomalie <strong>${reference(a.id)}</strong> « ${esc(a.titre)} » a été enregistrée.</p>
            <p class="aide">Statut initial : ${esc(a.statut.nom)} · Priorité : ${esc(a.priorite.nom)} · Système : ${esc(a.systeme.nom)}</p>
            <p class="aide">${icone('cloche')} Un accusé de réception a été envoyé au déclarant et le responsable technique a été notifié.</p>
            ${avertissement}`,
        pied: `<button class="btn btn-secondaire" data-action="liste">Retour à la liste</button>
               <button class="btn btn-primaire" data-action="fiche">Voir la fiche</button>`
    });
    element.querySelector('[data-action=liste]').addEventListener('click', () => naviguer('/anomalies'));
    element.querySelector('[data-action=fiche]').addEventListener('click', () => naviguer('/anomalies/' + a.id));
}
