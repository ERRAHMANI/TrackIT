// Utilitaires d'interface : échappement, formats, badges, notifications, modales, formulaires.
import { icone } from './icones.js';
import { locale, t } from './i18n.js';

export function esc(valeur) {
    return String(valeur ?? '').replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
}

export const LIBELLES_ROLES = {
    UTILISATEUR: 'Utilisateur', OPERATEUR: 'Opérateur', RESPONSABLE_TECHNIQUE: 'Responsable technique',
    ANALYSTE: 'Analyste', ADMINISTRATEUR: 'Administrateur'
};

export const reference = id => '#' + String(id).padStart(3, '0');

const FORMAT_DATE = new Intl.DateTimeFormat(locale(), { day: '2-digit', month: '2-digit', year: 'numeric' });
const FORMAT_HEURE = new Intl.DateTimeFormat(locale(), { hour: '2-digit', minute: '2-digit' });

export function date(valeur) {
    return valeur ? FORMAT_DATE.format(new Date(valeur)) : '—';
}

export function dateHeure(valeur) {
    if (!valeur) return '—';
    const d = new Date(valeur);
    return FORMAT_DATE.format(d) + ' ' + t('à') + ' ' + FORMAT_HEURE.format(d);
}

export function isoJour(d) {
    const z = n => String(n).padStart(2, '0');
    return `${d.getFullYear()}-${z(d.getMonth() + 1)}-${z(d.getDate())}`;
}

export function initiales(prenom, nom) {
    return ((prenom || '').charAt(0) + (nom || '').charAt(0)).toUpperCase() || '?';
}

const CLASSES_PRIORITE = { Critique: 'critique', Haute: 'haute', Moyenne: 'moyenne', Basse: 'basse' };
const CLASSES_STATUT = { NOUVELLE: 'nouvelle', EN_COURS: 'en-cours', RESOLUE: 'resolue', FERMEE: 'fermee' };

export function badgePriorite(priorite) {
    if (!priorite) return '';
    return `<span class="badge badge-${CLASSES_PRIORITE[priorite.nom] || 'neutre'}">${esc(priorite.nom)}</span>`;
}

export function badgeStatut(statut) {
    if (!statut) return '';
    return `<span class="badge badge-${CLASSES_STATUT[statut.code] || 'neutre'}">${esc(statut.nom)}</span>`;
}

// ------------------------------------------------------------------ Notifications éphémères

export function toast(message, type = 'info') {
    const zone = document.getElementById('toasts');
    const el = document.createElement('div');
    el.className = 'toast ' + type;
    el.textContent = message;
    zone.appendChild(el);
    setTimeout(() => el.remove(), 4500);
}

// ------------------------------------------------------------------ Modales

let modalOuverte = null;

/**
 * Ouvre une modale. `contenu` = { titre, sousTitre, corps (HTML), pied (HTML), pastille, large }.
 * Retourne l'élément et une fonction de fermeture.
 */
export function ouvrirModal({ titre, sousTitre = '', corps = '', pied = '', pastille = null, large = false }) {
    fermerModal();
    const racine = document.getElementById('modal-racine');
    const precedent = document.activeElement;
    racine.innerHTML = `
        <div class="modal-fond">
            <div class="modal ${large ? 'modal-large' : ''}" role="dialog" aria-modal="true" aria-labelledby="titre-modal">
                <header>
                    ${pastille ? `<div class="pastille-modal pastille-${pastille.type}">${icone(pastille.icone)}</div>` : ''}
                    <div><h2 id="titre-modal">${titre}</h2>${sousTitre ? `<p>${sousTitre}</p>` : ''}</div>
                </header>
                <div class="corps">${corps}</div>
                ${pied ? `<footer>${pied}</footer>` : ''}
            </div>
        </div>`;
    const fond = racine.querySelector('.modal-fond');
    const element = racine.querySelector('.modal');
    const fermer = () => {
        racine.innerHTML = '';
        document.removeEventListener('keydown', surTouche);
        modalOuverte = null;
        if (precedent && precedent.focus) precedent.focus();
    };
    const surTouche = e => { if (e.key === 'Escape') fermer(); };
    document.addEventListener('keydown', surTouche);
    fond.addEventListener('mousedown', e => { if (e.target === fond) fermer(); });
    element.querySelectorAll('[data-fermer]').forEach(b => b.addEventListener('click', fermer));
    const premier = element.querySelector('input:not([type=hidden]):not([disabled]), select, textarea, button.btn-primaire, button');
    if (premier) premier.focus();
    modalOuverte = { element, fermer };
    return modalOuverte;
}

