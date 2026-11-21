// Documents légaux publics : conditions d'utilisation (#/cgu), politique de confidentialité
// (#/confidentialite) et mentions légales (#/mentions-legales).
// Les textes sont rédigés ici dans les trois langues (et non dans traductions.js) : ils sont longs et
// doivent rester exacts. Le conteneur porte data-sans-traduction pour que i18n.js ne les retouche pas.
// Les passages entre [crochets] sont des informations propres à l'éditeur, à compléter avant toute
// mise en service réelle : aucune donnée réelle n'est inventée ici.
import { esc } from '../ui.js';
import { langue, selecteurLangue } from '../i18n.js';
import { session } from '../session.js';

export const DOCUMENTS_LEGAUX = [
    { cle: 'cgu', lien: '/cgu' },
    { cle: 'confidentialite', lien: '/confidentialite' },
    { cle: 'mentions-legales', lien: '/mentions-legales' }
];

const LIBELLES = {
    fr: { 'cgu': "Conditions d'utilisation", 'confidentialite': 'Politique de confidentialité', 'mentions-legales': 'Mentions légales',
          majLe: 'Dernière mise à jour :', retour: "Retour à l'accueil", espace: 'Retour à mon espace',
          acceptation: ['J\'accepte les ', "conditions d'utilisation", ' et la ', 'politique de confidentialité', '.'] },
    en: { 'cgu': 'Terms of use', 'confidentialite': 'Privacy policy', 'mentions-legales': 'Legal notice',
          majLe: 'Last updated:', retour: 'Back to home page', espace: 'Back to my workspace',
          acceptation: ['I accept the ', 'terms of use', ' and the ', 'privacy policy', '.'] },
    nl: { 'cgu': 'Gebruiksvoorwaarden', 'confidentialite': 'Privacybeleid', 'mentions-legales': 'Wettelijke vermeldingen',
          majLe: 'Laatst bijgewerkt:', retour: 'Terug naar de startpagina', espace: 'Terug naar mijn omgeving',
          acceptation: ['Ik aanvaard de ', 'gebruiksvoorwaarden', ' en het ', 'privacybeleid', '.'] }
};

const textes = () => LIBELLES[langue()] || LIBELLES.fr;

/** Libellé d'un document dans la langue courante (liens de pied de page). */
export const libelleDocument = cle => textes()[cle];

/** Pied de page des pages publiques : liens vers les trois documents. */
export function liensLegaux() {
    return `<nav class="liens-legaux" aria-label="${esc(textes()['mentions-legales'])}" data-sans-traduction>
        ${DOCUMENTS_LEGAUX.map(d => `<a href="#${d.lien}">${esc(textes()[d.cle])}</a>`).join('')}
    </nav>`;
}

/** Libellé de la case d'acceptation de l'inscription, avec les liens (ouverts dans un nouvel onglet). */
export function libelleAcceptation() {
    const [avant, cgu, milieu, confidentialite, apres] = textes().acceptation;
    return `<span data-sans-traduction>${esc(avant)}<a href="#/cgu" target="_blank" rel="noopener">${esc(cgu)}</a>${esc(milieu)}<a href="#/confidentialite" target="_blank" rel="noopener">${esc(confidentialite)}</a>${esc(apres)}</span>`;
}

// ------------------------------------------------------------------ Contenu
// Chaque document : { maj, sections: [[titre, [paragraphes…]]] }. Un paragraphe qui commence par
// « - » est un élément de liste.

