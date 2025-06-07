# Kotlin Ktor Application

This is a simple Kotlin web application built with Ktor and Java 21.

## Prerequisites

- Java 21 JDK
- Gradle (wrapper included)

## Running the Application

To run the application, use one of the following commands:

```bash
./gradlew run
```

The application will start on `http://localhost:8080`

## Testing

To run the tests:

```bash
./gradlew test
```

## Endpoints

- `GET /` - Returns a welcome message

## Build

To build the application:

```bash
./gradlew build
```

The built JAR file will be in `build/libs/` directory.

## Features

- Built with Ktor - A lightweight and flexible Kotlin web framework
- Uses Netty as the embedded server
- JSON support with content negotiation
- Comprehensive test suite using Ktor's test framework 