# 14 — Contrôle du DUMP de la base TrackIT

| | |
|---|---|
| Livrable | `database/14_DUMP_TrackIT.sql` (1 Mo environ, 880 Ko) |
| Base | `trackit` — MySQL, InnoDB, `utf8mb4` / `utf8mb4_unicode_ci` |
| Contrôle effectué le | 05/10/2026, sur MySQL installé localement |
| Mise à jour | 24/10/2026 : données de démonstration adaptées au contexte belge, contrôles rejoués sur MariaDB 10.11 (moteur de XAMPP) — voir § 13 |
| Résultat global | ✅ Import sans erreur, 0 orphelin, 0 incohérence métier ; démarrage Spring Boot validé le 05/10/2026, à revalider avec le backend (§ 13) |

## 1. Origine du dump

Le dump n'est pas un modèle refait à la main : il est **extrait de la base réellement utilisée par
le backend**, elle-même construite par les migrations Flyway du projet :

| Migration | Fichier | Contenu |
|---|---|---|
| V1 | `backend/src/main/resources/db/migration/V1__schema.sql` | 15 tables, PK, FK, UNIQUE, CHECK, index |
| V2 | `backend/src/main/resources/db/migration/V2__referentiels.sql` | rôles, statuts, priorités, types |
| V3 | `backend/src/main/resources/db/demo/V3__donnees_demonstration.sql` | données de démonstration, produites par `database/scripts/generer_donnees_demo.py` |

La structure (`CREATE TABLE`) est copiée telle quelle depuis MySQL (`SHOW CREATE TABLE`) ; les données
sont exportées table par table avec `mysqldump`. Le fichier est organisé ainsi :

