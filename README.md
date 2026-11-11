# TrackIT

Application web SaaS de **gestion, de suivi et de traitement des anomalies informatiques**,
réalisée dans le cadre d'un projet de fin d'études.

---

## 1. Présentation ##

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
L'interface est disponible en **français, anglais et néerlandais** (section 17).


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
| Frontend | HTML5, CSS3, JavaScript natif (modules ES), aucune étape de compilation, interface FR / EN / NL |
| Déploiement | Docker (image en deux étapes), Railway (conteneur + MySQL + volume) |
| Tests | JUnit 5, Spring Boot Test, MockMvc, H2 (mode MySQL) |

## 4. Prérequis

| Outil | Version testée | Vérification |
|---|---|---|
| JDK | 17 | `java -version` |
| Maven | 3.9+ | `mvn -v` |
| MySQL | 8.0+ | `mysql --version` |
| Node.js (pour servir le frontend) | 18+ | `node -v` |
| Python (uniquement pour régénérer les données) | 3.10+ | `python --version` |

Sous macOS : `brew install openjdk@17 maven mysql node` puis `brew services start mysql`.
Sous Windows : le MySQL de **XAMPP** (MariaDB) convient ; démarrer le module MySQL depuis le
XAMPP Control Panel.
Le port 8080 doit être libre : arrêter le module Tomcat de XAMPP s'il est démarré.

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
| `SERVER_PORT` | non (`8080`) | Port d'écoute de l'API |
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
  référentiels (V2) et les données de démonstration (V3), puis recale leurs dates
  pour qu'elles se terminent la veille du démarrage (V4).
- **À partir du dump** : importer d'abord le dump (section 10), puis démarrer le backend ;
  Flyway constate que la base est à jour.

```bash
cd backend
mvn spring-boot:run
# ou, après mvn package :
java -jar target/trackit-backend-1.0.0.jar
```

L'API écoute sur **http://localhost:8080/api/v1** (version dans l'URL ; chaque réponse porte
aussi l'en-tête `X-API-Version`).

Les pièces jointes de démonstration (`backend/uploads/demo/`, 138 fichiers fictifs) sont
**versionnées** pour que l'image Docker puisse les embarquer ; elles sont produites par
`database/scripts/generer_donnees_demo.py` (section 10). Les fichiers déposés par les
utilisateurs, eux, ne sont jamais versionnés. Lancer le backend depuis le dossier `backend/`
(ou définir `UPLOADS_DIR`) pour que les pièces jointes soient téléchargeables.

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
Elles sont entièrement fictives (entreprises et collaborateurs situés en Belgique) et le script,
relancé, reproduit exactement le même fichier, empreintes BCrypt comprises.

Pour régénérer le dump après modification des données :

```bash
pip install bcrypt                                      # une seule fois
python3 database/scripts/generer_donnees_demo.py        # réécrit la migration V3 et backend/uploads/demo/
# recréer une base vide, démarrer le backend (migrations), puis :
mysqldump -u root -p --databases trackit --single-transaction --routines --triggers \
  --set-gtid-purged=OFF --no-tablespaces --default-character-set=utf8mb4 > database/14_DUMP_TrackIT.sql
```

## 11. Accès Swagger

- Swagger UI : **http://localhost:8080/swagger-ui.html**
- Description OpenAPI : http://localhost:8080/v3/api-docs

Pour tester pendant la soutenance :

1. `POST /api/v1/auth/connexion` avec `{"email": "younes.errahmani@trackit.demo", "motDePasse": "TrackIT@2026"}` ;
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
| Utilisateur | `lucas.janssens@trackit.demo` |
| Opérateur | `younes.errahmani@trackit.demo` |
| Responsable technique | `sophie.peeters@trackit.demo` |
| Analyste | `nadia.lambert@trackit.demo` |
| Administrateur | `julien.dubois@trackit.demo` |

L'écran de connexion propose aussi ces comptes dans l'encadré « Comptes de démonstration »,
**affiché uniquement en local** (`localhost` / `127.0.0.1`) : il est masqué sur tout autre domaine.
Les autres utilisateurs générés ont des mots de passe aléatoires non communiqués.

