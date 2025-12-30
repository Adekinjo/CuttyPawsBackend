
# =========================
# 1) Build Stage
# =========================
FROM eclipse-temurin:21-jdk AS build

# Install Maven
RUN apt-get update && \
    apt-get install -y maven && \
    rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Copy Maven settings (Google mirror fix)
COPY .mvn /app/.mvn

# Copy project files
COPY pom.xml .
COPY src ./src

# Build application using settings.xml
RUN mvn -s /app/.mvn/settings.xml -DskipTests clean package

# =========================
# 2) Run Stage (Slim runtime)
# =========================
FROM eclipse-temurin:21-jre

WORKDIR /app

# Copy built jar from build stage
COPY --from=build /app/target/*.jar app.jar

# Expose backend port
EXPOSE 9393

# Run application
ENTRYPOINT ["java", "-jar", "app.jar"]
