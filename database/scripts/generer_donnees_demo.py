#!/usr/bin/env python3
"""
Génère les données de démonstration de TrackIT :
  - backend/src/main/resources/db/demo/V3__donnees_demonstration.sql (migration Flyway)
  - backend/uploads/demo/...  (fichiers réels des pièces jointes de démonstration)

Les données sont déterministes (graine fixe) et cohérentes : chaque anomalie suit le cycle
de vie Nouvelle → En cours → Résolue → Fermée, avec l'historique, les commentaires et les
notifications correspondants. Les mots de passe sont hachés en BCrypt (coût 10).

Contexte des données : entreprises et collaborateurs fictifs situés en Belgique.

Usage : python database/scripts/generer_donnees_demo.py
Prérequis : pip install bcrypt   (à défaut, l'outil htpasswd d'Apache est utilisé)
"""
import random
import subprocess
import unicodedata
from datetime import date, datetime, timedelta
from pathlib import Path

RACINE = Path(__file__).resolve().parents[2]
SORTIE_SQL = RACINE / "backend/src/main/resources/db/demo/V3__donnees_demonstration.sql"
DOSSIER_UPLOADS = RACINE / "backend/uploads"

random.seed(20261005)
# Générateur séparé pour les sels BCrypt : rend les empreintes reproductibles sans modifier
# la suite des tirages des données (donc le contenu et les volumes restent identiques).
GENERATEUR_SELS = random.Random(20261005)
ALPHABET_BCRYPT = "./ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
MAINTENANT = datetime(2026, 10, 5, 11, 30)
DEBUT_HISTORIQUE = datetime(2026, 4, 1, 8, 0)
MOT_DE_PASSE_DEMO = "TrackIT@2026"

ROLES = {"Utilisateur": 1, "Opérateur": 2, "Responsable technique": 3, "Analyste": 4, "Administrateur": 5}
STATUTS = {"Nouvelle": 1, "En cours": 2, "Résolue": 3, "Fermée": 4}
PRIORITES = {"Basse": 1, "Moyenne": 2, "Haute": 3, "Critique": 4}
TYPES = {"Réseau": 1, "Application": 2, "Matériel": 3, "Accès": 4, "Base de données": 5,
         "Performance": 6, "Fonctionnel": 7, "Autre": 8}


# --------------------------------------------------------------------------- outils

def sql(valeur):
    if valeur is None:
        return "NULL"
    if isinstance(valeur, bool):
        return "TRUE" if valeur else "FALSE"
    if isinstance(valeur, (int, float)):
        return str(valeur)
    if isinstance(valeur, datetime):
        return "'" + valeur.strftime("%Y-%m-%d %H:%M:%S") + "'"
    if isinstance(valeur, date):
        return "'" + valeur.strftime("%Y-%m-%d") + "'"
    return "'" + str(valeur).replace("\\", "\\\\").replace("'", "''") + "'"


def sans_accents(texte):
    return "".join(c for c in unicodedata.normalize("NFD", texte) if unicodedata.category(c) != "Mn")


def slug(texte):
    s = sans_accents(texte).lower()
    return "".join(c if c.isalnum() else "-" for c in s).strip("-").replace("--", "-")


def bcrypt(mot_de_passe):
    """Hachage BCrypt (coût 10), préfixe $2a$ attendu par Spring Security.

    Avec la bibliothèque Python « bcrypt » (pip install bcrypt), le sel est tiré d'un générateur
    à graine fixe : relancer le script produit exactement le même fichier SQL, donc la même
    empreinte Flyway. À défaut, l'outil htpasswd d'Apache est utilisé (sel aléatoire).
    Les préfixes $2b$ et $2y$ sont équivalents à $2a$ pour l'algorithme.
    """
    sel = "".join(GENERATEUR_SELS.choice(ALPHABET_BCRYPT) for _ in range(21)) + GENERATEUR_SELS.choice(".Oeu")
    try:
        import bcrypt as lib_bcrypt
        resultat = lib_bcrypt.hashpw(mot_de_passe.encode("utf-8"), f"$2b$10${sel}".encode("ascii")).decode("ascii")
        return "$2a$" + resultat[4:]
    except ImportError:
        resultat = subprocess.run(["htpasswd", "-bnBC", "10", "", mot_de_passe],
                                  capture_output=True, text=True, check=True).stdout.strip()
        return "$2a$" + resultat.lstrip(":")[4:]


def heure_ouvree(jour):
    """Horodatage plausible un jour donné (heures de bureau le plus souvent)."""
    if random.random() < 0.85:
        h = random.randint(8, 18)
    else:
        h = random.choice([6, 7, 19, 20, 21, 22])
    return jour.replace(hour=h, minute=random.randint(0, 59), second=random.randint(0, 59), microsecond=0)


def jour_aleatoire(debut, fin):
    delta = (fin - debut).days
    while True:
        j = debut + timedelta(days=random.randint(0, max(delta, 0)))
        if j.weekday() < 5 or random.random() < 0.15:
            return j


# --------------------------------------------------------------------------- référentiels de noms

PRENOMS = ["Lucas", "Emma", "Louis", "Olivia", "Arthur", "Léa", "Noah", "Louise", "Adam", "Mila",
           "Jules", "Élise", "Victor", "Manon", "Nathan", "Chloé", "Hugo", "Camille", "Liam", "Juliette",
           "Thomas", "Sarah", "Maxime", "Laura", "Antoine", "Marie", "Simon", "Lisa", "Mathis", "Inès",
           "Bram", "Lotte", "Wout", "Fien", "Jens", "Lien", "Ayoub", "Yasmine", "Mehdi", "Nora",
           "Ilyas", "Sofia", "Amine", "Salma", "Karim", "Nadia", "Kevin", "Julie", "Romain", "Charlotte"]
NOMS = ["PEETERS", "JANSSENS", "MAES", "JACOBS", "MERTENS", "WILLEMS", "CLAES", "GOOSSENS", "WOUTERS",
        "DE SMET", "DUBOIS", "LAMBERT", "DUPONT", "MARTIN", "SIMON", "LAURENT", "LECLERCQ", "LEJEUNE",
        "RENARD", "HERMANS", "VERMEULEN", "VAN DEN BROECK", "DE BACKER", "MICHEL", "LEROY", "COLLIGNON",
        "DESCAMPS", "FRANÇOIS", "GILLET", "HENRY", "STEVENS", "DE CLERCQ", "VERHAEGEN", "DELVAUX",
        "BOUZIDI", "EL AMRANI", "BENALI", "HAJJI", "AZZOUZI", "MOUSSAOUI", "CHARLIER", "DENIS",
        "GÉRARD", "MASSON", "PIRET"]

PREFIXES_ORG = ["Meuse", "Sambre", "Escaut", "Ardenne", "Brabant", "Hainaut", "Condroz", "Famenne",
                "Hesbaye", "Gaume", "Ourthe", "Semois", "Lesse", "Fagne", "Senne", "Dyle", "Thudinie",
                "Borinage", "Pajottenland", "Lys"]
SECTEURS_ORG = ["Logistique", "Assurances", "Distribution", "Santé", "Télécom", "Énergie", "Immobilier",
                "Agroalimentaire", "Textile", "Transport", "Pharma", "Industries", "Conseil", "Éducation",
                "Hôtellerie", "BTP", "Finance", "Automobile", "Médias", "Services"]