const CONTENU = {
    'cgu': {
        fr: { maj: '[date de mise à jour]', sections: [
            ['1. Objet', [
                "Les présentes conditions d'utilisation encadrent l'accès à TrackIT et son utilisation. TrackIT est une application web de déclaration, de suivi et de traitement des anomalies informatiques, éditée par [nom de l'éditeur] (voir les mentions légales).",
                "TrackIT est réalisé dans le cadre d'un projet de fin d'études. Tant qu'il n'est pas exploité commercialement, il est fourni à titre de démonstration : [préciser le statut du service au moment de la mise en ligne]."]],
            ['2. Inscription et acceptation', [
                "L'inscription crée l'espace d'une organisation et son premier compte Administrateur. La personne qui s'inscrit déclare être autorisée à engager cette organisation et accepte les présentes conditions en son nom, en cochant la case prévue à cet effet.",
                "Les autres comptes de l'organisation sont créés par son Administrateur, qui veille à ce que leurs titulaires prennent connaissance des présentes conditions."]],
            ['3. Comptes et sécurité', [
                "Chaque compte est personnel. Son titulaire garde son mot de passe confidentiel et prévient sans délai l'Administrateur de son organisation en cas d'utilisation non autorisée.",
                "Un changement ou une réinitialisation du mot de passe met fin aux sessions ouvertes. Après plusieurs échecs de connexion, le compte ou l'adresse IP est temporairement bloqué."]],
            ['4. Rôles', [
                "Les droits dépendent du rôle attribué par l'Administrateur : Utilisateur, Opérateur, Responsable technique, Analyste ou Administrateur. Chaque organisation n'accède qu'à ses propres données."]],
            ['5. Utilisation acceptable', [
                "Il est interdit notamment :",
                "- de déposer un contenu illicite, un logiciel malveillant ou des données personnelles sans base légale ;",
                "- de tenter d'accéder aux données d'une autre organisation ou de contourner les mesures de sécurité ;",
                "- de perturber le service, par exemple par des requêtes automatisées massives.",
                "En cas de manquement, l'accès peut être suspendu."]],
            ['6. Données de l\'organisation', [
                "Les données saisies dans TrackIT (anomalies, commentaires, pièces jointes, systèmes) restent la propriété de l'organisation cliente. L'éditeur les traite uniquement pour fournir le service, comme décrit dans la politique de confidentialité."]],
            ['7. Offre Premium', [
                "L'offre gratuite donne accès aux fonctions de base. L'offre Premium, souscrite par l'Administrateur, ajoute des fonctions décrites sur la page Premium, au prix qui y est affiché au moment de la souscription. Le paiement est traité par Stripe ; TrackIT ne reçoit ni ne conserve les données de carte.",
                "Conditions de facturation, de renouvellement, de résiliation et de remboursement : [à compléter avant toute vente réelle]. Pendant la démonstration, le paiement fonctionne en mode test : aucun paiement réel n'est encaissé."]],
            ['8. Disponibilité', [
                "L'éditeur s'efforce de maintenir le service accessible mais ne garantit pas une disponibilité permanente : maintenances, mises à jour ou incidents peuvent l'interrompre. Les données sont sauvegardées régulièrement."]],
            ['9. Responsabilité', [
                "TrackIT est un outil d'organisation : l'organisation cliente reste responsable de la résolution des anomalies et de l'usage qu'elle fait des informations. Dans les limites permises par la loi, la responsabilité de l'éditeur est limitée aux dommages directs : [plafond éventuel à compléter]."]],
            ['10. Suppression du compte', [
                "Un Administrateur peut supprimer un compte de son organisation et désinscrire l'organisation depuis son profil. Un compte lié à des anomalies est anonymisé afin de conserver un historique cohérent sans données personnelles."]],
            ['11. Propriété intellectuelle', [
                "Le logiciel TrackIT, son code et ses éléments graphiques sont protégés. Leur reproduction est interdite sans autorisation écrite de l'éditeur."]],
            ['12. Modification des conditions', [
                "Les présentes conditions peuvent évoluer. La date de mise à jour figure en tête du document ; une modification importante est signalée aux Administrateurs avant son entrée en vigueur."]],
            ['13. Droit applicable', [
                "Les présentes conditions sont soumises au droit belge. Tout litige relève des tribunaux de [arrondissement judiciaire à compléter], sans préjudice des règles impératives protégeant les consommateurs."]]
        ] },
        en: { maj: '[update date]', sections: [
            ['1. Purpose', [
                'These terms govern access to and use of TrackIT, a web application for reporting, tracking and handling IT incidents, published by [publisher name] (see the legal notice).',
                'TrackIT was built as a final-year project. Until it is operated commercially, it is provided for demonstration purposes: [state the status of the service when it goes online].']],
            ['2. Registration and acceptance', [
                "Registration creates an organisation's workspace and its first Administrator account. The person registering declares that they are authorised to bind the organisation and accepts these terms on its behalf by ticking the box provided.",
                "Other accounts are created by the organisation's Administrator, who makes sure their holders read these terms."]],
            ['3. Accounts and security', [
                "Each account is personal. Its holder keeps the password confidential and promptly informs their organisation's Administrator of any unauthorised use.",
                'Changing or resetting a password ends open sessions. After several failed sign-in attempts, the account or IP address is temporarily blocked.']],
            ['4. Roles', [
                'Permissions depend on the role assigned by the Administrator: User, Operator, Technical manager, Analyst or Administrator. Each organisation can only access its own data.']],
            ['5. Acceptable use', [
                'In particular, it is forbidden:',
                '- to upload unlawful content, malware or personal data without a legal basis;',
                "- to try to access another organisation's data or to bypass security measures;",
                '- to disrupt the service, for example with massive automated requests.',
                'Access may be suspended in case of breach.']],
            ["6. The organisation's data", [
                'Data entered in TrackIT (incidents, comments, attachments, systems) remains the property of the client organisation. The publisher processes it only to provide the service, as described in the privacy policy.']],
            ['7. Premium plan', [
                'The free plan gives access to the core features. The Premium plan, subscribed by the Administrator, adds the features described on the Premium page, at the price displayed there at the time of subscription. Payment is handled by Stripe; TrackIT neither receives nor stores card data.',
                'Billing, renewal, cancellation and refund terms: [to be completed before any real sale]. During the demonstration, payment runs in test mode: no real payment is collected.']],
            ['8. Availability', [
                'The publisher strives to keep the service available but does not guarantee permanent availability: maintenance, updates or incidents may interrupt it. Data is backed up regularly.']],
            ['9. Liability', [
                'TrackIT is an organisational tool: the client organisation remains responsible for resolving incidents and for how it uses the information. To the extent permitted by law, the publisher is only liable for direct damage: [possible cap to be completed].']],
            ['10. Account deletion', [
                'An Administrator can delete an account of their organisation and unregister the organisation from their profile. An account linked to incidents is anonymised so that the history stays consistent without personal data.']],
            ['11. Intellectual property', [
                "The TrackIT software, its code and graphic elements are protected. Reproduction is forbidden without the publisher's written permission."]],
            ['12. Changes to these terms', [
                'These terms may change. The update date is shown at the top of the document; a significant change is announced to Administrators before it takes effect.']],
            ['13. Governing law', [
                'These terms are governed by Belgian law. Any dispute falls under the courts of [judicial district to be completed], without prejudice to mandatory consumer protection rules.']]
        ] },
        nl: { maj: '[datum van bijwerking]', sections: [
            ['1. Voorwerp', [
                'Deze voorwaarden regelen de toegang tot en het gebruik van TrackIT, een webapplicatie voor het melden, opvolgen en behandelen van IT-incidenten, uitgegeven door [naam van de uitgever] (zie de wettelijke vermeldingen).',
                'TrackIT werd gerealiseerd als eindwerk. Zolang het niet commercieel wordt uitgebaat, wordt het aangeboden als demonstratie: [status van de dienst bij de onlinezetting vermelden].']],
            ['2. Inschrijving en aanvaarding', [
                'De inschrijving maakt de omgeving van een organisatie en haar eerste Beheerdersaccount aan. Wie zich inschrijft, verklaart de organisatie te mogen verbinden en aanvaardt deze voorwaarden namens haar door het daartoe voorziene vakje aan te vinken.',
                'De andere accounts worden aangemaakt door de Beheerder van de organisatie, die erop toeziet dat de houders deze voorwaarden lezen.']],
            ['3. Accounts en beveiliging', [
                'Elk account is persoonlijk. De houder houdt zijn wachtwoord geheim en verwittigt onmiddellijk de Beheerder van zijn organisatie bij onrechtmatig gebruik.',
                'Een wijziging of herinitialisatie van het wachtwoord beëindigt de open sessies. Na meerdere mislukte aanmeldpogingen wordt het account of het IP-adres tijdelijk geblokkeerd.']],
            ['4. Rollen', [
                'De rechten hangen af van de rol die de Beheerder toekent: Gebruiker, Operator, Technisch verantwoordelijke, Analist of Beheerder. Elke organisatie heeft enkel toegang tot haar eigen gegevens.']],
            ['5. Aanvaardbaar gebruik', [
                'Het is onder meer verboden:',
                '- onwettige inhoud, schadelijke software of persoonsgegevens zonder rechtsgrond te uploaden;',
                '- te proberen de gegevens van een andere organisatie te raadplegen of de beveiliging te omzeilen;',
                '- de dienst te verstoren, bijvoorbeeld met massale geautomatiseerde verzoeken.',
                'Bij een inbreuk kan de toegang worden opgeschort.']],
            ['6. Gegevens van de organisatie', [
                'De gegevens die in TrackIT worden ingevoerd (incidenten, opmerkingen, bijlagen, systemen) blijven eigendom van de klantorganisatie. De uitgever verwerkt ze enkel om de dienst te leveren, zoals beschreven in het privacybeleid.']],
            ['7. Premium-aanbod', [
                'Het gratis aanbod geeft toegang tot de basisfuncties. Het Premium-aanbod, af te sluiten door de Beheerder, voegt de functies toe die op de Premium-pagina staan, tegen de prijs die daar bij het afsluiten wordt getoond. De betaling verloopt via Stripe; TrackIT ontvangt noch bewaart kaartgegevens.',
                'Voorwaarden voor facturatie, verlenging, opzegging en terugbetaling: [aan te vullen vóór elke echte verkoop]. Tijdens de demonstratie werkt de betaling in testmodus: er wordt geen echte betaling geïnd.']],
            ['8. Beschikbaarheid', [
                'De uitgever streeft ernaar de dienst beschikbaar te houden, maar waarborgt geen permanente beschikbaarheid: onderhoud, updates of incidenten kunnen hem onderbreken. De gegevens worden regelmatig geback-upt.']],
            ['9. Aansprakelijkheid', [
                'TrackIT is een organisatiehulpmiddel: de klantorganisatie blijft verantwoordelijk voor het oplossen van de incidenten en voor het gebruik van de informatie. Binnen de wettelijke grenzen is de uitgever enkel aansprakelijk voor directe schade: [eventueel plafond aan te vullen].']],
            ['10. Verwijdering van het account', [
                'Een Beheerder kan een account van zijn organisatie verwijderen en de organisatie uitschrijven via zijn profiel. Een account dat aan incidenten gekoppeld is, wordt geanonimiseerd zodat de historiek coherent blijft zonder persoonsgegevens.']],
            ['11. Intellectuele eigendom', [
                'De software TrackIT, de code en de grafische elementen zijn beschermd. Reproductie is verboden zonder schriftelijke toestemming van de uitgever.']],
            ['12. Wijziging van de voorwaarden', [
                'Deze voorwaarden kunnen wijzigen. De datum van bijwerking staat bovenaan het document; een belangrijke wijziging wordt vooraf aan de Beheerders gemeld.']],
            ['13. Toepasselijk recht', [
                'Deze voorwaarden vallen onder het Belgisch recht. Elk geschil behoort tot de bevoegdheid van de rechtbanken van [gerechtelijk arrondissement aan te vullen], onverminderd de dwingende regels ter bescherming van consumenten.']]
        ] }
    },

    'confidentialite': {
        fr: { maj: '[date de mise à jour]', sections: [
            ['1. Qui traite vos données ?', [
                "Pour les comptes, l'inscription et l'abonnement, le responsable du traitement est [nom de l'éditeur], [adresse postale], joignable à [adresse e-mail de contact].",
                "Pour les données que l'organisation cliente saisit sur son activité (anomalies, commentaires, pièces jointes, comptes de ses collaborateurs), l'organisation cliente est responsable du traitement et l'éditeur agit comme sous-traitant, au sens de l'article 28 du RGPD : [accord de sous-traitance à conclure avec chaque organisation cliente]."]],
            ['2. Données traitées', [
                "- Organisation : nom et adresse e-mail de contact.",
                "- Comptes : nom, prénom, adresse e-mail, rôle, préférences (langue, notifications) et mot de passe, conservé uniquement sous forme d'empreinte BCrypt.",
                "- Activité : anomalies, commentaires, pièces jointes, historique des changements et notifications.",
                "- Sécurité : journal des connexions, échecs de connexion, accès refusés et opérations sur les comptes, avec l'adresse e-mail et l'adresse IP concernées. Ni mot de passe ni jeton n'y sont écrits.",
                "- Abonnement : offre choisie, montant, statut et référence du paiement. Les données de carte sont saisies chez Stripe et ne sont jamais transmises à TrackIT."]],
            ['3. Finalités et bases légales', [
                "- Fournir le service et gérer les comptes : exécution du contrat (article 6.1.b du RGPD).",
                "- Sécuriser le service (blocage après échecs, journal de sécurité, sauvegardes) : intérêt légitime de l'éditeur et de ses clients (article 6.1.f).",
                "- Conserver les pièces liées aux paiements : obligation légale (article 6.1.c).",
                "Aucune donnée n'est utilisée à des fins publicitaires, revendue ou utilisée pour du profilage."]],
            ['4. Destinataires et sous-traitants', [
                "Les données ne sont accessibles qu'aux membres autorisés de l'organisation, selon leur rôle, et à l'éditeur pour la maintenance.",
                "- Hébergement de l'application et de la base de données : Railway, [région d'hébergement et garanties de transfert à vérifier].",
                "- Paiement de l'offre Premium : Stripe, [garanties de transfert à vérifier].",
                "Si des données sont transférées hors de l'Espace économique européen, ce transfert repose sur les garanties prévues par le RGPD (par exemple des clauses contractuelles types) : [à vérifier et préciser]."]],
            ['5. Durées de conservation', [
                "- Comptes et données d'activité : pendant toute la durée d'utilisation du service. À la suppression, un compte sans activité est effacé ; un compte lié à des anomalies est anonymisé.",
                "- Journal de sécurité : [durée de conservation à fixer].",
                "- Sauvegardes de la base : 14 jours par défaut, puis suppression automatique.",
                "- Pièces liées aux paiements : [durée légale de conservation comptable]."]],
            ['6. Stockage dans le navigateur', [
                "TrackIT n'utilise ni cookie publicitaire ni outil de mesure d'audience. Il dépose un seul cookie, strictement nécessaire : le cookie de connexion TRACKIT_SESSION, inaccessible aux scripts de la page (HttpOnly), effacé à la fermeture du navigateur ou conservé jusqu'à l'expiration de la session si « Se souvenir de moi » est coché. Le navigateur garde aussi le profil affiché, la langue et les préférences d'affichage. La déconnexion efface la session."]],
            ['7. Sécurité', [
                "Mots de passe hachés (BCrypt), connexion chiffrée (HTTPS), droits par rôle, isolement des données entre organisations, blocage après échecs de connexion, politique de sécurité du contenu (CSP) et sauvegardes régulières."]],
            ['8. Vos droits', [
                "Vous pouvez demander l'accès à vos données, leur rectification, leur effacement, la limitation du traitement, leur portabilité ou vous opposer au traitement (articles 15 à 21 du RGPD), en écrivant à [adresse e-mail de contact]. Pour les données saisies par votre organisation, adressez-vous d'abord à son Administrateur.",
                "Vous pouvez aussi introduire une réclamation auprès de l'Autorité de protection des données (www.autoriteprotectiondonnees.be)."]],
            ['9. Modifications', [
                "Cette politique peut évoluer avec le service. La date de mise à jour figure en tête du document."]]
        ] },
        en: { maj: '[update date]', sections: [
            ['1. Who processes your data?', [
                'For accounts, registration and subscriptions, the controller is [publisher name], [postal address], reachable at [contact e-mail address].',
                "For the data a client organisation enters about its activity (incidents, comments, attachments, its staff's accounts), the client organisation is the controller and the publisher acts as a processor within the meaning of Article 28 GDPR: [data processing agreement to be concluded with each client organisation]."]],
            ['2. Data processed', [
                '- Organisation: name and contact e-mail address.',
                '- Accounts: last name, first name, e-mail address, role, preferences (language, notifications) and password, stored only as a BCrypt hash.',
                '- Activity: incidents, comments, attachments, change history and notifications.',
                '- Security: log of sign-ins, failed sign-ins, denied access and account operations, with the related e-mail and IP addresses. Neither passwords nor tokens are written to it.',
                '- Subscription: chosen plan, amount, status and payment reference. Card data is entered at Stripe and never sent to TrackIT.']],
            ['3. Purposes and legal bases', [
                '- Providing the service and managing accounts: performance of the contract (Article 6(1)(b) GDPR).',
                '- Securing the service (blocking after failures, security log, backups): legitimate interest of the publisher and its clients (Article 6(1)(f)).',
                '- Keeping payment records: legal obligation (Article 6(1)(c)).',
                'No data is used for advertising, sold or used for profiling.']],
            ['4. Recipients and processors', [
                'Data is only accessible to authorised members of the organisation, according to their role, and to the publisher for maintenance.',
                '- Hosting of the application and database: Railway, [hosting region and transfer safeguards to be verified].',
                '- Premium payment: Stripe, [transfer safeguards to be verified].',
                'If data is transferred outside the European Economic Area, the transfer relies on the safeguards provided by the GDPR (for example standard contractual clauses): [to be verified and specified].']],
            ['5. Retention periods', [
                '- Accounts and activity data: for as long as the service is used. On deletion, an account without activity is erased; an account linked to incidents is anonymised.',
                '- Security log: [retention period to be set].',
                '- Database backups: 14 days by default, then automatic deletion.',
                '- Payment records: [statutory accounting retention period].']],
            ['6. Browser storage', [
                'TrackIT uses neither advertising cookies nor audience measurement tools. It sets a single, strictly necessary cookie: the TRACKIT_SESSION sign-in cookie, which page scripts cannot read (HttpOnly), deleted when the browser closes or kept until the session expires if "Remember me" is ticked. The browser also keeps the displayed profile, the language and display preferences. Signing out clears the session.']],
            ['7. Security', [
                'Hashed passwords (BCrypt), encrypted connection (HTTPS), role-based permissions, data isolation between organisations, blocking after failed sign-ins, content security policy (CSP) and regular backups.']],
            ['8. Your rights', [
                'You can request access to your data, its rectification, erasure, restriction of processing or portability, or object to processing (Articles 15 to 21 GDPR), by writing to [contact e-mail address]. For data entered by your organisation, first contact its Administrator.',
                'You can also lodge a complaint with the Belgian Data Protection Authority (www.dataprotectionauthority.be).']],
            ['9. Changes', [
                'This policy may change with the service. The update date is shown at the top of the document.']]
        ] },
        nl: { maj: '[datum van bijwerking]', sections: [
            ['1. Wie verwerkt uw gegevens?', [
                'Voor de accounts, de inschrijving en het abonnement is de verwerkingsverantwoordelijke [naam van de uitgever], [postadres], bereikbaar via [contactadres e-mail].',
                'Voor de gegevens die de klantorganisatie over haar activiteit invoert (incidenten, opmerkingen, bijlagen, accounts van haar medewerkers) is de klantorganisatie verwerkingsverantwoordelijke en treedt de uitgever op als verwerker in de zin van artikel 28 AVG: [verwerkersovereenkomst te sluiten met elke klantorganisatie].']],
            ['2. Verwerkte gegevens', [
                '- Organisatie: naam en contactadres e-mail.',
                '- Accounts: naam, voornaam, e-mailadres, rol, voorkeuren (taal, meldingen) en wachtwoord, enkel bewaard als BCrypt-hash.',
                '- Activiteit: incidenten, opmerkingen, bijlagen, wijzigingshistoriek en meldingen.',
                '- Beveiliging: logboek van aanmeldingen, mislukte aanmeldingen, geweigerde toegang en accountbewerkingen, met het betrokken e-mail- en IP-adres. Wachtwoorden en tokens worden er nooit in geschreven.',
                '- Abonnement: gekozen aanbod, bedrag, status en betalingsreferentie. Kaartgegevens worden bij Stripe ingevoerd en nooit aan TrackIT doorgegeven.']],
            ['3. Doeleinden en rechtsgronden', [
                '- De dienst leveren en de accounts beheren: uitvoering van de overeenkomst (artikel 6.1.b AVG).',
                '- De dienst beveiligen (blokkering na mislukte pogingen, beveiligingslogboek, back-ups): gerechtvaardigd belang van de uitgever en zijn klanten (artikel 6.1.f).',
                '- Betalingsstukken bewaren: wettelijke verplichting (artikel 6.1.c).',
                'Geen enkel gegeven wordt gebruikt voor reclame, verkocht of gebruikt voor profilering.']],
            ['4. Ontvangers en verwerkers', [
                'De gegevens zijn enkel toegankelijk voor de gemachtigde leden van de organisatie, volgens hun rol, en voor de uitgever voor onderhoud.',
                '- Hosting van de applicatie en de databank: Railway, [hostingregio en waarborgen bij doorgifte te controleren].',
                '- Betaling van het Premium-aanbod: Stripe, [waarborgen bij doorgifte te controleren].',
                'Als gegevens buiten de Europese Economische Ruimte worden doorgegeven, steunt die doorgifte op de waarborgen van de AVG (bijvoorbeeld modelcontractbepalingen): [te controleren en te preciseren].']],
            ['5. Bewaartermijnen', [
                '- Accounts en activiteitsgegevens: zolang de dienst wordt gebruikt. Bij verwijdering wordt een account zonder activiteit gewist; een account dat aan incidenten gekoppeld is, wordt geanonimiseerd.',
                '- Beveiligingslogboek: [bewaartermijn vast te leggen].',
                '- Back-ups van de databank: standaard 14 dagen, daarna automatische verwijdering.',
                '- Betalingsstukken: [wettelijke boekhoudkundige bewaartermijn].']],
            ['6. Opslag in de browser', [
                'TrackIT gebruikt geen reclamecookies en geen tools voor publieksmeting. Het plaatst één strikt noodzakelijke cookie: de aanmeldcookie TRACKIT_SESSION, onleesbaar voor de scripts van de pagina (HttpOnly), gewist bij het sluiten van de browser of bewaard tot de sessie verloopt als "Onthoud mij" is aangevinkt. De browser bewaart ook het getoonde profiel, de taal en de weergavevoorkeuren. Afmelden wist de sessie.']],
            ['7. Beveiliging', [
                'Gehashte wachtwoorden (BCrypt), versleutelde verbinding (HTTPS), rechten per rol, scheiding van de gegevens tussen organisaties, blokkering na mislukte aanmeldingen, contentbeveiligingsbeleid (CSP) en regelmatige back-ups.']],
            ['8. Uw rechten', [
                'U kunt inzage, verbetering, wissing, beperking van de verwerking of overdraagbaarheid van uw gegevens vragen, of bezwaar maken tegen de verwerking (artikelen 15 tot 21 AVG), door te schrijven naar [contactadres e-mail]. Voor gegevens die uw organisatie heeft ingevoerd, richt u zich eerst tot haar Beheerder.',
                'U kunt ook een klacht indienen bij de Gegevensbeschermingsautoriteit (www.gegevensbeschermingsautoriteit.be).']],
            ['9. Wijzigingen', [
                'Dit beleid kan met de dienst evolueren. De datum van bijwerking staat bovenaan het document.']]
        ] }
    },

    'mentions-legales': {
        fr: { maj: '[date de mise à jour]', sections: [
            ['Éditeur', [
                "- Nom : [nom et prénom, ou dénomination de la société]",
                "- Adresse : [adresse postale]",
                "- E-mail : [adresse e-mail de contact]",
                "- Numéro d'entreprise (BCE) : [numéro BCE, si une entreprise est créée]",
                "- Numéro de TVA : [numéro de TVA, le cas échéant]",
                "- Responsable de la publication : [nom]"]],
            ['Hébergement', [
                "- Hébergeur : Railway, [dénomination complète et adresse à reprendre des conditions de l'hébergeur]",
                "- Région d'hébergement : [à vérifier dans le projet Railway]"]],
            ['Contexte', [
                "TrackIT est un projet de fin d'études. [Préciser l'établissement et l'année académique, ou supprimer ce paragraphe lors d'une exploitation commerciale.]",
                "Les comptes et les données de démonstration sont fictifs."]],
            ['Propriété intellectuelle', [
                "© 2026 Younes ERRAHMANI. Tous droits réservés. Les composants open source utilisés restent soumis à leurs propres licences (voir COMPOSANTS_TIERS.md dans le dépôt)."]],
            ['Données personnelles', [
                "Le traitement des données personnelles est décrit dans la politique de confidentialité."]]
        ] },
        en: { maj: '[update date]', sections: [
            ['Publisher', [
                '- Name: [full name, or company name]',
                '- Address: [postal address]',
                '- E-mail: [contact e-mail address]',
                '- Company number (CBE): [CBE number, if a company is set up]',
                '- VAT number: [VAT number, if applicable]',
                '- Publication manager: [name]']],
            ['Hosting', [
                "- Host: Railway, [full company name and address taken from the host's terms]",
                '- Hosting region: [to be checked in the Railway project]']],
            ['Context', [
                'TrackIT is a final-year project. [State the institution and academic year, or remove this paragraph for commercial operation.]',
                'Demonstration accounts and data are fictitious.']],
            ['Intellectual property', [
                '© 2026 Younes ERRAHMANI. All rights reserved. The open source components used remain subject to their own licences (see COMPOSANTS_TIERS.md in the repository).']],
            ['Personal data', [
                'The processing of personal data is described in the privacy policy.']]
        ] },
        nl: { maj: '[datum van bijwerking]', sections: [
            ['Uitgever', [
                '- Naam: [naam en voornaam, of naam van de vennootschap]',
                '- Adres: [postadres]',
                '- E-mail: [contactadres e-mail]',
                '- Ondernemingsnummer (KBO): [KBO-nummer, als een onderneming wordt opgericht]',
                '- Btw-nummer: [btw-nummer, indien van toepassing]',
                '- Verantwoordelijke uitgever: [naam]']],
            ['Hosting', [
                '- Hostingprovider: Railway, [volledige benaming en adres over te nemen uit de voorwaarden van de provider]',
                '- Hostingregio: [te controleren in het Railway-project]']],
            ['Context', [
                'TrackIT is een eindwerk. [Instelling en academiejaar vermelden, of deze alinea schrappen bij commerciële uitbating.]',
                'De demonstratieaccounts en -gegevens zijn fictief.']],
            ['Intellectuele eigendom', [
                '© 2026 Younes ERRAHMANI. Alle rechten voorbehouden. De gebruikte opensourcecomponenten blijven onderworpen aan hun eigen licenties (zie COMPOSANTS_TIERS.md in de repository).']],
            ['Persoonsgegevens', [
                'De verwerking van persoonsgegevens wordt beschreven in het privacybeleid.']]
        ] }
    }
};

