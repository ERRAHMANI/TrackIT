-- Référentiels de TrackIT : valeurs définies par les documents de conception.

INSERT INTO role (id_role, nom, description) VALUES
    (1, 'Utilisateur', 'Consulte et recherche les anomalies de son organisation'),
    (2, 'Opérateur', 'Déclare, classifie et traite les anomalies'),
    (3, 'Responsable technique', 'Supervise, priorise et réaffecte les anomalies, contrôle les délais'),
    (4, 'Analyste', 'Produit les statistiques, les rapports et les exports'),
    (5, 'Administrateur', 'Gère les comptes, les référentiels, les systèmes et l''abonnement Premium');

INSERT INTO statut (id_statut, nom, description) VALUES
    (1, 'Nouvelle', 'Anomalie déclarée, en attente de prise en charge'),
    (2, 'En cours', 'Anomalie en cours de diagnostic ou de correction'),
    (3, 'Résolue', 'Correction appliquée, en attente de validation'),
    (4, 'Fermée', 'Résolution validée, anomalie définitivement close');

INSERT INTO priorite (id_priorite, nom, niveau, description) VALUES
    (1, 'Basse', 1, 'Gêne mineure, contournement possible'),
    (2, 'Moyenne', 2, 'Fonctionnalité dégradée pour une partie des utilisateurs'),
    (3, 'Haute', 3, 'Fonctionnalité importante indisponible'),
    (4, 'Critique', 4, 'Service essentiel interrompu, impact sur l''activité');

INSERT INTO type_anomalie (id_type, nom, description) VALUES
    (1, 'Réseau', 'Connectivité, VPN, Wi-Fi, DNS, équipements réseau'),
    (2, 'Application', 'Dysfonctionnement d''une application métier'),
    (3, 'Matériel', 'Poste de travail, imprimante, serveur physique, périphérique'),
    (4, 'Accès', 'Authentification, droits, comptes verrouillés'),
    (5, 'Base de données', 'Erreur, lenteur ou indisponibilité d''une base de données'),
    (6, 'Performance', 'Temps de réponse dégradé, saturation de ressources'),
    (7, 'Fonctionnel', 'Comportement non conforme aux règles de gestion'),
    (8, 'Autre', 'Anomalie ne relevant d''aucune autre catégorie');