FORMES_ORG = ["SA", "SRL", "Groupe", "Holding", ""]

SYSTEMES_ORG1 = [
    ("ERP", "Progiciel de gestion intégré (achats, ventes, comptabilité)", "erp.trackit.demo"),
    ("Réseau", "Réseau local du siège : commutateurs, routeurs, câblage", "10.10.0.0/16"),
    ("Messagerie", "Messagerie Exchange et calendriers partagés", "mail.trackit.demo"),
    ("Serveur de fichiers", "Partages réseau des services (FS01)", "\\\\fs01.trackit.demo"),
    ("Base de données Finance", "Instance Oracle de la direction financière", "db-fin01.trackit.demo:1521"),
    ("Poste utilisateur", "Parc de postes de travail Windows et macOS", None),
    ("Application mobile", "Application mobile clients (Android / iOS)", "api.mobile.trackit.demo"),
    ("Portail RH", "Portail de gestion des congés et de la paie", "rh.trackit.demo"),
    ("VPN", "Accès distant sécurisé des collaborateurs", "vpn.trackit.demo"),
    ("Active Directory", "Annuaire et authentification centralisée", "dc01.trackit.demo"),
    ("Téléphonie IP", "Standard et postes téléphoniques IP", "ipbx.trackit.demo"),
    ("Imprimantes", "Imprimantes multifonctions des étages", None),
    ("Sauvegarde", "Sauvegardes quotidiennes Veeam", "backup01.trackit.demo"),
    ("Site e-commerce", "Boutique en ligne et paiement", "www.trackit.demo"),
    ("Wi-Fi", "Réseau sans fil des bâtiments A et B", None),
    ("Intranet", "Portail d'information interne", "intranet.trackit.demo"),
    ("CRM", "Gestion de la relation client", "crm.trackit.demo"),
]
SYSTEMES_GENERIQUES = [
    ("ERP", "Progiciel de gestion intégré"), ("Messagerie", "Messagerie et agendas"),
    ("Réseau", "Infrastructure réseau du siège"), ("Serveur de fichiers", "Partages réseau"),
    ("Poste utilisateur", "Postes de travail"), ("VPN", "Accès distant"),
    ("CRM", "Relation client"), ("Site web", "Site institutionnel"),
    ("Base de données", "Serveur de bases de données"), ("Téléphonie IP", "Téléphonie"),
    ("Application métier", "Application de gestion interne"), ("Wi-Fi", "Réseau sans fil"),
]

# (titre, description, type, systèmes cibles, priorités possibles)
MODELES = [
    ("Indisponibilité du serveur de fichiers", "Les partages réseau {lieu} sont inaccessibles depuis {moment}. Les utilisateurs obtiennent l'erreur « Chemin réseau introuvable ».", "Réseau", ["Serveur de fichiers"], ["Haute", "Critique"]),
    ("Erreur de connexion à l'ERP", "Message « Session expirée » à la connexion à l'ERP pour les utilisateurs {lieu}. Le problème est apparu {moment}.", "Application", ["ERP"], ["Haute", "Critique"]),
    ("Temps de réponse élevé de l'application", "Les écrans de saisie mettent plus de 20 secondes à s'afficher {lieu} depuis {moment}.", "Performance", ["ERP", "CRM", "Portail RH", "Site e-commerce", "Application métier"], ["Moyenne", "Haute"]),
    ("Échec d'authentification", "Plusieurs collaborateurs {lieu} ne parviennent plus à s'authentifier depuis {moment} : « Identifiants invalides » alors que le mot de passe est correct.", "Accès", ["Active Directory", "Messagerie", "VPN", "ERP"], ["Haute", "Critique"]),
    ("Interruption du service réseau", "Coupure complète du réseau {lieu} depuis {moment}. Aucun poste n'obtient d'adresse IP.", "Réseau", ["Réseau", "Wi-Fi"], ["Critique", "Haute"]),
    ("Erreur de synchronisation", "La synchronisation nocturne entre le CRM et l'ERP a échoué {moment}. Les commandes du jour ne sont pas remontées.", "Application", ["CRM", "ERP"], ["Moyenne", "Haute"]),
    ("Espace disque insuffisant", "Alerte de supervision : le volume de données est rempli à 96 %. Les écritures risquent d'échouer.", "Performance", ["Serveur de fichiers", "Base de données Finance", "Sauvegarde", "Base de données"], ["Haute", "Moyenne"]),
    ("Problème de connexion VPN", "Les connexions VPN échouent pour les collaborateurs en télétravail depuis {moment} (délai d'attente dépassé).", "Accès", ["VPN"], ["Haute", "Moyenne"]),
    ("Application indisponible", "L'application est inaccessible depuis {moment}. Un message d'erreur 503 s'affiche lors de la connexion.", "Application", ["ERP", "Application mobile", "Portail RH", "CRM", "Application métier", "Site web"], ["Critique", "Haute"]),
    ("Lenteurs sur le serveur", "Le serveur répond très lentement {lieu} ; l'utilisation processeur reste au-dessus de 90 % depuis {moment}.", "Performance", ["Serveur de fichiers", "ERP", "Base de données Finance"], ["Moyenne", "Haute"]),
    ("Erreur base de données", "Erreur ORA-01653 lors de l'insertion des écritures comptables : impossible d'étendre la table.", "Base de données", ["Base de données Finance", "Base de données"], ["Haute", "Critique"]),
    ("Problème d'impression", "L'imprimante {lieu} affiche « Hors ligne » et les travaux restent bloqués dans la file d'attente.", "Matériel", ["Imprimantes", "Poste utilisateur"], ["Basse", "Moyenne"]),
    ("Accès bloqué à Outlook", "Outlook redemande en boucle le mot de passe {lieu} depuis {moment}.", "Accès", ["Messagerie"], ["Moyenne", "Haute"]),
    ("Bug sur l'application mobile", "L'application mobile se ferme à l'ouverture de l'écran « Mes commandes » sur Android 14.", "Fonctionnel", ["Application mobile"], ["Haute", "Moyenne"]),
    ("Coupure réseau bâtiment A", "Plus aucune connectivité au bâtiment A depuis {moment}. Le commutateur d'étage ne répond plus.", "Réseau", ["Réseau", "Wi-Fi"], ["Critique"]),
    ("Poste de travail ne démarre plus", "Le poste {lieu} reste bloqué sur l'écran de démarrage après la mise à jour {moment}.", "Matériel", ["Poste utilisateur"], ["Moyenne", "Basse"]),
    ("Messages bloqués dans la file d'envoi", "Les courriels externes restent dans la boîte d'envoi depuis {moment} ; les envois internes fonctionnent.", "Application", ["Messagerie"], ["Haute", "Moyenne"]),
    ("Échec de la sauvegarde nocturne", "Le travail de sauvegarde s'est terminé en erreur {moment} (cible de stockage inaccessible).", "Autre", ["Sauvegarde"], ["Haute", "Moyenne"]),
    ("Calcul erroné des congés", "Le solde de congés affiché dans le portail ne tient pas compte des jours posés le mois précédent.", "Fonctionnel", ["Portail RH", "Application métier"], ["Moyenne", "Basse"]),
    ("Paiement refusé sur le site", "Les paiements par carte échouent sur le site avec le code « 05 » depuis {moment}.", "Fonctionnel", ["Site e-commerce", "Site web"], ["Critique", "Haute"]),
    ("Wi-Fi instable en salle de réunion", "Déconnexions fréquentes du Wi-Fi {lieu}, en particulier pendant les visioconférences.", "Réseau", ["Wi-Fi"], ["Moyenne", "Basse"]),
    ("Compte utilisateur verrouillé", "Le compte d'un collaborateur {lieu} est verrouillé après plusieurs tentatives infructueuses.", "Accès", ["Active Directory", "ERP", "Messagerie"], ["Basse", "Moyenne"]),
    ("Téléphones IP sans tonalité", "Les postes téléphoniques {lieu} n'ont plus de tonalité depuis {moment}.", "Réseau", ["Téléphonie IP"], ["Haute", "Moyenne"]),
    ("Export comptable incomplet", "L'export mensuel des écritures vers le cabinet comptable omet les avoirs.", "Fonctionnel", ["ERP"], ["Moyenne"]),
    ("Certificat SSL expiré", "Le navigateur signale un certificat expiré sur le site depuis {moment}.", "Autre", ["Intranet", "Site e-commerce", "Site web", "Portail RH"], ["Haute"]),
    ("Requêtes lentes sur la base", "Le rapport des ventes met plus de 3 minutes à s'exécuter ; plusieurs requêtes sans index identifiées.", "Base de données", ["Base de données Finance", "Base de données", "CRM"], ["Moyenne", "Haute"]),
    ("Écran noir sur poste portable", "L'écran du portable reste noir au réveil de la veille {lieu}.", "Matériel", ["Poste utilisateur"], ["Basse"]),
    ("Droits d'accès manquants", "Un nouvel arrivant {lieu} n'a pas accès au dossier partagé de son service.", "Accès", ["Serveur de fichiers", "Active Directory"], ["Basse", "Moyenne"]),
    ("Erreur 500 sur l'intranet", "La page d'accueil de l'intranet renvoie une erreur 500 depuis {moment}.", "Application", ["Intranet", "Site web"], ["Moyenne"]),
    ("Doublons de fiches clients", "Des fiches clients sont créées en double lors de l'import du fichier des partenaires.", "Fonctionnel", ["CRM"], ["Moyenne", "Basse"]),
    ("Service API indisponible", "L'API REST renvoie des erreurs 502 depuis {moment} ; les applications partenaires ne peuvent plus transmettre leurs commandes.", "Application", ["Application mobile", "Site e-commerce", "Application métier", "CRM"], ["Critique", "Haute"]),
    ("Échec de sauvegarde", "La sauvegarde complète planifiée {moment} s'est arrêtée à 63 % avec l'erreur « Snapshot creation failed ».", "Autre", ["Sauvegarde", "Serveur de fichiers", "Base de données Finance"], ["Haute", "Moyenne"]),
    ("Problème d'accès à la base de données", "Les requêtes échouent avec « Too many connections » depuis {moment} ; le pool de connexions est saturé.", "Base de données", ["Base de données Finance", "Base de données", "ERP"], ["Critique", "Haute"]),
    ("Interruption du réseau interne", "Les utilisateurs {lieu} perdent l'accès aux ressources internes par intermittence depuis {moment}.", "Réseau", ["Réseau", "VPN", "Wi-Fi"], ["Haute", "Critique"]),
    ("Erreur lors de l'authentification", "Message « Votre mot de passe a expiré » affiché en boucle {lieu}, même après réinitialisation.", "Accès", ["Active Directory", "Portail RH", "Intranet"], ["Moyenne", "Haute"]),
    ("Fuite mémoire sur le serveur d'application", "La mémoire consommée augmente de 2 Go par heure depuis le déploiement {moment} ; redémarrage nécessaire toutes les 6 heures.", "Performance", ["ERP", "CRM", "Application métier", "Site e-commerce"], ["Haute"]),
    ("Facture générée avec un mauvais taux de TVA", "Les factures émises {moment} appliquent un taux de 6 % au lieu de 21 % sur les prestations de service.", "Fonctionnel", ["ERP", "Site e-commerce"], ["Haute", "Moyenne"]),
    ("Scanner réseau introuvable", "Le copieur multifonction {lieu} ne trouve plus le dossier de numérisation partagé.", "Matériel", ["Imprimantes", "Serveur de fichiers"], ["Basse", "Moyenne"]),
]