1. informations du dump ;
2. `SET NAMES utf8mb4`, `FOREIGN_KEY_CHECKS = 1` (les clés étrangères restent **actives** pendant l'import) ;
3. `CREATE DATABASE IF NOT EXISTS trackit` et `USE trackit` ;
4. `DROP TABLE IF EXISTS` dans l'ordre inverse des dépendances (le script est rejouable) ;
5. `CREATE TABLE` dans l'ordre des dépendances, avec PK, FK, UNIQUE, INDEX, CHECK et DEFAULT ;
6. données référentielles : `role`, `statut`, `priorite`, `type_anomalie` ;
7. données métier : `organisation` → `utilisateur` → `systeme_it` → `anomalie` → `commentaire` →
   `historique` → `piece_jointe` → `notification` → `rapport` → `abonnement` → `paiement` ;
8. `flyway_schema_history` : historique des migrations, pour que le backend démarre sur la base
   importée sans tenter de rejouer les migrations.

## 2. Tableau de synthèse

| Table | Nombre de lignes | PK | FK | Test OK |
|-------|-----------------:|----|----|---------|
| `role` | 5 | `id_role` | — | ✅ |
| `statut` | 4 | `id_statut` | — | ✅ |
| `priorite` | 4 | `id_priorite` | — | ✅ |
| `type_anomalie` | 8 | `id_type` | — | ✅ |
| `organisation` | 101 | `id_organisation` | — | ✅ |
| `utilisateur` | 383 | `id_utilisateur` | id_organisation → organisation, id_role → role | ✅ |
| `systeme_it` | 218 | `id_systeme` | id_organisation → organisation | ✅ |
| `anomalie` | 427 | `id_anomalie` | id_auteur → utilisateur, id_operateur → utilisateur, id_priorite → priorite, id_statut → statut, id_systeme → systeme_it, id_type → type_anomalie | ✅ |
| `commentaire` | 744 | `id_commentaire` | id_anomalie → anomalie, id_utilisateur → utilisateur | ✅ |
| `historique` | 1 889 | `id_historique` | id_anomalie → anomalie, id_utilisateur → utilisateur | ✅ |
| `piece_jointe` | 138 | `id_piece_jointe` | id_anomalie → anomalie, id_utilisateur → utilisateur | ✅ |
| `notification` | 2 456 | `id_notification` | id_anomalie → anomalie, id_utilisateur → utilisateur | ✅ |
| `rapport` | 127 | `id_rapport` | id_analyste → utilisateur | ✅ |
| `abonnement` | 368 | `id_abonnement` | id_organisation → organisation | ✅ |
| `paiement` | 368 | `id_paiement` | id_abonnement → abonnement | ✅ |
| `flyway_schema_history` | 3 | `installed_rank` | — | ✅ |

**16 tables** : les 15 tables du schéma 08 / dictionnaire 09, plus `flyway_schema_history`, table technique
créée par Flyway. **Test OK** signifie : table importée, PK présente, aucune ligne orpheline sur ses clés étrangères.

## 3. Volumes

| Table | Nature | Lignes | Seuil | Justification du volume |
|---|---|---:|---|---|
| `role` | Référentiel | 5 | non applicable | 5 rôles du diagramme de cas d'utilisation |
| `statut` | Référentiel | 4 | non applicable | 4 statuts officiels |
| `priorite` | Référentiel | 4 | non applicable | 4 niveaux (dictionnaire : niveau 1 à 4) |
| `type_anomalie` | Référentiel | 8 | non applicable | 8 types visibles dans les maquettes 02 et 05 |
| `organisation` | Métier | 101 | ≥ 100 ✅ | 1 organisation de démonstration + 100 clientes (« +100 organisations » de la page d'accueil) |
| `utilisateur` | Métier | 383 | ≥ 100 ✅ | ≥ 1 administrateur et 1 opérateur par organisation |
| `systeme_it` | Métier | 218 | ≥ 100 ✅ | 1 à 3 systèmes par organisation cliente, 17 pour l'organisation principale |
| `anomalie` | Métier | 427 | ≥ 100 ✅ | 230 pour l'organisation principale (avril → octobre 2026), 1 à 3 par organisation cliente |
| `commentaire` | Métier | 744 | ≥ 100 ✅ | diagnostic, résolution et clôture |
| `historique` | Métier | 1 889 | ≥ 100 ✅ | création, affectation, priorité, statuts, pièces jointes |
| `piece_jointe` | Métier | 138 | ≥ 100 ✅ | fichiers réels fournis dans backend/uploads/demo |
| `notification` | Métier | 2 456 | ≥ 100 ✅ | générées à chaque événement (déclaration, affectation, statut, commentaire) |
| `rapport` | Métier | 127 | ≥ 100 ✅ | rapports mensuels des analystes |
| `abonnement` | Métier | 368 | ≥ 100 ✅ | historique des souscriptions (renouvellements mensuels) |
| `paiement` | Métier | 368 | ≥ 100 ✅ | 1 paiement par tentative de souscription |
| `flyway_schema_history` | Technique | 3 | non applicable | 3 migrations V1, V2, V3 |

Les référentiels ne contiennent que les valeurs réellement définies par le projet (pas de remplissage
artificiel). Toutes les tables métier dépassent 100 lignes. Les tables qu'on pouvait attendre plus petites
ont un volume justifié : 101 organisations correspondent au chiffre « +100 organisations » de la maquette
d'accueil, et les abonnements forment un historique de renouvellements mensuels.

Distribution des données de démonstration :

| Mesure | Valeur |
|---|---|
| Anomalies par statut | Nouvelle : 37, En cours : 52, Résolue : 82, Fermée : 256 |
| Anomalies par priorité | Critique : 89, Haute : 170, Moyenne : 111, Basse : 57 |
| Anomalies par type | Accès : 82, Réseau : 81, Application : 68, Performance : 56, Fonctionnel : 44, Base de données : 41, Matériel : 29, Autre : 26 |
| Utilisateurs par rôle | Utilisateur : 39, Opérateur : 145, Responsable technique : 59, Analyste : 39, Administrateur : 101 |
| Variété | 304 titres distincts, 261 descriptions distinctes, 120 commentaires distincts |
| Période couverte | 148 jours distincts, du 2026-04-01 au 2026-10-05 |
| Délai moyen de résolution (h) | Critique : 8.1, Haute : 23.8, Moyenne : 51.4, Basse : 148.7 |
| Paiements | REUSSI : 335, ANNULE : 17, ECHOUE : 16 |
| Abonnements | EXPIRE : 269, ACTIF : 66, ANNULE : 33 |
| Organisation principale « TrackIT » | 230 anomalies (organisation des comptes de démonstration) |

## 4. Clés primaires

Chaque table possède une clé primaire `BIGINT AUTO_INCREMENT` nommée `id_<table>`, conformément au
dictionnaire (`installed_rank` pour la table Flyway).

## 5. Clés étrangères

20 clés étrangères, toutes vérifiées par une requête `LEFT JOIN … IS NULL` après import :

| Table | Contrainte | Colonne | Référence | ON DELETE | ON UPDATE | Orphelins |
|---|---|---|---|---|---|---:|
| `utilisateur` | `fk_utilisateur_organisation` | `id_organisation` | `organisation.id_organisation` | NO ACTION | NO ACTION | 0 |
| `utilisateur` | `fk_utilisateur_role` | `id_role` | `role.id_role` | NO ACTION | NO ACTION | 0 |
| `systeme_it` | `fk_systeme_it_organisation` | `id_organisation` | `organisation.id_organisation` | NO ACTION | NO ACTION | 0 |
| `anomalie` | `fk_anomalie_auteur` | `id_auteur` | `utilisateur.id_utilisateur` | NO ACTION | NO ACTION | 0 |
| `anomalie` | `fk_anomalie_operateur` | `id_operateur` | `utilisateur.id_utilisateur` | NO ACTION | NO ACTION | 0 |
| `anomalie` | `fk_anomalie_priorite` | `id_priorite` | `priorite.id_priorite` | NO ACTION | NO ACTION | 0 |
| `anomalie` | `fk_anomalie_statut` | `id_statut` | `statut.id_statut` | NO ACTION | NO ACTION | 0 |
| `anomalie` | `fk_anomalie_systeme` | `id_systeme` | `systeme_it.id_systeme` | NO ACTION | NO ACTION | 0 |
| `anomalie` | `fk_anomalie_type` | `id_type` | `type_anomalie.id_type` | NO ACTION | NO ACTION | 0 |
| `commentaire` | `fk_commentaire_anomalie` | `id_anomalie` | `anomalie.id_anomalie` | CASCADE | NO ACTION | 0 |
| `commentaire` | `fk_commentaire_utilisateur` | `id_utilisateur` | `utilisateur.id_utilisateur` | NO ACTION | NO ACTION | 0 |
| `historique` | `fk_historique_anomalie` | `id_anomalie` | `anomalie.id_anomalie` | CASCADE | NO ACTION | 0 |
| `historique` | `fk_historique_utilisateur` | `id_utilisateur` | `utilisateur.id_utilisateur` | NO ACTION | NO ACTION | 0 |
| `piece_jointe` | `fk_piece_jointe_anomalie` | `id_anomalie` | `anomalie.id_anomalie` | CASCADE | NO ACTION | 0 |
| `piece_jointe` | `fk_piece_jointe_utilisateur` | `id_utilisateur` | `utilisateur.id_utilisateur` | NO ACTION | NO ACTION | 0 |
| `notification` | `fk_notification_anomalie` | `id_anomalie` | `anomalie.id_anomalie` | CASCADE | NO ACTION | 0 |
| `notification` | `fk_notification_utilisateur` | `id_utilisateur` | `utilisateur.id_utilisateur` | NO ACTION | NO ACTION | 0 |
| `rapport` | `fk_rapport_analyste` | `id_analyste` | `utilisateur.id_utilisateur` | NO ACTION | NO ACTION | 0 |
| `abonnement` | `fk_abonnement_organisation` | `id_organisation` | `organisation.id_organisation` | NO ACTION | NO ACTION | 0 |
| `paiement` | `fk_paiement_abonnement` | `id_abonnement` | `abonnement.id_abonnement` | NO ACTION | NO ACTION | 0 |

**ON DELETE / ON UPDATE** — choix repris tels quels du projet, sans CASCADE systématique :

- `NO ACTION` (restrictif) par défaut : on ne peut pas supprimer un rôle, un statut, une priorité, un type,
  une organisation, un système ou un utilisateur encore référencé. Le backend renvoie alors une erreur 409
  et propose de **désactiver** (`actif = FALSE`) plutôt que de supprimer.
- `CASCADE` uniquement pour les dépendances d'une anomalie (`commentaire`, `historique`,
  `piece_jointe`, `notification`) : la suppression d'une anomalie (UC-06, réservée à l'administrateur)
  supprime ses éléments rattachés, qui n'ont pas de sens seuls.
