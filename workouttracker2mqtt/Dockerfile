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

# Copy application files from the app image
COPY --from=app /deployments /app

# Copy run script
COPY run.sh /etc/services.d/workouttracker2mqtt/run
RUN chmod a+x /etc/services.d/workouttracker2mqtt/run
