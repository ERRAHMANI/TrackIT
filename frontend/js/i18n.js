// Traduction de l'interface (français, anglais, néerlandais).
// Les écrans sont écrits en français : ce module remplace les textes au moment de leur affichage,
// y compris ceux renvoyés par l'API. Les données saisies par les utilisateurs ne sont pas traduites.
import { preferences } from './preferences.js';
import { DICTIONNAIRE, MOTIFS } from './traductions.js';

export const LANGUES = [
    { code: 'fr', libelle: 'Français', locale: 'fr-BE' },
    { code: 'en', libelle: 'English', locale: 'en-GB' },
    { code: 'nl', libelle: 'Nederlands', locale: 'nl-BE' }
];

const ATTRIBUTS = ['placeholder', 'title', 'aria-label', 'alt'];
const EXCLUSIONS = 'script, style, textarea, code, .contenu-commentaire, .texte-long, [data-sans-traduction]';

function langueInitiale() {
    const choisie = preferences.get().langue;
    if (LANGUES.some(l => l.code === choisie)) return choisie;
    const navigateur = (navigator.language || 'fr').slice(0, 2);
    return LANGUES.some(l => l.code === navigateur) ? navigateur : 'fr';
}

const langueCourante = langueInitiale();
const rang = { en: 0, nl: 1 }[langueCourante];

export const langue = () => langueCourante;
export const locale = () => LANGUES.find(l => l.code === langueCourante).locale;

/** Change de langue : le rechargement réaffiche tous les écrans dans la langue choisie. */
export function changerLangue(code) {
    preferences.enregistrer({ langue: code });
    location.reload();
}

/** Nombre décimal avec le séparateur de la langue. */
export function decimal(valeur) {
    return langueCourante === 'en' ? String(valeur) : String(valeur).replace('.', ',');
}

function chercher(texte) {
    const exact = DICTIONNAIRE[texte];
    if (exact) return exact[rang];
    for (const motif of MOTIFS) {
        const m = motif[0].exec(texte);
        if (m) return motif[1 + rang].replace(/\$(\d)/g, (_, n) => chercher(m[n] ?? '') ?? m[n] ?? '');
    }
    return null;
}

/** Traduit un texte français dans la langue courante, en conservant ses espaces de début et de fin. */
export function t(texte) {
    if (langueCourante === 'fr' || !/\p{L}/u.test(texte)) return texte;
    const normalise = texte.replace(/\s+/g, ' ').trim();
    const traduit = chercher(normalise);
    if (traduit === null) return texte;
    // Les guillemets anglais et néerlandais se collent au mot, contrairement aux guillemets français.
    const avant = traduit.startsWith('”') ? '' : texte.match(/^\s*/)[0];
    const apres = traduit.endsWith('“') ? '' : texte.match(/\s*$/)[0];
    return avant + traduit + apres;
}

/** Liste déroulante de changement de langue. */
export function selecteurLangue() {
    return `<select class="selecteur-langue" data-langue data-sans-traduction aria-label="${t("Langue de l'interface")}">
        ${LANGUES.map(l => `<option value="${l.code}" ${l.code === langueCourante ? 'selected' : ''}>${l.code.toUpperCase()}</option>`).join('')}
    </select>`;
}

const ignore = noeud => noeud.closest?.(EXCLUSIONS) || noeud.parentElement?.closest(EXCLUSIONS);

function traduireTexte(noeud) {
    if (ignore(noeud)) return;
    const traduit = t(noeud.nodeValue);
    if (traduit !== noeud.nodeValue) noeud.nodeValue = traduit;
}

function traduireAttributs(element) {
    if (element.closest('[data-sans-traduction]')) return;
    ATTRIBUTS.forEach(nom => {
        const valeur = element.getAttribute(nom);
        if (!valeur) return;
        const traduit = t(valeur);
        if (traduit !== valeur) element.setAttribute(nom, traduit);
    });
}

function traduireArbre(racine) {
    if (racine.nodeType === Node.TEXT_NODE) return traduireTexte(racine);
    if (racine.nodeType !== Node.ELEMENT_NODE) return;
    traduireAttributs(racine);
    const parcours = document.createTreeWalker(racine, NodeFilter.SHOW_ELEMENT | NodeFilter.SHOW_TEXT);
    while (parcours.nextNode()) {
        const noeud = parcours.currentNode;
        if (noeud.nodeType === Node.TEXT_NODE) traduireTexte(noeud);
        else traduireAttributs(noeud);
    }
}

/** À appeler une fois au démarrage : traduit la page, puis tout ce qui s'affiche ensuite. */
export function demarrerTraduction() {
    document.documentElement.lang = langueCourante;
    document.addEventListener('change', e => {
        if (e.target.matches('[data-langue]')) changerLangue(e.target.value);
    });
    if (langueCourante === 'fr') return;
    traduireArbre(document.documentElement);
    new MutationObserver(mutations => mutations.forEach(m => {
        if (m.type === 'childList') m.addedNodes.forEach(traduireArbre);
        else if (m.type === 'characterData') traduireTexte(m.target);
        else traduireAttributs(m.target);
    })).observe(document.documentElement, {
        subtree: true, childList: true, characterData: true, attributes: true, attributeFilter: ATTRIBUTS
    });
}