- `ON UPDATE` : aucune action. Les clés primaires techniques ne sont jamais modifiées.

## 6. Index et contraintes

| Table | Index secondaires | UNIQUE | CHECK |
|---|---|---|---|
| `role` | `uk_role_nom` | `uk_role_nom` | — |
| `statut` | `uk_statut_nom` | `uk_statut_nom` | — |
| `priorite` | `uk_priorite_nom` | `uk_priorite_nom` | `ck_priorite_niveau` : (`niveau` between 1 and 4) |
| `type_anomalie` | `uk_type_anomalie_nom` | `uk_type_anomalie_nom` | — |
| `organisation` | `uk_organisation_email` | `uk_organisation_email` | — |
| `utilisateur` | `idx_utilisateur_organisation`, `idx_utilisateur_role`, `uk_utilisateur_email` | `uk_utilisateur_email` | — |
| `systeme_it` | `uk_systeme_it_nom_organisation` | `uk_systeme_it_nom_organisation` | — |
| `anomalie` | `idx_anomalie_auteur`, `idx_anomalie_date_creation`, `idx_anomalie_date_resolution`, `idx_anomalie_operateur`, `idx_anomalie_priorite`, `idx_anomalie_statut`, `idx_anomalie_systeme`, `idx_anomalie_type` | — | `ck_anomalie_dates` : ((`date_resolution` is null) or (`date_resolution` >= `date_creation`)) |
| `commentaire` | `idx_commentaire_anomalie`, `idx_commentaire_utilisateur` | — | — |
| `historique` | `idx_historique_anomalie`, `idx_historique_utilisateur` | — | — |
| `piece_jointe` | `idx_piece_jointe_anomalie`, `idx_piece_jointe_utilisateur` | — | — |
| `notification` | `idx_notification_anomalie`, `idx_notification_utilisateur_lu` | — | — |
| `rapport` | `idx_rapport_analyste` | — | `ck_rapport_format` : (`format` in (\\'PDF\\',\\'EXCEL\\')) |
| `abonnement` | `idx_abonnement_organisation` | — | `ck_abonnement_dates` : ((`date_fin` is null) or (`date_fin` >= `date_debut`))<br>`ck_abonnement_statut` : (`statut` in (\\'EN_ATTENTE\\',\\'ACTIF\\',\\'EXPIRE\\',\\'ANNULE\\')) |
| `paiement` | `idx_paiement_abonnement`, `uk_paiement_reference_stripe` | `uk_paiement_reference_stripe` | `ck_paiement_montant` : (`montant` > 0)<br>`ck_paiement_statut` : (`statut` in (\\'EN_ATTENTE\\',\\'REUSSI\\',\\'ECHOUE\\',\\'ANNULE\\')) |
| `flyway_schema_history` | `flyway_schema_history_s_idx` | — | — |

