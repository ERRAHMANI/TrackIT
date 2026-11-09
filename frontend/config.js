// Adresse de l'API TrackIT.
// En développement (frontend servi par serveur.mjs sur le port 5173), l'API écoute sur localhost:8080.
// En production, le backend sert aussi le frontend : l'API est sur la même origine que la page.
(function () {
    const developpement = window.location.port === '5173';
    const origine = developpement ? 'http://localhost:8080' : window.location.origin;
    window.TRACKIT_CONFIG = {
        apiUrl: origine + '/api/v1',
        swaggerUrl: origine + '/swagger-ui.html',
        // Comptes de démonstration affichés uniquement sur le poste local (jamais en production).
        demo: ['localhost', '127.0.0.1'].includes(window.location.hostname)
    };
})();