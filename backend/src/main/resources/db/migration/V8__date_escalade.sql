-- Escalade automatique des délais dépassés (UC-02 Supervision).
-- Date à laquelle le dépassement du délai cible a été signalé : une anomalie n'est escaladée qu'une
-- seule fois. NULL = pas encore escaladée. Aucune donnée existante n'est modifiée.
ALTER TABLE anomalie ADD COLUMN date_escalade DATETIME NULL AFTER date_resolution;
