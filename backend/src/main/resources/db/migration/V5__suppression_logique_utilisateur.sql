-- Suppression logique des comptes (droit à l'oubli, RGPD art. 17).
-- Un compte lié à des anomalies, commentaires, historiques, pièces jointes ou rapports n'est pas
-- effacé : ses données personnelles sont anonymisées, il est désactivé et cette date est renseignée.
-- Les traces métier restent ainsi cohérentes. Un compte sans aucune trace est supprimé physiquement.

ALTER TABLE utilisateur
    ADD COLUMN date_suppression DATETIME NULL AFTER date_creation;
