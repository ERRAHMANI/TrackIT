package com.trackit.service.rapport;

import com.trackit.dto.AnomalieResume;
import com.trackit.dto.LibelleValeur;
import com.trackit.dto.StatistiquesResponse;
import com.trackit.mapper.DtoMapper;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class ExcelRapportGenerateur {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_HEURE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public byte[] generer(ContenuRapport contenu) {
        try (Workbook classeur = new XSSFWorkbook(); ByteArrayOutputStream sortie = new ByteArrayOutputStream()) {
            CellStyle entete = styleEntete(classeur);
            StatistiquesResponse s = contenu.statistiques();

            Sheet synthese = classeur.createSheet("Synthèse");
            int ligne = 0;
            ligne = ecrire(synthese, ligne, null, "TrackIT — " + contenu.titre());
            ligne = ecrire(synthese, ligne, null, "Organisation", contenu.organisation());
            ligne = ecrire(synthese, ligne, null, "Période", DATE.format(s.debut()) + " au " + DATE.format(s.fin()));
            ligne = ecrire(synthese, ligne, null, "Généré le", DATE_HEURE.format(contenu.dateGeneration()) + " par " + contenu.auteur());
            ligne++;
            ligne = ecrire(synthese, ligne, entete, "Indicateur", "Valeur");
            ligne = ecrire(synthese, ligne, null, "Anomalies déclarées", s.totalDeclarees());
            ligne = ecrire(synthese, ligne, null, "Anomalies résolues", s.totalResolues());
            ligne = ecrire(synthese, ligne, null, "Taux de résolution (%)", s.tauxResolutionPourcent());
            ligne = ecrire(synthese, ligne, null, "Durée moyenne de résolution (h)", s.dureeMoyenneResolutionHeures());
            ligne++;
            ligne = repartition(synthese, ligne, entete, "Statut", s.parStatut());
            ligne = repartition(synthese, ligne, entete, "Priorité", s.parPriorite());
            ligne = repartition(synthese, ligne, entete, "Type", s.parType());
            ligne = repartition(synthese, ligne, entete, "Système", s.parSysteme());
            // Même contenu que le PDF : durée moyenne de résolution par priorité
            ligne = ecrire(synthese, ligne, entete, "Priorité", "Durée moyenne de résolution (h)");
            for (var d : s.dureeMoyenneParPriorite()) {
                ligne = ecrire(synthese, ligne, null, d.priorite(), d.heures());
            }
            synthese.setColumnWidth(0, 40 * 256);
            synthese.setColumnWidth(1, 30 * 256);

            Sheet recurrents = classeur.createSheet("Incidents récurrents");
            int r = ecrire(recurrents, 0, entete, "Système", "Type", "Occurrences");
            for (var i : s.incidentsRecurrents()) {
                r = ecrire(recurrents, r, null, i.systeme(), i.type(), i.nombre());
            }
            ajuster(recurrents, 3);

            if (contenu.type() == TypeRapport.LISTE_ANOMALIES) {
                Sheet liste = classeur.createSheet("Anomalies");
                int l = ecrire(liste, 0, entete, "N°", "Titre", "Système", "Type", "Priorité", "Statut", "Déclarant",
                        "Opérateur", "Création", "Résolution");
                for (AnomalieResume a : contenu.anomalies()) {
                    l = ecrire(liste, l, null, DtoMapper.reference(a.id()), a.titre(), a.systeme().nom(), a.type().nom(),
                            a.priorite().nom(), a.statut().nom(), a.auteur().nomComplet(),
                            a.operateur() == null ? "Non affectée" : a.operateur().nomComplet(),
                            DATE_HEURE.format(a.dateCreation()),
                            a.dateResolution() == null ? "" : DATE_HEURE.format(a.dateResolution()));
                }
                liste.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(0, Math.max(0, l - 1), 0, 9));
                ajuster(liste, 10);
            }
            classeur.write(sortie);
            return sortie.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private int repartition(Sheet feuille, int ligne, CellStyle entete, String libelle, List<LibelleValeur> valeurs) {
        ligne = ecrire(feuille, ligne, entete, libelle, "Nombre");
        for (LibelleValeur v : valeurs) {
            ligne = ecrire(feuille, ligne, null, v.libelle(), v.valeur());
        }
        return ligne + 1;
    }

    private int ecrire(Sheet feuille, int index, CellStyle style, Object... valeurs) {
        Row row = feuille.createRow(index);
        for (int i = 0; i < valeurs.length; i++) {
            Cell cellule = row.createCell(i);
            Object v = valeurs[i];
            if (v instanceof Number n) {
                cellule.setCellValue(n.doubleValue());
            } else {
                cellule.setCellValue(v == null ? "" : v.toString());
            }
            if (style != null) {
                cellule.setCellStyle(style);
            }
        }
        return index + 1;
    }

    private static void ajuster(Sheet feuille, int colonnes) {
        for (int i = 0; i < colonnes; i++) {
            feuille.autoSizeColumn(i);
        }
    }

    private static CellStyle styleEntete(Workbook classeur) {
        CellStyle style = classeur.createCellStyle();
        Font police = classeur.createFont();
        police.setBold(true);
        police.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(police);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }
}
