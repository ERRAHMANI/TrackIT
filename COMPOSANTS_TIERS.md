# Composants tiers

TrackIT intègre les bibliothèques ci-dessous, chacune soumise à sa propre licence.
Ce relevé satisfait l'obligation de conserver les mentions de licence imposée par
les licences Apache 2.0 et MIT. Licences relevées dans les descripteurs Maven des
versions effectivement utilisées.

| Composant | Version | Licence |
|---|---|---|
| Spring Boot (web, data-jpa, security, validation) | 3.3.5 | Apache License 2.0 |
| Flyway (core, mysql) | gérée par Spring Boot | Apache License 2.0 |
| springdoc-openapi-starter-webmvc-ui | 2.6.0 | Apache License 2.0 |
| jjwt (api, impl, jackson) | 0.12.6 | Apache License 2.0 |
| Apache POI (poi-ooxml) | 5.3.0 | Apache License 2.0 |
| stripe-java | 29.5.0 | MIT License |
| Lombok | gérée par Spring Boot | MIT License |
| OpenPDF | 2.0.3 | LGPL 2.1 et MPL (double licence) |
| H2 Database *(tests uniquement)* | gérée par Spring Boot | MPL 2.0 et EPL 1.0 |
| mysql-connector-j | gérée par Spring Boot | **GPL v2 avec Universal FOSS Exception v1.0** |

## Frontend

Le client web n'utilise aucune bibliothèque JavaScript ni police externe. Les pictogrammes
(`frontend/js/icones.js`) sont des tracés SVG au trait dessinés pour TrackIT ; certains
reprennent ou adaptent des tracés de **Feather Icons** (par exemple la roue dentée des
paramètres, aux coordonnées arrondies).

| Composant | Version | Licence |
|---|---|---|
| Feather Icons *(tracés repris ou adaptés)* | — | MIT License |

Mention de licence de Feather Icons (reproduite telle quelle, comme l'exige la licence MIT) :

```text
The MIT License (MIT)

Copyright (c) 2013-2023 Cole Bemis

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

## Déploiement

Images Docker utilisées par le `Dockerfile` (téléchargées au moment de la construction,
non incluses dans le dépôt) :

| Image | Étape | Contenu et licence |
|---|---|---|
| `maven:3.9-eclipse-temurin-17` | compilation uniquement | Apache Maven (Apache License 2.0) et JDK Eclipse Temurin |
| `eclipse-temurin:17-jre` | exécution | Eclipse Temurin (OpenJDK) : **GPL v2 avec Classpath Exception** ; système de base Ubuntu (licences de ses paquets) |

La Classpath Exception permet d'exécuter une application sous n'importe quelle licence sur
cette machine virtuelle Java, sans que la GPL s'étende à l'application.

## Point d'attention : mysql-connector-j

Le pilote de connexion MySQL est distribué sous licence **GPL version 2**, assortie
de l'« Universal FOSS Exception v1.0 ». Cette exception autorise la liaison depuis
une application distribuée sous une licence libre figurant sur la liste approuvée.

TrackIT étant publié sous une mention « tous droits réservés » (voir `LICENSE`),
l'exception ne s'applique pas à une **distribution** du logiciel. La question ne se
pose pas en l'état, le dépôt étant publié pour consultation et non distribué. Le
déploiement sur Railway ne change rien : l'application est **mise à disposition en ligne**
(SaaS), le JAR et l'image ne sont remis à personne, ce qui ne constitue pas une
distribution au sens de la GPL v2. Elle
devrait être tranchée avant toute livraison à un tiers, par l'une des voies
suivantes :

- acquérir une licence commerciale MySQL auprès d'Oracle ;
- publier TrackIT sous une licence libre compatible ;
- remplacer le pilote par **MariaDB Connector/J**, distribué sous LGPL.

Voir le chapitre 6 du livrable 18 « Aspects juridiques et cadre légal » du rapport de TFE (document remis séparément, hors du dépôt).