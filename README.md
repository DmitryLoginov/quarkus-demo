# Quarkus Demo Project

## Running the application

```bash
.\mvnw quarkus:dev
.\mvnw.cmd quarkus:dev
```

## Testing

```bash
.\mvnw test
.\mvnw.cmd test
```

## Using
* Java 21
* `io.quarkus.platform:quarkus-bom:3.39.3` for dependency management
* `io.quarkus.platform:quarkus-maven-plugin:3.39.3` for packaging the application and providing development mode (live reload)

## Live Reload

Live Reload allow us to see changes immediately after editing Java or resource files just refreshing browser page, no application restart needed.

## Test Endpoint

`GET /api/info`

Response:

```json
{
  "name": "book-catalog",
  "framework": "Quarkus"
}
```

## Components

* `InfoResource` - HTTP-layer
* `InfoService` - business logic
* `CatalogConfig` - configuration properties

## Profiles

* dev - profile for development mode
* test - profile for testing