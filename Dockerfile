FROM eclipse-temurin:17-jdk-alpine

RUN apk add --no-cache nodejs npm maven bash

WORKDIR /app

COPY backend/pom.xml backend/pom.xml
RUN cd backend && mvn dependency:go-offline -q

COPY backend/src  backend/src
COPY backend/data backend/data

COPY frontend/package*.json frontend/
RUN cd frontend && npm install

COPY frontend/ frontend/

EXPOSE 8081 5173

CMD sh -c " \
  cd /app/backend && mvn spring-boot:run & \
  cd /app/frontend && npm run dev -- --host 0.0.0.0 \
"
