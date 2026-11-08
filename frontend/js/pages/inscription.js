// Inscription d'une organisation et de son administrateur (boutons « S'inscrire » des maquettes).
import { api } from '../api.js';
import { session } from '../session.js';
import { champ, validerFormulaire, lireFormulaire, erreurFormulaire, pendant, toast } from '../ui.js';
import { naviguer } from '../routeur.js';

export default function inscription(app) {
    document.title = 'Inscription — TrackIT';
    app.innerHTML = `
    <div class="page-centree">
        <div class="carte-connexion" style="width:min(560px,100%)">
            <a class="marque" href="#/"><img src="assets/logo.svg" alt="">TrackIT</a>
            <h1>Créer l'espace de votre organisation</h1>
            <p class="sous-titre">Vous deviendrez administrateur de votre espace TrackIT. Les champs suivis d'un astérisque (*) sont obligatoires.</p>
            <form id="form-inscription" novalidate>
                <div class="bandeau-formulaire"></div>
                ${champ({ nom: 'nomOrganisation', libelle: "Nom de l'organisation", requis: true, attributs: 'maxlength="150"' })}
                ${champ({ nom: 'emailOrganisation', libelle: "E-mail de l'organisation", type: 'email', requis: true, attributs: 'maxlength="150"' })}
                <div class="grille-2">
                    ${champ({ nom: 'nom', libelle: 'Nom', requis: true, attributs: 'maxlength="100" autocomplete="family-name"' })}
                    ${champ({ nom: 'prenom', libelle: 'Prénom', requis: true, attributs: 'maxlength="100" autocomplete="given-name"' })}
                </div>
                ${champ({ nom: 'email', libelle: 'Votre adresse e-mail', type: 'email', requis: true, attributs: 'maxlength="150" autocomplete="email"' })}
                <div class="grille-2">
                    ${champ({ nom: 'motDePasse', libelle: 'Mot de passe', type: 'password', requis: true, aide: '8 caractères minimum', attributs: 'data-min="8" autocomplete="new-password"' })}
                    ${champ({ nom: 'confirmation', libelle: 'Confirmation', type: 'password', requis: true, attributs: 'data-egal="motDePasse" autocomplete="new-password"' })}
                </div>
                <button class="btn btn-primaire btn-large" type="submit">Créer mon espace</button>
            </form>
            <p class="bas-carte">Déjà inscrit ? <a href="#/connexion">Se connecter</a></p>
        </div>
    </div>`;

    const form = app.querySelector('#form-inscription');
    form.addEventListener('submit', async e => {
        e.preventDefault();
        if (validerFormulaire(form)) return;
        const { confirmation, ...donnees } = lireFormulaire(form);
        try {
            const reponse = await pendant(form.querySelector('button[type=submit]'), () => api.post('/auth/inscription', donnees), 'Création…');
            session.ouvrir(reponse, false);
            toast(`Bienvenue ! L'espace « ${donnees.nomOrganisation} » a été créé.`, 'succes');
            naviguer('/systemes');
        } catch (erreur) {
            erreurFormulaire(form, erreur);
        }
    });
}
