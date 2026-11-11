-- Données de démonstration : décalage des dates pour qu'elles se terminent la veille
-- du jour où la migration est appliquée (les données de V3 s'arrêtent le 5 octobre 2026).
-- Seules les lignes datées au plus tard de ce jour sont décalées : les données créées
-- ensuite dans l'application gardent leur date réelle.
-- V3 n'est pas modifiée : sa somme de contrôle Flyway reste valide.
-- Pour obtenir des dates à jour, recréer une base vide puis démarrer le backend.

SET @fin_demo = '2026-10-05 23:59:59';
SET @decalage = GREATEST(DATEDIFF(CURDATE(), '2026-10-05') - 1, 0);

UPDATE organisation SET date_creation = date_creation + INTERVAL @decalage DAY
 WHERE date_creation <= @fin_demo;
UPDATE utilisateur  SET date_creation = date_creation + INTERVAL @decalage DAY
 WHERE date_creation <= @fin_demo;
UPDATE anomalie     SET date_creation   = date_creation   + INTERVAL @decalage DAY,
                        date_resolution = date_resolution + INTERVAL @decalage DAY
 WHERE date_creation <= @fin_demo;
UPDATE commentaire  SET date_creation = date_creation + INTERVAL @decalage DAY
 WHERE date_creation <= @fin_demo;
UPDATE historique   SET date_modification = date_modification + INTERVAL @decalage DAY
 WHERE date_modification <= @fin_demo;
UPDATE piece_jointe SET date_ajout = date_ajout + INTERVAL @decalage DAY
 WHERE date_ajout <= @fin_demo;
UPDATE notification SET date_creation = date_creation + INTERVAL @decalage DAY
 WHERE date_creation <= @fin_demo;
UPDATE rapport      SET date_generation = date_generation + INTERVAL @decalage DAY
 WHERE date_generation <= @fin_demo;
UPDATE abonnement   SET date_debut = date_debut + INTERVAL @decalage DAY,
                        date_fin   = date_fin   + INTERVAL @decalage DAY
 WHERE date_debut <= @fin_demo;
UPDATE paiement     SET date_paiement = date_paiement + INTERVAL @decalage DAY
 WHERE date_paiement <= @fin_demo;