Chaque clé étrangère est indexée. Index de recherche supplémentaires sur `anomalie.date_creation`,
`anomalie.date_resolution` et `notification (id_utilisateur, lu)`, utilisés par les filtres, les
statistiques et le compteur de notifications.

Contraintes NOT NULL / DEFAULT principales (dictionnaire) : champs obligatoires `NOT NULL` ;
`actif DEFAULT TRUE` ; `lu DEFAULT FALSE` ; dates `DEFAULT CURRENT_TIMESTAMP` ;
`anomalie.date_resolution`, `anomalie.id_operateur`, `notification.id_anomalie`,
`rapport.chemin_fichier`, `abonnement.date_fin` et `paiement.reference_stripe` facultatifs.

## 7. Test d'import

| Étape | Résultat |
|---|---|
| Import dans une base temporaire `trackit_controle` (`mysql < dump`) | ✅ code retour 0, aucune erreur ni avertissement, 0.2 s |
| Réimport du même fichier sur la base déjà remplie (script rejouable) | ✅ code retour 0 |
| Import dans la base réelle `trackit` | ✅ |
| Lignes orphelines (20 clés étrangères) | ✅ 0 |
| Jointure anomalie ⨝ auteur ⨝ système ⨝ organisation ⨝ type ⨝ priorité ⨝ statut | ✅ 427 / 427 anomalies |
| Jointure commentaire ⨝ anomalie ⨝ utilisateur | ✅ 744 / 744 |
| Jointure paiement ⨝ abonnement ⨝ organisation | ✅ 368 / 368 |
| Démarrage Spring Boot sur `trackit_controle` (Flyway `validate` + Hibernate `ddl-auto: validate`) ¹ | ✅ « Successfully validated 3 migrations », « Schema is up to date », application démarrée |
| Connexion des 5 comptes de démonstration (BCrypt) ¹ | ✅ HTTP 200 pour les 5 rôles |
| API sur la base importée : recherche, tableau de bord, historique, téléchargement d'une pièce jointe ¹ | ✅ HTTP 200 |

