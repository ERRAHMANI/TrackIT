package com.trackit.exception;

import com.trackit.dto.ErreurResponse;
import com.trackit.security.JournalSecurite;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final JournalSecurite journal;

    public GlobalExceptionHandler(JournalSecurite journal) {
        this.journal = journal;
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErreurResponse> handleApi(ApiException ex, HttpServletRequest request) {
        if (ex.getStatus() == HttpStatus.FORBIDDEN) {
            journal.accesRefuse(request);
        }
        return reponse(ex.getStatus(), ex.getMessage(), request, null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErreurResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> champs = new LinkedHashMap<>();
        for (FieldError erreur : ex.getBindingResult().getFieldErrors()) {
            champs.putIfAbsent(erreur.getField(), erreur.getDefaultMessage());
        }
        ex.getBindingResult().getGlobalErrors()
                .forEach(erreur -> champs.putIfAbsent(erreur.getObjectName(), erreur.getDefaultMessage()));
        String message = champs.size() == 1
                ? "1 champ est invalide."
                : champs.size() + " champs sont invalides.";
        return reponse(HttpStatus.BAD_REQUEST, message, request, champs);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErreurResponse> handleConstraint(ConstraintViolationException ex, HttpServletRequest request) {
        Map<String, String> champs = new LinkedHashMap<>();
        ex.getConstraintViolations().forEach(v -> {
            String chemin = v.getPropertyPath().toString();
            champs.putIfAbsent(chemin.substring(chemin.lastIndexOf('.') + 1), v.getMessage());
        });
        return reponse(HttpStatus.BAD_REQUEST, "Paramètres invalides.", request, champs);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class, MissingServletRequestPartException.class})
    public ResponseEntity<ErreurResponse> handleRequeteIllisible(Exception ex, HttpServletRequest request) {
        String message = ex instanceof MethodArgumentTypeMismatchException mismatch
                ? "Valeur invalide pour le paramètre « " + mismatch.getName() + " »."
                : ex instanceof MissingServletRequestParameterException missing
                ? "Paramètre obligatoire manquant : " + missing.getParameterName() + "."
                : ex instanceof MissingServletRequestPartException part
                ? "Élément obligatoire manquant : " + part.getRequestPartName() + "."
                : "Corps de requête invalide ou mal formé.";
        return reponse(HttpStatus.BAD_REQUEST, message, request, null);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErreurResponse> handleBadCredentials(HttpServletRequest request) {
        return reponse(HttpStatus.UNAUTHORIZED,
                "Identifiants incorrects. Vérifiez votre adresse e-mail et votre mot de passe.", request, null);
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ErreurResponse> handleDisabled(HttpServletRequest request) {
        return reponse(HttpStatus.UNAUTHORIZED,
                "Ce compte est désactivé. Contactez l'administrateur de votre organisation.", request, null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErreurResponse> handleAccessDenied(HttpServletRequest request) {
        // Refus venant de @PreAuthorize ou d'une règle d'organisation : journalisé comme tentative d'accès.
        journal.accesRefuse(request);
        return reponse(HttpStatus.FORBIDDEN, "Votre rôle ne permet pas d'effectuer cette action.", request, null);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErreurResponse> handleTaille(HttpServletRequest request) {
        return reponse(HttpStatus.PAYLOAD_TOO_LARGE, "Le fichier dépasse la taille maximale autorisée (10 Mo).", request, null);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErreurResponse> handleIntegrite(DataIntegrityViolationException ex, HttpServletRequest request) {
        LOG.warn("Violation d'intégrité : {}", ex.getMostSpecificCause().getMessage());
        return reponse(HttpStatus.CONFLICT,
                "L'opération viole une contrainte d'intégrité (valeur déjà utilisée ou donnée liée).", request, null);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErreurResponse> handleMethode(HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        return reponse(HttpStatus.METHOD_NOT_ALLOWED, "Méthode HTTP non supportée : " + ex.getMethod() + ".", request, null);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErreurResponse> handleNoResource(HttpServletRequest request) {
        return reponse(HttpStatus.NOT_FOUND, "Ressource introuvable.", request, null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErreurResponse> handleInattendue(Exception ex, HttpServletRequest request) {
        LOG.error("Erreur inattendue sur {}", request.getRequestURI(), ex);
        return reponse(HttpStatus.INTERNAL_SERVER_ERROR,
                "Une erreur interne est survenue. Réessayez ou contactez l'administrateur.", request, null);
    }

    private ResponseEntity<ErreurResponse> reponse(HttpStatus status, String message, HttpServletRequest request,
                                                   Map<String, String> champs) {
        ErreurResponse corps = new ErreurResponse(LocalDateTime.now(), status.value(), status.getReasonPhrase(),
                message, request.getRequestURI(), champs);
        return ResponseEntity.status(status).body(corps);
    }
}