> **Données fictives, réservées à la démonstration.** Ces comptes n'existent que dans la base de
> démonstration (migration V3). Pour un environnement réel, démarrer **sans** données de
> démonstration (`FLYWAY_LOCATIONS=classpath:db/migration`) puis créer les comptes par
> l'inscription : aucun mot de passe connu n'est alors présent en base.

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
  `stripe listen --forward-to localhost:8080/api/v1/stripe/webhook` puis renseigner `STRIPE_WEBHOOK_SECRET`.

## 14. Structure du projet

```
TrackIT/
├── backend/
│   ├── pom.xml
│   ├── .env                          secrets locaux (ignoré par Git)
│   ├── uploads/demo/                 pièces jointes fictives de démonstration (versionnées)
│   ├── target/                       compilation Maven (ignoré par Git)
│   └── src/
│       ├── main/java/com/trackit/
│       │   ├── TrackItApplication.java   point d'entrée Spring Boot
│       │   ├── config/               propriétés, OpenAPI, versioning de l'API
│       │   ├── controller/           contrôleurs REST (13)
│       │   ├── dto/                  objets d'échange (requêtes / réponses)
│       │   ├── entity/               entités JPA (15 tables) et énumérations métier
│       │   ├── exception/            ApiException, gestion globale des erreurs
│       │   ├── mapper/               conversion entité → DTO
│       │   ├── repository/           Spring Data JPA, Specifications, agrégations
│       │   ├── security/             JWT, filtre, configuration Spring Security
│       │   └── service/              règles métier (anomalies, statistiques, supervision, Premium…)
│       │       └── rapport/          génération des rapports PDF (OpenPDF) et Excel (Apache POI)
│       ├── main/resources/
│       │   ├── application.yml
│       │   ├── db/migration/         V1 schéma, V2 référentiels
│       │   └── db/demo/              V3 données de démonstration, V4 recalage des dates
│       └── test/
│           ├── java/com/trackit/api/     tests d'intégration (MockMvc) + DonneesTest, TestIntegrationBase
│           ├── java/com/trackit/domain/  tests unitaires
│           └── resources/application-test.yml   profil de test (H2, sans Flyway)
├── frontend/
│   ├── index.html · config.js · serveur.mjs
│   ├── css/styles.css
│   └── js/                           app.js (routes), api.js, ui.js, graphiques.js, i18n.js, traductions.js, pages/…
├── database/
│   ├── 14_DUMP_TrackIT.sql
│   ├── 14_CONTROLE_DUMP_TrackIT.md
│   └── scripts/generer_donnees_demo.py
├── scripts/
│   └── verifier-frontend.mjs         vérification du frontend exécutée par Jenkins
├── Jenkinsfile                       pipeline d'intégration continue (Frontend → Build → Test → Package)
├── Dockerfile                        image de production (backend + frontend, section 18)
├── docker-entrypoint.sh              démarrage du conteneur (volume, port)
├── .dockerignore                     fichiers exclus de l'image (secrets, fichiers locaux)
├── .gitattributes                    fins de ligne LF pour les scripts shell
├── .env.example                      modèle des variables d'environnement (valeurs factices)
├── .gitignore
├── COMPOSANTS_TIERS.md               composants tiers et leurs licences
├── LICENSE
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

## 16. Intégration continue (Jenkins)

Le fichier `Jenkinsfile` décrit le pipeline exécuté par Jenkins à chaque `push` sur GitHub
(webhook) :

| Étape | Commande | Rôle |
|---|---|---|
| Frontend | `node scripts/verifier-frontend.mjs frontend` (à la racine) | Fichiers non vides, syntaxe JavaScript, imports existants, pages des routes présentes, ressources d'`index.html` présentes |
| Build | `mvn -B clean compile` (dans `backend/`) | Vérifie que le projet compile |
| Test | `mvn -B test` (dans `backend/`) | Exécute les tests automatisés |
| Package | `mvn -B package -DskipTests` (dans `backend/`) | Produit le JAR |

Les résultats des tests (`backend/target/surefire-reports/*.xml`) sont publiés à chaque build
(page « Résultats des tests » et courbe de tendance). En cas de succès, le JAR
(`backend/target/*.jar`) est archivé dans Jenkins. En local, Jenkins
écoute sur le port **8082** (le port 8080 est réservé à l'API) ; le webhook GitHub passe par un
tunnel ngrok. Aucun secret n'est écrit dans le `Jenkinsfile`.

## 17. Langues de l'interface

L'interface est disponible en **français** (langue de référence), **anglais** et
**néerlandais**. La langue se choisit dans l'en-tête, sur les pages publiques (accueil,
connexion, inscription) ou dans **Mon profil → Préférences** ; le choix est conservé sur le
navigateur.

- `frontend/js/traductions.js` contient le dictionnaire (textes exacts et motifs pour les
  phrases variables, par exemple « 12 résultats ») ; `frontend/js/i18n.js` traduit les pages
  à l'affichage.
- Les dates et les nombres suivent la langue : `fr-BE`, `en-GB`, `nl-BE`
  (« 12,5 % » en français et en néerlandais, « 12.5 % » en anglais).
- Les **référentiels** (statuts, priorités, types) sont traduits. Les **données saisies**
  (titres, descriptions, commentaires, noms des systèmes) restent dans leur langue d'origine.

## 18. Déploiement (Docker et Railway)

En production, le backend sert aussi le frontend : la page et l'API sont sur la **même
origine** (`frontend/config.js` choisit l'adresse de l'API selon l'environnement), et
`SecurityConfig` n'ouvre en lecture que les fichiers du client (`/`, `index.html`,
`config.js`, `css/`, `js/`, `assets/`).

### Image Docker

Le `Dockerfile` (à la racine) construit l'image en deux étapes :

1. **build** (`maven:3.9-eclipse-temurin-17`) : compile le backend et produit le JAR ;
2. **exécution** (`eclipse-temurin:17-jre`) : ne garde que le JAR, le frontend et les
   pièces jointes de démonstration.

Au démarrage, `docker-entrypoint.sh` crée les dossiers sur le volume `/data`, y copie une seule
fois les pièces jointes de démonstration, puis lance Java sur le port fourni par l'hébergeur
(`PORT`, 8080 par défaut). Les tests ne sont pas rejoués dans l'image : Jenkins les exécute.

Essai en local (Docker Desktop) :

```bash
docker build -t trackit .
docker run --env-file .env -e DB_HOST=host.docker.internal -p 8080:8080 trackit
```

Puis ouvrir http://localhost:8080.

### Railway

1. Créer un projet Railway à partir du dépôt GitHub : Railway détecte le `Dockerfile`.
2. Ajouter un service **MySQL** au projet.
3. Ajouter un **volume** au service TrackIT, monté sur `/data` (pièces jointes et rapports
   conservés entre deux déploiements).
4. Renseigner les variables du service TrackIT (onglet *Variables*) :

| Variable | Valeur |
|---|---|
| `DB_HOST`, `DB_PORT`, `DB_NAME` | Valeurs du service MySQL (`MYSQLHOST`, `MYSQLPORT`, `MYSQLDATABASE`) |
| `DB_USERNAME`, `DB_PASSWORD` | Valeurs du service MySQL (`MYSQLUSER`, `MYSQLPASSWORD`) |
| `DB_OPTIONS` | Options JDBC ajoutées à l'URL si nécessaire, par exemple `&allowPublicKeyRetrieval=true&useSSL=false` |
| `JWT_SECRET` | Nouvelle clé d'au moins 32 caractères, **différente** de celle du poste local |
| `FRONTEND_URL` | Adresse publique du site, par exemple `https://trackit.up.railway.app` |
| `FLYWAY_LOCATIONS` | `classpath:db/migration` pour démarrer **sans** données de démonstration |
| `STRIPE_SECRET_KEY`, `STRIPE_WEBHOOK_SECRET` | Facultatives : sans clé, le paiement est simulé |

5. Générer un domaine public (*Settings → Networking*), puis vérifier l'accueil, la connexion
   et Swagger (`/swagger-ui.html`).

> **Données de démonstration en ligne.** Les mots de passe des comptes de démonstration sont
> publiés dans ce README. Sur une instance accessible depuis Internet, démarrer sans données de
> démonstration (`FLYWAY_LOCATIONS=classpath:db/migration`) et créer les comptes par
> l'inscription ; l'encadré des comptes de démonstration n'est de toute façon pas affiché hors
> de `localhost`.

Aucun secret n'est écrit dans le dépôt ni dans l'image : `.env` est exclu par `.gitignore` et
`.dockerignore`, et les valeurs de production sont saisies uniquement dans Railway.