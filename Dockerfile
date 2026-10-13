FROM maven:3.9-eclipse-temurin-21 AS api-build
WORKDIR /api
COPY api/pom.xml .
COPY api/src ./src
RUN mvn -q -DskipTests package

FROM node:20-alpine AS web-build
WORKDIR /web
COPY web/package.json web/package-lock.json ./
RUN npm ci
COPY web/ .
ENV API_PROXY=http://127.0.0.1:8040
ENV NEXT_TELEMETRY_DISABLED=1
RUN npm run build

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

RUN apt-get update \
  && apt-get install -y --no-install-recommends ca-certificates curl xz-utils \
  && curl -fsSL https://nodejs.org/dist/v20.18.1/node-v20.18.1-linux-x64.tar.xz \
    | tar -xJ -C /usr/local --strip-components=1 \
  && rm -rf /var/lib/apt/lists/*

COPY --from=api-build /api/target/contracts-api-0.1.0.jar /app/api.jar
COPY --from=web-build /web/.next/standalone /app/web
COPY --from=web-build /web/.next/static /app/web/.next/static

COPY start.sh /app/start.sh
RUN sed -i 's/\r$//' /app/start.sh && chmod +x /app/start.sh \
  && mkdir -p /data/documents

ENV DOCUMENT_DIR=/data/documents
ENV SERVER_PORT=8040
ENV PORT=3040
ENV HOSTNAME=0.0.0.0
ENV NEXT_TELEMETRY_DISABLED=1
ENV API_PROXY=http://127.0.0.1:8040

EXPOSE 3040
CMD ["/app/start.sh"]
