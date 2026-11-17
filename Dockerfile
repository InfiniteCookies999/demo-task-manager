# Building the React application.
# -------------------------------
FROM node:22-alpine AS client-build

WORKDIR /app/client

# Install npm packages (Clean install).
COPY client/package.json ./
COPY client/package-lock.json ./
RUN npm ci

COPY client/ ./
RUN npm run build

# Building the Spring Boot backend.
# -------------------------------
FROM eclipse-temurin:25-jdk AS server-build

WORKDIR /app/server

COPY server/ ./

# Include React build in Spring Boot static resources.
COPY --from=client-build /app/client/dist ./src/main/resources/static

RUN chmod +x gradlew && ./gradlew clean bootJar --no-daemon

# Production runtime
# -------------------------------
FROM eclipse-temurin:25-jre-alpine AS production

WORKDIR /app

COPY --from=server-build /app/server/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]