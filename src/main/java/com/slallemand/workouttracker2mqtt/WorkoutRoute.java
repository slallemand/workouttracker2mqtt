package com.slallemand.workouttracker2mqtt;

import jakarta.enterprise.context.ApplicationScoped;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Camel route that fetches data from a REST API with API key authentication
 * and sends it to an MQTT broker
 */
@ApplicationScoped
public class WorkoutRoute extends RouteBuilder {

    @ConfigProperty(name = "workouttracker.api.server.url")
    String restApiServerUrl;

    @ConfigProperty(name = "workouttracker.api.endpoint.workouts")
    String restApiEndpoint;

    @ConfigProperty(name = "workouttracker.api.endpoint.totals")
    String restApiTotalsEndpoint;

    @ConfigProperty(name = "workouttracker.api.endpoint.statistics")
    String restApiStatisticsEndpoint;

    @ConfigProperty(name = "workouttracker.api.key")
    String restApiKey;

    @ConfigProperty(name = "workouttracker.api.key.header.name", defaultValue = "Authorization")
    String apiKeyHeaderName;

    @ConfigProperty(name = "mqtt.broker.url")
    String mqttBrokerUrl;

    @ConfigProperty(name = "mqtt.broker.client.id")
    String mqttClientId;

    @ConfigProperty(name = "mqtt.broker.topic.workouts")
    String mqttTopic;

    @ConfigProperty(name = "mqtt.broker.topic.totals")
    String mqttTotalsTopic;

    @ConfigProperty(name = "mqtt.broker.topic.statistics")
    String mqttStatisticsTopic;

    @ConfigProperty(name = "mqtt.broker.qos", defaultValue = "1")
    int mqttQos;

    @ConfigProperty(name = "mqtt.broker.retained", defaultValue = "false")
    boolean mqttRetained;

    @ConfigProperty(name = "mqtt.broker.username")
    String mqttBrokerUsername;

    @ConfigProperty(name = "mqtt.broker.password")
    String mqttBrokerPassword;

    @ConfigProperty(name = "camel.route.timer.period", defaultValue = "60000")
    long timerPeriod;

    @ConfigProperty(name = "camel.route.timer.delay", defaultValue = "1000")
    long timerDelay;

    @ConfigProperty(name = "homeassistant.discovery.enabled", defaultValue = "true")
    boolean haDiscoveryEnabled;

    @ConfigProperty(name = "homeassistant.discovery.prefix", defaultValue = "homeassistant")
    String haDiscoveryPrefix;

    @ConfigProperty(name = "homeassistant.discovery.node.id", defaultValue = "workouttracker")
    String haDiscoveryNodeId;

    @ConfigProperty(name = "workout.types", defaultValue = "running,cycling")
    String workoutTypes;

