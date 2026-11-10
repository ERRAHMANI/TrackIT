// Graphiques SVG légers : courbes d'évolution, anneau de répartition, barres horizontales.
import { decimal } from './i18n.js';
import { esc } from './ui.js';

export const PALETTE = ['#1872E6', '#f0a020', '#7c4ddb', '#1f9d63', '#e05260', '#14a3b8', '#8a6d3b', '#6b7a90'];

export function courbes(points, series, { hauteur = 240 } = {}) {
    const largeur = 640, marge = { g: 36, d: 22, h: 12, b: 28 };
    const l = largeur - marge.g - marge.d, h = hauteur - marge.h - marge.b;
    const max = Math.max(1, ...points.flatMap(p => series.map(s => p[s.cle])));
    const pas = Math.max(1, Math.ceil(max / 4));
    const plafond = pas * 4;
    const x = i => marge.g + (points.length <= 1 ? l / 2 : (i * l) / (points.length - 1));
    const y = v => marge.h + h - (v / plafond) * h;
    let svg = '';
    for (let k = 0; k <= 4; k++) {
        const v = k * pas;
        svg += `<line x1="${marge.g}" x2="${largeur - marge.d}" y1="${y(v)}" y2="${y(v)}" stroke="#e8ecf2"/>`;
        svg += `<text x="${marge.g - 6}" y="${y(v) + 4}" font-size="11" text-anchor="end" fill="#7a8699">${v}</text>`;
    }
    const intervalle = Math.max(1, Math.ceil(points.length / 7));
    points.forEach((p, i) => {
        if (i % intervalle === 0 || i === points.length - 1) {
            const [a, m, j] = p.jour.split('-');
            svg += `<text x="${x(i)}" y="${hauteur - 8}" font-size="11" text-anchor="middle" fill="#7a8699">${j}/${m}</text>`;
        }
    });
    series.forEach(s => {
        const chemin = points.map((p, i) => `${i ? 'L' : 'M'}${x(i).toFixed(1)},${y(p[s.cle]).toFixed(1)}`).join(' ');
        svg += `<path d="${chemin}" fill="none" stroke="${s.couleur}" stroke-width="2.4" stroke-linejoin="round"/>`;
        if (points.length <= 45) {
            points.forEach((p, i) => { svg += `<circle cx="${x(i)}" cy="${y(p[s.cle])}" r="2.6" fill="${s.couleur}"><title>${esc(s.libelle)} : ${p[s.cle]} (${p.jour})</title></circle>`; });
        }
    });
    const legende = series.map(s => `<span><i style="background:${s.couleur}"></i>${esc(s.libelle)}</span>`).join('');
    return `<div class="legende">${legende}</div><div class="graphique"><svg viewBox="0 0 ${largeur} ${hauteur}" role="img" aria-label="Évolution des anomalies">${svg}</svg></div>`;
}

export function anneau(valeurs) {
    const total = valeurs.reduce((s, v) => s + v.valeur, 0);
    if (!total) return '<p class="etat-vide">Aucune donnée sur la période.</p>';
    const r = 70, c = 2 * Math.PI * r;
    let decalage = 0;
    const arcs = valeurs.map((v, i) => {
        const longueur = (v.valeur / total) * c;
        const arc = `<circle r="${r}" cx="100" cy="100" fill="none" stroke="${PALETTE[i % PALETTE.length]}" stroke-width="42"
            stroke-dasharray="${longueur} ${c - longueur}" stroke-dashoffset="${-decalage}" transform="rotate(-90 100 100)">
            <title>${esc(v.libelle)} : ${v.valeur}</title></circle>`;
        decalage += longueur;
        return arc;
    }).join('');
    const legende = valeurs.map((v, i) => `<li><i style="background:${PALETTE[i % PALETTE.length]}"></i>${esc(v.libelle)}
        <strong style="margin-left:auto;padding-left:.6rem">${Math.round((100 * v.valeur) / total)} %</strong></li>`).join('');
    return `<div class="repartition"><div class="graphique"><svg viewBox="0 0 200 200" role="img" aria-label="Répartition">${arcs}
        <text x="100" y="98" text-anchor="middle" font-size="26" font-weight="700" fill="#202B38">${total}</text>
        <text x="100" y="118" text-anchor="middle" font-size="12" fill="#7a8699">anomalies</text></svg></div><ul>${legende}</ul></div>`;
}

export function barres(valeurs, { suffixe = '', couleur = '#1872E6' } = {}) {
    if (!valeurs.length) return '<p class="etat-vide">Aucune donnée sur la période.</p>';
    const max = Math.max(...valeurs.map(v => v.valeur), 1);
    return `<div class="barres">${valeurs.map(v => `<div class="barre-ligne">
        <span title="${esc(v.libelle)}">${esc(v.libelle)}</span>
        <div class="piste"><div class="remplissage" style="width:${(100 * v.valeur) / max}%;background:${couleur}"></div></div>
        <span class="val">${decimal(v.valeur)}${suffixe}</span></div>`).join('')}</div>`;
}
