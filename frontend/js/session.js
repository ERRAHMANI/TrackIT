// Session utilisateur : jeton JWT et profil. « Se souvenir de moi » → localStorage, sinon sessionStorage.
const CLE = 'trackit.session';

function lire(stockage) {
    try {
        return JSON.parse(stockage.getItem(CLE));
    } catch {
        return null;
    }
}

export const session = {
    get() {
        return lire(sessionStorage) || lire(localStorage);
    },
    ouvrir(reponseAuth, seSouvenir) {
        const donnees = {
            jeton: reponseAuth.jeton,
            expireLe: Date.now() + reponseAuth.expireDansSecondes * 1000,
            utilisateur: reponseAuth.utilisateur
        };
        this.fermer();
        (seSouvenir ? localStorage : sessionStorage).setItem(CLE, JSON.stringify(donnees));
    },
    majUtilisateur(utilisateur) {
        const s = this.get();
        if (!s) return;
        s.utilisateur = utilisateur;
        const stockage = lire(localStorage) ? localStorage : sessionStorage;
        stockage.setItem(CLE, JSON.stringify(s));
    },
    fermer() {
        localStorage.removeItem(CLE);
        sessionStorage.removeItem(CLE);
    },
    estConnecte() {
        const s = this.get();
        return !!s && s.expireLe > Date.now();
    },
    utilisateur() {
        return this.get()?.utilisateur ?? null;
    },
    role() {
        return this.utilisateur()?.codeRole ?? null;
    },
    aLeRole(...roles) {
        return roles.includes(this.role());
    }
};
