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

## Point d'attention : mysql-connector-j

Le pilote de connexion MySQL est distribué sous licence **GPL version 2**, assortie
de l'« Universal FOSS Exception v1.0 ». Cette exception autorise la liaison depuis
une application distribuée sous une licence libre figurant sur la liste approuvée.

TrackIT étant publié sous une mention « tous droits réservés » (voir `LICENSE`),
l'exception ne s'applique pas à une **distribution** du logiciel. La question ne se
pose pas en l'état, le dépôt étant publié pour consultation et non distribué. Elle
devrait être tranchée avant toute livraison à un tiers, par l'une des voies
suivantes :

- acquérir une licence commerciale MySQL auprès d'Oracle ;
- publier TrackIT sous une licence libre compatible ;
- remplacer le pilote par **MariaDB Connector/J**, distribué sous LGPL.

Voir le chapitre 6 du livrable 18 « Aspects juridiques et cadre légal » du rapport de TFE.
