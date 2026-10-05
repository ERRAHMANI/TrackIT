# TrackIT

Application web SaaS de **gestion, de suivi et de traitement des anomalies informatiques**,
réalisée dans le cadre d'un projet de fin d'études.

---

## 1. Présentation

TrackIT permet à une organisation de :

- **déclarer** une anomalie (titre, description, système IT, type, priorité, pièce jointe) ;
- la **suivre** : recherche, filtres cumulables, affectation à un opérateur, commentaires,
  historique complet des modifications, notifications ;
- la **traiter** selon un cycle de vie strict à quatre statuts :
  **Nouvelle → En cours → Résolue → Fermée** (réouverture possible : Résolue → En cours) ;
- la **superviser** : contrôle des délais par priorité, charge des opérateurs ;
- l'**analyser** : tableau de bord, statistiques, incidents récurrents, rapports PDF / Excel ;
- l'**administrer** : comptes et rôles, systèmes IT, référentiels, abonnement Premium (Stripe).

Chaque organisation cliente ne voit que ses propres données (multi-organisation).


## 2. Architecture

```
Navigateur ── HTML / CSS / JavaScript (frontend/)      ← conforme au diagramme de déploiement
     │  JSON + JWT (Authorization: Bearer …)
     ▼
Spring Boot (backend/)
  Controller → Service → Repository (Spring Data JPA) → MySQL
  Spring Security + JWT · Bean Validation · gestion globale des erreurs
  Flyway (migrations) · OpenAPI / Swagger · OpenPDF / Apache POI · Stripe Checkout
     │
     ├── MySQL 8 (base « trackit »)
     └── Stripe API (mode test) — ou mode simulation sans clé
```

- Les contrôles d'accès sont faits **côté serveur** (`@PreAuthorize` + règles métier dans les
  services) ; le frontend se contente de masquer les actions non autorisées.
