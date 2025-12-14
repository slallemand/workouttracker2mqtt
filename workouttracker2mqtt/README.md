# Workout Tracker to MQTT Add-on

This Home Assistant add-on bridges [workout-tracker](https://github.com/jovandeginste/workout-tracker) with MQTT, enabling seamless integration with Home Assistant.

## Features

- **Workout Tracker Integration**: Connects to workout-tracker's REST API to fetch workout data and statistics
- **MQTT Publishing**: Sends workout data to an MQTT broker for real-time monitoring
- **Home Assistant Autodiscovery**: Automatically creates Home Assistant MQTT autodiscovery topics for seamless integration
- **Configurable Workout Types**: Filter and monitor specific workout types (e.g., running, cycling)
- **Automatic Updates**: Periodically fetches the latest workouts and statistics from workout-tracker

## Configuration

### Required Options

- **workouttracker_api_server_url**: The base URL of your workout-tracker instance
  - Example: `http://workout-tracker:8080`
  - If workout-tracker is running as another add-on, use the add-on name as hostname
  - If running externally, use the full URL including protocol

- **workouttracker_api_key**: The API key for authenticating with workout-tracker
  - Must include the "Bearer " prefix
  - Example: `Bearer your-api-key-here`
  - Obtain this from your workout-tracker user settings

- **mqtt_broker_url**: The MQTT broker connection URL
  - For Home Assistant's built-in MQTT broker: `tcp://core-mosquitto:1883`
  - For external MQTT broker: `tcp://your-mqtt-broker:1883`

### Optional Options

- **mqtt_broker_username**: MQTT broker username (leave empty for Home Assistant's built-in broker)
- **mqtt_broker_password**: MQTT broker password (leave empty for Home Assistant's built-in broker)
- **workout_types**: Comma-separated list of workout types to monitor (default: `running,cycling`)

**Note:** MQTT topics are hardcoded to `workouttracker/workouts/<activity>` for workouts and `workouttracker/statistics/<activity>` for statistics, where `<activity>` is the workout type (e.g., `running`, `cycling`).
- **homeassistant_discovery_enabled**: Enable Home Assistant autodiscovery (default: `true`)
- **homeassistant_discovery_prefix**: Home Assistant discovery topic prefix (default: `homeassistant`)
- **homeassistant_discovery_node_id**: Node ID for Home Assistant discovery (default: `workouttracker`)
- **camel_route_timer_period**: Polling interval in milliseconds (default: `60000`)

## Home Assistant Integration

When `homeassistant_discovery_enabled` is set to `true` (default), the add-on automatically publishes Home Assistant MQTT autodiscovery configuration messages. This allows Home Assistant to automatically discover and configure sensors for:

- Latest workout distance per workout type
- Latest workout duration per workout type
- Latest workout name per workout type
- Latest workout date per workout type
- Latest workout average speed per workout type
- Statistics (total distance and workouts) per workout type

The sensors will appear in Home Assistant's MQTT integration automatically, requiring no manual configuration.

## How It Works

1. The add-on periodically polls the workout-tracker API (default: every 60 seconds)
2. It fetches the latest workouts and statistics from the `/api/v1/workouts` and `/api/v1/statistics` endpoints
3. Workout data is filtered by the configured workout types
4. The latest workout for each type is published to MQTT topics (e.g., `workouttracker/workouts/running`)
5. Statistics (aggregated by workout type) are published to statistics topics (e.g., `workouttracker/statistics/running`)
6. If enabled, Home Assistant autodiscovery messages are published on startup

## Installation

1. Add this repository to your Home Assistant add-on store
2. Go to **Supervisor** → **Add-on Store** → **Repositories**
3. Add the repository URL: `https://github.com/slallemand/workouttracker2mqtt`
4. Find **Workout Tracker to MQTT** in the add-on store
5. Click **Install**
6. Configure the required options
7. Start the add-on

## Support

For issues, feature requests, or questions, please visit the [GitHub repository](https://github.com/slallemand/workouttracker2mqtt).

