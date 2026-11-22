-- Verrou optimiste sur les anomalies (modifications simultanées).
-- Hibernate incrémente cette colonne à chaque modification et vérifie, dans la même requête UPDATE,
-- qu'elle n'a pas changé depuis la lecture : si un autre utilisateur a modifié l'anomalie entre-temps,
-- aucune ligne n'est mise à jour et l'API répond 409 au lieu d'écraser son travail.
-- Les anomalies existantes démarrent à 0 ; aucune autre donnée n'est modifiée.
ALTER TABLE anomalie ADD COLUMN version BIGINT NOT NULL DEFAULT 0 AFTER date_escalade;