export function fermerModal() {
    if (modalOuverte) modalOuverte.fermer();
}

/** Demande une confirmation avant une action sensible. Résout true si confirmé. */
export function confirmer({ titre, message, libelle = 'Confirmer', danger = false }) {
    return new Promise(resolve => {
        let reponse = false;
        const { element, fermer } = ouvrirModal({
            titre,
            pastille: { type: danger ? 'danger' : 'info', icone: danger ? 'alerte' : 'info' },
            corps: `<p>${message}</p>`,
            pied: `<button class="btn btn-secondaire" data-fermer>Annuler</button>
                   <button class="btn ${danger ? 'btn-danger' : 'btn-primaire'}" data-ok>${esc(libelle)}</button>`
        });
        const observer = new MutationObserver(() => {
            if (!document.body.contains(element)) {
                observer.disconnect();
                resolve(reponse);
            }
        });
        observer.observe(document.getElementById('modal-racine'), { childList: true });
        element.querySelector('[data-ok]').addEventListener('click', () => { reponse = true; fermer(); });
    });
}

// ------------------------------------------------------------------ Formulaires

/** Lit les champs nommés d'un formulaire (valeurs texte, nettoyées). */
export function lireFormulaire(formulaire) {
    const valeurs = {};
    new FormData(formulaire).forEach((v, k) => { valeurs[k] = typeof v === 'string' ? v.trim() : v; });
    return valeurs;
}

/**
 * Vérifie les champs obligatoires (attribut data-requis) et les règles simples
 * (data-min, type=email). Affiche les messages sous les champs. Retourne le nombre d'erreurs.
 */
export function validerFormulaire(formulaire) {
    effacerErreurs(formulaire);
    const erreurs = {};
    formulaire.querySelectorAll('[name]').forEach(champ => {
        const valeur = (champ.value || '').trim();
        if (champ.dataset.requis !== undefined && !valeur) {
            erreurs[champ.name] = champ.dataset.requis || 'Ce champ est obligatoire.';
        } else if (valeur && champ.type === 'email' && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(valeur)) {
            erreurs[champ.name] = "L'adresse e-mail n'est pas valide.";
        } else if (valeur && champ.dataset.min && valeur.length < Number(champ.dataset.min)) {
            erreurs[champ.name] = `Au moins ${champ.dataset.min} caractères.`;
        } else if (champ.dataset.egal && valeur !== (formulaire.querySelector(`[name="${champ.dataset.egal}"]`)?.value || '')) {
            erreurs[champ.name] = 'Les deux valeurs ne correspondent pas.';
        }
    });
    afficherErreurs(formulaire, erreurs);
    return Object.keys(erreurs).length;
}

// ------------------------------------------------------------------ Robustesse du mot de passe

const NIVEAUX_FORCE = ['', 'Trop court', 'Acceptable', 'Bon', 'Très bon', 'Trop courant'];

/**
 * Mots de passe très courants, repris tels quels de la liste du serveur
 * (backend/src/main/resources/securite/mots-de-passe-interdits.txt) : la jauge ne contredit jamais le serveur.
 */
const COURANTS = new Set([
    '123456789012', '1234567890123', '123456789123', '111111111111', 'abcdefghijkl', 'abcdef123456',
    'qwertyuiop12', 'qwertyuiop123', 'qwerty123456', 'azertyuiop12', 'azertyuiop123', 'azerty123456',
    'azertyazerty', 'qwertyqwerty', 'password1234', 'password123456', 'passwordpassword', 'motdepasse123',
    'motdepasse1234', 'wachtwoord123', 'bienvenue123', 'bienvenue2026', 'welcome12345', 'iloveyou1234',
    'admin1234567', 'administrateur', 'utilisateur1', 'bruxelles123', 'belgique1234', 'changeme1234'
]);

/**
 * Niveau indicatif d'un mot de passe, de 0 (vide) à 4 : la longueur compte le plus (NIST SP 800-63B).
 * Le serveur reste seul juge : il refuse aussi les mots de passe courants et les informations personnelles.
 */