# Formulations alternatives : un même incident n'est pas toujours décrit de la même façon.
VARIANTES_TITRES = {
    "Indisponibilité du serveur de fichiers": ["Partages réseau inaccessibles", "Serveur de fichiers ne répond plus"],
    "Erreur de connexion à l'ERP": ["Impossible de se connecter à l'ERP", "Session ERP expirée à la connexion"],
    "Temps de réponse élevé de l'application": ["Lenteurs importantes de l'application", "Écrans de saisie très lents"],
    "Échec d'authentification": ["Connexion refusée malgré un mot de passe correct", "Authentification impossible"],
    "Interruption du service réseau": ["Perte totale du réseau", "Aucune adresse IP attribuée aux postes"],
    "Erreur de synchronisation": ["Échec de synchronisation des données", "Synchronisation nocturne CRM / ERP en erreur"],
    "Espace disque insuffisant": ["Volume de données presque saturé", "Alerte disque plein"],
    "Problème de connexion VPN": ["VPN inaccessible en télétravail", "Délai d'attente dépassé sur le VPN"],
    "Application indisponible": ["Erreur 503 à l'ouverture de l'application", "Application métier inaccessible"],
    "Lenteurs sur le serveur": ["Processeur du serveur saturé", "Serveur très lent"],
    "Erreur base de données": ["Table comptable impossible à étendre", "Erreur ORA-01653 sur la base"],
    "Problème d'impression": ["Imprimante hors ligne", "Travaux d'impression bloqués"],
    "Accès bloqué à Outlook": ["Outlook redemande le mot de passe en boucle", "Messagerie Outlook inaccessible"],
    "Bug sur l'application mobile": ["Plantage de l'application mobile sous Android", "Fermeture inattendue de l'écran Mes commandes"],
    "Poste de travail ne démarre plus": ["Poste bloqué au démarrage", "Écran figé après mise à jour Windows"],
    "Messages bloqués dans la file d'envoi": ["Courriels externes non envoyés", "File d'envoi de la messagerie bloquée"],
    "Échec de la sauvegarde nocturne": ["Sauvegarde nocturne en erreur", "Travail de sauvegarde interrompu"],
    "Paiement refusé sur le site": ["Paiements par carte refusés", "Échec des transactions en ligne"],
    "Wi-Fi instable en salle de réunion": ["Coupures Wi-Fi pendant les visioconférences", "Signal Wi-Fi faible"],
    "Compte utilisateur verrouillé": ["Compte verrouillé après plusieurs tentatives", "Déverrouillage de compte demandé"],
    "Certificat SSL expiré": ["Alerte de sécurité : certificat expiré", "Certificat HTTPS à renouveler"],
    "Requêtes lentes sur la base": ["Rapport des ventes très lent", "Requêtes SQL sans index"],
    "Droits d'accès manquants": ["Accès refusé au dossier du service", "Habilitations manquantes pour un nouvel arrivant"],
    "Service API indisponible": ["API partenaires en erreur 502", "Passerelle API ne répond plus"],
    "Problème d'accès à la base de données": ["Pool de connexions saturé", "Connexions à la base refusées"],
}
PRECISIONS = ["bâtiment A", "bâtiment B", "agence de Bruxelles", "agence de Liège", "site de Namur", "service comptabilité",
              "service RH", "service commercial", "direction générale", "entrepôt de Charleroi", "2e étage", "accueil",
              "service achats", "télétravail", "salle de réunion Meuse", "plateau du 3e étage"]