¹ Contrôles réalisés le 05/10/2026 sur la version précédente des données ; à rejouer avec le backend
sur la version belge (§ 13). Les autres lignes de ce tableau ont été rejouées le 24/10/2026.

Requêtes de comptage exécutées après import (extrait) :

```sql
SELECT COUNT(*) FROM utilisateur;   -- 383
SELECT COUNT(*) FROM anomalie;      -- 427
SELECT COUNT(*) FROM commentaire;   -- 744
SELECT COUNT(*) FROM historique;    -- 1889
```

### Contraintes mises à l'épreuve

Après import, des écritures invalides ont été tentées sur la base temporaire ; MySQL doit les refuser :

| Tentative | Résultat | Message MySQL |
|---|---|---|
| FK : anomalie avec auteur inexistant | ✅ rejeté | `ERROR 1452 (23000)` |
| FK : paiement sans abonnement | ✅ rejeté | `ERROR 1452 (23000)` |
| UNIQUE : e-mail utilisateur en double | ✅ rejeté | `ERROR 1062 (23000)` |
| CHECK : priorité de niveau 5 | ✅ rejeté | `ERROR 3819 (HY000)` |
| CHECK : résolution avant création | ✅ rejeté | `ERROR 3819 (HY000)` |
| CHECK : montant négatif | ✅ rejeté | `ERROR 3819 (HY000)` |
| CHECK : statut d'abonnement inconnu | ✅ rejeté | `ERROR 3819 (HY000)` |
| NOT NULL : anomalie sans titre | ✅ rejeté | `ERROR 1048 (23000)` |
| FK RESTRICT : supprimer un statut utilisé | ✅ rejeté | `ERROR 1451 (23000)` |

### Contrôles de cohérence métier

| Contrôle | Lignes en défaut | OK |
|---|---:|---|
| Statuts hors référentiel officiel | 0 | ✅ |
| Nouvelle/En cours avec date de résolution | 0 | ✅ |
| Résolue/Fermée sans date de résolution | 0 | ✅ |
| Résolution antérieure à la création | 0 | ✅ |
| En cours/Résolue/Fermée sans opérateur | 0 | ✅ |
| Auteur qui n'est pas Opérateur | 0 | ✅ |
| Opérateur affecté qui n'est pas Opérateur | 0 | ✅ |
| Système d'une autre organisation que l'auteur | 0 | ✅ |
| Opérateur d'une autre organisation | 0 | ✅ |
| Commentaire antérieur à l'anomalie | 0 | ✅ |
| Historique antérieur à l'anomalie | 0 | ✅ |
| Événement daté dans le futur | 0 | ✅ |
| Anomalie sans événement de création | 0 | ✅ |
| Statut final ≠ dernier statut de l'historique | 0 | ✅ |
| Fermeture par un rôle non autorisé | 0 | ✅ |
| Rapport généré par un non-Analyste | 0 | ✅ |
| Abonnement date_fin < date_debut | 0 | ✅ |
| Abonnement ACTIF/EXPIRE sans paiement réussi | 0 | ✅ |
| Abonnement ANNULE avec paiement réussi | 0 | ✅ |
| Mot de passe non haché BCrypt | 0 | ✅ |
| Notification vers une autre organisation | 0 | ✅ |
| Textes de remplissage (lorem, test, xxx…) | 0 | ✅ |

## 8. Cohérence avec les entités JPA

Pour chaque entité de `backend/src/main/java/com/trackit/entity`, le nom de table, les colonnes, les types,
le caractère obligatoire, l'unicité et les relations `@ManyToOne` / `@JoinColumn` correspondent à la base
importée. Hibernate le vérifie lui-même au démarrage (`spring.jpa.hibernate.ddl-auto: validate`) : ce
démarrage a réussi sur la base issue du dump.

