-- Révocation des jetons après un changement de mot de passe (S3, OWASP A07).
-- Un jeton JWT émis avant cette date est refusé : changer ou réinitialiser un mot de passe
-- déconnecte donc toutes les sessions ouvertes avec l'ancien mot de passe.
-- Précision à la milliseconde (DATETIME(3)) : la comparaison avec l'instant d'émission du jeton
-- doit distinguer un jeton émis juste avant le changement d'un jeton émis juste après.
-- Les comptes existants reçoivent la date courante : les jetons déjà émis ne sont plus acceptés
-- et chacun se reconnecte une fois après le déploiement.

ALTER TABLE utilisateur
    ADD COLUMN mot_de_passe_modifie_le DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) AFTER mot_de_passe;