LIEUX = ["au service comptabilité", "au 2e étage", "au service RH", "à l'accueil", "au service commercial",
         "au bâtiment B", "à l'agence de Bruxelles", "à l'agence de Liège", "au service achats",
         "en salle de réunion Meuse", "à la direction générale", "à l'entrepôt de Charleroi", "au service juridique"]
MOMENTS = ["ce matin", "8 h 30", "hier soir", "la mise à jour de la nuit", "le redémarrage de lundi",
           "midi", "l'ouverture des bureaux", "la coupure électrique de ce week-end"]

COMMENTAIRES_DIAGNOSTIC_GENERIQUES = [
    "Diagnostic en cours : les journaux du serveur montrent des erreurs répétées depuis l'heure signalée.",
    "Incident reproduit sur un poste de test. Analyse des journaux applicatifs en cours.",
    "Le fournisseur a été contacté, ticket ouvert de son côté.",
    "Contrôle de la supervision : pic de charge confirmé au moment de l'incident.",
    "Plusieurs utilisateurs confirment le problème, l'impact est plus large que prévu.",
    "Vérification du câblage et des équipements réseau de l'étage effectuée.",
    "Je prends en charge, premier diagnostic dans l'heure.",
    "Capture d'écran et extrait des journaux ajoutés en pièce jointe.",
]
COMMENTAIRES_RESOLUTION_GENERIQUES = [
    "Redémarrage du service effectué, fonctionnement nominal constaté.",
    "Correctif appliqué et validé avec l'utilisateur.",
    "Remplacement de l'équipement défectueux, tests concluants.",
    "Espace libéré et alerte de supervision ajustée.",
    "Configuration corrigée, les utilisateurs ont pu se reconnecter.",
    "Mise à jour du pilote installée, le problème ne se reproduit plus.",
]
COMMENTAIRES_FERMETURE = [
    "Résolution validée, aucune récidive constatée depuis 48 h.",
    "Validation par le demandeur, anomalie clôturée.",
    "Clôture après vérification de la supervision.",
]

DIAGNOSTICS = {
    "Réseau": ["Le commutateur d'étage ne répond plus au ping depuis {heure}, déplacement sur site en cours.",
               "Perte de paquets de {n} % mesurée vers la passerelle du segment {systeme}.",
               "Port uplink en erreur sur le commutateur, câble remplacé à titre de test.",
               "Bail DHCP épuisé sur le VLAN utilisateurs : {n} adresses restantes."],
    "Application": ["Les journaux de {systeme} montrent des erreurs 503 répétées depuis {heure}.",
                    "Le service applicatif {systeme} a redémarré {n} fois cette nuit (OutOfMemoryError).",
                    "Incident reproduit sur le poste de test avec un compte standard.",
                    "L'éditeur de {systeme} a confirmé un défaut connu, correctif en préparation."],
    "Matériel": ["Diagnostic matériel lancé : erreur détectée sur le disque.",
                 "Le voyant d'erreur clignote orange, kit de maintenance commandé.",
                 "Pilote réinstallé, le périphérique est de nouveau détecté par le poste.",
                 "Poste de prêt remis à l'utilisateur pendant la réparation."],
    "Accès": ["Le compte apparaît verrouillé dans l'annuaire depuis {heure} après {n} tentatives.",
              "Les droits du groupe de sécurité ont été modifiés lors de la dernière revue des habilitations.",
              "Synchronisation de l'annuaire en retard de {n} minutes, forçage en cours.",
              "Le jeton d'authentification expire trop tôt : paramètre de session à vérifier sur {systeme}."],
    "Base de données": ["Tablespace rempli à {n} % sur {systeme}, extension planifiée.",
                        "Requête bloquante identifiée (verrou posé depuis {heure}).",
                        "Plan d'exécution dégradé après la mise à jour des statistiques.",
                        "Le nombre de connexions ouvertes atteint la limite configurée ({n})."],
    "Performance": ["Utilisation processeur de {systeme} à {n} % depuis {heure}.",
                    "Temps de réponse moyen mesuré : {n} secondes sur l'écran de saisie.",
                    "Pic de charge lié au traitement batch de fin de mois.",
                    "La mémoire disponible sur le serveur {systeme} est inférieure à 5 %."],
    "Fonctionnel": ["Cas reproduit avec un jeu de données de test, règle de calcul en cause.",
                    "Écart constaté sur {n} enregistrements, extraction transmise au métier.",
                    "La règle de gestion a été modifiée lors de la dernière version de {systeme}.",
                    "Contournement communiqué aux utilisateurs en attendant le correctif."],
    "Autre": ["Le travail planifié s'est arrêté à {heure}, journal d'exécution analysé.",
              "Ticket ouvert chez le prestataire, retour attendu sous 4 heures.",
              "Espace de stockage cible à {n} %, purge des anciennes sauvegardes nécessaire."],
}
RESOLUTIONS = {
    "Réseau": ["Commutateur redémarré et port réaffecté, connectivité rétablie pour tout l'étage.",
               "Plage DHCP élargie, tous les postes ont obtenu une adresse.",
               "Point d'accès Wi-Fi remplacé, signal stable mesuré pendant 2 heures."],
    "Application": ["Correctif de l'éditeur appliqué sur {systeme}, service de nouveau disponible.",
                    "Redémarrage du service et augmentation de la mémoire allouée, fonctionnement nominal.",
                    "Configuration de session corrigée, les utilisateurs se reconnectent normalement."],
    "Matériel": ["Pièce défectueuse remplacée, tests concluants avec l'utilisateur.",
                 "Équipement remplacé par un modèle de stock, données transférées."],
    "Accès": ["Compte déverrouillé et mot de passe réinitialisé, connexion validée avec l'utilisateur.",
              "Droits rétablis dans le groupe de sécurité adéquat."],
    "Base de données": ["Tablespace étendu de 20 Go, insertions de nouveau possibles.",
                        "Index ajouté, le rapport s'exécute désormais en {n} secondes.",
                        "Session bloquante arrêtée et pool de connexions redimensionné."],
    "Performance": ["Traitement batch déplacé en heures creuses, temps de réponse revenu sous 2 secondes.",
                    "Fuite mémoire corrigée par la mise à jour {n}.4 de {systeme}."],
    "Fonctionnel": ["Règle de calcul corrigée et données recalculées sur la période concernée.",
                    "Correctif livré dans la version de maintenance, validé par le métier."],
    "Autre": ["Sauvegarde relancée manuellement et terminée avec succès.",
              "Certificat renouvelé et déployé sur les serveurs frontaux."],
}