    /**
     * Capitalizes the first letter of a string
     */
    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }

    /**
     * Publishes a message to MQTT with retry logic. Retries until successful.
     * 
     * @param topic MQTT topic to publish to
     * @param message Message body to publish
     * @param description Description of what is being published (for logging)
     * @param maxRetryDelay Maximum delay between retries in milliseconds (default: 30000)
     * @param initialRetryDelay Initial delay between retries in milliseconds (default: 1000)
     */
    private void publishToMqttWithRetry(String topic, String message, String description, long maxRetryDelay, long initialRetryDelay) {
        String mqttEndpoint = "paho:" + topic + 
            "?brokerUrl=" + mqttBrokerUrl + 
            "&clientId=" + mqttClientId + 
            "&qos=" + mqttQos + 
            "&retained=" + mqttRetained + 
            "&userName=" + mqttBrokerUsername + 
            "&password=" + mqttBrokerPassword + 
            "&lazyStartProducer=true";
        
        long retryDelay = initialRetryDelay;
        int attemptCount = 0;
        boolean success = false;
        
        while (!success) {
            attemptCount++;
            try {
                getContext().createProducerTemplate().sendBody(mqttEndpoint, message);
                if (attemptCount > 1) {
                    log.info("MQTT broker is now available. Successfully published " + description + " to MQTT after " + attemptCount + " attempts");
                } else {
                    log.debug("Published " + description + " to MQTT topic: " + topic);
                }
                success = true;
            } catch (Exception e) {
                log.warn("MQTT broker unavailable for " + description + " (attempt " + attemptCount + "): " + e.getMessage() + 
                    ". Waiting for broker to become available, retrying in " + retryDelay + "ms...");
                
                // Wait before retrying
                try {
                    Thread.sleep(retryDelay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    log.error("Interrupted while waiting to retry MQTT connection");
                    throw new RuntimeException("MQTT retry interrupted", ie);
                }
                
                // Exponential backoff with maximum delay
                retryDelay = Math.min(retryDelay * 2, maxRetryDelay);
            }
        }
    }

    /**
     * Publishes Home Assistant MQTT discovery configuration for a sensor
     * Uses retry logic to ensure the message is published even if MQTT broker is temporarily unavailable
     */
    private void publishHomeAssistantDiscovery(org.apache.camel.Exchange exchange, String sensorId, String sensorName, String unit, 
                                                String valueTemplate, String stateTopic, String deviceClass) {
        if (!haDiscoveryEnabled) {
            return;
        }
        
        try {
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode config = mapper.createObjectNode();
            
            // Basic sensor configuration
            config.put("name", sensorName);
            config.put("state_topic", stateTopic);
            config.put("unique_id", haDiscoveryNodeId + "_" + sensorId);
            if (deviceClass != null && !deviceClass.isEmpty()) {
                config.put("device_class", deviceClass);
            }
            if (unit != null && !unit.isEmpty()) {
                config.put("unit_of_measurement", unit);
            }
            if (valueTemplate != null && !valueTemplate.isEmpty()) {
                config.put("value_template", valueTemplate);
            }
            
            // Device information
            ObjectNode device = mapper.createObjectNode();
            device.putArray("identifiers").add(haDiscoveryNodeId);
            device.put("name", "Workout Tracker");
            device.put("manufacturer", "Workout2MQTT");
            device.put("model", "Workout Tracker Integration");
            config.set("device", device);
            
            // Publish discovery message
            String discoveryTopic = haDiscoveryPrefix + "/sensor/" + haDiscoveryNodeId + "/" + sensorId + "/config";
            String configJson = mapper.writeValueAsString(config);
            
            String mqttEndpoint = "paho:" + discoveryTopic + 
                "?brokerUrl=" + mqttBrokerUrl + 
                "&clientId=" + mqttClientId + "-discovery" + 
                "&qos=0" + 
                "&retained=true" + 
                "&userName=" + mqttBrokerUsername + 
                "&password=" + mqttBrokerPassword + 
                "&lazyStartProducer=true";
            
            // Use retry logic to ensure discovery messages are published
            long retryDelay = 1000; // 1 second
            long maxRetryDelay = 30000; // 30 seconds
            int attemptCount = 0;
            boolean success = false;
            
            while (!success) {
                attemptCount++;
                try {
                    exchange.getContext().createProducerTemplate().sendBody(mqttEndpoint, configJson);
                    if (attemptCount > 1) {
                        log.info("Successfully published Home Assistant discovery for " + sensorName + " after " + attemptCount + " attempts");
                    } else {
                        log.debug("Published Home Assistant discovery for: " + sensorName + " (topic: " + discoveryTopic + ")");
                    }
                    success = true;
                } catch (Exception e) {
                    log.warn("MQTT broker unavailable for Home Assistant discovery (" + sensorName + ") (attempt " + attemptCount + "): " + e.getMessage() + 
                        ". Retrying in " + retryDelay + "ms...");
                    
                    // Wait before retrying
                    try {
                        Thread.sleep(retryDelay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.error("Interrupted while waiting to retry Home Assistant discovery");
                        throw new RuntimeException("Home Assistant discovery retry interrupted", ie);
                    }
                    
                    // Exponential backoff with maximum delay
                    retryDelay = Math.min(retryDelay * 2, maxRetryDelay);
                }
            }
        } catch (Exception e) {
            log.error("Failed to publish Home Assistant discovery for " + sensorName + ": " + e.getMessage(), e);
        }
    }

    @Override
    public void configure() throws Exception {
        // Global exception handler for errors (must be defined before any routes)
        onException(Exception.class)
            .handled(true)
            .log("Error in route: ${exception.message}")
            .process(exchange -> {
                // If it's a totals route error, write to stdout
                String routeId = exchange.getFromRouteId();
                if (routeId != null && routeId.contains("totals")) {
                    String body = exchange.getIn().getBody(String.class);
                    if (body != null) {
                        System.out.println("TOTALS (MQTT failed): " + body);
                    }
                }
            })
            .end();

        // Build the workouts list API URL
        String workoutsListUrl = restApiServerUrl + restApiEndpoint;
        
        // Parse workout types from configuration
        Set<String> selectedTypes = Arrays.stream(workoutTypes.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .collect(Collectors.toSet());
        
        // Route to publish Home Assistant discovery messages on startup (runs once after context is ready)
        if (haDiscoveryEnabled) {
            from("timer:ha-discovery?repeatCount=1&delay=5000")
                .log("Publishing Home Assistant MQTT discovery configurations...")
                .process(exchange -> {
                    // Discovery for each workout type
                    for (String workoutType : selectedTypes) {
                        String typeTopic = mqttTopic + "/" + workoutType;
                        String typeId = workoutType.toLowerCase().replaceAll("[^a-z0-9]", "_");
                        
                        publishHomeAssistantDiscovery(
                            exchange,
                            typeId + "_distance",
                            "Latest " + capitalize(workoutType) + " Distance",
                            "km",
                            "{{ value_json.data.totalDistance | default(0) / 1000 }}",
                            typeTopic,
                            "distance"
                        );
                        
                        publishHomeAssistantDiscovery(
                            exchange,
                            typeId + "_duration",
                            "Latest " + capitalize(workoutType) + " Duration",
                            "min",
                            "{{ value_json.data.totalDuration | default(0) / 1000000000 / 60 }}",
                            typeTopic,
                            "duration"
                        );
                        
                        publishHomeAssistantDiscovery(
                            exchange,
                            typeId + "_name",
                            "Latest " + capitalize(workoutType) + " Name",
                            "",
                            "{{ value_json.name | default('Unknown') }}",
                            typeTopic,
                            null
                        );
                        
                        publishHomeAssistantDiscovery(
                            exchange,
                            typeId + "_date",
                            "Latest " + capitalize(workoutType) + " Date",
                            "",
                            "{{ value_json.date | default('') }}",
                            typeTopic,
                            "timestamp"
                        );
                        
                        publishHomeAssistantDiscovery(
                            exchange,
                            typeId + "_average_speed",
                            "Latest " + capitalize(workoutType) + " Average Speed",
                            "km/h",
                            "{{ value_json.data.averageSpeed | default(0) * 3.6 }}",
                            typeTopic,
                            "speed"
                        );
                    }
                    
                    // Discovery for totals data
                    publishHomeAssistantDiscovery(
                        exchange,
                        "total_workouts",
                        "Total Workouts",
                        "",
                        "{{ value_json.results.workouts | default(0) }}",
                        mqttTotalsTopic,
                        "measurement"
                    );
                    
                    publishHomeAssistantDiscovery(
                        exchange,
                        "total_distance",
                        "Total Distance",
                        "km",
                        "{{ value_json.results.distance | default(0) / 1000 }}",
                        mqttTotalsTopic,
                        "distance"
                    );
                    
                    publishHomeAssistantDiscovery(
                        exchange,
                        "total_duration",
                        "Total Duration",
                        "d",
                        "{{ value_json.results.duration | default(0) / 1000000000 / 3600 / 24 }}",
                        mqttTotalsTopic,
                        "duration"
                    );
                    
                    // Discovery for statistics data (total distance and workouts by type)
                    for (String workoutType : selectedTypes) {
                        String typeId = workoutType.toLowerCase().replaceAll("[^a-z0-9]", "_");
                        String statisticsTopic = mqttStatisticsTopic + "/" + workoutType.toLowerCase();
                        
                        publishHomeAssistantDiscovery(
                            exchange,
                            "statistics_" + typeId + "_total_distance",
                            capitalize(workoutType) + " Total Distance",
                            "km",
                            "{{ value_json.totalDistance | default(0) / 1000 }}",
                            statisticsTopic,
                            "distance"
                        );
                        
                        publishHomeAssistantDiscovery(
                            exchange,
                            "statistics_" + typeId + "_total_workouts",
                            capitalize(workoutType) + " Total Workouts",
                            "",
                            "{{ value_json.totalWorkouts | default(0) }}",
                            statisticsTopic,
                            "measurement"
                        );
                    }
                    
                    log.info("Home Assistant discovery configurations published");
                });
        }

        // Timer-based route that triggers every X milliseconds
        fromF("timer:workout-timer?period=%d&delay=%d", timerPeriod, timerDelay)
            .log("Fetching workouts from REST API: " + workoutsListUrl)
            // Set the API key header
            .setHeader(apiKeyHeaderName, constant(restApiKey))
            .setHeader(Exchange.HTTP_METHOD, constant("GET"))
            // Step 1: Fetch all workouts (limit=1 doesn't work, so we fetch all and filter)
            .toF("%s?bridgeEndpoint=true&throwExceptionOnFailure=false", workoutsListUrl)
            // .log("Received workouts response: ${body}")
            // Check if first API call was successful
            .choice()
                .when(exchange -> {
                    Integer statusCode = exchange.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE, Integer.class);
                    return statusCode != null && statusCode < 300;
                })
                    .log("Workouts list retrieved successfully")
                    // Step 2: Filter workouts by type and find the latest workout for each selected type
                    .process(exchange -> {
                        String body = exchange.getIn().getBody(String.class);
                        ObjectMapper mapper = new ObjectMapper();
                        JsonNode response = mapper.readTree(body);
                        
                        log.debug("Full API response: " + body);
                        
                        // The API response structure is typically: { "results": [...] }
                        JsonNode workoutsArray = null;
                        if (response.has("results")) {
                            workoutsArray = response.get("results");
                            log.debug("Found 'results' field in response");
                        } else if (response.isArray()) {
                            workoutsArray = response;
                            log.debug("Response is directly an array");
                        } else {
                            // Try to find any array field
                            response.fieldNames().forEachRemaining(fieldName -> {
                                JsonNode field = response.get(fieldName);
                                if (field.isArray()) {
                                    log.debug("Found array field: " + fieldName);
                                }
                            });
                            throw new RuntimeException("Unexpected response format. Response keys: " + 
                                response.fieldNames().toString() + ". Full response: " + body);
                        }
                        
                        // Check if we have at least one workout
                        if (workoutsArray == null || workoutsArray.size() == 0) {
                            throw new RuntimeException("No workouts found in response. Full response: " + body);
                        }
                        
                        // Store latest workouts by type in exchange properties
                        for (String workoutType : selectedTypes) {
                            JsonNode latestWorkoutForType = null;
                            long latestId = 0;
                            String latestDate = "";
                            
                            // Find the latest workout of this type
                            for (int i = 0; i < workoutsArray.size(); i++) {
                                JsonNode workout = workoutsArray.get(i);
                                
                                // Check if workout matches the type (case-insensitive)
                                String workoutTypeValue = workout.has("type") ? workout.get("type").asText() : "";
                                if (!workoutTypeValue.equalsIgnoreCase(workoutType)) {
                                    continue;
                                }
                                
                                long workoutId = workout.has("id") ? workout.get("id").asLong() : 0;
                                String workoutDate = workout.has("date") ? workout.get("date").asText() : "";
                                
                                // Check if this is the latest workout for this type
                                boolean isNewer = false;
                                if (latestWorkoutForType == null) {
                                    isNewer = true;
                                } else if (workoutId > latestId) {
                                    isNewer = true;
                                } else if (workoutId == latestId && !workoutDate.isEmpty() && !latestDate.isEmpty()) {
                                    if (workoutDate.compareTo(latestDate) > 0) {
                                        isNewer = true;
                                    }
                                } else if (!workoutDate.isEmpty() && latestDate.isEmpty()) {
                                    isNewer = true;
                                }
                                
                                if (isNewer) {
                                    latestWorkoutForType = workout;
                                    latestId = workoutId;
                                    latestDate = workoutDate;
                                }
                            }
                            
                            // Process and store the latest workout for this type
                            if (latestWorkoutForType != null) {
                                // Remove data.details from the workout before sending
                                String workoutJsonString = mapper.writeValueAsString(latestWorkoutForType);
                                ObjectNode workoutCopy = (ObjectNode) mapper.readTree(workoutJsonString);
                                
                                if (workoutCopy.has("data")) {
                                    JsonNode dataNode = workoutCopy.get("data");
                                    if (dataNode.isObject() && dataNode.has("details")) {
                                        ObjectNode dataObject = (ObjectNode) dataNode;
                                        dataObject.remove("details");
                                    }
                                }
                                
                                String workoutJson = mapper.writeValueAsString(workoutCopy);
                                exchange.setProperty("latest_workout_" + workoutType.toLowerCase(), workoutJson);
                                log.debug("Found latest " + workoutType + " workout (ID: " + latestId + ")");
                            } else {
                                log.debug("No workouts found for type: " + workoutType);
                            }
                        }
                        
                        // Set body to indicate processing is complete
                        exchange.getIn().setBody("processed");
                    })
                    // Step 3: Send latest workout for each type to MQTT (with retry logic)
                    .process(exchange -> {
                        for (String workoutType : selectedTypes) {
                            String workoutJson = exchange.getProperty("latest_workout_" + workoutType.toLowerCase(), String.class);
                            if (workoutJson != null) {
                                String typeTopic = mqttTopic + "/" + workoutType.toLowerCase();
                                publishToMqttWithRetry(typeTopic, workoutJson, "latest " + workoutType + " workout", 30000, 1000);
                            }
                        }
                    })
                .otherwise()
                    .log("Failed to fetch workouts list. Status: ${header.CamelHttpResponseCode}, Body: ${body}")
            .endChoice();

        // Second route: Fetch totals from /api/v1/totals endpoint
        String totalsUrl = restApiServerUrl + restApiTotalsEndpoint;
        
        fromF("timer:totals-timer?period=%d&delay=%d", timerPeriod, timerDelay + 5000)
            .log("Fetching totals from REST API: " + totalsUrl)
            // Set the API key header
            .setHeader(apiKeyHeaderName, constant(restApiKey))
            .setHeader(Exchange.HTTP_METHOD, constant("GET"))
            // Fetch totals from API
            .toF("%s?bridgeEndpoint=true&throwExceptionOnFailure=false", totalsUrl)
            .log("Received totals response: ${body}")
            // Check if API call was successful
            .choice()
                .when(exchange -> {
                    Integer statusCode = exchange.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE, Integer.class);
                    return statusCode != null && statusCode < 300;
                })
                    .log("Totals retrieved successfully, sending to MQTT")
                    // Send totals to MQTT (with retry logic)
                    .process(exchange -> {
                        String body = exchange.getIn().getBody(String.class);
                        publishToMqttWithRetry(mqttTotalsTopic, body, "totals", 30000, 1000);
                    })
                .otherwise()
                    .log("Failed to fetch totals. Status: ${header.CamelHttpResponseCode}, Body: ${body}")
            .endChoice();

        // Third route: Fetch statistics from /api/v1/statistics endpoint and aggregate by workout type
        String statisticsUrl = restApiServerUrl + restApiStatisticsEndpoint;
        
        fromF("timer:statistics-timer?period=%d&delay=%d", timerPeriod, timerDelay + 10000)
            .log("Fetching statistics from REST API: " + statisticsUrl)
            // Set the API key header
            .setHeader(apiKeyHeaderName, constant(restApiKey))
            .setHeader(Exchange.HTTP_METHOD, constant("GET"))
            // Fetch statistics from API
            .toF("%s?bridgeEndpoint=true&throwExceptionOnFailure=false", statisticsUrl)
            .log("Received statistics response")
            // Check if API call was successful
            .choice()
                .when(exchange -> {
                    Integer statusCode = exchange.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE, Integer.class);
                    return statusCode != null && statusCode < 300;
                })
                    .log("Statistics retrieved successfully, processing and sending to MQTT")
                    // Process statistics: aggregate total distance and workouts by type
                    .process(exchange -> {
                        String body = exchange.getIn().getBody(String.class);
                        ObjectMapper mapper = new ObjectMapper();
                        JsonNode response = mapper.readTree(body);
                        
                        log.debug("Full statistics API response: " + body);
                        
                        // The API response structure is: { "results": { "buckets": { "running": { "buckets": { "2020-06-14": {...}, ... } }, "cycling": { "buckets": { "2022-08-04": {...}, ... } } } } }
                        // Note: The inner "buckets" is an object with date keys, not an array
                        JsonNode results = response.has("results") ? response.get("results") : response;
                        JsonNode buckets = results.has("buckets") ? results.get("buckets") : null;
                        
                        if (buckets == null || !buckets.isObject()) {
                            throw new RuntimeException("Unexpected statistics response format. Expected 'results.buckets' object.");
                        }
                        
                        // Process each workout type that we're interested in
                        for (String workoutType : selectedTypes) {
                            String typeLower = workoutType.toLowerCase();
                            
                            // Check if this workout type exists in the buckets
                            if (!buckets.has(typeLower)) {
                                log.warn("No statistics found for workout type: " + workoutType);
                                continue;
                            }
                            
                            JsonNode typeBucket = buckets.get(typeLower);
                            JsonNode typeBuckets = typeBucket.has("buckets") ? typeBucket.get("buckets") : null;
                            
                            if (typeBuckets == null || !typeBuckets.isObject()) {
                                log.warn("No monthly buckets found for workout type: " + workoutType);
                                continue;
                            }
                            
                            // Aggregate: sum distance and workouts across all months
                            // The buckets object has date keys (e.g., "2022-08-04", "2022-09-13")
                            double totalDistance = 0.0;
                            int totalWorkouts = 0;
                            
                            // Iterate over the object fields (date keys)
                            java.util.Iterator<String> dateKeys = typeBuckets.fieldNames();
                            while (dateKeys.hasNext()) {
                                String dateKey = dateKeys.next();
                                JsonNode monthBucket = typeBuckets.get(dateKey);
                                
                                if (monthBucket.has("distance")) {
                                    totalDistance += monthBucket.get("distance").asDouble(0.0);
                                }
                                
                                if (monthBucket.has("workouts")) {
                                    totalWorkouts += monthBucket.get("workouts").asInt(0);
                                }
                            }
                            
                            // Create aggregated JSON object
                            ObjectNode aggregatedStats = mapper.createObjectNode();
                            aggregatedStats.put("workoutType", workoutType);
                            aggregatedStats.put("totalDistance", totalDistance);
                            aggregatedStats.put("totalWorkouts", totalWorkouts);
                            
                            String aggregatedJson = mapper.writeValueAsString(aggregatedStats);
                            
                            // Send to MQTT with type-specific topic
                            String typeTopic = mqttStatisticsTopic + "/" + typeLower;
                            publishToMqttWithRetry(typeTopic, aggregatedJson, workoutType + " statistics", 30000, 1000);
                            
                            log.info("Published statistics for " + workoutType + ": " + totalWorkouts + " workouts, " + 
                                String.format("%.2f", totalDistance / 1000) + " km total distance");
                        }
                    })
                .otherwise()
                    .log("Failed to fetch statistics. Status: ${header.CamelHttpResponseCode}, Body: ${body}")
            .endChoice();
    }
}