| Table | Entité JPA | Colonnes réelles (type, NOT NULL, DEFAULT, AI = AUTO_INCREMENT) | Hibernate `validate` |
|---|---|---|---|
| `role` | `Role` | `id_role` bigint NOT NULL AI, `nom` varchar(50) NOT NULL, `description` varchar(255) | ✅ |
| `statut` | `Statut` | `id_statut` bigint NOT NULL AI, `nom` varchar(50) NOT NULL, `description` varchar(255) | ✅ |
| `priorite` | `Priorite` | `id_priorite` bigint NOT NULL AI, `nom` varchar(50) NOT NULL, `niveau` int NOT NULL, `description` varchar(255) | ✅ |
| `type_anomalie` | `TypeAnomalie` | `id_type` bigint NOT NULL AI, `nom` varchar(100) NOT NULL, `description` varchar(255) | ✅ |
| `organisation` | `Organisation` | `id_organisation` bigint NOT NULL AI, `nom` varchar(150) NOT NULL, `email` varchar(150) NOT NULL, `date_creation` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP | ✅ |
| `utilisateur` | `Utilisateur` | `id_utilisateur` bigint NOT NULL AI, `nom` varchar(100) NOT NULL, `prenom` varchar(100) NOT NULL, `email` varchar(150) NOT NULL, `mot_de_passe` varchar(255) NOT NULL, `actif` tinyint(1) NOT NULL DEFAULT 1, `date_creation` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP, `id_role` bigint NOT NULL, `id_organisation` bigint NOT NULL | ✅ |
| `systeme_it` | `SystemeIt` | `id_systeme` bigint NOT NULL AI, `nom` varchar(150) NOT NULL, `description` text, `adresse` varchar(255), `actif` tinyint(1) NOT NULL DEFAULT 1, `id_organisation` bigint NOT NULL | ✅ |
| `anomalie` | `Anomalie` | `id_anomalie` bigint NOT NULL AI, `titre` varchar(200) NOT NULL, `description` text NOT NULL, `date_creation` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP, `date_resolution` datetime, `id_auteur` bigint NOT NULL, `id_operateur` bigint, `id_systeme` bigint NOT NULL, `id_type` bigint NOT NULL, `id_priorite` bigint NOT NULL, `id_statut` bigint NOT NULL | ✅ |
| `commentaire` | `Commentaire` | `id_commentaire` bigint NOT NULL AI, `contenu` text NOT NULL, `date_creation` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP, `id_anomalie` bigint NOT NULL, `id_utilisateur` bigint NOT NULL | ✅ |
| `historique` | `Historique` | `id_historique` bigint NOT NULL AI, `action` varchar(255) NOT NULL, `ancien_statut` varchar(50), `nouveau_statut` varchar(50), `ancienne_priorite` varchar(50), `nouvelle_priorite` varchar(50), `date_modification` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP, `id_anomalie` bigint NOT NULL, `id_utilisateur` bigint NOT NULL | ✅ |
| `piece_jointe` | `PieceJointe` | `id_piece_jointe` bigint NOT NULL AI, `nom_fichier` varchar(255) NOT NULL, `chemin` varchar(500) NOT NULL, `type_fichier` varchar(100) NOT NULL, `date_ajout` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP, `id_anomalie` bigint NOT NULL, `id_utilisateur` bigint NOT NULL | ✅ |
| `notification` | `Notification` | `id_notification` bigint NOT NULL AI, `message` text NOT NULL, `date_creation` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP, `lu` tinyint(1) NOT NULL DEFAULT 0, `id_utilisateur` bigint NOT NULL, `id_anomalie` bigint | ✅ |
| `rapport` | `Rapport` | `id_rapport` bigint NOT NULL AI, `titre` varchar(200) NOT NULL, `type` varchar(100) NOT NULL, `format` varchar(20) NOT NULL, `date_generation` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP, `chemin_fichier` varchar(500), `id_analyste` bigint NOT NULL | ✅ |
| `abonnement` | `Abonnement` | `id_abonnement` bigint NOT NULL AI, `offre` varchar(100) NOT NULL, `date_debut` date NOT NULL, `date_fin` date, `statut` varchar(50) NOT NULL, `id_organisation` bigint NOT NULL | ✅ |
| `paiement` | `Paiement` | `id_paiement` bigint NOT NULL AI, `montant` decimal(10,2) NOT NULL, `devise` varchar(10) NOT NULL, `date_paiement` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP, `statut` varchar(50) NOT NULL, `reference_stripe` varchar(255), `id_abonnement` bigint NOT NULL | ✅ |

Correspondances particulières :

- `BOOLEAN` est stocké par MySQL en `tinyint(1)` : correspond aux champs Java `boolean` (`actif`, `lu`).
- `abonnement.statut`, `paiement.statut` et `rapport.format` sont des `VARCHAR` mappés par
  `@Enumerated(EnumType.STRING)` ; les contraintes CHECK de la base reprennent exactement les valeurs des
  énumérations Java (`StatutAbonnement`, `StatutPaiement`, `FormatRapport`).