/** Échappe le texte puis signale les passages [à compléter]. */
function texte(brut) {
    return esc(brut).replace(/\[([^\]]+)\]/g, '<mark class="a-completer">[$1]</mark>');
}

function sections(liste) {
    return liste.map(([titre, paragraphes]) => {
        let html = `<h2>${esc(titre)}</h2>`;
        let liste = [];
        const viderListe = () => { if (liste.length) { html += `<ul>${liste.join('')}</ul>`; liste = []; } };
        for (const p of paragraphes) {
            if (p.startsWith('- ')) liste.push(`<li>${texte(p.slice(2))}</li>`);
            else { viderListe(); html += `<p>${texte(p)}</p>`; }
        }
        viderListe();
        return html;
    }).join('');
}

export default function legal(app, { route }) {
    const cle = route.document;
    const doc = CONTENU[cle][langue()] || CONTENU[cle].fr;
    const t = textes();
    document.title = `${t[cle]} — TrackIT`;
    const connecte = session.estConnecte();
    app.innerHTML = `
    <div class="page-publique">
        <header class="entete-public">
            <a class="marque" href="#/"><img src="assets/logo.svg" alt="">TrackIT</a>
            <div class="droite">
                ${selecteurLangue()}
                <a class="btn btn-secondaire" href="#${connecte ? '/tableau-de-bord' : '/'}" data-sans-traduction>${esc(connecte ? t.espace : t.retour)}</a>
            </div>
        </header>
        <article class="section-publique document-legal" data-sans-traduction>
            <h1>${esc(t[cle])}</h1>
            <p class="aide">${esc(t.majLe)} ${texte(doc.maj)}</p>
            ${sections(doc.sections)}
        </article>
        <footer class="pied-public">${liensLegaux()}</footer>
    </div>`;
    window.scrollTo(0, 0);
}
