FROM gradle:9.5.1-jdk21-jammy AS builder

WORKDIR /app

COPY build.gradle settings.gradle gradlew ./
COPY gradle ./gradle
RUN ./gradlew dependencies --no-daemon

COPY . .
RUN ./gradlew :sandbox-runner:app:installDist --no-daemon

FROM eclipse-temurin:21-jre-jammy

RUN addgroup --system appgroup && adduser --system appuser --ingroup appgroup
USER appuser

WORKDIR /app
COPY --from=builder /app/sandbox-runner/app/build/install/app/ .

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s \
  CMD /app/bin/app --health || exit 1

CMD ["bin/app"]
