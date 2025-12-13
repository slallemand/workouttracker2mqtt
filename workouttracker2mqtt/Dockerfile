# BUILD_FROM is passed from the workflow and contains the correct base image for each architecture
# ARG must be defined before any FROM statement that uses it
ARG BUILD_FROM=ghcr.io/home-assistant/amd64-base:latest

# Stage 1: Get the application from the workouttracker2mqtt image
FROM ghcr.io/slallemand/workouttracker2mqtt:latest AS app

# Stage 2: Build the addon with Home Assistant base image
FROM ${BUILD_FROM}

# Install Java runtime and CA certificates (Home Assistant base is Alpine-based)
# Install java-cacerts separately without --no-scripts so its update script runs
# This script imports system CA certificates into Java's truststore
RUN apk add --no-cache openjdk21-jre-headless ca-certificates
# RUN apk add --no-cache --no-scripts openjdk21-jre-headless ca-certificates && \
    # apk add --no-cache java-cacerts && \
    # if [ -f /usr/lib/jvm/java-21-openjdk/bin/java ]; then \
    #     ln -sf /usr/lib/jvm/java-21-openjdk/bin/java /usr/bin/java; \
    # elif [ -f /usr/lib/jvm/default-jvm/bin/java ]; then \
    #     ln -sf /usr/lib/jvm/default-jvm/bin/java /usr/bin/java; \
    # else \
    #     JAVA_PATH=$(find /usr/lib/jvm -name "java" -type f -executable 2>/dev/null | head -1); \
    #     if [ -n "$JAVA_PATH" ]; then \
    #         ln -sf "$JAVA_PATH" /usr/bin/java; \
    #     fi; \
    # fi && \
    # java -version || echo "Java symlink created but verification failed"

# Copy application files from the app image
COPY --from=app /deployments /app

# Copy run script
COPY run.sh /etc/services.d/workouttracker2mqtt/run
RUN chmod a+x /etc/services.d/workouttracker2mqtt/run