- `statut.nom` et `role.nom` contiennent exactement les libellés attendus par `StatutCode` et `RoleCode`.

## 9. Comptes de démonstration

Les mots de passe sont stockés **uniquement** sous forme d'empreintes BCrypt `$2a$10$…`, compatibles avec
le `BCryptPasswordEncoder` de Spring Security. Aucun mot de passe n'est en clair dans le dump.

Organisation **TrackIT** — mot de passe de démonstration : `TrackIT@2026`.

| Rôle | E-mail |
|---|---|
| Utilisateur | lucas.janssens@trackit.demo |
| Opérateur | younes.errahmani@trackit.demo |
| Responsable technique | sophie.peeters@trackit.demo |
| Analyste | nadia.lambert@trackit.demo |
| Administrateur | julien.dubois@trackit.demo |

Les autres comptes ont des mots de passe aléatoires non communiqués : ils ne servent qu'à peupler les données.

## 10. Écarts détectés

Ces écarts entre la base réelle et les documents de conception ne sont pas masqués. Aucun n'empêche le dump
de fonctionner, donc aucun n'a été corrigé dans le modèle. Ils sont aussi tracés dans
`docs/ANALYSE_PROJET.md`.

| # | Écart | Réalité de la base | Impact |
|---|---|---|---|
| 1 | `historique.action` : VARCHAR(100) dans le dictionnaire, VARCHAR(255) dans le schéma 08 | VARCHAR(255) | Aucun (taille compatible avec les deux) |
| 2 | Contrainte `UNIQUE (id_organisation, nom)` sur `systeme_it`, absente du dictionnaire | présente | Empêche deux systèmes de même nom dans une organisation |
| 3 | Contraintes CHECK (niveau 1–4, montant > 0, date_fin ≥ date_debut, date_resolution ≥ date_creation, listes de statuts et de formats) | présentes | Traduisent les « règles du domaine » du dictionnaire |
| 4 | `flyway_schema_history` : table absente du schéma 08 | présente | Table technique, nécessaire au démarrage du backend sur la base importée |
| 5 | Diagramme de classes : `Abonnement` rattaché à `Utilisateur` | rattaché à `organisation` (schéma 08 et dictionnaire) | Aucun |
| 6 | État-transition : états « Assignée » et « Suspendue » | 4 statuts seulement : Nouvelle, En cours, Résolue, Fermée | L'affectation est la colonne `id_operateur` |
| 7 | `rapport.chemin_fichier` des données de démonstration | NULL (facultatif dans le dictionnaire) | L'interface affiche « Fichier non conservé » ; les rapports générés depuis l'application ont un fichier |
| 8 | `piece_jointe.chemin` pointe vers des fichiers | 138 fichiers réels fournis dans `backend/uploads/demo/` | À conserver avec le dump pour pouvoir télécharger les pièces jointes |
| 9 | `paiement.reference_stripe` | identifiants de session au format Stripe test (`cs_test_…`), fictifs | Aucun paiement réel |

## 11. Corrections effectuées pour ce livrable

Aucune modification du modèle (entités, migrations V1/V2, contraintes).

| Correction | Raison |
|---|---|
| Dump reconstruit dans l'**ordre des dépendances**, avec `FOREIGN_KEY_CHECKS = 1` | L'export `mysqldump` brut classait les tables par ordre alphabétique et désactivait les clés étrangères pendant l'import |
| Suppression des instructions `SET @@GLOBAL.GTID_PURGED` et `DEFAULT ENCRYPTION` | Elles empêchent l'import sur un autre serveur ou avec phpMyAdmin (droits insuffisants, syntaxe propre à MySQL 8) |
| Générateur de données : titres variés (formulations alternatives, lieu ou système précisé), 9 nouveaux cas d'incident, commentaires adaptés au type et au système | La première version n'avait que 30 titres distincts pour 438 anomalies et 18 commentaires distincts : trop répétitif pour une soutenance |
| Générateur : un incident n'est associé qu'à un système compatible | Évite des associations absurdes (ex. « sauvegarde interrompue » sur un poste utilisateur) |
| Générateur : au moins 100 rapports garantis | Le volume dépendait du tirage aléatoire (95 à un essai) |
| Générateur : organisations parcourues dans un ordre fixe (`sorted(bases)`) | Le parcours d'un `set` Python dépend de la session : les noms d'organisations changeaient d'une exécution à l'autre malgré la graine fixe |
| Générateur : sels BCrypt tirés d'un générateur à graine fixe, séparé des données | Le script reproduit désormais exactement le même fichier V3, donc la même empreinte Flyway |
| Générateur : données adaptées au contexte belge (24/10/2026) | Noms, organisations, lieux et taux de TVA cohérents avec une entreprise située en Belgique |