def texte(modeles, systeme, quand):
    return random.choice(modeles).format(systeme=systeme, heure=quand.strftime("%H h %M"), n=random.randint(3, 97))


FICHIERS_PJ = {
    "Réseau": [("trace_reseau.log", "log"), ("ping_passerelle.txt", "txt")],
    "Application": [("journal_application.log", "log"), ("message_erreur.txt", "txt")],
    "Performance": [("metriques_cpu.csv", "csv"), ("journal_lenteurs.log", "log")],
    "Accès": [("journal_authentification.log", "log")],
    "Base de données": [("alert_oracle.log", "log"), ("requetes_lentes.csv", "csv")],
    "Matériel": [("rapport_diagnostic.txt", "txt")],
    "Fonctionnel": [("exemple_donnees.csv", "csv"), ("etapes_reproduction.txt", "txt")],
    "Autre": [("journal_sauvegarde.log", "log")],
}
MIME = {"log": "text/plain", "txt": "text/plain", "csv": "text/csv"}


# --------------------------------------------------------------------------- génération

class Generateur:
    def __init__(self):
        self.organisations = []
        self.utilisateurs = []
        self.systemes = []
        self.anomalies = []
        self.commentaires = []
        self.historique = []
        self.pieces = []
        self.notifications = []
        self.rapports = []
        self.abonnements = []
        self.paiements = []
        self.emails = set()
        self.fichiers = {}

    # -- organisations, utilisateurs, systèmes

    def organisation(self, nom, email, creation):
        oid = len(self.organisations) + 1
        self.organisations.append(dict(id=oid, nom=nom, email=email, date_creation=creation))
        return oid

    def utilisateur(self, org, role, prenom, nom, domaine, creation, mot_de_passe=None, actif=True):
        base = slug(prenom).replace("-", "") + "." + slug(nom).replace("-", "")
        email = f"{base}@{domaine}"
        n = 2
        while email in self.emails:
            email = f"{base}{n}@{domaine}"
            n += 1
        self.emails.add(email)
        uid = len(self.utilisateurs) + 1
        mdp = mot_de_passe or "".join(random.choice("ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789") for _ in range(16))
        self.utilisateurs.append(dict(id=uid, nom=nom, prenom=prenom, email=email, mot_de_passe=bcrypt(mdp),
                                      actif=actif, date_creation=creation, id_role=ROLES[role], id_organisation=org,
                                      role=role))
        return uid

    def systeme(self, org, nom, description, adresse=None, actif=True):
        sid = len(self.systemes) + 1
        self.systemes.append(dict(id=sid, nom=nom, description=description, adresse=adresse, actif=actif,
                                  id_organisation=org))
        return sid

    def nom_aleatoire(self):
        return random.choice(PRENOMS), random.choice(NOMS)

    # -- cycle de vie d'une anomalie

    def evenement(self, anomalie, uid, quand, action, ancien_statut=None, nouveau_statut=None,
                  ancienne_priorite=None, nouvelle_priorite=None):
        self.historique.append(dict(id=len(self.historique) + 1, action=action, ancien_statut=ancien_statut,
                                    nouveau_statut=nouveau_statut, ancienne_priorite=ancienne_priorite,
                                    nouvelle_priorite=nouvelle_priorite, date_modification=quand,
                                    id_anomalie=anomalie["id"], id_utilisateur=uid))

    def commentaire(self, anomalie, uid, quand, contenu):
        self.commentaires.append(dict(id=len(self.commentaires) + 1, contenu=contenu, date_creation=quand,
                                      id_anomalie=anomalie["id"], id_utilisateur=uid))

    def notifier(self, destinataire, anomalie, quand, message):
        if destinataire is None:
            return
        lu = (MAINTENANT - quand).days > 5 or random.random() < 0.3
        self.notifications.append(dict(id=len(self.notifications) + 1, message=message, date_creation=quand,
                                       lu=lu, id_utilisateur=destinataire, id_anomalie=anomalie["id"]))

    def nom_complet(self, uid):
        u = self.utilisateurs[uid - 1]
        return f"{u['prenom']} {u['nom']}"

    def anomalie(self, org, operateurs, responsables, valideurs, systemes_org, creation, statut_cible, modele=None):
        noms_systemes = {s["nom"] for s in systemes_org}
        compatibles = [m for m in MODELES if noms_systemes & set(m[3])] or MODELES
        titre, description, type_nom, cibles, priorites = modele or random.choice(compatibles)
        candidats = [s for s in systemes_org if s["nom"] in cibles] or systemes_org
        systeme = random.choice(candidats)
        if random.random() < 0.45:
            titre = random.choice(VARIANTES_TITRES.get(titre, [titre]))
        if random.random() < 0.55:
            titre = f"{titre} – {random.choice(PRECISIONS)}"
        elif systeme["nom"].lower() not in titre.lower() and random.random() < 0.5:
            titre = f"{titre} ({systeme['nom']})"
        description = description.format(lieu=random.choice(LIEUX), moment=random.choice(MOMENTS))
        priorite = random.choice(priorites)
        auteur = random.choice(operateurs)
        aid = len(self.anomalies) + 1
        ref = f"#{aid:03d}"
        a = dict(id=aid, titre=titre, description=description, date_creation=creation, date_resolution=None,
                 id_auteur=auteur, id_operateur=None, id_systeme=systeme["id"], id_type=TYPES[type_nom],
                 id_priorite=PRIORITES[priorite], id_statut=STATUTS["Nouvelle"], type_nom=type_nom)
        self.anomalies.append(a)

        self.evenement(a, auteur, creation, f"Création de l'anomalie (système : {systeme['nom']})",
                       nouveau_statut="Nouvelle", nouvelle_priorite=priorite)
        for rt in responsables:
            self.notifier(rt, a, creation, f"Nouvelle anomalie {ref} « {titre} » ({priorite}) déclarée par "
                                           f"{self.nom_complet(auteur)}.")
        self.notifier(auteur, a, creation, f"Accusé de réception : votre anomalie {ref} « {titre} » a été "
                                           f"enregistrée avec le statut Nouvelle.")

        t = creation
        niveau = {"Nouvelle": 0, "En cours": 1, "Résolue": 2, "Fermée": 3}[statut_cible]

        # Repriorisation occasionnelle par le responsable technique
        if responsables and random.random() < 0.12:
            t2 = t + timedelta(minutes=random.randint(10, 90))
            if t2 < MAINTENANT:
                nouvelle = random.choice([p for p in PRIORITES if p != priorite])
                rt = random.choice(responsables)
                self.evenement(a, rt, t2, "Changement de priorité", ancienne_priorite=priorite, nouvelle_priorite=nouvelle)
                self.notifier(auteur, a, t2, f"La priorité de l'anomalie {ref} est passée de « {priorite} » à « {nouvelle} ».")
                priorite = nouvelle
                a["id_priorite"] = PRIORITES[nouvelle]
                t = t2

        if niveau == 0:
            t2 = t + timedelta(minutes=random.randint(5, 90))
            if responsables and random.random() < 0.4 and t2 < MAINTENANT:
                self.affecter(a, responsables, operateurs, t2)
            return a

        # Affectation puis prise en charge (aucun événement postérieur à la date du jour)
        t += timedelta(minutes=random.randint(5, 120))
        if t >= MAINTENANT:
            return a
        self.affecter(a, responsables, operateurs, t)
        operateur = a["id_operateur"]
        t += timedelta(minutes=random.randint(5, 60))
        if t >= MAINTENANT:
            return a
        self.changer_statut(a, operateur, t, "Nouvelle", "En cours", auteur)
        if random.random() < 0.7 and t + timedelta(hours=3) < MAINTENANT:
            t += timedelta(minutes=random.randint(10, 180))
            self.commentaire(a, operateur, t, texte(DIAGNOSTICS[type_nom] + COMMENTAIRES_DIAGNOSTIC_GENERIQUES, systeme["nom"], t))
            self.notifier(auteur, a, t, f"Nouveau commentaire de {self.nom_complet(operateur)} sur l'anomalie {ref}.")
        if random.random() < 0.35 and t + timedelta(minutes=30) < MAINTENANT:
            self.piece_jointe(a, operateur, t + timedelta(minutes=random.randint(1, 30)))
        if niveau == 1:
            return a

        # Résolution (durée selon la priorité)
        heures = {"Critique": (1, 8), "Haute": (3, 36), "Moyenne": (8, 96), "Basse": (24, 240)}[priorite]
        t += timedelta(minutes=random.randint(heures[0] * 60, heures[1] * 60))
        if t >= MAINTENANT:
            return a
        self.changer_statut(a, operateur, t, "En cours", "Résolue", auteur)
        a["date_resolution"] = t
        self.commentaire(a, operateur, t, texte(RESOLUTIONS[type_nom] + COMMENTAIRES_RESOLUTION_GENERIQUES, systeme["nom"], t))

        # Réouverture occasionnelle
        if random.random() < 0.06 and t + timedelta(hours=60) < MAINTENANT:
            t += timedelta(hours=random.randint(2, 30))
            self.changer_statut(a, auteur, t, "Résolue", "En cours", auteur, action="Réouverture de l'anomalie")
            self.commentaire(a, auteur, t, "Le problème est réapparu chez l'utilisateur, réouverture de l'anomalie.")
            a["date_resolution"] = None
            t += timedelta(hours=random.randint(1, 24))
            self.changer_statut(a, operateur, t, "En cours", "Résolue", auteur)
            a["date_resolution"] = t
        if niveau == 2:
            return a

        # Clôture : réservée au responsable technique (ou à l'administrateur à défaut)
        cloture_par = random.choice(valideurs)
        t += timedelta(hours=random.randint(2, 72))
        if t >= MAINTENANT:
            return a
        self.changer_statut(a, cloture_par, t, "Résolue", "Fermée", auteur)
        if random.random() < 0.5:
            self.commentaire(a, cloture_par, t, random.choice(COMMENTAIRES_FERMETURE))
        return a

    def affecter(self, a, responsables, operateurs, quand):
        ref = f"#{a['id']:03d}"
        operateur = random.choice(operateurs)
        if responsables and random.random() < 0.75:
            rt = random.choice(responsables)
            self.evenement(a, rt, quand, f"Affectation à {self.nom_complet(operateur)} (Opérateur)")
            self.notifier(operateur, a, quand, f"L'anomalie {ref} « {a['titre']} » vous a été affectée par "
                                               f"{self.nom_complet(rt)}.")
        else:
            self.evenement(a, operateur, quand, f"Prise en charge par {self.nom_complet(operateur)}")
        if operateur != a["id_auteur"]:
            self.notifier(a["id_auteur"], a, quand, f"Votre anomalie {ref} est prise en charge par "
                                                    f"{self.nom_complet(operateur)}.")
        a["id_operateur"] = operateur
        return quand

    def changer_statut(self, a, uid, quand, ancien, nouveau, auteur, action="Changement de statut"):
        self.evenement(a, uid, quand, action, ancien_statut=ancien, nouveau_statut=nouveau)
        a["id_statut"] = STATUTS[nouveau]
        if uid != auteur:
            self.notifier(auteur, a, quand, f"L'anomalie #{a['id']:03d} est passée du statut « {ancien} » au "
                                            f"statut « {nouveau} » ({self.nom_complet(uid)}).")

    def piece_jointe(self, a, uid, quand):
        nom, ext = random.choice(FICHIERS_PJ[a["type_nom"]])
        chemin = f"demo/anomalie-{a['id']:03d}/{nom}"
        self.fichiers[chemin] = contenu_fichier(a, nom, ext, quand)
        self.pieces.append(dict(id=len(self.pieces) + 1, nom_fichier=nom, chemin=chemin, type_fichier=MIME[ext],
                                date_ajout=quand, id_anomalie=a["id"], id_utilisateur=uid))


