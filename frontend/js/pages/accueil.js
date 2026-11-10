// Page d'accueil publique (maquette 01).
import { api } from '../api.js';
import { session } from '../session.js';
import { icone } from '../icones.js';
import { esc } from '../ui.js';
import { decimal, locale, selecteurLangue } from '../i18n.js';

const FORMAT_NOMBRE = new Intl.NumberFormat(locale());

export default async function accueil(app) {
    const connecte = session.estConnecte();
    document.title = 'TrackIT — Gestion des anomalies IT';
    app.innerHTML = `
    <div class="page-publique">
        <header class="entete-public">
            <a class="marque" href="#/"><img src="assets/logo.svg" alt="">TrackIT</a>
            <nav aria-label="Sections">
                <a href="#fonctionnalites" data-ancre>Fonctionnalités</a>
                <a href="#tarifs" data-ancre>Tarifs</a>
                <a href="#contact" data-ancre>Contact</a>
            </nav>
            <div class="droite">
                ${selecteurLangue()}
                ${connecte
                    ? '<a class="btn btn-primaire" href="#/tableau-de-bord">Accéder à mon espace</a>'
                    : '<a class="btn btn-secondaire" href="#/connexion">Se connecter</a><a class="btn btn-primaire" href="#/inscription">S\'inscrire</a>'}
            </div>
        </header>

        <section class="accroche">
            <div>
                <h1>Simplifiez la gestion de vos <em>anomalies IT</em></h1>
                <p>TrackIT est une solution simple et efficace pour déclarer, suivre et résoudre les anomalies informatiques au sein de votre organisation.</p>
                <div class="boutons">
                    <a class="btn btn-primaire" href="#/inscription">Commencer maintenant</a>
                    <a class="btn btn-secondaire" href="#/connexion?demo=1">Voir la démo</a>
                </div>
            </div>
            <div class="illustration" aria-hidden="true">
                <div class="ecran-portable"><div class="faux-ecran"><div class="faux-menu"></div>
                    <div class="faux-contenu"><div class="faux-kpis"><span></span><span></span><span></span><span></span></div><div class="faux-lignes"></div></div>
                </div></div>
                <div class="pastille-flottante" style="top:-14px;right:22%"><span class="fond-bleu">${icone('plus')}</span>Déclarer</div>
                <div class="pastille-flottante" style="top:40%;right:-6px"><span class="fond-bleu">${icone('liste')}</span>Suivre</div>
                <div class="pastille-flottante" style="bottom:-18px;right:20%"><span class="fond-vert">${icone('coche')}</span>Résoudre</div>
            </div>
        </section>

        <section class="chiffres" id="chiffres" aria-label="Chiffres clés">
            <div><strong>…</strong><span>Organisations</span></div>
            <div><strong>…</strong><span>Anomalies traitées</span></div>
            <div><strong>…</strong><span>Taux de résolution</span></div>
            <div><strong>…</strong><span>Systèmes suivis</span></div>
        </section>

        <section class="section-publique" id="fonctionnalites">
            <h2>Fonctionnalités</h2>
            <div class="fonctionnalites">
                ${[
                    ['plus', 'Déclarer', 'Formulaire guidé : titre, description, système, type et priorité, avec pièces jointes.'],
                    ['liste', 'Suivre', 'Recherche, filtres cumulables, affectation aux opérateurs et historique complet.'],
                    ['coche', 'Résoudre', 'Cycle de vie clair : Nouvelle, En cours, Résolue, Fermée, avec notifications.'],
                    ['statistiques', 'Analyser', 'Tableau de bord, statistiques, incidents récurrents et rapports PDF / Excel.']
                ].map(([i, t, d]) => `<div class="carte"><div class="kpi-icone fond-bleu" style="margin-bottom:.8rem">${icone(i)}</div><h3>${t}</h3><p class="aide" style="font-size:.92rem">${d}</p></div>`).join('')}
            </div>
        </section>

        <section class="section-publique" id="tarifs">
            <h2>Tarifs</h2>
            <div class="offres" id="offres">
                <div class="offre"><h3>Gratuit</h3><div class="prix">0 €</div>
                    <ul><li>Déclaration et suivi des anomalies</li><li>Tableau de bord et statistiques</li><li>Rôles et historique</li></ul>
                    <a class="btn btn-secondaire" href="#/inscription">Créer mon organisation</a></div>
            </div>
        </section>

        <section class="section-publique" id="contact">
            <h2>Contact</h2>
            <p style="text-align:center">Une question sur TrackIT ? Écrivez-nous à <a href="mailto:contact@trackit.demo">contact@trackit.demo</a>.</p>
        </section>
        <footer class="pied-public">TrackIT — projet de fin d'études · Gestion, suivi et traitement des anomalies informatiques</footer>
    </div>`;

    app.querySelectorAll('[data-ancre]').forEach(a => a.addEventListener('click', e => {
        e.preventDefault();
        document.querySelector(a.getAttribute('href')).scrollIntoView({ behavior: 'smooth' });
    }));

    api.get('/public/chiffres').then(c => {
        const valeurs = [
            '+' + FORMAT_NOMBRE.format(c.organisations),
            FORMAT_NOMBRE.format(c.anomaliesTraitees),
            c.tauxResolutionPourcent == null ? '—' : decimal(c.tauxResolutionPourcent) + ' %',
            FORMAT_NOMBRE.format(c.systemesSuivis)
        ];
        app.querySelectorAll('#chiffres strong').forEach((el, i) => { el.textContent = valeurs[i]; });
    }).catch(() => app.querySelector('#chiffres').remove());

    api.get('/abonnements/offres').then(offres => {
        app.querySelector('#offres').insertAdjacentHTML('beforeend', offres.map((o, i) => `
            <div class="offre ${i === 1 ? 'mise-en-avant' : ''}"><h3>${esc(o.libelle)}</h3>
                <div class="prix">${esc(decimal(o.montant))} ${esc(o.devise === 'EUR' ? '€' : o.devise)}
                    <small class="aide">/ ${o.dureeMois === 1 ? 'mois' : o.dureeMois + ' mois'}</small></div>
                <ul><li>Toute l'offre gratuite</li><li>Export des rapports PDF et Excel</li><li>Historique des rapports générés</li></ul>
                <a class="btn btn-primaire" href="#/inscription">Choisir cette offre</a></div>`).join(''));
    }).catch(() => {});
}