export function niveauMotDePasse(valeur) {
    const longueur = [...(valeur || '')].length;
    if (!longueur) return 0;
    if (longueur < 12) return 1;
    // Même règle que le serveur pour les motifs répétés (« abcabcabcabc ») et les mots de passe connus.
    const minuscule = valeur.toLowerCase();
    if (COURANTS.has(minuscule) || /^(.{1,4})\1+$/.test(minuscule)) return 5;
    const familles = [/[a-z]/, /[A-Z]/, /\d/, /[^A-Za-z0-9]/].filter(r => r.test(valeur)).length;
    if (longueur >= 20 && familles >= 3) return 4;
    if (longueur >= 16 || familles >= 3) return 3;
    return 2;
}

/** Jauge affichée sous tout champ portant l'attribut data-force, mise à jour à chaque frappe. */
function afficherForce(input) {
    const bloc = input.closest('.champ');
    if (!bloc) return;
    let jauge = bloc.querySelector('.force-mdp');
    if (!jauge) {
        jauge = document.createElement('div');
        jauge.className = 'force-mdp';
        jauge.setAttribute('aria-live', 'polite');
        jauge.innerHTML = '<span class="force-barre"><span></span></span><span class="force-texte"></span>';
        bloc.insertBefore(jauge, bloc.querySelector('.erreur-champ'));
    }
    const niveau = niveauMotDePasse(input.value);
    jauge.dataset.niveau = String(niveau);
    jauge.querySelector('.force-texte').textContent = NIVEAUX_FORCE[niveau];
}

document.addEventListener('input', e => {
    if (e.target instanceof HTMLInputElement && e.target.matches('[data-force]')) afficherForce(e.target);
});

export function afficherErreurs(formulaire, champs) {
    Object.entries(champs || {}).forEach(([nom, message]) => {
        const input = formulaire.querySelector(`[name="${nom}"]`);
        const bloc = input && input.closest('.champ');
        if (!bloc) return;
        bloc.classList.add('invalide');
        input.setAttribute('aria-invalid', 'true');
        let el = bloc.querySelector('.erreur-champ');
        if (!el) {
            el = document.createElement('span');
            el.className = 'erreur-champ';
            bloc.appendChild(el);
        }
        el.textContent = message;
    });
    const premier = formulaire.querySelector('.champ.invalide [name]');
    if (premier) premier.focus();
}

export function effacerErreurs(formulaire) {
    formulaire.querySelectorAll('.champ.invalide').forEach(b => {
        b.classList.remove('invalide');
        b.querySelector('[name]')?.removeAttribute('aria-invalid');
    });
    const bandeau = formulaire.querySelector('.bandeau-formulaire');
    if (bandeau) bandeau.innerHTML = '';
}

/** Affiche un bandeau d'erreur dans le formulaire (zone .bandeau-formulaire). */
export function bandeau(formulaire, message, type = 'erreur') {
    const zone = formulaire.querySelector('.bandeau-formulaire');
    if (zone) zone.innerHTML = message ? `<div class="bandeau bandeau-${type}" role="alert">${icone(type === 'succes' ? 'coche' : 'alerte')}<span>${esc(message)}</span></div>` : '';
}

/** Traite une erreur d'API sur un formulaire : erreurs par champ + bandeau. */
export function erreurFormulaire(formulaire, erreur) {
    afficherErreurs(formulaire, erreur.champs);
    bandeau(formulaire, erreur.message);
}

export function champ({ nom, libelle, type = 'text', requis = false, valeur = '', aide = '', attributs = '', options = null, placeholder = '' }) {
    const id = 'champ-' + nom;
    const req = requis ? ` data-requis="${esc(typeof requis === 'string' ? requis : libelle.replace(/\s*\*$/, '') + ' est obligatoire.')}"` : '';
    let saisie;
    if (options) {
        saisie = `<select class="input" id="${id}" name="${nom}"${req} ${attributs}>
            <option value="">Sélectionner…</option>
            ${options.map(o => `<option value="${esc(o.valeur)}" ${String(o.valeur) === String(valeur) ? 'selected' : ''}>${esc(o.libelle)}</option>`).join('')}
        </select>`;
    } else if (type === 'textarea') {
        saisie = `<textarea class="input" id="${id}" name="${nom}"${req} placeholder="${esc(placeholder)}" ${attributs}>${esc(valeur)}</textarea>`;
    } else {
        saisie = `<input class="input" id="${id}" name="${nom}" type="${type}" value="${esc(valeur)}" placeholder="${esc(placeholder)}"${req} ${attributs}>`;
    }
    return `<div class="champ">
        <label for="${id}">${esc(libelle)}${requis ? ' <span class="requis" aria-hidden="true">*</span>' : ''}</label>
        ${saisie}
        ${aide ? `<span class="aide">${esc(aide)}</span>` : ''}
        <span class="erreur-champ"></span>
    </div>`;
}

