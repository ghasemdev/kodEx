FROM gradle:9.5.1-jdk25 AS builder

WORKDIR /app
COPY . .
RUN ./gradlew :sandbox-runner:app:installDist --no-daemon

FROM eclipse-temurin:25-jre

WORKDIR /app
COPY --from=builder /app/sandbox-runner/app/build/install/app/ .

EXPOSE 8081

HEALTHCHECK --interval=10s --timeout=5s --retries=3 \
    CMD curl -f http://localhost:8081/health || exit 1

CMD ["bin/app"]