- Les recherches, filtres, tris, paginations et statistiques sont **exécutés en base**
  (Specifications JPA et requêtes d'agrégation), jamais en mémoire.
- Les entités JPA ne sont jamais exposées : l'API ne manipule que des DTO.

## 3. Technologies

| Couche | Technologies |
|---|---|
| Backend | Java 17, Spring Boot 3.3, Spring Data JPA / Hibernate, Spring Security, JWT (jjwt), Bean Validation, Maven |
| Base de données | MySQL 8, migrations Flyway |
| Documentation API | springdoc-openapi (Swagger UI) |
| Exports | OpenPDF (PDF), Apache POI (Excel .xlsx) |
| Paiement | Stripe Java (Checkout, webhooks signés) |
| Frontend | HTML5, CSS3, JavaScript natif (modules ES), aucune étape de compilation |
| Tests | JUnit 5, Spring Boot Test, MockMvc, H2 (mode MySQL) |

## 4. Prérequis

| Outil | Version testée | Vérification |
|---|---|---|
| JDK | 17 | `java -version` |
| Maven | 3.9+ | `mvn -v` |
| MySQL | 8.0+ | `mysql --version` |
| Node.js (pour servir le frontend) | 18+ | `node -v` |

Sous macOS : `brew install openjdk@17 maven mysql node` puis `brew services start mysql`.

## 5. Configuration MySQL

Créer la base et un utilisateur dédié (remplacer le mot de passe) :

```sql
CREATE DATABASE trackit CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'trackit'@'localhost' IDENTIFIED BY 'votre-mot-de-passe';
GRANT ALL PRIVILEGES ON trackit.* TO 'trackit'@'localhost';
FLUSH PRIVILEGES;
```

```bash
mysql -u root -p < fichier.sql   # ou coller les commandes dans le client mysql
```

## 6. Variables d'environnement

Copier le modèle puis renseigner les valeurs :

```bash
cp .env.example .env
```

Le backend lit automatiquement `trackit/.env` (ou `trackit/backend/.env`) ; les variables
d'environnement du système sont aussi prises en compte. **Le fichier `.env` est ignoré par Git.**

| Variable | Obligatoire | Rôle |
|---|---|---|
| `DB_HOST`, `DB_PORT`, `DB_NAME` | non (défaut `localhost`, `3306`, `trackit`) | Connexion MySQL |
| `DB_USERNAME`, `DB_PASSWORD` | **oui** | Identifiants MySQL |
| `JWT_SECRET` | **oui** (≥ 32 caractères) | Clé de signature des jetons — `openssl rand -base64 48` |
| `JWT_EXPIRATION_MINUTES` | non (480) | Durée de validité d'un jeton |
| `FRONTEND_URL` | non (`http://localhost:5173`) | URL de retour après paiement Stripe |
| `CORS_ALLOWED_ORIGINS` | non | Origines autorisées à appeler l'API |
| `STRIPE_SECRET_KEY` | non | Clé **de test** `sk_test_…` ; vide = mode simulation |
| `STRIPE_WEBHOOK_SECRET` | non | Secret `whsec_…` du webhook Stripe |
| `UPLOADS_DIR`, `REPORTS_DIR` | non (`./uploads`, `./reports`) | Stockage des pièces jointes et rapports |
| `FLYWAY_LOCATIONS` | non | `classpath:db/migration` pour une base **sans** données de démonstration |

## 7. Installation

```bash
git clone <url-du-depot> trackit
cd trackit
cp .env.example .env        # puis éditer DB_PASSWORD et JWT_SECRET
cd backend
mvn clean package           # compile, exécute les 45 tests et produit le JAR
```

## 8. Lancement du backend

Deux possibilités, au choix :

- **Base vide** : démarrer simplement le backend. Flyway crée le schéma (V1), insère les
  référentiels (V2) et les données de démonstration (V3).
- **À partir du dump** : importer d'abord le dump (section 10), puis démarrer le backend ;
  Flyway constate que la base est à jour.

```bash
cd backend
mvn spring-boot:run
# ou, après mvn package :
java -jar target/trackit-backend-1.0.0.jar
```

L'API écoute sur **http://localhost:8080/api**. Les pièces jointes de démonstration sont
fournies dans `backend/uploads/demo/` : lancer le backend depuis le dossier `backend/`
(ou définir `UPLOADS_DIR`) pour qu'elles soient téléchargeables.

## 9. Lancement du frontend

```bash
cd frontend
node serveur.mjs            # ou : npm start
```

Ouvrir **http://localhost:5173**. Si l'API n'est pas sur `localhost:8080`, modifier
`frontend/config.js`. Tout autre serveur de fichiers statiques convient, à condition de
servir le dossier `frontend/` sur une origine déclarée dans `CORS_ALLOWED_ORIGINS`.

## 10. Import du dump SQL

`database/14_DUMP_TrackIT.sql` est un dump MySQL complet et réimportable : structure
(clés primaires et étrangères, contraintes UNIQUE / CHECK, index), référentiels, données de
démonstration et table d'historique Flyway. Il crée la base `trackit` si nécessaire. Le rapport
de contrôle de l'import est dans `database/14_CONTROLE_DUMP_TrackIT.md`.

```bash
mysql -u root -p < database/14_DUMP_TrackIT.sql
```

Volumétrie :

| Table | Lignes | | Table | Lignes |
|---|---:|---|---|---:|
| role | 5 | | commentaire | 744 |
| statut | 4 | | historique | 1 889 |
| priorite | 4 | | piece_jointe | 138 |
| type_anomalie | 8 | | notification | 2 456 |
| organisation | 101 | | rapport | 127 |
| utilisateur | 383 | | abonnement | 368 |
| systeme_it | 218 | | paiement | 368 |
| anomalie | 427 | |  |  |

Les référentiels (rôles, statuts, priorités, types) ne contiennent que les valeurs réellement
définies par les documents. Les données de démonstration sont produites de façon déterministe
par `database/scripts/generer_donnees_demo.py` : chaque anomalie suit un cycle de vie cohérent
(affectation, changements de statut, commentaires, notifications, dates de résolution).

Pour régénérer le dump après modification des données :

```bash
python3 database/scripts/generer_donnees_demo.py        # réécrit la migration V3
# recréer une base vide, démarrer le backend (migrations), puis :
mysqldump -u root -p --databases trackit --single-transaction --routines --triggers \
  --set-gtid-purged=OFF --no-tablespaces --default-character-set=utf8mb4 > database/14_DUMP_TrackIT.sql
```

## 11. Accès Swagger

- Swagger UI : **http://localhost:8080/swagger-ui.html**
- Description OpenAPI : http://localhost:8080/v3/api-docs

Pour tester pendant la soutenance :

1. `POST /api/auth/connexion` avec `{"email": "younes.errahmani@trackit.demo", "motDePasse": "TrackIT@2026"}` ;
2. copier la valeur `jeton` ;
3. cliquer sur **Authorize** et la coller (sans le préfixe `Bearer`).

Les codes HTTP utilisés : 200, 201, 204, 400 (validation, détail par champ dans `champs`),
401 (non authentifié), 402 (Premium requis), 403 (rôle insuffisant), 404, 409 (règle métier :
transition interdite, doublon, suppression impossible), 413, 500.

## 12. Comptes de démonstration

Organisation **TrackIT** — mot de passe commun : **`TrackIT@2026`**
(stocké uniquement sous forme de hash BCrypt en base).

| Rôle | E-mail |
|---|---|
| Utilisateur | `salma.bennani@trackit.demo` |
| Opérateur | `younes.errahmani@trackit.demo` |
| Responsable technique | `karim.alaoui@trackit.demo` |
| Analyste | `nadia.tazi@trackit.demo` |
| Administrateur | `amine.idrissi@trackit.demo` |

L'écran de connexion propose aussi ces comptes dans l'encadré « Comptes de démonstration ».
Les autres utilisateurs générés ont des mots de passe aléatoires non communiqués.

## 13. Rôles et permissions

| Action | Utilisateur | Opérateur | Resp. technique | Analyste | Administrateur |
|---|:-:|:-:|:-:|:-:|:-:|
| Consulter, rechercher, filtrer les anomalies | ✔ | ✔ | ✔ | ✔ | ✔ |
| Déclarer une anomalie | | ✔ | | | |
| Modifier / classifier, changer statut ou priorité | | ✔ ¹ | ✔ | | |
| Commenter, joindre un fichier | | ✔ | ✔ | | |
| Prendre en charge une anomalie non affectée | | ✔ | | | |
| Affecter / réaffecter à un opérateur | | | ✔ | | |
| Fermer une anomalie résolue | | | ✔ | | ✔ |
| Supervision et contrôle des délais | | | ✔ | | |
| Statistiques détaillées | | | ✔ | ✔ | |
| Rapports PDF / Excel (Premium) | | | | ✔ | |
| Comptes, rôles, systèmes IT, référentiels | | | | | ✔ |
| Abonnement Premium, suppression d'anomalie | | | | | ✔ |

¹ uniquement sur les anomalies qu'il a déclarées ou qui lui sont affectées.

Transitions de statut autorisées : Nouvelle → En cours, En cours → Résolue,
Résolue → En cours (réouverture), Résolue → Fermée. Une anomalie fermée n'est plus modifiable.
Chaque création, modification, affectation, changement de statut ou de priorité et ajout de
pièce jointe est enregistré dans l'historique (auteur, date, ancienne et nouvelle valeur).

### Premium et Stripe

L'export des rapports PDF / Excel est réservé aux organisations ayant un abonnement actif.

- **Sans clé Stripe** (par défaut) : le bouton « Souscrire » mène à une page de **simulation**
  (paiement accepté, refusé ou annulé). Aucun appel à Stripe n'est effectué.
- **Avec une clé de test** (`STRIPE_SECRET_KEY=sk_test_…`) : redirection vers Stripe Checkout
  (carte de test `4242 4242 4242 4242`). Au retour, l'état est relu auprès de Stripe. Pour
  recevoir les webhooks en local :
  `stripe listen --forward-to localhost:8080/api/stripe/webhook` puis renseigner `STRIPE_WEBHOOK_SECRET`.

## 14. Structure du projet

```
trackit/
├── backend/
│   ├── pom.xml
│   ├── uploads/demo/                 pièces jointes des données de démonstration
│   └── src/
│       ├── main/java/com/trackit/
│       │   ├── config/               propriétés, OpenAPI
│       │   ├── controller/           contrôleurs REST
│       │   ├── dto/                  objets d'échange (requêtes / réponses)
│       │   ├── entity/               entités JPA (15 tables) et énumérations métier
│       │   ├── exception/            ApiException, gestion globale des erreurs
│       │   ├── mapper/               conversion entité → DTO
│       │   ├── repository/           Spring Data JPA, Specifications, agrégations
│       │   ├── security/             JWT, filtre, configuration Spring Security
│       │   └── service/              règles métier (anomalies, statistiques, Premium, rapports…)
│       ├── main/resources/
│       │   ├── application.yml
│       │   └── db/migration (V1 schéma, V2 référentiels) · db/demo (V3 données)
│       └── test/                     tests d'intégration et unitaires
├── frontend/
│   ├── index.html · config.js · serveur.mjs
│   ├── css/styles.css
│   └── js/                           app.js (routes), api.js, ui.js, graphiques.js, pages/…
├── database/
│   ├── 14_DUMP_TrackIT.sql
│   └── scripts/generer_donnees_demo.py
├
├── .env.example
└── README.md
```

## 15. Procédure de test

### Tests automatisés

```bash
cd backend
mvn test
```

45 tests sur une base H2 en mémoire (aucune base MySQL nécessaire) :

| Classe | Couvre |
|---|---|
| `AuthentificationTest` | connexion, mauvais mot de passe, compte désactivé, jeton absent ou falsifié, inscription |
| `AutorisationTest` | droits par rôle, Premium requis (402), isolation entre organisations |
| `CycleDeVieAnomalieTest` | déclaration (champs obligatoires, auteur et date automatiques), transitions de statut, fermeture, réouverture, priorité, affectation, historique, notifications |
| `RechercheAnomaliesTest` | texte, statut, numéro, système, opérateur, « Mes tâches », pagination et tri |
| `StatutCodeTest` | les quatre statuts et leurs transitions |

### Scénario de démonstration manuel

1. **Accueil** (http://localhost:5173) → « Se connecter » → compte Opérateur.
2. **Tableau de bord** : indicateurs, évolution, répartition ; cliquer sur « En cours » ouvre la liste filtrée.
3. **Anomalies** : rechercher « réseau », filtrer par statut et priorité, trier par colonne.
4. **+ Nouvelle anomalie** : cliquer « Enregistrer » sans rien remplir → erreurs sous les champs ;
   compléter (pièce jointe facultative) → confirmation « Anomalie créée » → « Voir la fiche ».
5. **Fiche** : « Changer le statut » → En cours (l'opérateur prend l'anomalie en charge) →
   « Voir l'historique » ; ajouter un commentaire ; télécharger la pièce jointe.
6. Se reconnecter en **Responsable technique** : « Assigner » un opérateur depuis une fiche,
   écran **Supervision** (retards, charge), fermer une anomalie résolue.
7. **Analyste** : **Statistiques** puis **Rapports** → générer un PDF ou un Excel.
8. **Administrateur** : **Utilisateurs** (création, rôle, désactivation), **Systèmes IT**,
   **Référentiels**, **Premium** → souscription en mode simulation.
9. **Utilisateur** : consultation seule, aucune action de modification proposée ;
   l'API refuse aussi ces actions (403), par exemple dans Swagger.
