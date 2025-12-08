ARG BUILD_FROM
FROM ${BUILD_FROM}

# Copy the application from the workouttracker2mqtt image
FROM ghcr.io/slallemand/workouttracker2mqtt:latest AS app

FROM ${BUILD_FROM}

# Install Java runtime
RUN \
    apk add --no-cache \
        openjdk21-jre-headless \
    || \
    apt-get update && \
    apt-get install -y --no-install-recommends \
        openjdk-21-jre-headless && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Copy application files from the app image
COPY --from=app /deployments /app

# Copy run script
COPY run.sh /etc/services.d/workouttracker2mqtt/run
RUN chmod a+x /etc/services.d/workouttracker2mqtt/run

