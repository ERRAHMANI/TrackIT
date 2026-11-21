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
mvn clean package           # compile, exécute les 83 tests et produit le JAR
```

## 8. Lancement du backend

Deux possibilités, au choix :

- **Base vide** : démarrer simplement le backend. Flyway crée le schéma (V1), insère les
  référentiels (V2) et les données de démonstration (V3), puis recale leurs dates
  pour qu'elles se terminent la veille du démarrage (V4). V5 ajoute ensuite la colonne de
  suppression logique des comptes (section 13).
- **À partir du dump** : importer d'abord le dump (section 10), puis démarrer le backend ;
  Flyway applique seulement les migrations postérieures au dump.

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

La documentation de l'API est **désactivée par défaut** (donc en production) : sans
`SWAGGER_ENABLED=true`, `/swagger-ui.html` et `/v3/api-docs` répondent 401. En local, pour la
démonstration, lancer le backend avec `SWAGGER_ENABLED=true` (fichier `.env` ou variable
d'environnement).

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

Ces comptes et ce mot de passe sont **les mêmes en local et sur le site en ligne** :
**https://trackit-production-a02e2e8b.up.railway.app**

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

Les **types d'anomalie et priorités** sont propres à chaque organisation : son Administrateur
gère uniquement les siens (8 types et 4 priorités par défaut sont créés à l'inscription). Les
**rôles et les quatre statuts** sont communs et en lecture seule, car ils portent le cycle de vie
de l'application.

Transitions de statut autorisées : Nouvelle → En cours, En cours → Résolue,
Résolue → En cours (réouverture), Résolue → Fermée. Une anomalie fermée n'est plus modifiable.
Chaque création, modification, affectation, changement de statut ou de priorité et ajout de
pièce jointe est enregistré dans l'historique (auteur, date, ancienne et nouvelle valeur).

### Escalade automatique des délais

Toutes les 15 minutes (`trackit.sla.intervalle-escalade`), une tâche planifiée repère les
anomalies **Nouvelle** ou **En cours** dont l'âge dépasse le délai cible de leur priorité (Basse
168 h, Moyenne 72 h, Haute 24 h, Critique 4 h : même règle que l'écran Supervision). Elle notifie
les responsables techniques actifs de l'organisation et l'opérateur affecté, **une seule fois** par
anomalie (colonne `date_escalade`, migration V8). Le cycle de vie n'est pas modifié.

### Notifications

| Événement | Personnes notifiées |
|---|---|
| Déclaration | les responsables techniques ; le déclarant reçoit un accusé de réception |
| Affectation / réaffectation | le nouvel opérateur ; l'ancien opérateur ; le déclarant, sauf s'il a déjà reçu l'un des deux messages précédents |
| Changement de statut ou de priorité | le déclarant et l'opérateur affecté (avec le nom de l'auteur du changement) |
| Passage à « Résolue » | en plus, les responsables techniques : une validation (fermeture) les attend |
| Commentaire | le déclarant et l'opérateur affecté |

L'auteur d'une action n'est jamais notifié de sa propre action.

### Suppression d'un compte (suppression logique)

Un compte est supprimé par l'administrateur (**Utilisateurs**) ou par l'utilisateur lui-même
(**Mon profil → Supprimer mon compte**, mot de passe exigé ; `POST /api/v1/profil/suppression`).

- **Compte sans aucune trace** : supprimé définitivement.
- **Compte lié à des anomalies, commentaires, historiques, pièces jointes ou rapports** :
  suppression logique (*soft delete*). La ligne reste en base pour que les clés étrangères et
  l'historique restent cohérents, mais les données personnelles sont effacées : nom « SUPPRIMÉ »,
  prénom « Utilisateur », e-mail `supprime-<id>@trackit.invalid`, mot de passe aléatoire,
  `actif = false` et `date_suppression` renseignée (migration V5). Le compte disparaît de la
  gestion des comptes, ne peut plus se connecter, son jeton est refusé immédiatement et son
  ancienne adresse e-mail peut être réutilisée.
- Les notifications, personnelles, sont effacées dans les deux cas.
- Le dernier administrateur actif d'une organisation ne peut pas supprimer son propre compte.

Code : `UtilisateurService.supprimer`, `supprimerMonCompte`, `retirer` et `anonymiser`.

### Protection contre la force brute

La connexion (`POST /api/v1/auth/connexion`) compte les échecs sur une fenêtre de 15 minutes :

| Seuil | Effet |
|---|---|
| 5 échecs pour une même adresse e-mail | ce compte est bloqué 15 minutes |
| 20 échecs depuis une même adresse IP | toutes les tentatives de cette adresse sont bloquées 15 minutes |

- Pendant le blocage, la réponse est **429 Too Many Requests**, même avec le bon mot de passe.
- Une adresse e-mail inexistante est bloquée de la même façon : la réponse ne révèle pas si un compte existe.
- Une connexion réussie remet à zéro le compteur du compte.
- Chaque blocage est écrit dans le journal (niveau `WARN`).
- Derrière le proxy de Railway, l'adresse IP du client est lue dans `X-Forwarded-For`
  (`server.forward-headers-strategy: native`).
- Les seuils se règlent dans `application.yml` (`trackit.connexion.*`). Les compteurs sont en
  mémoire : ils repartent de zéro au redémarrage, ce qui suffit pour une seule instance.

Code : `security/ProtectionConnexion.java`, appelé par `AuthService.connecter`.

### Session du client web (cookie HttpOnly)

Le client web ne voit jamais le jeton JWT : à la connexion, le serveur le dépose dans le cookie
`TRACKIT_SESSION` (`HttpOnly`, `SameSite=Strict`, `Secure` en HTTPS, `Path=/api`) et ne le renvoie
pas dans le corps de la réponse. Une faille XSS ne peut donc pas voler le jeton.

- « Se souvenir de moi » coché : cookie persistant jusqu'à l'expiration du jeton ; sinon cookie de
  session, effacé à la fermeture du navigateur. `POST /api/v1/auth/deconnexion` l'efface.
- CSRF : en plus de `SameSite=Strict`, une requête qui modifie des données avec le cookie doit
  porter l'en-tête `X-TrackIT-Client` (qu'un autre site ne peut pas ajouter) ; sinon elle reste
  anonyme (401).
- Swagger et les scripts utilisent toujours l'en-tête `Authorization: Bearer <jeton>` ; sans
  l'en-tête `X-TrackIT-Client`, la connexion renvoie le jeton dans le corps.

### Politique de mot de passe

Inspirée de la recommandation NIST SP 800-63B, elle s'applique à l'inscription, à la création
d'un compte, au changement et à la réinitialisation du mot de passe :

- au moins **12 caractères**, et au plus **72 octets** (au-delà, BCrypt ignorerait la fin) ;
- refus des **mots de passe courants** (`resources/securite/mots-de-passe-interdits.txt`) et des
  motifs triviaux (un caractère ou un petit bloc répété : `aaaaaaaaaaaa`, `abcabcabcabc`) ;
- refus d'un mot de passe qui contient le **nom, le prénom ou l'adresse e-mail** de l'utilisateur ;
- **aucune règle de composition imposée** (majuscule, chiffre…) : la longueur protège mieux.

Le formulaire affiche une jauge indicative (Trop court, Acceptable, Bon, Très bon) ; le serveur
reste seul juge. Les mots de passe existants restent valables jusqu'à leur prochain changement.

Code : `validation/PolitiqueMotDePasse.java` et l'annotation `@MotDePasseRobuste` sur les requêtes.

### Journal de sécurité

Les événements sensibles sont écrits par le logger `trackit.securite`, une ligne par événement au
format `evenement=NOM cle=valeur` (sortie standard, donc visibles dans les logs de Railway) :

| Niveau | Événements |
|---|---|
| `WARN` | `CONNEXION_ECHOUEE`, `CONNEXION_REFUSEE_BLOCAGE`, `COMPTE_BLOQUE`, `ADRESSE_BLOQUEE`, `ACCES_REFUSE` (403), `JETON_REFUSE` (401 avec un jeton) |
| `INFO` | `CONNEXION_REUSSIE`, `INSCRIPTION`, `COMPTE_CREE`, `ROLE_MODIFIE`, `COMPTE_DESACTIVE`, `COMPTE_REACTIVE`, `MOT_DE_PASSE_CHANGE`, `MOT_DE_PASSE_REINITIALISE`, `COMPTE_SUPPRIME`, `COMPTE_ANONYMISE`, `DESINSCRIPTION` |

- Jamais de mot de passe, de jeton ni de clé dans le journal ; les comptes sont désignés par leur identifiant.
- Les valeurs saisies sont nettoyées (retours à la ligne retirés, longueur bornée) pour empêcher
  l'injection de fausses lignes.
- Exemple : `evenement=ACCES_REFUSE utilisateur=12(OPERATEUR) methode=GET uri=/api/v1/utilisateurs ip=…`

Code : `security/JournalSecurite.java`.

### Premium et Stripe

L'export des rapports PDF / Excel est réservé aux organisations ayant un abonnement actif.

- **Sans clé Stripe** (par défaut) : le bouton « Souscrire » mène à une page de **simulation**
  (paiement accepté, refusé ou annulé). Aucun appel à Stripe n'est effectué.
- **Avec une clé de test** (`STRIPE_SECRET_KEY=sk_test_…`) : redirection vers Stripe Checkout
  (carte de test `4242 4242 4242 4242`). Au retour, l'état est relu auprès de Stripe. Pour
  recevoir les webhooks en local :
  `stripe listen --forward-to localhost:8080/api/v1/stripe/webhook` puis renseigner `STRIPE_WEBHOOK_SECRET`.
- Seul l'**administrateur** souscrit ; les autres rôles voient l'état de l'abonnement.
- Une **prolongation** commence le lendemain de la fin du dernier abonnement payé (en cours ou
  déjà programmé) : aucun jour payé n'est perdu ni facturé deux fois. Tant qu'elle n'a pas
  commencé, elle est affichée « À venir ».
- Un paiement resté « En attente » plus de 24 h (page de paiement abandonnée) est classé « Annulé »,
  comme une session Stripe Checkout expirée.
- TrackIT ne voit ni ne stocke aucune donnée de carte : la saisie se fait sur la page hébergée de
  Stripe ; seule la référence de session est conservée.

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
│       │   ├── db/migration/         V1 schéma, V2 référentiels, V5 suppression logique
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
│   ├── verifier-frontend.mjs         vérification du frontend exécutée par Jenkins
│   ├── sauvegarde.sh                 sauvegarde de la base (dump compressé, rétention)
│   ├── restauration.sh               restauration d'une sauvegarde (exige --confirmer)
│   └── Dockerfile.sauvegarde         image du service de sauvegarde planifié (Railway)
├── .github/workflows/ci.yml          intégration continue de référence (GitHub Actions, section 16)
├── .github/dependabot.yml            analyse et mise à jour des dépendances (section 16)
├── Jenkinsfile                       pipeline d'intégration continue (Frontend → Build → Test → Package)
├── Dockerfile                        image de production (backend + frontend, section 18)
├── docker-entrypoint.sh              démarrage du conteneur (volume, port, utilisateur non-root)
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

116 tests : 112 sur une base H2 en mémoire (rapides, aucune base MySQL nécessaire) et 4 sur un
**vrai MySQL 8** démarré dans un conteneur par Testcontainers (`MigrationsMySqlTest`). Ces 4 tests
appliquent toutes les migrations Flyway comme en production ; ils exigent Docker et sont ignorés
(et non en échec) sur une machine sans Docker.

La couverture de code est mesurée par **JaCoCo** à chaque `mvn test` : rapport dans
`backend/target/site/jacoco/index.html` (et résumé en pourcentage dans chaque exécution GitHub
Actions).

| Classe | Couvre |
|---|---|
| `AbonnementTest` | Premium en mode simulation : paiement réservé à l'administrateur, activation, prolongations successives sans chevauchement, paiement abandonné annulé après 24 h |
| `AuthentificationTest` | connexion, mauvais mot de passe, compte désactivé, jeton absent ou falsifié, inscription, acceptation obligatoire des conditions |
| `AutorisationTest` | droits par rôle, Premium requis (402), isolation entre organisations |
| `DocumentationApiTest` | Swagger et `/v3/api-docs` fermés (401) quand la documentation est désactivée (production) |
| `IsolationReferentielsTest` | types et priorités propres à chaque organisation : lecture, modification, suppression et utilisation refusées pour une autre organisation, statuts en lecture seule, valeurs par défaut à l'inscription |
| `MigrationsMySqlTest` | MySQL réel (Testcontainers) : migrations V1 à V8 appliquées, schéma validé par Hibernate, cohérence de la migration V6, inscription, révocation du jeton après changement de mot de passe |
| `CycleDeVieAnomalieTest` | déclaration (champs obligatoires, auteur et date automatiques), transitions de statut, fermeture, réouverture, priorité, affectation, historique, notifications (responsable prévenu à la résolution, pas de notification en double), deux prises en charge simultanées (la seconde reçoit 409 au lieu d'écraser la première) |
| `EnTetesSecuriteTest` | En-têtes de sécurité HTTP : Content-Security-Policy, X-Content-Type-Options, X-Frame-Options, Referrer-Policy, Permissions-Policy, et HSTS uniquement en HTTPS |
| `EscaladeDelaisTest` | escalade des délais dépassés : notification unique au responsable et à l'opérateur affecté, rien dans les délais, rien pour une anomalie résolue ou fermée, rien vers une autre organisation |
| `ForceBruteTest` | blocage après 5 échecs par compte et 20 par adresse IP, remise à zéro après une connexion réussie, compte inexistant traité de la même façon, 5 inscriptions par adresse IP et par heure (429 au-delà) |
| `JournalSecuriteTest` | journalisation des échecs et réussites de connexion, des 403, des jetons refusés et des suppressions, sans mot de passe ni jeton ; neutralisation des retours à la ligne |
| `MotDePasseTest` | politique de mot de passe : longueur, 72 octets, mots de passe courants et répétitifs, nom de l'utilisateur, changement et réinitialisation ; ancien jeton refusé après un changement ou une réinitialisation |
| `RechercheAnomaliesTest` | texte, statut, numéro, système, opérateur, « Mes tâches », pagination et tri |
| `SessionCookieTest` | cookie HttpOnly / SameSite=Strict à la connexion, jeton absent du corps pour le client web, « Se souvenir de moi » persistant, cookie suffisant en lecture, en-tête X-TrackIT-Client exigé pour modifier (CSRF), déconnexion |
| `SuppressionCompteTest` | suppression définitive sans trace, anonymisation avec traces, connexion et jeton refusés, adresse libérée, désinscription (mot de passe exigé, dernier administrateur protégé) |
| `ValidationTest` | validation côté serveur : champs obligatoires, e-mail invalide, longueur maximale, période de rapport inversée, JSON illisible sans détail technique |
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
8. **Administrateur** : **Utilisateurs** (création, rôle, désactivation, suppression d'un
   compte lié à des anomalies → anonymisé), **Systèmes IT**,
   **Référentiels**, **Premium** → souscription en mode simulation (ou Stripe Checkout en mode test si une clé `sk_test_…` est configurée).
9. **Utilisateur** : consultation seule, aucune action de modification proposée ;
   l'API refuse aussi ces actions (403), par exemple dans Swagger.

## 16. Intégration continue (GitHub Actions et Jenkins)

### GitHub Actions (référence)

Le fichier `.github/workflows/ci.yml` est l'intégration continue **de référence** : GitHub
l'exécute à chaque `push` et à chaque pull request, sans dépendre d'un poste personnel.

| Étape | Rôle |
|---|---|
| Vérifier le frontend | `node scripts/verifier-frontend.mjs frontend` |
| `mvn -B -f backend/pom.xml verify` | compilation, 116 tests (dont les 4 tests sur MySQL réel, Docker étant disponible), couverture JaCoCo, JAR |
| Résumé de la couverture | pourcentage d'instructions et de branches couvertes, affiché sur la page de l'exécution |
| Rapports | rapports des tests et de la couverture téléchargeables (artefact `rapports-tests`) |
| Images Docker | construction de l'image de l'application et de celle du service de sauvegarde |

### Dépendances (Dependabot)

`.github/dependabot.yml` active l'analyse des dépendances Maven, des actions GitHub et des images
Docker : GitHub signale les vulnérabilités connues et ouvre chaque semaine une pull request de mise
à jour, vérifiée par la CI avant fusion. Dans *Settings → Code security* du dépôt, activer aussi les
**Dependabot alerts**.

### Jenkins (poste local)

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
tunnel ngrok : il ne fonctionne que lorsque le poste est allumé, d'où GitHub Actions comme
référence. Sans Docker sur le poste, les 4 tests MySQL y sont ignorés. Aucun secret n'est écrit
dans le `Jenkinsfile`.

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

L'application ne tourne **jamais en root** : l'image crée un utilisateur système `trackit`
(sans shell de connexion) et démarre sous cet utilisateur (`USER trackit`). Si le conteneur
démarre quand même en root (cas de Railway, voir ci-dessous), `docker-entrypoint.sh` rend le
volume `/data` à `trackit`, puis lance Java sous cet utilisateur avec `setpriv`, sans aucune
capacité root.

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
| `RAILWAY_RUN_UID` | `0` : Railway monte les volumes en root ; l'entrypoint rend ensuite `/data` à `trackit` et lance Java sans privilège |
| `SWAGGER_ENABLED` | Ne pas définir (désactivé). `true` seulement le temps d'une démonstration |

5. Générer un domaine public (*Settings → Networking*), puis vérifier l'accueil et la
   connexion, et que `/swagger-ui.html` répond 401.

> **Données de démonstration en ligne.** L'instance en ligne de l'évaluation contient les
> données fictives de démonstration, avec les comptes et le mot de passe publiés dans ce README
> (section 12), à la demande du jury. Pour une instance réelle, démarrer sans données de
> démonstration (`FLYWAY_LOCATIONS=classpath:db/migration`) et créer les comptes par
> l'inscription ; l'encadré des comptes de démonstration n'est de toute façon pas affiché hors
> de `localhost`.

Aucun secret n'est écrit dans le dépôt ni dans l'image : `.env` est exclu par `.gitignore` et
`.dockerignore`, et les valeurs de production sont saisies uniquement dans Railway.

### Sauvegarde et restauration de la base

`scripts/sauvegarde.sh` produit un dump MySQL compressé et horodaté
(`trackit-<base>-AAAAMMJJ-HHMMSSZ.sql.gz`) :

- `mysqldump --single-transaction` : sauvegarde cohérente sans bloquer l'application ;
- le mot de passe passe par `MYSQL_PWD`, jamais en argument de commande ;
- le fichier est écrit en `.partiel` puis renommé seulement si le dump est complet
  (dernière ligne « Dump completed ») ;
- les sauvegardes plus anciennes que `SAUVEGARDE_RETENTION_JOURS` (14 par défaut) sont
  supprimées.

```bash
# en local, avec les variables de .env chargées
sh scripts/sauvegarde.sh
```

**Sur Railway**, la sauvegarde est un service séparé, lancé par une tâche planifiée (cron) :

1. Dans le même projet, *New → GitHub Repo* sur ce dépôt, pour créer un second service
   nommé par exemple `trackit-sauvegarde`.
2. Variables de ce service : `RAILWAY_DOCKERFILE_PATH=scripts/Dockerfile.sauvegarde`,
   `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD` (mêmes références au service
   MySQL que pour TrackIT) et, si besoin, `SAUVEGARDE_RETENTION_JOURS`.
3. Ajouter un **volume** à ce service, monté sur `/sauvegardes`.
4. *Settings → Cron Schedule* : par exemple `0 2 * * *`. Railway interprète l'expression en
   **UTC** (2 h UTC = 3 h en hiver à Bruxelles) et n'accepte pas d'intervalle inférieur à
   5 minutes. Le script se termine après chaque sauvegarde, comme Railway l'exige pour une
   tâche planifiée.

> Le volume de sauvegarde reste chez le même hébergeur. Télécharger régulièrement une
> sauvegarde hors de Railway pour disposer d'une copie externe.

**Restauration** : `scripts/restauration.sh` remplace le contenu de la base `DB_NAME` et exige
`--confirmer` :

```bash
sh scripts/restauration.sh sauvegardes/trackit-trackit-20261121-020000Z.sql.gz --confirmer
```

**Test de restauration** (à faire après chaque changement de schéma) : créer une base vide
distincte, restaurer dedans, puis comparer quelques comptages avec la base d'origine.

```bash
mysql -u root -p -e "CREATE DATABASE trackit_restauration CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
DB_NAME=trackit_restauration sh scripts/restauration.sh <fichier.sql.gz> --confirmer
mysql -u root -p -e "SELECT COUNT(*) FROM trackit.anomalie; SELECT COUNT(*) FROM trackit_restauration.anomalie;"
```
