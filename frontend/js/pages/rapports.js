// Rapports (UC-03 « Générer rapport », « Exporter PDF / Excel ») — fonctionnalité Premium.
import { api } from '../api.js';
import { icone } from '../icones.js';
import { esc, dateHeure, champ, validerFormulaire, lireFormulaire, erreurFormulaire, pendant, toast, pagination, isoJour, etatErreur } from '../ui.js';
import { chargerPremium } from '../etat.js';
import { locale, t } from '../i18n.js';

export default async function rapports(contenu) {
    document.title = 'Rapports — TrackIT';
    const premium = await chargerPremium();
    const aujourdHui = new Date();
    const debutMois = new Date(aujourdHui.getFullYear(), aujourdHui.getMonth() - 1, 1);
    const finMois = new Date(aujourdHui.getFullYear(), aujourdHui.getMonth(), 0);
    const nomMois = debutMois.toLocaleDateString(locale(), { month: 'long', year: 'numeric' });

    contenu.innerHTML = `
        <div class="entete-page"><div><h1>Rapports</h1><p>Générez et exportez des rapports PDF ou Excel à partir des données de votre organisation</p></div></div>
        ${premium.premiumActif ? '' : `<div class="bandeau bandeau-attention">${icone('bouclier')}<span>L'export des rapports est une fonctionnalité <strong>Premium</strong>.
            Demandez à votre administrateur de souscrire une offre depuis la page <a href="#/premium">Abonnement Premium</a>.</span></div>`}
        <div class="grille-2-cartes" style="grid-template-columns:minmax(0,1fr) minmax(0,1.4fr)">
            <form class="carte" id="form-rapport" novalidate>
                <h2>Nouveau rapport</h2>
                <div class="bandeau-formulaire"></div>
                ${champ({ nom: 'titre', libelle: 'Titre du rapport', requis: 'Le titre est obligatoire.', valeur: t('Bilan des anomalies –') + ' ' + nomMois, attributs: 'maxlength="200"' })}
                ${champ({ nom: 'type', libelle: 'Type de rapport', requis: 'Le type est obligatoire.', valeur: 'SYNTHESE',
                    options: [{ valeur: 'SYNTHESE', libelle: 'Synthèse statistique' }, { valeur: 'LISTE_ANOMALIES', libelle: 'Liste détaillée des anomalies' }] })}
                ${champ({ nom: 'format', libelle: 'Format', requis: 'Le format est obligatoire.', valeur: 'PDF',
                    options: [{ valeur: 'PDF', libelle: 'PDF' }, { valeur: 'EXCEL', libelle: 'Excel (.xlsx)' }] })}
                <div class="grille-2">
                    ${champ({ nom: 'dateDebut', libelle: 'Du', type: 'date', requis: 'La date de début est obligatoire.', valeur: isoJour(debutMois) })}
                    ${champ({ nom: 'dateFin', libelle: 'Au', type: 'date', requis: 'La date de fin est obligatoire.', valeur: isoJour(finMois) })}
                </div>
                <div class="actions-formulaire"><button class="btn btn-primaire" type="submit" ${premium.premiumActif ? '' : 'disabled'}>${icone('rapport')} Générer et télécharger</button></div>
            </form>
            <section class="carte"><h2>Rapports générés</h2><div id="liste"></div></section>
        </div>`;

    const form = contenu.querySelector('#form-rapport');
    const liste = contenu.querySelector('#liste');
    let page = 0;

    async function chargerListe() {
        try {
            const p = await api.get('/rapports', { page, taille: 8 });
            if (!p.contenu.length) {
                liste.innerHTML = '<p class="etat-vide">Aucun rapport généré.</p>';
                return;
            }
            liste.innerHTML = `<div class="tableau-conteneur"><table class="tableau">
                <thead><tr><th>Titre</th><th>Type</th><th>Format</th><th>Généré le</th><th>Par</th><th></th></tr></thead>
                <tbody>${p.contenu.map(r => `<tr><td class="titre-cellule">${esc(r.titre)}</td><td>${esc(r.type)}</td>
                    <td><span class="badge badge-neutre">${esc(r.format)}</span></td><td>${dateHeure(r.dateGeneration)}</td><td>${esc(r.analyste.nomComplet)}</td>
                    <td>${r.fichierDisponible ? `<button class="btn btn-secondaire btn-petit" data-rapport="${r.id}">${icone('telecharger')} Télécharger</button>`
                        : '<span class="aide" title="Le fichier n\'a pas été conservé sur le serveur">Fichier non conservé</span>'}</td></tr>`).join('')}</tbody>
            </table></div><div class="pied-tableau"><span class="aide">${p.totalElements} rapport(s)</span><div id="pagination-rapports"></div></div>`;
            liste.querySelector('#pagination-rapports').replaceWith(pagination(p.page, p.totalPages, n => { page = n; chargerListe(); }));
            liste.querySelectorAll('[data-rapport]').forEach(b => b.addEventListener('click', () =>
                api.telecharger(`/rapports/${b.dataset.rapport}/fichier`, 'rapport').catch(e => toast(e.message, 'erreur'))));
        } catch (e) {
            etatErreur(liste, e);
        }
    }

    form.addEventListener('submit', async e => {
        e.preventDefault();
        if (validerFormulaire(form)) return;
        const v = lireFormulaire(form);
        if (v.dateFin < v.dateDebut) {
            return erreurFormulaire(form, { message: 'La période est invalide.', champs: { dateFin: 'La date de fin doit être postérieure ou égale à la date de début.' } });
        }
        try {
            const rapport = await pendant(form.querySelector('button[type=submit]'), () => api.post('/rapports', v), 'Génération…');
            toast(`Rapport « ${rapport.titre} » généré.`, 'succes');
            await api.telecharger(`/rapports/${rapport.id}/fichier`, 'rapport');
            page = 0;
            chargerListe();
        } catch (erreur) {
            erreurFormulaire(form, erreur);
        }
    });
    await chargerListe();
}
