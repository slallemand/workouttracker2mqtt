# workouttracker2mqtt

A Quarkus-based application that connects to [workout-tracker](https://github.com/jovandeginste/workout-tracker), fetches workout data, and publishes it to an MQTT broker. This enables integration with Home Assistant and other MQTT-compatible systems.

## Features

- **Workout Tracker Integration**: Connects to workout-tracker's REST API to fetch workout data and statistics
- **MQTT Publishing**: Sends workout data to an MQTT broker for real-time monitoring
- **Home Assistant Integration**: Automatically creates Home Assistant MQTT autodiscovery topics for seamless integration
- **Configurable Workout Types**: Filter and monitor specific workout types (e.g., running, cycling)
- **Automatic Updates**: Periodically fetches the latest workouts and totals from workout-tracker

## Disclaimer

This code was primarily developed as an experiment with vibe-coding (AI-assisted development). While it works fine for its intended purpose, the implementation may not be optimal. The code structure, error handling, and performance characteristics reflect this experimental approach. Use at your own discretion and feel free to improve it!


## Quick Start with Podman / Docker

The application can be run using a container image. You need to provide the following mandatory environment variables:

### Mandatory Environment Variables

- `WORKOUTTRACKER_API_SERVER_URL`: The base URL of your workout-tracker instance (e.g., `http://workout-tracker:8080`)
- `WORKOUTTRACKER_API_KEY`: The API key for authenticating with workout-tracker (obtain this from your workout-tracker user settings)
- `MQTT_BROKER_URL`: The MQTT broker connection URL (e.g., `tcp://mqtt-broker:1883`)

### Optional Environment Variables

- `MQTT_BROKER_USERNAME`: MQTT broker username (if authentication is required)
- `MQTT_BROKER_PASSWORD`: MQTT broker password (if authentication is required)
- `MQTT_BROKER_TOPIC_WORKOUTS`: MQTT topic for workouts (default: `workouttracker/workouts`)
- `MQTT_BROKER_TOPIC_TOTALS`: MQTT topic for totals (default: `workouttracker/totals`)
- `WORKOUT_TYPES`: Comma-separated list of workout types to monitor (default: `running,cycling`)
- `HOMEASSISTANT_DISCOVERY_ENABLED`: Enable Home Assistant autodiscovery (default: `true`)
- `HOMEASSISTANT_DISCOVERY_PREFIX`: Home Assistant discovery topic prefix (default: `homeassistant`)
- `HOMEASSISTANT_DISCOVERY_NODE_ID`: Node ID for Home Assistant discovery (default: `workouttracker`)
- `CAMEL_ROUTE_TIMER_PERIOD`: Polling interval in milliseconds (default: `60000`)

### Example Podman Run Command

```bash
podman run -d \
  -e WORKOUTTRACKER_API_SERVER_URL=http://workout-tracker:8080 \
  -e WORKOUTTRACKER_API_KEY="Bearer your-api-key-here" \
  -e MQTT_BROKER_URL=tcp://mqtt-broker:1883 \
  -e MQTT_BROKER_USERNAME=mqtt_user \
  -e MQTT_BROKER_PASSWORD=mqtt_password \
  ghcr.io/slallemand/workouttracker2mqtt:latest
```

## Installation as Home Assistant Add-on

This application is available as a Home Assistant add-on for easy installation and management.

### Installation Steps

1. In Home Assistant, go to **Supervisor** → **Add-on Store**
2. Click the three-dot menu (⋮) in the top right corner
3. Select **Repositories**
4. Add the repository URL: `https://github.com/slallemand/workouttracker2mqtt`
5. Click **Add** and then **Close**
6. Find **Workout Tracker to MQTT** in the add-on store
7. Click **Install**
8. Configure the required options:
   - **workouttracker_api_server_url**: Base URL of your workout-tracker instance (e.g., `http://workout-tracker:8080`)
   - **workouttracker_api_key**: API key with "Bearer " prefix (e.g., `Bearer your-api-key-here`)
   - **mqtt_broker_url**: MQTT broker URL (use `tcp://core-mosquitto:1883` for Home Assistant's built-in broker)
9. Click **Start** to launch the add-on

### Configuration

The add-on supports all the same configuration options as the Docker/Podman version. See the [addon README](workouttracker2mqtt/README.md) for detailed configuration options.

## Home Assistant Integration

When `HOMEASSISTANT_DISCOVERY_ENABLED` is set to `true` (default), the application automatically publishes Home Assistant MQTT autodiscovery configuration messages. This allows Home Assistant to automatically discover and configure sensors for:

- Latest workout distance per workout type
- Latest workout duration per workout type
- Latest workout name per workout type
- Total distance across all workouts
- Total duration across all workouts

The sensors will appear in Home Assistant's MQTT integration automatically, requiring no manual configuration.

## How It Works

1. The application periodically polls the workout-tracker API (default: every 60 seconds)
2. It fetches the latest workouts and totals from the `/api/v1/workouts` and `/api/v1/totals` endpoints
3. Workout data is filtered by the configured workout types
4. The latest workout for each type is published to MQTT topics (e.g., `workouttracker/workouts/running`)
5. Total statistics are published to the totals topic
6. If enabled, Home Assistant autodiscovery messages are published on startup

## Development

This project uses Quarkus, the Supersonic Subatomic Java Framework.

If you want to learn more about Quarkus, please visit its website: <https://quarkus.io/>.

### Running the application in dev mode

You can run your application in dev mode that enables live coding using:

```shell script
./mvnw quarkus:dev
```

> **_NOTE:_**  Quarkus now ships with a Dev UI, which is available in dev mode only at <http://localhost:8080/q/dev/>.

### Packaging and running the application

The application can be packaged using:

```shell script
./mvnw package
```

It produces the `quarkus-run.jar` file in the `target/quarkus-app/` directory.
Be aware that it's not an _über-jar_ as the dependencies are copied into the `target/quarkus-app/lib/` directory.

The application is now runnable using `java -jar target/quarkus-app/quarkus-run.jar`.

If you want to build an _über-jar_, execute the following command:

```shell script
./mvnw package -Dquarkus.package.jar.type=uber-jar
```

The application, packaged as an _über-jar_, is now runnable using `java -jar target/*-runner.jar`.

### Creating a native executable

You can create a native executable using:

```shell script
./mvnw package -Dnative
```

Or, if you don't have GraalVM installed, you can run the native executable build in a container using:

```shell script
./mvnw package -Dnative -Dquarkus.native.container-build=true
```

You can then execute your native executable with: `./target/workout2mqtt-1.0.0-SNAPSHOT-runner`

If you want to learn more about building native executables, please consult <https://quarkus.io/guides/maven-tooling>.