def contenu_fichier(a, nom, ext, quand):
    lignes = []
    t = quand - timedelta(minutes=15)
    if ext == "csv":
        lignes.append("horodatage;indicateur;valeur")
        for i in range(12):
            lignes.append(f"{(t + timedelta(minutes=i)).strftime('%Y-%m-%d %H:%M')};{nom.split('.')[0]};{random.randint(55, 99)}")
    elif ext == "log":
        niveaux = ["INFO", "WARN", "ERROR"]
        for i in range(15):
            niveau = random.choices(niveaux, weights=[5, 3, 2])[0]
            lignes.append(f"{(t + timedelta(seconds=37 * i)).strftime('%Y-%m-%d %H:%M:%S')} {niveau} "
                          f"[{a['titre'][:28]}] événement {1000 + i} – code retour {random.choice([0, 0, 1, 503, 504])}")
    else:
        lignes += [f"Anomalie #{a['id']:03d} – {a['titre']}", "", "Constat :", a["description"], "",
                   "Étapes réalisées :", "1. Vérification de l'alimentation et des voyants",
                   "2. Redémarrage de l'équipement", "3. Test avec un second utilisateur", "",
                   f"Relevé le {quand.strftime('%d/%m/%Y à %H:%M')}"]
    return "\n".join(lignes) + "\n"


