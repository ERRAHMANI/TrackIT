# Image de production TrackIT : le backend Spring Boot sert aussi le client web (même origine).

# --- Étape 1 : compilation du backend ---
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY backend/pom.xml .
RUN mvn -q -B dependency:go-offline
COPY backend/src ./src
RUN mvn -q -B -DskipTests package

# --- Étape 2 : exécution ---
FROM eclipse-temurin:17-jre
# Utilisateur système dédié, sans shell de connexion : l'application ne tourne jamais en root.
RUN groupadd --system trackit \
    && useradd --system --gid trackit --no-create-home --shell /usr/sbin/nologin trackit
WORKDIR /app
COPY --from=build /build/target/trackit-backend-1.0.0.jar app.jar
COPY frontend ./frontend
COPY backend/uploads/demo ./demo-uploads
COPY docker-entrypoint.sh ./docker-entrypoint.sh
# Code et client web en lecture seule pour l'application ; /data (pièces jointes, rapports) en écriture.
RUN chmod 755 docker-entrypoint.sh \
    && mkdir -p /data/uploads /data/reports \
    && chown -R trackit:trackit /data

# Fichiers déposés et rapports : à monter sur un volume persistant (/data).
ENV UPLOADS_DIR=/data/uploads \
    REPORTS_DIR=/data/reports \
    SPRING_WEB_RESOURCES_STATIC_LOCATIONS=file:/app/frontend/ \
    JAVA_OPTS="-XX:MaxRAMPercentage=75"

USER trackit
EXPOSE 8080
# Railway monte ses volumes en root : il faut y définir RAILWAY_RUN_UID=0. Le conteneur démarre alors en root,
# et docker-entrypoint.sh rend /data à l'utilisateur trackit puis lance Java sous cet utilisateur.
ENTRYPOINT ["./docker-entrypoint.sh"]