⚠️ La migration V3 (données de démonstration) a été régénérée : une base créée avec l'ancienne V3 serait
refusée par Flyway (checksum différent). Dans ce cas, réimporter simplement `14_DUMP_TrackIT.sql`.

## 12. Procédure de réimport

```bash
mysql -u root -p < database/14_DUMP_TrackIT.sql
```

phpMyAdmin : onglet **Importer**, choisir le fichier, format SQL, encodage utf-8. La base `trackit` est créée
automatiquement. Le fichier fait environ 0,9 Mo, ce qui reste sous la limite d'import par défaut de
phpMyAdmin (souvent 2 Mo).

Contrôles rapides après import :

```sql
USE trackit;
SELECT COUNT(*) FROM anomalie;                                   -- 427
SELECT COUNT(*) FROM commentaire c LEFT JOIN anomalie a USING(id_anomalie) WHERE a.id_anomalie IS NULL;  -- 0
SELECT s.nom, COUNT(*) FROM anomalie JOIN statut s USING(id_statut) GROUP BY s.nom;
```

## 13. Mise à jour du 24/10/2026 — adaptation au contexte belge

Les données de démonstration ont été régénérées avec une version adaptée de
`database/scripts/generer_donnees_demo.py`, puis le dump a été reconstruit.

**Ce qui change** (contenu des données uniquement) :

- prénoms et noms : Peeters, Janssens, Dubois, Lambert, Mertens… ;
- organisations clientes : Meuse, Sambre, Ardenne, Brabant… en SA / SRL ;
- lieux cités dans les anomalies : agences de Bruxelles et de Liège, site de Namur, entrepôt de Charleroi ;
- taux de TVA de l'incident de facturation : 6 % appliqué au lieu de 21 % ;
- comptes de démonstration (§ 9) ; le compte Opérateur `younes.errahmani@trackit.demo` est conservé.

**Ce qui ne change pas** : structure (V1), référentiels (V2), contraintes, format du dump, et tous les
volumes et distributions des § 2 et § 3. Les nouvelles listes de noms ont la même taille que les
anciennes : la suite des tirages aléatoires est donc identique, seuls les libellés diffèrent.

**Contrôles rejoués sur MariaDB 10.11** (moteur de la base livrée avec XAMPP) :

| Contrôle | Résultat |
|---|---|
| Import du dump dans une base vide | ✅ aucune erreur |
| Réimport du même fichier sur la base remplie | ✅ code retour 0 |
| Volumes et distributions (§ 2, § 3) | ✅ identiques aux valeurs indiquées |
| Lignes orphelines sur les 20 clés étrangères | ✅ 0 |
| Contrôles de cohérence métier (§ 7) | ✅ 0 ligne en défaut pour chacun |
| Jointure anomalie ⨝ auteur ⨝ système ⨝ organisation ⨝ type ⨝ priorité ⨝ statut | ✅ 427 / 427 |
| Jointure paiement ⨝ abonnement ⨝ organisation | ✅ 368 / 368 |
| E-mails en double | ✅ 0 |
| Mot de passe `TrackIT@2026` vérifié contre l'empreinte BCrypt des 5 comptes de démonstration | ✅ 5 / 5 |
| Références aux anciennes données (noms, villes, formes juridiques) | ✅ aucune |
| Exécution du générateur deux fois de suite | ✅ fichiers V3 et pièces jointes identiques à l'octet près |

**Flyway** : l'empreinte de la nouvelle `V3__donnees_demonstration.sql` (`230304009`) est enregistrée
dans `flyway_schema_history`. La méthode de calcul a été validée en reconstruisant l'ancienne V3 à
partir de l'ancien dump : on retrouve exactement l'empreinte qui y figurait (`-941003426`).

**À faire avec le backend** : utiliser la nouvelle V3 et les nouvelles pièces jointes de
`backend/uploads/demo/` (une ancienne V3 serait refusée par Flyway), puis rejouer les contrôles
marqués ¹ au § 7 : démarrage Spring Boot, connexion HTTP des 5 comptes, appels API.