def generer():
    g = Generateur()

    # ---- Organisation de démonstration principale (maquettes : organisation « TrackIT »)
    org1 = g.organisation("TrackIT", "contact@trackit.demo", datetime(2026, 1, 12, 9, 15))
    dom1 = "trackit.demo"
    creation_comptes = datetime(2026, 1, 12, 10, 0)
    demo = {
        "Administrateur": g.utilisateur(org1, "Administrateur", "Julien", "DUBOIS", dom1, creation_comptes, MOT_DE_PASSE_DEMO),
        "Opérateur": g.utilisateur(org1, "Opérateur", "Younes", "ERRAHMANI", dom1, creation_comptes, MOT_DE_PASSE_DEMO),
        "Responsable technique": g.utilisateur(org1, "Responsable technique", "Sophie", "PEETERS", dom1, creation_comptes, MOT_DE_PASSE_DEMO),
        "Analyste": g.utilisateur(org1, "Analyste", "Nadia", "LAMBERT", dom1, creation_comptes, MOT_DE_PASSE_DEMO),
        "Utilisateur": g.utilisateur(org1, "Utilisateur", "Lucas", "JANSSENS", dom1, creation_comptes, MOT_DE_PASSE_DEMO),
    }
    operateurs1 = [demo["Opérateur"]]
    responsables1 = [demo["Responsable technique"]]
    for role, nombre in [("Opérateur", 7), ("Responsable technique", 1), ("Analyste", 1), ("Utilisateur", 14)]:
        for _ in range(nombre):
            prenom, nom = g.nom_aleatoire()
            uid = g.utilisateur(org1, role, prenom, nom, dom1,
                                heure_ouvree(jour_aleatoire(datetime(2026, 1, 12), datetime(2026, 4, 1))))
            if role == "Opérateur":
                operateurs1.append(uid)
            elif role == "Responsable technique":
                responsables1.append(uid)
    prenom, nom = g.nom_aleatoire()
    g.utilisateur(org1, "Opérateur", prenom, nom, dom1, datetime(2026, 2, 3, 9, 0), actif=False)

    systemes1 = [g.systemes[g.systeme(org1, n, d, adr) - 1] for n, d, adr in SYSTEMES_ORG1]

    # 230 anomalies réparties d'avril à octobre 2026, statut fonction de l'ancienneté
    dates = sorted(heure_ouvree(jour_aleatoire(DEBUT_HISTORIQUE, MAINTENANT - timedelta(days=1)))
                   for _ in range(222))
    dates += sorted(MAINTENANT - timedelta(hours=random.randint(1, 20)) for _ in range(8))
    for creation in dates:
        age = (MAINTENANT - creation).days
        if age > 45:
            statut = random.choices(["Fermée", "Résolue", "En cours"], weights=[80, 17, 3])[0]
        elif age > 14:
            statut = random.choices(["Fermée", "Résolue", "En cours", "Nouvelle"], weights=[45, 30, 18, 7])[0]
        elif age > 3:
            statut = random.choices(["Fermée", "Résolue", "En cours", "Nouvelle"], weights=[15, 30, 35, 20])[0]
        else:
            statut = random.choices(["Résolue", "En cours", "Nouvelle"], weights=[15, 40, 45])[0]
        g.anomalie(org1, operateurs1, responsables1, responsables1, systemes1, creation, statut)

    # ---- 100 organisations clientes
    bases = set()
    while len(bases) < 100:
        bases.add(f"{random.choice(PREFIXES_ORG)} {random.choice(SECTEURS_ORG)}")
    noms_org = set()
    for base in sorted(bases):  # ordre fixe : un set Python n'a pas d'ordre garanti
        forme = random.choice(FORMES_ORG)
        noms_org.add(f"{forme} {base}" if forme in ("Groupe", "Holding") else f"{base} {forme}".strip())
    for nom in sorted(noms_org):
        creation_org = heure_ouvree(jour_aleatoire(datetime(2025, 11, 1), datetime(2026, 8, 31)))
        domaine = slug(nom.replace("Groupe ", "").replace("Holding ", "").replace(" SRL", "").replace(" SA", "")) + ".demo"
        oid = g.organisation(nom, f"contact@{domaine}", creation_org)
        apres = creation_org + timedelta(hours=1)
        prenom, nom_p = g.nom_aleatoire()
        admin = g.utilisateur(oid, "Administrateur", prenom, nom_p, domaine, apres)
        ops = []
        for _ in range(random.choice([1, 1, 2])):
            prenom, nom_p = g.nom_aleatoire()
            ops.append(g.utilisateur(oid, "Opérateur", prenom, nom_p, domaine, apres + timedelta(days=1)))
        rts = []
        if random.random() < 0.5:
            prenom, nom_p = g.nom_aleatoire()
            rts.append(g.utilisateur(oid, "Responsable technique", prenom, nom_p, domaine, apres + timedelta(days=1)))
        if random.random() < 0.4:
            prenom, nom_p = g.nom_aleatoire()
            g.utilisateur(oid, "Analyste", prenom, nom_p, domaine, apres + timedelta(days=2))
        if random.random() < 0.3:
            prenom, nom_p = g.nom_aleatoire()
            g.utilisateur(oid, "Utilisateur", prenom, nom_p, domaine, apres + timedelta(days=2))

        systemes_org = [g.systemes[g.systeme(oid, n, d) - 1]
                        for n, d in random.sample(SYSTEMES_GENERIQUES, random.choice([1, 2, 2, 3]))]
        debut_activite = max(creation_org + timedelta(days=2), DEBUT_HISTORIQUE)
        if debut_activite < MAINTENANT - timedelta(days=2):
            for _ in range(random.choice([1, 2, 2, 3])):
                creation = heure_ouvree(jour_aleatoire(debut_activite, MAINTENANT - timedelta(days=1)))
                age = (MAINTENANT - creation).days
                statut = random.choices(["Fermée", "Résolue", "En cours", "Nouvelle"],
                                        weights=[60, 20, 12, 8] if age > 20 else [10, 25, 40, 25])[0]
                g.anomalie(oid, ops, rts, rts or [admin], systemes_org, creation, statut)

    # ---- Rapports générés par les analystes (le fichier n'est pas conservé dans le jeu de démonstration)
    analystes = [u for u in g.utilisateurs if u["role"] == "Analyste"]
    mois = ["avril", "mai", "juin", "juillet", "août", "septembre"]
    for u in analystes:
        n = 25 if u["id_organisation"] == org1 else random.randint(1, 3)
        for _ in range(n):
            m = random.randrange(len(mois))
            type_rapport = random.choice(["Synthèse statistique", "Liste détaillée des anomalies"])
            titre = random.choice([f"Bilan des anomalies – {mois[m]} 2026", f"Rapport mensuel {mois[m]} 2026",
                                   f"Suivi des incidents – {mois[m]} 2026", f"Revue de service – {mois[m]} 2026"])
            quand = heure_ouvree(datetime(2026, 5 + m, random.randint(1, 5)) if m < 5 else datetime(2026, 10, random.randint(1, 4)))
            if quand > MAINTENANT:
                quand = MAINTENANT - timedelta(hours=2)
            g.rapports.append(dict(id=len(g.rapports) + 1, titre=titre, type=type_rapport,
                                   format=random.choice(["PDF", "PDF", "EXCEL"]), date_generation=quand,
                                   chemin_fichier=None, id_analyste=u["id"]))

    # ---- Abonnements et paiements
    def abonnement(org, offre, debut, fin, statut):
        aid = len(g.abonnements) + 1
        g.abonnements.append(dict(id=aid, offre=offre, date_debut=debut, date_fin=fin, statut=statut, id_organisation=org))
        return aid

    def paiement(abo, montant, quand, statut):
        ref = "cs_test_" + "".join(random.choice("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789")
                                   for _ in range(56))
        g.paiements.append(dict(id=len(g.paiements) + 1, montant=montant, devise="EUR", date_paiement=quand,
                                statut=statut, reference_stripe=ref, id_abonnement=abo))

    a = abonnement(org1, "Premium mensuel", date(2026, 1, 15), date(2026, 2, 14), "EXPIRE")
    paiement(a, "29.00", datetime(2026, 1, 15, 10, 12), "REUSSI")
    a = abonnement(org1, "Premium annuel", date(2026, 2, 15), date(2027, 2, 14), "ACTIF")
    paiement(a, "290.00", datetime(2026, 2, 14, 16, 40), "REUSSI")

    for org in g.organisations[1:]:
        debut = org["date_creation"].date() + timedelta(days=random.randint(3, 30))
        if debut > MAINTENANT.date():
            continue
        scenario = random.choices(["actif_mensuel", "actif_annuel", "expire", "echec_puis_actif", "annule"],
                                  weights=[30, 25, 20, 15, 10])[0]
        if scenario == "echec_puis_actif":
            abo = abonnement(org["id"], "Premium mensuel", debut, None, "ANNULE")
            paiement(abo, "29.00", datetime.combine(debut, datetime.min.time()) + timedelta(hours=10), "ECHOUE")
            debut += timedelta(days=1)
            scenario = "actif_mensuel"
        if scenario == "annule":
            abo = abonnement(org["id"], "Premium annuel", debut, None, "ANNULE")
            paiement(abo, "290.00", datetime.combine(debut, datetime.min.time()) + timedelta(hours=11), "ANNULE")
            continue
        if scenario == "actif_annuel":
            fin = debut + timedelta(days=364)
            abo = abonnement(org["id"], "Premium annuel", debut, fin, "ACTIF" if fin >= MAINTENANT.date() else "EXPIRE")
            paiement(abo, "290.00", datetime.combine(debut, datetime.min.time()) + timedelta(hours=9, minutes=30), "REUSSI")
            continue
        # Mensuel : renouvellements successifs jusqu'à aujourd'hui, ou arrêt (expiré)
        renouvellements = 99 if scenario == "actif_mensuel" else random.randint(1, 3)
        while debut <= MAINTENANT.date() and renouvellements > 0:
            fin = debut + timedelta(days=30)
            statut = "ACTIF" if debut <= MAINTENANT.date() <= fin else "EXPIRE"
            abo = abonnement(org["id"], "Premium mensuel", debut, fin, statut)
            paiement(abo, "29.00", datetime.combine(debut, datetime.min.time()) + timedelta(hours=random.randint(8, 18)), "REUSSI")
            debut = fin + timedelta(days=1)
            renouvellements -= 1

    return g, demo


