# Stage 1: build
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /app

# Cache de dependencias: se re-descarga solo si cambia pom.xml
COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests -B

# Stage 2: runtime
FROM eclipse-temurin:17-jre-jammy AS runtime

# Usuario no-root para reducir superficie de ataque
RUN groupadd --system appuser && useradd --system --gid appuser appuser

WORKDIR /app

COPY --from=build /app/target/consultorio-backend-0.0.1-SNAPSHOT.jar app.jar

RUN chown appuser:appuser app.jar

USER appuser

EXPOSE 8080

ENV SPRING_PROFILES_ACTIVE=prod
ENV JAVA_TOOL_OPTIONS="-Dfile.encoding=UTF-8"

ENTRYPOINT ["java", "-jar", "app.jar"]
