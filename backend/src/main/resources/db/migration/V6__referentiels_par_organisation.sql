-- Types d'anomalie et priorités propres à chaque organisation (OWASP A01, action P7).
-- Jusqu'ici ces référentiels étaient communs à toute la plateforme : l'administrateur d'une
-- organisation cliente pouvait donc modifier ou supprimer les valeurs utilisées par les autres.
-- Chaque organisation existante reçoit sa propre copie des valeurs actuelles, ses anomalies sont
-- rattachées à ces copies, puis les anciennes valeurs communes sont supprimées.
-- Les statuts restent communs : ils portent le cycle de vie fixe de l'application (StatutCode).

-- 1. Le nom n'est plus unique sur toute la plateforme, mais au sein d'une organisation.
ALTER TABLE type_anomalie DROP INDEX uk_type_anomalie_nom;
ALTER TABLE priorite DROP INDEX uk_priorite_nom;

ALTER TABLE type_anomalie ADD COLUMN id_organisation BIGINT NULL AFTER id_type;
ALTER TABLE priorite ADD COLUMN id_organisation BIGINT NULL AFTER id_priorite;

-- 2. Copie des valeurs communes pour chaque organisation existante.
INSERT INTO type_anomalie (id_organisation, nom, description)
SELECT o.id_organisation, t.nom, t.description
FROM organisation o
CROSS JOIN type_anomalie t
WHERE t.id_organisation IS NULL
ORDER BY o.id_organisation, t.id_type;

INSERT INTO priorite (id_organisation, nom, niveau, description)
SELECT o.id_organisation, p.nom, p.niveau, p.description
FROM organisation o
CROSS JOIN priorite p
WHERE p.id_organisation IS NULL
ORDER BY o.id_organisation, p.id_priorite;

-- 3. Chaque anomalie pointe vers la copie de son organisation (organisation de son système).
UPDATE anomalie a
JOIN systeme_it s ON s.id_systeme = a.id_systeme
JOIN type_anomalie commun ON commun.id_type = a.id_type AND commun.id_organisation IS NULL
JOIN type_anomalie copie ON copie.id_organisation = s.id_organisation AND copie.nom = commun.nom
SET a.id_type = copie.id_type;

UPDATE anomalie a
JOIN systeme_it s ON s.id_systeme = a.id_systeme
JOIN priorite commune ON commune.id_priorite = a.id_priorite AND commune.id_organisation IS NULL
JOIN priorite copie ON copie.id_organisation = s.id_organisation AND copie.nom = commune.nom
SET a.id_priorite = copie.id_priorite;

-- 4. Plus aucune anomalie n'utilise les valeurs communes : elles sont supprimées.
DELETE FROM type_anomalie WHERE id_organisation IS NULL;
DELETE FROM priorite WHERE id_organisation IS NULL;

-- 5. Contraintes définitives.
ALTER TABLE type_anomalie
    MODIFY id_organisation BIGINT NOT NULL,
    ADD CONSTRAINT uk_type_anomalie_organisation_nom UNIQUE (id_organisation, nom),
    ADD CONSTRAINT fk_type_anomalie_organisation FOREIGN KEY (id_organisation) REFERENCES organisation (id_organisation);

ALTER TABLE priorite
    MODIFY id_organisation BIGINT NOT NULL,
    ADD CONSTRAINT uk_priorite_organisation_nom UNIQUE (id_organisation, nom),
    ADD CONSTRAINT fk_priorite_organisation FOREIGN KEY (id_organisation) REFERENCES organisation (id_organisation);