# --------------------------------------------------------------------------- écriture

def inserts(table, colonnes, lignes, cles=None):
    cles = cles or colonnes
    sortie = []
    for i in range(0, len(lignes), 100):
        lot = lignes[i:i + 100]
        valeurs = ",\n".join("    (" + ", ".join(sql(l[c]) for c in cles) + ")" for l in lot)
        sortie.append(f"INSERT INTO {table} ({', '.join(colonnes)}) VALUES\n{valeurs};\n")
    return "\n".join(sortie)


def ecrire(g):
    parties = [
        "-- TrackIT — données de démonstration (générées par database/scripts/generer_donnees_demo.py).\n"
        "-- Comptes de démonstration : voir README.md. Mots de passe hachés en BCrypt.\n",
        inserts("organisation", ["id_organisation", "nom", "email", "date_creation"], g.organisations,
                ["id", "nom", "email", "date_creation"]),
        inserts("utilisateur", ["id_utilisateur", "nom", "prenom", "email", "mot_de_passe", "actif", "date_creation",
                                "id_role", "id_organisation"], g.utilisateurs,
                ["id", "nom", "prenom", "email", "mot_de_passe", "actif", "date_creation", "id_role", "id_organisation"]),
        inserts("systeme_it", ["id_systeme", "nom", "description", "adresse", "actif", "id_organisation"], g.systemes,
                ["id", "nom", "description", "adresse", "actif", "id_organisation"]),
        inserts("anomalie", ["id_anomalie", "titre", "description", "date_creation", "date_resolution", "id_auteur",
                             "id_operateur", "id_systeme", "id_type", "id_priorite", "id_statut"], g.anomalies,
                ["id", "titre", "description", "date_creation", "date_resolution", "id_auteur", "id_operateur",
                 "id_systeme", "id_type", "id_priorite", "id_statut"]),
        inserts("commentaire", ["id_commentaire", "contenu", "date_creation", "id_anomalie", "id_utilisateur"],
                g.commentaires, ["id", "contenu", "date_creation", "id_anomalie", "id_utilisateur"]),
        inserts("historique", ["id_historique", "action", "ancien_statut", "nouveau_statut", "ancienne_priorite",
                               "nouvelle_priorite", "date_modification", "id_anomalie", "id_utilisateur"], g.historique,
                ["id", "action", "ancien_statut", "nouveau_statut", "ancienne_priorite", "nouvelle_priorite",
                 "date_modification", "id_anomalie", "id_utilisateur"]),
        inserts("piece_jointe", ["id_piece_jointe", "nom_fichier", "chemin", "type_fichier", "date_ajout",
                                 "id_anomalie", "id_utilisateur"], g.pieces,
                ["id", "nom_fichier", "chemin", "type_fichier", "date_ajout", "id_anomalie", "id_utilisateur"]),
        inserts("notification", ["id_notification", "message", "date_creation", "lu", "id_utilisateur", "id_anomalie"],
                g.notifications, ["id", "message", "date_creation", "lu", "id_utilisateur", "id_anomalie"]),
        inserts("rapport", ["id_rapport", "titre", "type", "format", "date_generation", "chemin_fichier", "id_analyste"],
                g.rapports, ["id", "titre", "type", "format", "date_generation", "chemin_fichier", "id_analyste"]),
        inserts("abonnement", ["id_abonnement", "offre", "date_debut", "date_fin", "statut", "id_organisation"],
                g.abonnements, ["id", "offre", "date_debut", "date_fin", "statut", "id_organisation"]),
        inserts("paiement", ["id_paiement", "montant", "devise", "date_paiement", "statut", "reference_stripe",
                             "id_abonnement"], g.paiements,
                ["id", "montant", "devise", "date_paiement", "statut", "reference_stripe", "id_abonnement"]),
    ]
    SORTIE_SQL.parent.mkdir(parents=True, exist_ok=True)
    texte = "\n".join(parties)
    # Les montants sont des chaînes « 29.00 » : on retire les guillemets pour un DECIMAL propre.
    texte = texte.replace("'29.00'", "29.00").replace("'290.00'", "290.00")
    SORTIE_SQL.write_text(texte, encoding="utf-8")

    demo_dir = DOSSIER_UPLOADS / "demo"
    if demo_dir.exists():
        for f in sorted(demo_dir.rglob("*"), reverse=True):
            f.unlink() if f.is_file() else f.rmdir()
    for chemin, contenu in g.fichiers.items():
        cible = DOSSIER_UPLOADS / chemin
        cible.parent.mkdir(parents=True, exist_ok=True)
        cible.write_text(contenu, encoding="utf-8")


if __name__ == "__main__":
    gen, comptes = generer()
    ecrire(gen)
    print(f"Fichier SQL : {SORTIE_SQL.relative_to(RACINE)}")
    for nom, lignes in [("organisation", gen.organisations), ("utilisateur", gen.utilisateurs),
                        ("systeme_it", gen.systemes), ("anomalie", gen.anomalies),
                        ("commentaire", gen.commentaires), ("historique", gen.historique),
                        ("piece_jointe", gen.pieces), ("notification", gen.notifications),
                        ("rapport", gen.rapports), ("abonnement", gen.abonnements), ("paiement", gen.paiements)]:
        print(f"  {nom:<14} {len(lignes):>5} lignes")
