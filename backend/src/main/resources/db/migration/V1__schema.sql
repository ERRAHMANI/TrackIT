-- TrackIT — schéma de la base de données
-- Conforme au schéma 08 et au dictionnaire de données 09.

CREATE TABLE role (
    id_role      BIGINT       NOT NULL AUTO_INCREMENT,
    nom          VARCHAR(50)  NOT NULL,
    description  VARCHAR(255) NULL,
    CONSTRAINT pk_role PRIMARY KEY (id_role),
    CONSTRAINT uk_role_nom UNIQUE (nom)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE organisation (
    id_organisation BIGINT       NOT NULL AUTO_INCREMENT,
    nom             VARCHAR(150) NOT NULL,
    email           VARCHAR(150) NOT NULL,
    date_creation   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_organisation PRIMARY KEY (id_organisation),
    CONSTRAINT uk_organisation_email UNIQUE (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE utilisateur (
    id_utilisateur  BIGINT       NOT NULL AUTO_INCREMENT,
    nom             VARCHAR(100) NOT NULL,
    prenom          VARCHAR(100) NOT NULL,
    email           VARCHAR(150) NOT NULL,
    mot_de_passe    VARCHAR(255) NOT NULL,
    actif           BOOLEAN      NOT NULL DEFAULT TRUE,
    date_creation   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    id_role         BIGINT       NOT NULL,
    id_organisation BIGINT       NOT NULL,
    CONSTRAINT pk_utilisateur PRIMARY KEY (id_utilisateur),
    CONSTRAINT uk_utilisateur_email UNIQUE (email),
    CONSTRAINT fk_utilisateur_role FOREIGN KEY (id_role) REFERENCES role (id_role),
    CONSTRAINT fk_utilisateur_organisation FOREIGN KEY (id_organisation) REFERENCES organisation (id_organisation),
    INDEX idx_utilisateur_role (id_role),
    INDEX idx_utilisateur_organisation (id_organisation)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE systeme_it (
    id_systeme      BIGINT       NOT NULL AUTO_INCREMENT,
    nom             VARCHAR(150) NOT NULL,
    description     TEXT         NULL,
    adresse         VARCHAR(255) NULL,
    actif           BOOLEAN      NOT NULL DEFAULT TRUE,
    id_organisation BIGINT       NOT NULL,
    CONSTRAINT pk_systeme_it PRIMARY KEY (id_systeme),
    CONSTRAINT uk_systeme_it_nom_organisation UNIQUE (id_organisation, nom),
    CONSTRAINT fk_systeme_it_organisation FOREIGN KEY (id_organisation) REFERENCES organisation (id_organisation)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE type_anomalie (
    id_type     BIGINT       NOT NULL AUTO_INCREMENT,
    nom         VARCHAR(100) NOT NULL,
    description VARCHAR(255) NULL,
    CONSTRAINT pk_type_anomalie PRIMARY KEY (id_type),
    CONSTRAINT uk_type_anomalie_nom UNIQUE (nom)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE priorite (
    id_priorite BIGINT       NOT NULL AUTO_INCREMENT,
    nom         VARCHAR(50)  NOT NULL,
    niveau      INT          NOT NULL,
    description VARCHAR(255) NULL,
    CONSTRAINT pk_priorite PRIMARY KEY (id_priorite),
    CONSTRAINT uk_priorite_nom UNIQUE (nom),
    CONSTRAINT ck_priorite_niveau CHECK (niveau BETWEEN 1 AND 4)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE statut (
    id_statut   BIGINT       NOT NULL AUTO_INCREMENT,
    nom         VARCHAR(50)  NOT NULL,
    description VARCHAR(255) NULL,
    CONSTRAINT pk_statut PRIMARY KEY (id_statut),
    CONSTRAINT uk_statut_nom UNIQUE (nom)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE anomalie (
    id_anomalie     BIGINT       NOT NULL AUTO_INCREMENT,
    titre           VARCHAR(200) NOT NULL,
    description     TEXT         NOT NULL,
    date_creation   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    date_resolution DATETIME     NULL,
    id_auteur       BIGINT       NOT NULL,
    id_operateur    BIGINT       NULL,
    id_systeme      BIGINT       NOT NULL,
    id_type         BIGINT       NOT NULL,
    id_priorite     BIGINT       NOT NULL,
    id_statut       BIGINT       NOT NULL,
    CONSTRAINT pk_anomalie PRIMARY KEY (id_anomalie),
    CONSTRAINT fk_anomalie_auteur FOREIGN KEY (id_auteur) REFERENCES utilisateur (id_utilisateur),
    CONSTRAINT fk_anomalie_operateur FOREIGN KEY (id_operateur) REFERENCES utilisateur (id_utilisateur),
    CONSTRAINT fk_anomalie_systeme FOREIGN KEY (id_systeme) REFERENCES systeme_it (id_systeme),
    CONSTRAINT fk_anomalie_type FOREIGN KEY (id_type) REFERENCES type_anomalie (id_type),
    CONSTRAINT fk_anomalie_priorite FOREIGN KEY (id_priorite) REFERENCES priorite (id_priorite),
    CONSTRAINT fk_anomalie_statut FOREIGN KEY (id_statut) REFERENCES statut (id_statut),
    CONSTRAINT ck_anomalie_dates CHECK (date_resolution IS NULL OR date_resolution >= date_creation),
    INDEX idx_anomalie_auteur (id_auteur),
    INDEX idx_anomalie_operateur (id_operateur),
    INDEX idx_anomalie_systeme (id_systeme),
    INDEX idx_anomalie_type (id_type),
    INDEX idx_anomalie_priorite (id_priorite),
    INDEX idx_anomalie_statut (id_statut),
    INDEX idx_anomalie_date_creation (date_creation),
    INDEX idx_anomalie_date_resolution (date_resolution)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE commentaire (
    id_commentaire BIGINT   NOT NULL AUTO_INCREMENT,
    contenu        TEXT     NOT NULL,
    date_creation  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    id_anomalie    BIGINT   NOT NULL,
    id_utilisateur BIGINT   NOT NULL,
    CONSTRAINT pk_commentaire PRIMARY KEY (id_commentaire),
    CONSTRAINT fk_commentaire_anomalie FOREIGN KEY (id_anomalie) REFERENCES anomalie (id_anomalie) ON DELETE CASCADE,
    CONSTRAINT fk_commentaire_utilisateur FOREIGN KEY (id_utilisateur) REFERENCES utilisateur (id_utilisateur),
    INDEX idx_commentaire_anomalie (id_anomalie),
    INDEX idx_commentaire_utilisateur (id_utilisateur)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE historique (
    id_historique     BIGINT       NOT NULL AUTO_INCREMENT,
    action            VARCHAR(255) NOT NULL,
    ancien_statut     VARCHAR(50)  NULL,
    nouveau_statut    VARCHAR(50)  NULL,
    ancienne_priorite VARCHAR(50)  NULL,
    nouvelle_priorite VARCHAR(50)  NULL,
    date_modification DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    id_anomalie       BIGINT       NOT NULL,
    id_utilisateur    BIGINT       NOT NULL,
    CONSTRAINT pk_historique PRIMARY KEY (id_historique),
    CONSTRAINT fk_historique_anomalie FOREIGN KEY (id_anomalie) REFERENCES anomalie (id_anomalie) ON DELETE CASCADE,
    CONSTRAINT fk_historique_utilisateur FOREIGN KEY (id_utilisateur) REFERENCES utilisateur (id_utilisateur),
    INDEX idx_historique_anomalie (id_anomalie),
    INDEX idx_historique_utilisateur (id_utilisateur)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE piece_jointe (
    id_piece_jointe BIGINT       NOT NULL AUTO_INCREMENT,
    nom_fichier     VARCHAR(255) NOT NULL,
    chemin          VARCHAR(500) NOT NULL,
    type_fichier    VARCHAR(100) NOT NULL,
    date_ajout      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    id_anomalie     BIGINT       NOT NULL,
    id_utilisateur  BIGINT       NOT NULL,
    CONSTRAINT pk_piece_jointe PRIMARY KEY (id_piece_jointe),
    CONSTRAINT fk_piece_jointe_anomalie FOREIGN KEY (id_anomalie) REFERENCES anomalie (id_anomalie) ON DELETE CASCADE,
    CONSTRAINT fk_piece_jointe_utilisateur FOREIGN KEY (id_utilisateur) REFERENCES utilisateur (id_utilisateur),
    INDEX idx_piece_jointe_anomalie (id_anomalie),
    INDEX idx_piece_jointe_utilisateur (id_utilisateur)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE notification (
    id_notification BIGINT   NOT NULL AUTO_INCREMENT,
    message         TEXT     NOT NULL,
    date_creation   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    lu              BOOLEAN  NOT NULL DEFAULT FALSE,
    id_utilisateur  BIGINT   NOT NULL,
    id_anomalie     BIGINT   NULL,
    CONSTRAINT pk_notification PRIMARY KEY (id_notification),
    CONSTRAINT fk_notification_utilisateur FOREIGN KEY (id_utilisateur) REFERENCES utilisateur (id_utilisateur),
    CONSTRAINT fk_notification_anomalie FOREIGN KEY (id_anomalie) REFERENCES anomalie (id_anomalie) ON DELETE CASCADE,
    INDEX idx_notification_utilisateur_lu (id_utilisateur, lu),
    INDEX idx_notification_anomalie (id_anomalie)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE rapport (
    id_rapport      BIGINT       NOT NULL AUTO_INCREMENT,
    titre           VARCHAR(200) NOT NULL,
    type            VARCHAR(100) NOT NULL,
    format          VARCHAR(20)  NOT NULL,
    date_generation DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    chemin_fichier  VARCHAR(500) NULL,
    id_analyste     BIGINT       NOT NULL,
    CONSTRAINT pk_rapport PRIMARY KEY (id_rapport),
    CONSTRAINT fk_rapport_analyste FOREIGN KEY (id_analyste) REFERENCES utilisateur (id_utilisateur),
    CONSTRAINT ck_rapport_format CHECK (format IN ('PDF', 'EXCEL')),
    INDEX idx_rapport_analyste (id_analyste)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE abonnement (
    id_abonnement   BIGINT       NOT NULL AUTO_INCREMENT,
    offre           VARCHAR(100) NOT NULL,
    date_debut      DATE         NOT NULL,
    date_fin        DATE         NULL,
    statut          VARCHAR(50)  NOT NULL,
    id_organisation BIGINT       NOT NULL,
    CONSTRAINT pk_abonnement PRIMARY KEY (id_abonnement),
    CONSTRAINT fk_abonnement_organisation FOREIGN KEY (id_organisation) REFERENCES organisation (id_organisation),
    CONSTRAINT ck_abonnement_dates CHECK (date_fin IS NULL OR date_fin >= date_debut),
    CONSTRAINT ck_abonnement_statut CHECK (statut IN ('EN_ATTENTE', 'ACTIF', 'EXPIRE', 'ANNULE')),
    INDEX idx_abonnement_organisation (id_organisation)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE paiement (
    id_paiement      BIGINT        NOT NULL AUTO_INCREMENT,
    montant          DECIMAL(10,2) NOT NULL,
    devise           VARCHAR(10)   NOT NULL,
    date_paiement    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    statut           VARCHAR(50)   NOT NULL,
    reference_stripe VARCHAR(255)  NULL,
    id_abonnement    BIGINT        NOT NULL,
    CONSTRAINT pk_paiement PRIMARY KEY (id_paiement),
    CONSTRAINT uk_paiement_reference_stripe UNIQUE (reference_stripe),
    CONSTRAINT fk_paiement_abonnement FOREIGN KEY (id_abonnement) REFERENCES abonnement (id_abonnement),
    CONSTRAINT ck_paiement_montant CHECK (montant > 0),
    CONSTRAINT ck_paiement_statut CHECK (statut IN ('EN_ATTENTE', 'REUSSI', 'ECHOUE', 'ANNULE')),
    INDEX idx_paiement_abonnement (id_abonnement)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
