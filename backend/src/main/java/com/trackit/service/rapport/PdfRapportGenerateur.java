package com.trackit.service.rapport;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.trackit.dto.AnomalieResume;
import com.trackit.dto.DureeMoyenne;
import com.trackit.dto.IncidentRecurrent;
import com.trackit.dto.LibelleValeur;
import com.trackit.dto.StatistiquesResponse;
import com.trackit.mapper.DtoMapper;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class PdfRapportGenerateur {

    private static final Color BLEU = new Color(0x16, 0x36, 0x58);
    private static final Color GRIS = new Color(0xF5, 0xF7, 0xFA);
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_HEURE = DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm");

    private final Font titre = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, BLEU);
    private final Font sousTitre = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, BLEU);
    private final Font normal = FontFactory.getFont(FontFactory.HELVETICA, 9);
    private final Font entete = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);

    public byte[] generer(ContenuRapport contenu) {
        ByteArrayOutputStream sortie = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4.rotate(), 36, 36, 36, 36);
        PdfWriter.getInstance(document, sortie);
        document.open();

        document.add(new Paragraph("TrackIT — " + contenu.titre(), titre));
        StatistiquesResponse s = contenu.statistiques();
        document.add(new Paragraph(contenu.type().getLibelle() + " · " + contenu.organisation()
                + " · période du " + DATE.format(s.debut()) + " au " + DATE.format(s.fin()), normal));
        document.add(new Paragraph("Généré le " + DATE_HEURE.format(contenu.dateGeneration()) + " par " + contenu.auteur(), normal));
        document.add(Chunk.NEWLINE);

        if (contenu.type() == TypeRapport.SYNTHESE) {
            synthese(document, s);
        } else {
            liste(document, contenu.anomalies());
        }
        document.close();
        return sortie.toByteArray();
    }

    private void synthese(Document document, StatistiquesResponse s) {
        section(document, "Indicateurs clés");
        tableau(document, new String[]{"Indicateur", "Valeur"}, List.of(
                new String[]{"Anomalies déclarées", String.valueOf(s.totalDeclarees())},
                new String[]{"Anomalies résolues sur la période", String.valueOf(s.totalResolues())},
                new String[]{"Taux de résolution", s.tauxResolutionPourcent() == null ? "—" : s.tauxResolutionPourcent() + " %"},
                new String[]{"Durée moyenne de résolution", s.dureeMoyenneResolutionHeures() == null ? "—" : s.dureeMoyenneResolutionHeures() + " h"}));
        repartition(document, "Répartition par statut", s.parStatut());
        repartition(document, "Répartition par priorité", s.parPriorite());
        repartition(document, "Répartition par type", s.parType());
        repartition(document, "Répartition par système", s.parSysteme());

        section(document, "Durée moyenne de résolution par priorité");
        tableau(document, new String[]{"Priorité", "Durée moyenne (heures)"},
                s.dureeMoyenneParPriorite().stream().map((DureeMoyenne d) -> new String[]{d.priorite(), String.valueOf(d.heures())}).toList());

        section(document, "Incidents récurrents (au moins 3 occurrences)");
        tableau(document, new String[]{"Système", "Type", "Occurrences"},
                s.incidentsRecurrents().stream().map((IncidentRecurrent i) -> new String[]{i.systeme(), i.type(), String.valueOf(i.nombre())}).toList());
    }

    private void liste(Document document, List<AnomalieResume> anomalies) {
        section(document, anomalies.size() + " anomalie(s) déclarée(s) sur la période");
        tableau(document, new String[]{"N°", "Titre", "Système", "Type", "Priorité", "Statut", "Opérateur", "Création", "Résolution"},
                anomalies.stream().map(a -> new String[]{
                        DtoMapper.reference(a.id()), a.titre(), a.systeme().nom(), a.type().nom(), a.priorite().nom(),
                        a.statut().nom(), a.operateur() == null ? "Non affectée" : a.operateur().nomComplet(),
                        DATE.format(a.dateCreation()), a.dateResolution() == null ? "—" : DATE.format(a.dateResolution())}).toList());
    }

    private void repartition(Document document, String libelle, List<LibelleValeur> valeurs) {
        section(document, libelle);
        long total = valeurs.stream().mapToLong(LibelleValeur::valeur).sum();
        tableau(document, new String[]{"Valeur", "Nombre", "Part"}, valeurs.stream().map(v -> new String[]{
                v.libelle(), String.valueOf(v.valeur()), total == 0 ? "—" : Math.round(100.0 * v.valeur() / total) + " %"}).toList());
    }

    private void section(Document document, String libelle) {
        Paragraph p = new Paragraph(libelle, sousTitre);
        p.setSpacingBefore(10);
        p.setSpacingAfter(4);
        document.add(p);
    }

    private void tableau(Document document, String[] colonnes, List<String[]> lignes) {
        if (lignes.isEmpty()) {
            document.add(new Paragraph("Aucune donnée sur la période.", normal));
            return;
        }
        PdfPTable table = new PdfPTable(colonnes.length);
        table.setWidthPercentage(100);
        for (String colonne : colonnes) {
            PdfPCell cellule = new PdfPCell(new Phrase(colonne, entete));
            cellule.setBackgroundColor(BLEU);
            cellule.setPadding(4);
            table.addCell(cellule);
        }
        int i = 0;
        for (String[] ligne : lignes) {
            for (String valeur : ligne) {
                PdfPCell cellule = new PdfPCell(new Phrase(valeur, normal));
                cellule.setPadding(3);
                if (i % 2 == 1) {
                    cellule.setBackgroundColor(GRIS);
                }
                table.addCell(cellule);
            }
            i++;
        }
        document.add(table);
    }
}
