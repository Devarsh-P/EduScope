# ─────────────────────────────────────────────────────────────────────────────
# EduScope – Single image: Spring Boot (8081) + Vite dev server (5173)
# Base: Alpine → ~3–4× smaller than ubuntu:22.04
# ─────────────────────────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jdk-alpine

# Install Node.js, npm, and Maven (all tiny on Alpine)
RUN apk add --no-cache nodejs npm maven bash

WORKDIR /app

# ── Backend: cache Maven dependencies before copying source ──────────────────
COPY backend/pom.xml backend/pom.xml
RUN cd backend && mvn dependency:go-offline -q

COPY backend/src  backend/src
COPY backend/data backend/data

# ── Frontend: cache node_modules before copying source ───────────────────────
COPY frontend/package*.json frontend/
RUN cd frontend && npm install

COPY frontend/ frontend/

# ─────────────────────────────────────────────────────────────────────────────
EXPOSE 8081 5173

# --host makes Vite listen on 0.0.0.0 so the port is reachable from the host
CMD sh -c " \
  cd /app/backend && mvn spring-boot:run & \
  cd /app/frontend && npm run dev -- --host 0.0.0.0 \
"