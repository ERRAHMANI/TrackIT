// Serveur statique minimal pour le frontend TrackIT (aucune dépendance).
// Usage : node serveur.mjs   (port 5173 par défaut, modifiable via PORT=xxxx)
import http from 'node:http';
import { readFile } from 'node:fs/promises';
import { extname, join, normalize, sep } from 'node:path';
import { fileURLToPath } from 'node:url';

const racine = fileURLToPath(new URL('.', import.meta.url));
const port = Number(process.env.PORT || 5173);
const types = {
    '.html': 'text/html; charset=utf-8',
    '.js': 'text/javascript; charset=utf-8',
    '.mjs': 'text/javascript; charset=utf-8',
    '.css': 'text/css; charset=utf-8',
    '.svg': 'image/svg+xml',
    '.png': 'image/png',
    '.ico': 'image/x-icon'
};

http.createServer(async (requete, reponse) => {
    let chemin = decodeURIComponent(new URL(requete.url, 'http://localhost').pathname);
    if (chemin.endsWith('/')) chemin += 'index.html';
    const fichier = normalize(join(racine, chemin));
    if (!fichier.startsWith(racine) || fichier.includes(sep + '.')) {
        reponse.writeHead(403).end();
        return;
    }
    try {
        const contenu = await readFile(fichier);
        reponse.writeHead(200, { 'Content-Type': types[extname(fichier)] || 'application/octet-stream', 'Cache-Control': 'no-cache' });
        reponse.end(contenu);
    } catch {
        reponse.writeHead(404, { 'Content-Type': 'text/plain; charset=utf-8' }).end('Introuvable');
    }
}).listen(port, () => console.log(`Frontend TrackIT disponible sur http://localhost:${port}`));
