# Build the jar first (on your machine or in CI):  .\mvnw.cmd clean package
# Then:  docker build -t trade-service:0.0.1 .

# ---- Stage 1: split the fat jar into layers (a throw-away stage; nothing from it ships except what we COPY) ----
FROM eclipse-temurin:21-jre-jammy AS layers
WORKDIR /build
COPY target/trade-service-0.0.1-SNAPSHOT.jar app.jar
# Spring Boot's own tool: dependencies, snapshot dependencies and our application code become separate folders.
RUN java -Djarmode=tools -jar app.jar extract --layers --destination extracted

# ---- Stage 2: the image that runs ----
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
# Least-changing layer first. Docker caches each COPY: when only our code changes, the big "dependencies"
# layer is reused and a rebuild and a push move a few KB instead of 89 MB.
COPY --from=layers /build/extracted/dependencies/ ./
COPY --from=layers /build/extracted/snapshot-dependencies/ ./
COPY --from=layers /build/extracted/application/ ./
# Never run as root. A NUMERIC user id lets Kubernetes verify "runAsNonRoot" without looking inside the image.
USER 10001
EXPOSE 8080
# Exec form (a JSON list): java is process 1 and receives SIGTERM directly, so Spring shuts down gracefully.
# MaxRAMPercentage: size the heap from the CONTAINER's memory limit (75%), not from the host's RAM.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-XX:+ExitOnOutOfMemoryError", "-jar", "app.jar"]
