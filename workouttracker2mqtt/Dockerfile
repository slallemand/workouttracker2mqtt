ARG BUILD_FROM=ghcr.io/home-assistant/amd64-base:latest
FROM ${BUILD_FROM}

# Copy the application from the workouttracker2mqtt image
FROM ghcr.io/slallemand/workouttracker2mqtt:latest AS app

ARG BUILD_FROM=ghcr.io/home-assistant/amd64-base:latest
FROM ${BUILD_FROM}

# Install Java runtime (Home Assistant base is Alpine-based)
RUN apk add --no-cache openjdk21-jre-headless

# Copy application files from the app image
COPY --from=app /deployments /app

# Copy run script
COPY run.sh /etc/services.d/workouttracker2mqtt/run
RUN chmod a+x /etc/services.d/workouttracker2mqtt/run

