#!/usr/bin/with-contenv bashio

# Export Home Assistant addon options as environment variables
export WORKOUTTRACKER_API_SERVER_URL=$(bashio::config 'workouttracker_api_server_url')
export WORKOUTTRACKER_API_KEY=$(bashio::config 'workouttracker_api_key')
export MQTT_BROKER_URL=$(bashio::config 'mqtt_broker_url')

# Optional MQTT broker credentials
# Only export if non-empty values are provided (defaults are empty strings)
MQTT_USERNAME=$(bashio::config 'mqtt_broker_username' '')
if [ -n "$MQTT_USERNAME" ]; then
    export MQTT_BROKER_USERNAME="$MQTT_USERNAME"
fi

MQTT_PASSWORD=$(bashio::config 'mqtt_broker_password' '')
if [ -n "$MQTT_PASSWORD" ]; then
    export MQTT_BROKER_PASSWORD="$MQTT_PASSWORD"
fi

# Optional MQTT topic configuration
# Only export if user explicitly set a value (allows Java default to work if not set)
if bashio::config.has_value 'mqtt_broker_topic_workouts'; then
    export MQTT_BROKER_TOPIC_WORKOUTS=$(bashio::config 'mqtt_broker_topic_workouts')
fi

# # Optional MQTT client instance ID (for multi-instance support)
# # Only export if a non-empty value is provided (default is empty string)
# INSTANCE_ID=$(bashio::config 'mqtt_broker_client_instance_id' '')
# if [ -n "$INSTANCE_ID" ]; then
#     export MQTT_BROKER_CLIENT_INSTANCE_ID="$INSTANCE_ID"
# fi

# Optional workout types
# Only export if user explicitly set a value (allows Java default to work if not set)
if bashio::config.has_value 'workout_types'; then
    export WORKOUT_TYPES=$(bashio::config 'workout_types')
fi

# Optional Home Assistant discovery configuration
# Only export if user explicitly set values (allows Java defaults to work if not set)
if bashio::config.has_value 'homeassistant_discovery_enabled'; then
    export HOMEASSISTANT_DISCOVERY_ENABLED=$(bashio::config 'homeassistant_discovery_enabled')
fi

if bashio::config.has_value 'homeassistant_discovery_prefix'; then
    export HOMEASSISTANT_DISCOVERY_PREFIX=$(bashio::config 'homeassistant_discovery_prefix')
fi

if bashio::config.has_value 'homeassistant_discovery_node_id'; then
    export HOMEASSISTANT_DISCOVERY_NODE_ID=$(bashio::config 'homeassistant_discovery_node_id')
fi

# Optional timer configuration
# Only export if user explicitly set a value (allows Java default to work if not set)
if bashio::config.has_value 'camel_route_timer_period'; then
    export CAMEL_ROUTE_TIMER_PERIOD=$(bashio::config 'camel_route_timer_period')
fi

# Start the application
cd /app
exec java -jar quarkus-run.jar
