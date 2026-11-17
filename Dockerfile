# Building the React application.
FROM node:22-alpine AS client-build

WORKDIR /app/client

# Install npm packages (Clean install).
COPY client/package.json ./
RUN npm ci

COPY client/ ./
RUN npm run build
