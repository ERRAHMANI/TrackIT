-- Données de démonstration : décalage de toutes les dates pour qu'elles se terminent la veille
-- du jour où la migration est appliquée (les données de V3 s'arrêtent le 5 octobre 2026).
-- V3 n'est pas modifiée : sa somme de contrôle Flyway reste valide.
-- Pour obtenir des dates à jour, recréer une base vide puis démarrer le backend.

SET @decalage = GREATEST(DATEDIFF(CURDATE(), '2026-10-05') - 1, 0);

UPDATE organisation SET date_creation = date_creation + INTERVAL @decalage DAY;
UPDATE utilisateur  SET date_creation = date_creation + INTERVAL @decalage DAY;
UPDATE anomalie     SET date_creation   = date_creation   + INTERVAL @decalage DAY,
                        date_resolution = date_resolution + INTERVAL @decalage DAY;
UPDATE commentaire  SET date_creation = date_creation + INTERVAL @decalage DAY;
UPDATE historique   SET date_modification = date_modification + INTERVAL @decalage DAY;
UPDATE piece_jointe SET date_ajout = date_ajout + INTERVAL @decalage DAY;
UPDATE notification SET date_creation = date_creation + INTERVAL @decalage DAY;
UPDATE rapport      SET date_generation = date_generation + INTERVAL @decalage DAY;
UPDATE abonnement   SET date_debut = date_debut + INTERVAL @decalage DAY,
                        date_fin   = date_fin   + INTERVAL @decalage DAY;
UPDATE paiement     SET date_paiement = date_paiement + INTERVAL @decalage DAY;
