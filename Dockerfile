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
WORKDIR /app
COPY --from=build /build/target/trackit-backend-1.0.0.jar app.jar
COPY frontend ./frontend
COPY backend/uploads/demo ./demo-uploads
COPY docker-entrypoint.sh ./docker-entrypoint.sh
RUN chmod +x docker-entrypoint.sh

# Fichiers déposés et rapports : à monter sur un volume persistant (/data).
ENV UPLOADS_DIR=/data/uploads \
    REPORTS_DIR=/data/reports \
    SPRING_WEB_RESOURCES_STATIC_LOCATIONS=file:/app/frontend/ \
    JAVA_OPTS="-XX:MaxRAMPercentage=75"

EXPOSE 8080
ENTRYPOINT ["./docker-entrypoint.sh"]