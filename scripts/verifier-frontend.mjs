// Vérification du frontend pour l'intégration continue (aucune dépendance).
// Usage : node scripts/verifier-frontend.mjs frontend
// Contrôles : fichiers non vides, syntaxe JavaScript, imports existants,
// pages déclarées dans app.js présentes, ressources d'index.html présentes.
import { readFileSync, readdirSync, statSync, existsSync } from 'node:fs';
import { join, dirname, resolve, relative } from 'node:path';
import { execFileSync } from 'node:child_process';

const racine = resolve(process.argv[2] || 'frontend');
const erreurs = [];
const rel = f => relative(racine, f).replaceAll('\\', '/');

function lister(dossier) {
    return readdirSync(dossier).flatMap(nom => {
        const chemin = join(dossier, nom);
        return statSync(chemin).isDirectory() ? lister(chemin) : [chemin];
    });
}

if (!existsSync(join(racine, 'js'))) {
    console.error(`Dossier introuvable : ${join(racine, 'js')}`);
    process.exit(1);
}
const fichiersJs = lister(join(racine, 'js')).filter(f => f.endsWith('.js'));
fichiersJs.push(join(racine, 'config.js'), join(racine, 'serveur.mjs'));

for (const fichier of fichiersJs) {
    if (!existsSync(fichier)) { erreurs.push(`Fichier manquant : ${rel(fichier)}`); continue; }
    const contenu = readFileSync(fichier, 'utf8');
    // 1. Fichier vide (ex. : committé avant d'avoir collé le contenu).
    if (contenu.trim() === '') { erreurs.push(`Fichier vide : ${rel(fichier)}`); continue; }
    // 2. Syntaxe JavaScript.
    try {
        execFileSync(process.execPath, ['--check', fichier], { stdio: 'pipe' });
    } catch (e) {
        erreurs.push(`Erreur de syntaxe dans ${rel(fichier)} :\n${String(e.stderr).trim()}`);
    }
    // 3. Imports relatifs (import … from './x.js') : le fichier visé doit exister.
    for (const [, cible] of contenu.matchAll(/(?:import|from)\s*['"](\.{1,2}\/[^'"]+)['"]/g)) {
        if (!existsSync(join(dirname(fichier), cible))) {
            erreurs.push(`Import introuvable dans ${rel(fichier)} : ${cible}`);
        }
    }
}

// 4. Chaque page déclarée dans les routes d'app.js doit exister dans js/pages/.
const app = readFileSync(join(racine, 'js', 'app.js'), 'utf8');
const pages = new Set([...app.matchAll(/page:\s*'([^']+)'/g)].map(m => m[1]));
for (const page of pages) {
    if (!existsSync(join(racine, 'js', 'pages', page + '.js'))) {
        erreurs.push(`Page déclarée dans app.js mais absente : js/pages/${page}.js`);
    }
}

// 5. Ressources référencées par index.html (scripts, feuille de style, icône).
const index = readFileSync(join(racine, 'index.html'), 'utf8');
for (const [, cible] of index.matchAll(/(?:src|href)="(?!https?:|#|mailto:)([^"]+)"/g)) {
    if (!existsSync(join(racine, cible))) erreurs.push(`Ressource d'index.html introuvable : ${cible}`);
}

if (erreurs.length) {
    console.error(`Vérification du frontend : ${erreurs.length} erreur(s)\n- ` + erreurs.join('\n- '));
    process.exit(1);
}
console.log(`Vérification du frontend réussie : ${fichiersJs.length} fichiers JavaScript, ${pages.size} pages déclarées.`);
