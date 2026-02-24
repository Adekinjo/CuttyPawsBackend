#
## =========================
## 1) Build Stage
## =========================
#FROM eclipse-temurin:21-jdk AS build
#
## Install Maven
#RUN apt-get update && \
#    apt-get install -y maven && \
#    rm -rf /var/lib/apt/lists/*
#
#WORKDIR /app
#
## Copy Maven settings (Google mirror fix)
#COPY .mvn /app/.mvn
#
## Copy project files
#COPY pom.xml .
#COPY src ./src
#
## Build application using settings.xml
#RUN mvn -s /app/.mvn/settings.xml -DskipTests clean package
#
## =========================
## 2) Run Stage (Slim runtime)
## =========================
#FROM eclipse-temurin:21-jre
#
#WORKDIR /app
#
## Copy built jar from build stage
#COPY --from=build /app/target/*.jar app.jar
#
## Expose backend port
#EXPOSE 9393
#
## Run application
#ENTRYPOINT ["java", "-jar", "app.jar"]






# ---------- Build stage ----------
FROM maven:3.9-eclipse-temurin-17 AS build
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
FROM eclipse-temurin:17-jre
WORKDIR /app

# Copy built jar (adjust if your jar name differs)
COPY --from=build /app/target/*.jar app.jar

# Render sets PORT automatically; Spring reads SERVER_PORT if you use it
ENV SERVER_PORT=8080

EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]