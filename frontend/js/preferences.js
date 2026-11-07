// Préférences d'affichage propres au navigateur (onglet « Préférences » du profil).
const CLE = 'trackit.preferences';
const DEFAUT = { taille: 10, accueil: '/tableau-de-bord' };

export const preferences = {
    get() {
        try {
            return { ...DEFAUT, ...JSON.parse(localStorage.getItem(CLE) || '{}') };
        } catch {
            return { ...DEFAUT };
        }
    },
    enregistrer(valeurs) {
        localStorage.setItem(CLE, JSON.stringify({ ...this.get(), ...valeurs }));
    }
};
