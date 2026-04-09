FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copy pom first for dependency caching
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .
RUN chmod +x mvnw
RUN ./mvnw -q -DskipTests dependency:go-offline

# Copy source and build
COPY src src
RUN ./mvnw -DskipTests clean package

# ---------- Run stage ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

# Copy built jar (adjust if your jar name differs)
COPY --from=build /app/target/*.jar app.jar

# Render sets PORT automatically; Spring reads SERVER_PORT if you use it
ENV SERVER_PORT=8080

EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]