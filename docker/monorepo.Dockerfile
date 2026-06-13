# docker/monorepo.Dockerfile

FROM gradle:9.5.1-jdk25-jammy AS builder

WORKDIR /app

COPY . .

RUN --mount=type=cache,id=gradle-cache,target=/home/gradle/.gradle \
    ./gradlew \
    :server:app:installDist \
    :sandbox-runner:app:installDist \
    --parallel \
    --build-cache \
    --configuration-cache \
    --no-daemon


FROM eclipse-temurin:25-jre-jammy AS server

RUN addgroup --system appgroup && adduser --system appuser --ingroup appgroup

USER appuser
WORKDIR /app

COPY --from=builder /app/server/app/build/install/app/ .

EXPOSE 8080

CMD ["bin/app"]


FROM eclipse-temurin:25-jre-jammy AS sandbox-runner

RUN addgroup --system appgroup && adduser --system appuser --ingroup appgroup

USER appuser
WORKDIR /app

COPY --from=builder /app/sandbox-runner/app/build/install/app/ .

EXPOSE 8081

CMD ["bin/app"]
