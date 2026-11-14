// Systèmes IT de l'organisation : consultation pour tous, gestion par l'administrateur.
import { api } from '../api.js';
import { session } from '../session.js';
import { icone } from '../icones.js';
import { esc, champ, ouvrirModal, confirmer, toast, validerFormulaire, lireFormulaire, erreurFormulaire, pendant, etatErreur } from '../ui.js';
import { chargerReferentiels } from '../etat.js';

export default async function systemes(contenu) {
    document.title = 'Systèmes IT — TrackIT';
    const admin = session.aLeRole('ADMINISTRATEUR');
    contenu.innerHTML = `
        <div class="entete-page"><div><h1>Systèmes IT</h1><p>Systèmes informatiques suivis par votre organisation</p></div>
            ${admin ? `<button class="btn btn-primaire" id="nouveau">${icone('plus')} Nouveau système</button>` : ''}</div>
        <section class="carte"><div id="liste"></div></section>`;
    const liste = contenu.querySelector('#liste');
    contenu.querySelector('#nouveau')?.addEventListener('click', () => editer(null));

    async function charger() {
        try {
            const s = await api.get('/systemes');
            liste.innerHTML = s.length ? `<div class="tableau-conteneur"><table class="tableau">
                <thead><tr><th>Nom</th><th>Description</th><th>Adresse / identifiant</th><th>État</th><th>Anomalies</th>${admin ? '<th>Actions</th>' : ''}</tr></thead>
                <tbody>${s.map(x => `<tr><td class="titre-cellule">${icone('serveur')} ${esc(x.nom)}</td><td>${esc(x.description || '—')}</td>
                    <td><code>${esc(x.adresse || '—')}</code></td>
                    <td>${x.actif ? '<span class="badge badge-ok">Actif</span>' : '<span class="badge badge-fermee">Inactif</span>'}</td>
                    <td><a href="#/anomalies?idSysteme=${x.id}">Voir</a></td>
                    ${admin ? `<td style="white-space:nowrap"><button class="btn btn-secondaire btn-petit" data-editer="${x.id}">${icone('crayon')} Modifier</button>
                        <button class="btn btn-secondaire btn-petit" data-supprimer="${x.id}" aria-label="Supprimer ${esc(x.nom)}">${icone('poubelle')}</button></td>` : ''}</tr>`).join('')}</tbody>
            </table></div>` : `<p class="etat-vide">Aucun système IT.${admin ? ' Créez le premier système pour permettre la déclaration d\'anomalies.' : ''}</p>`;
            liste.querySelectorAll('[data-editer]').forEach(b => b.addEventListener('click', () => editer(s.find(x => String(x.id) === b.dataset.editer))));
            liste.querySelectorAll('[data-supprimer]').forEach(b => b.addEventListener('click', () => supprimer(s.find(x => String(x.id) === b.dataset.supprimer))));
        } catch (e) {
            etatErreur(liste, e);
        }
    }

    function editer(x) {
        const { element, fermer } = ouvrirModal({
            titre: x ? `Modifier « ${esc(x.nom)} »` : 'Nouveau système IT',
            corps: `<form novalidate><div class="bandeau-formulaire"></div>
                ${champ({ nom: 'nom', libelle: 'Nom du système', requis: true, valeur: x?.nom || '', attributs: 'maxlength="150"' })}
                ${champ({ nom: 'description', libelle: 'Description', type: 'textarea', valeur: x?.description || '', attributs: 'maxlength="2000" style="min-height:80px"' })}
                ${champ({ nom: 'adresse', libelle: 'Adresse ou identifiant technique', valeur: x?.adresse || '', placeholder: 'Ex. : erp.entreprise.local', attributs: 'maxlength="255"' })}
                <label class="case"><input type="checkbox" name="actif" ${!x || x.actif ? 'checked' : ''}> Système actif (proposé lors de la déclaration)</label>
            </form>`,
            pied: `<button class="btn btn-secondaire" data-fermer>Annuler</button><button class="btn btn-primaire" data-ok>${x ? 'Enregistrer' : 'Créer'}</button>`
        });
        const form = element.querySelector('form');
        element.querySelector('[data-ok]').addEventListener('click', async () => {
            if (validerFormulaire(form)) return;
            const corps = { ...lireFormulaire(form), actif: form.actif.checked };
            try {
                await pendant(element.querySelector('[data-ok]'), () => x ? api.put('/systemes/' + x.id, corps) : api.post('/systemes', corps));
                fermer();
                toast(x ? 'Système mis à jour.' : `Système « ${corps.nom} » créé.`, 'succes');
                await chargerReferentiels(true);
                charger();
            } catch (erreur) {
                erreurFormulaire(form, erreur);
            }
        });
    }

    async function supprimer(x) {
        if (!(await confirmer({ titre: `Supprimer « ${esc(x.nom)} » ?`, message: 'La suppression est refusée si des anomalies référencent ce système : désactivez-le dans ce cas.', libelle: 'Supprimer', danger: true }))) return;
        try {
            await api.supprimer('/systemes/' + x.id);
            toast('Système supprimé.', 'succes');
            await chargerReferentiels(true);
            charger();
        } catch (e) {
            toast(e.message, 'erreur');
        }
    }

    await charger();
}