/** Désactive le bouton pendant une action asynchrone. */
export async function pendant(bouton, action, libelleAttente = 'Enregistrement…') {
    const texte = bouton.innerHTML;
    bouton.disabled = true;
    bouton.textContent = libelleAttente;
    try {
        return await action();
    } finally {
        bouton.disabled = false;
        bouton.innerHTML = texte;
    }
}

// ------------------------------------------------------------------ Divers

export function pagination(page, totalPages, surChangement) {
    const conteneur = document.createElement('nav');
    conteneur.className = 'pagination';
    conteneur.setAttribute('aria-label', 'Pagination');
    if (totalPages <= 1) return conteneur;
    const pages = new Set([0, totalPages - 1, page - 1, page, page + 1].filter(p => p >= 0 && p < totalPages));
    const bouton = (libelle, cible, actif = false, desactive = false, aria = '') => {
        const b = document.createElement('button');
        b.type = 'button';
        b.innerHTML = libelle;
        if (actif) { b.classList.add('actif'); b.setAttribute('aria-current', 'page'); }
        if (aria) b.setAttribute('aria-label', aria);
        b.disabled = desactive;
        b.addEventListener('click', () => surChangement(cible));
        conteneur.appendChild(b);
    };
    bouton('‹', page - 1, false, page === 0, 'Page précédente');
    let precedente = -1;
    [...pages].sort((a, b) => a - b).forEach(p => {
        if (p - precedente > 1) {
            const s = document.createElement('span');
            s.textContent = '…';
            s.style.padding = '0 .3rem';
            conteneur.appendChild(s);
        }
        bouton(String(p + 1), p, p === page);
        precedente = p;
    });
    bouton('›', page + 1, false, page >= totalPages - 1, 'Page suivante');
    return conteneur;
}

/** Active les menus déroulants (élément .deroulant contenant un [data-basculer]). */
export function activerDeroulants(racine) {
    racine.querySelectorAll('.deroulant [data-basculer]').forEach(bouton => {
        bouton.addEventListener('click', e => {
            e.stopPropagation();
            const parent = bouton.closest('.deroulant');
            const ouvert = parent.classList.contains('ouvert');
            fermerDeroulants();
            parent.classList.toggle('ouvert', !ouvert);
            bouton.setAttribute('aria-expanded', String(!ouvert));
            if (!ouvert) positionnerDansTableau(parent, bouton);
        });
    });
}

/**
 * Un tableau défilant (overflow) coupe un menu positionné normalement : dans un tableau, le menu est
 * donc placé par rapport à la fenêtre, sous le bouton, ou au-dessus s'il n'y a pas la place en bas.
 */
function positionnerDansTableau(parent, bouton) {
    const menu = parent.querySelector('.menu-deroulant');
    if (!menu || !parent.closest('.tableau-conteneur')) return;
    const r = bouton.getBoundingClientRect();
    menu.style.position = 'fixed';
    menu.style.right = `${Math.max(8, window.innerWidth - r.right)}px`;
    const hauteur = menu.offsetHeight;
    const enBas = r.bottom + 6 + hauteur <= window.innerHeight;
    menu.style.top = `${enBas ? r.bottom + 6 : Math.max(8, r.top - 6 - hauteur)}px`;
}

function fermerDeroulants() {
    document.querySelectorAll('.deroulant.ouvert').forEach(d => {
        d.classList.remove('ouvert');
        d.querySelector('[data-basculer]')?.setAttribute('aria-expanded', 'false');
    });
}

document.addEventListener('click', e => {
    if (!e.target.closest('.menu-deroulant')) fermerDeroulants();
});
// Un menu placé par rapport à la fenêtre ne suivrait pas le défilement : on le ferme.
window.addEventListener('resize', fermerDeroulants);
document.addEventListener('scroll', e => {
    if (!(e.target instanceof Element && e.target.closest('.menu-deroulant'))) fermerDeroulants();
}, true);

export function etatChargement(conteneur, message = 'Chargement…') {
    conteneur.innerHTML = `<div class="chargement">${esc(message)}</div>`;
}

export function etatErreur(conteneur, erreur) {
    conteneur.innerHTML = `<div class="bandeau bandeau-erreur" role="alert">${icone('alerte')}<span>${esc(erreur.message || erreur)}</span></div>`;
}
