// Données partagées entre les écrans (référentiels, état Premium), chargées une fois par session.
import { api } from './api.js';

let referentiels = null;
let premium = null;

export async function chargerReferentiels(forcer = false) {
    if (!referentiels || forcer) {
        const [statuts, priorites, types, systemes] = await Promise.all([
            api.get('/statuts'), api.get('/priorites'), api.get('/types'), api.get('/systemes')
        ]);
        referentiels = { statuts, priorites, types, systemes };
    }
    return referentiels;
}

export async function chargerPremium(forcer = false) {
    if (!premium || forcer) premium = await api.get('/abonnements/etat');
    return premium;
}

export function reinitialiserEtat() {
    referentiels = null;
    premium = null;
}
