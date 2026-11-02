package com.trackit.service;

import com.trackit.config.TrackItProperties;
import com.trackit.dto.PieceJointeResponse;
import com.trackit.entity.Anomalie;
import com.trackit.entity.PieceJointe;
import com.trackit.entity.Utilisateur;
import com.trackit.exception.ApiException;
import com.trackit.mapper.DtoMapper;
import com.trackit.repository.PieceJointeRepository;
import com.trackit.repository.UtilisateurRepository;
import com.trackit.security.UtilisateurConnecte;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static com.trackit.mapper.DtoMapper.reference;

@Service
public class PieceJointeService {

    /** Formats autorisés (extension → type MIME enregistré). */
    private static final Map<String, String> FORMATS = Map.ofEntries(
            Map.entry("pdf", "application/pdf"),
            Map.entry("png", "image/png"),
            Map.entry("jpg", "image/jpeg"),
            Map.entry("jpeg", "image/jpeg"),
            Map.entry("gif", "image/gif"),
            Map.entry("txt", "text/plain"),
            Map.entry("log", "text/plain"),
            Map.entry("csv", "text/csv"),
            Map.entry("json", "application/json"),
            Map.entry("xml", "application/xml"),
            Map.entry("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
            Map.entry("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
            Map.entry("zip", "application/zip"));
    private static final long TAILLE_MAX = 10L * 1024 * 1024;

    private final PieceJointeRepository pieceJointeRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final AnomalieService anomalieService;
    private final AnomaliePermissions permissions;
    private final HistoriqueService historiqueService;
    private final Path racine;

    public PieceJointeService(PieceJointeRepository pieceJointeRepository, UtilisateurRepository utilisateurRepository,
                              AnomalieService anomalieService, AnomaliePermissions permissions,
                              HistoriqueService historiqueService, TrackItProperties properties) {
        this.pieceJointeRepository = pieceJointeRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.anomalieService = anomalieService;
        this.permissions = permissions;
        this.historiqueService = historiqueService;
        this.racine = Path.of(properties.storage().uploadsDir()).toAbsolutePath().normalize();
    }

    @Transactional(readOnly = true)
    public List<PieceJointeResponse> lister(UtilisateurConnecte courant, Long idAnomalie) {
        anomalieService.charger(courant, idAnomalie);
        return pieceJointeRepository.findByAnomalieIdOrderByDateAjoutAsc(idAnomalie).stream()
                .map(DtoMapper::pieceJointe).toList();
    }

    @Transactional
    public PieceJointeResponse ajouter(UtilisateurConnecte courant, Long idAnomalie, MultipartFile fichier) {
        Anomalie anomalie = anomalieService.charger(courant, idAnomalie);
        if (!permissions.peutJoindreFichier(courant, anomalie)) {
            throw ApiException.interdit("Vous ne pouvez pas joindre de fichier à l'anomalie " + reference(idAnomalie) + ".");
        }
        if (fichier == null || fichier.isEmpty()) {
            throw ApiException.invalide("Le fichier est vide.");
        }
        if (fichier.getSize() > TAILLE_MAX) {
            throw ApiException.invalide("Le fichier dépasse la taille maximale autorisée (10 Mo).");
        }
        String nomOriginal = nettoyerNom(fichier.getOriginalFilename());
        String extension = extension(nomOriginal);
        String typeMime = FORMATS.get(extension);
        if (typeMime == null) {
            throw ApiException.invalide("Format non autorisé. Formats acceptés : " + String.join(", ", FORMATS.keySet().stream().sorted().toList()) + ".");
        }

        String cheminRelatif = "anomalies/" + idAnomalie + "/" + UUID.randomUUID() + "_" + nomOriginal;
        Path destination = resoudre(cheminRelatif);
        try (InputStream contenu = fichier.getInputStream()) {
            Files.createDirectories(destination.getParent());
            Files.copy(contenu, destination, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IllegalStateException("Impossible d'enregistrer le fichier", e);
        }

        Utilisateur auteur = utilisateurRepository.findWithRoleById(courant.id())
                .orElseThrow(() -> ApiException.introuvable("Utilisateur introuvable."));
        PieceJointe pieceJointe = new PieceJointe();
        pieceJointe.setAnomalie(anomalie);
        pieceJointe.setUtilisateur(auteur);
        pieceJointe.setNomFichier(nomOriginal);
        pieceJointe.setChemin(cheminRelatif);
        pieceJointe.setTypeFichier(typeMime);
        pieceJointeRepository.save(pieceJointe);
        historiqueService.journaliser(anomalie, auteur, "Ajout de la pièce jointe « " + nomOriginal + " »");
        return DtoMapper.pieceJointe(pieceJointe);
    }

    public record Telechargement(Resource ressource, String nomFichier, String typeFichier) {
    }

    @Transactional(readOnly = true)
    public Telechargement telecharger(UtilisateurConnecte courant, Long idPieceJointe) {
        PieceJointe pieceJointe = pieceJointeRepository.findWithAnomalieById(idPieceJointe)
                .orElseThrow(() -> ApiException.introuvable("Pièce jointe introuvable."));
        anomalieService.charger(courant, pieceJointe.getAnomalie().getId());
        Path fichier = resoudre(pieceJointe.getChemin());
        if (!Files.isRegularFile(fichier)) {
            throw ApiException.introuvable("Le fichier « " + pieceJointe.getNomFichier() + " » n'est plus disponible sur le serveur.");
        }
        return new Telechargement(new PathResource(fichier), pieceJointe.getNomFichier(), pieceJointe.getTypeFichier());
    }

    private Path resoudre(String cheminRelatif) {
        Path chemin = racine.resolve(cheminRelatif).normalize();
        if (!chemin.startsWith(racine)) {
            throw ApiException.invalide("Chemin de fichier invalide.");
        }
        return chemin;
    }

    private static String nettoyerNom(String nom) {
        String base = nom == null ? "" : Path.of(nom.replace('\\', '/')).getFileName().toString();
        String nettoye = base.replaceAll("[^\\p{L}\\p{N}._ -]", "_").trim();
        if (nettoye.isEmpty() || nettoye.startsWith(".")) {
            throw ApiException.invalide("Nom de fichier invalide.");
        }
        return nettoye.length() > 150 ? nettoye.substring(nettoye.length() - 150) : nettoye;
    }

    private static String extension(String nom) {
        int point = nom.lastIndexOf('.');
        return point < 0 ? "" : nom.substring(point + 1).toLowerCase(Locale.ROOT);
    }
}
