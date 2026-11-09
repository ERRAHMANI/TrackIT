// Navigation par fragment (#/chemin?parametres) : fonctionne sans configuration serveur.
export function naviguer(chemin, { remplacer = false } = {}) {
    const cible = '#' + chemin;
    if (remplacer) {
        history.replaceState(null, '', cible);
        window.dispatchEvent(new HashChangeEvent('hashchange'));
    } else if (location.hash === cible) {
        window.dispatchEvent(new HashChangeEvent('hashchange'));
    } else {
        location.hash = cible;
    }
}

export function routeCourante() {
    const brut = location.hash.replace(/^#/, '') || '/';
    const [chemin, requete = ''] = brut.split('?');
    return { chemin: chemin || '/', query: new URLSearchParams(requete) };
}

/** Met à jour les paramètres de l'URL courante sans recharger la page. */
export function majQuery(params) {
    const { chemin } = routeCourante();
    const q = new URLSearchParams();
    Object.entries(params).forEach(([k, v]) => { if (v !== undefined && v !== null && v !== '' && v !== false) q.set(k, v); });
    const texte = q.toString();
    history.replaceState(null, '', '#' + chemin + (texte ? '?' + texte : ''));
}
