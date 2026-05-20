FROM eclipse-temurin:25-jre

# Network isolation: run with --network=none when executing untrusted code
# Mount submission as read-only volume: -v /path/to/code:/app/submission:ro
# Mount output as writable volume:      -v /path/to/output:/app/output

WORKDIR /app

COPY submission/ ./submission/

RUN useradd -r -u 1001 -g root sandbox
USER sandbox

CMD ["java", "-jar", "submission/solution.jar"]
