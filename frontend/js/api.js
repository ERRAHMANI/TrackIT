// Client HTTP de l'API TrackIT : ajoute le jeton JWT et normalise les erreurs.
import { session } from './session.js';

const BASE = (window.TRACKIT_CONFIG && window.TRACKIT_CONFIG.apiUrl) || 'http://localhost:8080/api/v1';

export class ErreurApi extends Error {
    constructor(status, corps) {
        super((corps && corps.message) || messageParDefaut(status));
        this.status = status;
        this.champs = (corps && corps.champs) || {};
    }
}

function messageParDefaut(status) {
    if (status === 0) return "Impossible de joindre le serveur TrackIT. Vérifiez que le backend est démarré.";
    if (status === 401) return 'Votre session a expiré. Veuillez vous reconnecter.';
    if (status === 403) return "Votre rôle ne permet pas d'effectuer cette action.";
    if (status === 404) return 'Élément introuvable.';
    return 'Une erreur est survenue (' + status + ').';
}

function construireUrl(chemin, params) {
    const url = new URL(BASE + chemin);
    Object.entries(params || {}).forEach(([cle, valeur]) => {
        if (valeur !== undefined && valeur !== null && valeur !== '') url.searchParams.set(cle, valeur);
    });
    return url.toString();
}

async function requete(methode, chemin, { corps, params, formulaire, brut } = {}) {
    const entetes = {};
    const s = session.get();
    if (s && s.jeton) entetes.Authorization = 'Bearer ' + s.jeton;
    let body;
    if (formulaire) {
        body = formulaire;
    } else if (corps !== undefined) {
        entetes['Content-Type'] = 'application/json';
        body = JSON.stringify(corps);
    }
    let reponse;
    try {
        reponse = await fetch(construireUrl(chemin, params), { method: methode, headers: entetes, body });
    } catch {
        throw new ErreurApi(0, null);
    }
    if (reponse.status === 401 && s && !chemin.startsWith('/auth/')) {
        session.fermer();
        window.dispatchEvent(new CustomEvent('trackit:session-expiree'));
    }
    if (!reponse.ok) {
        let contenu = null;
        try { contenu = await reponse.json(); } catch { /* corps non JSON */ }
        throw new ErreurApi(reponse.status, contenu);
    }
    if (brut) return reponse;
    if (reponse.status === 204) return null;
    const type = reponse.headers.get('Content-Type') || '';
    return type.includes('application/json') ? reponse.json() : reponse.text();
}

export const api = {
    get: (chemin, params) => requete('GET', chemin, { params }),
    post: (chemin, corps) => requete('POST', chemin, { corps }),
    put: (chemin, corps) => requete('PUT', chemin, { corps }),
    patch: (chemin, corps) => requete('PATCH', chemin, { corps }),
    supprimer: (chemin) => requete('DELETE', chemin),
    envoyerFichier: (chemin, fichier) => {
        const donnees = new FormData();
        donnees.append('fichier', fichier);
        return requete('POST', chemin, { formulaire: donnees });
    },
    /** Télécharge un fichier protégé par le jeton et le propose à l'enregistrement. */
    async telecharger(chemin, nomParDefaut) {
        const reponse = await requete('GET', chemin, { brut: true });
        const disposition = reponse.headers.get('Content-Disposition') || '';
        const correspondance = /filename\*=UTF-8''([^;]+)/i.exec(disposition) || /filename="?([^";]+)"?/i.exec(disposition);
        const nom = correspondance ? decodeURIComponent(correspondance[1]) : nomParDefaut;
        const blob = await reponse.blob();
        const lien = document.createElement('a');
        lien.href = URL.createObjectURL(blob);
        lien.download = nom;
        document.body.appendChild(lien);
        lien.click();
        lien.remove();
        setTimeout(() => URL.revokeObjectURL(lien.href), 2000);
    }
};