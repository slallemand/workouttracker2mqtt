# Changelog

All notable changes to this add-on will be documented in this file.

## [1.7.0] - 2025-12-14

### Changed
- Version bump to 1.7.0

## [1.6.0] - 2025-12-13

### Changed
- Version bump to 1.6.0

## [1.6.0] - 2025-12-13

### Changed
- Version bump to 1.6.0

## [1.6.0] - 2025-12-13

### Changed
- Version bump to 1.6.0

## [1.6.0] - 2025-12-13

### Changed
- Version bump to 1.6.0

## [1.5.3] - 2025-12-13

### Changed
- Version bump to 1.5.3

## [1.5.3] - 2025-12-13

### Changed
- Version bump to 1.5.3

## [1.5.3] - 2025-12-13

### Changed
- Version bump to 1.5.3

## [1.5.3] - 2025-12-13

### Changed
- Version bump to 1.5.3

## [1.5.2] - 2025-12-13

### Changed
- Version bump to 1.5.2

## [1.5.2] - 2025-12-13

### Changed
- Version bump to 1.5.2

## [1.5.1] - 2025-12-13

### Changed
- Version bump to 1.5.1

## [1.5.1] - 2025-12-13

### Changed
- Version bump to 1.5.1

## [1.5.0] - 2025-12-13

### Changed
- Version bump to 1.5.0

## [1.4.4] - 2025-12-13

### Changed
- Version bump to 1.4.4

## [1.4.3] - 2025-12-13

### Changed
- Version bump to 1.4.3

## [1.4.2] - 2025-12-13

### Changed
- Version bump to 1.4.2

## [1.4.2] - 2025-12-13

### Changed
- Version bump to 1.4.2

## [1.2.0] - 2025-12-13

### Changed
- feat: add statistics (919566c)
- Refactor Dockerfile to use Home Assistant base image and streamline Java installation (aab90fa)
- update directory strucure for addon (1118c8d)
- Add installation instructions for Home Assistant add-on in README (d585e2b)
- Home Assistant addon capability (0c4846b)
- Add disclaimer to README highlighting experimental nature of the project (e624c57)
- Update README and configuration for workouttracker2mqtt integration with workout-tracker and MQTT broker (0d7abf8)
- rename project (8b155fb)
- add github actions (9246b32)
- retry mqtt connection (4feb1b7)



The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.1.0] - 2024-XX-XX

### Added
- Initial release of the Workout Tracker to MQTT add-on
- Support for fetching workouts from workout-tracker API
- MQTT publishing for workout data
- Home Assistant MQTT autodiscovery integration
- Configurable workout types filtering
- Automatic periodic updates

### Changed
- Improved MQTT connection retry logic with exponential backoff
- Added log messages when MQTT broker becomes available
