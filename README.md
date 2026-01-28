# Kafka Cartographer

Kafka Cartographer is a static analysis tool designed to map Kafka topic definitions and usages across your Java projects. It scans your codebase to identify where topics are defined (as constants), where they are consumed (via `@KafkaListener`), and where they are produced (via `send()` methods).

## Purpose

In large distributed systems, tracking Kafka topic dependencies can be difficult. This tool helps visualize and audit the flow of messages by generating a JSON report linking topic definitions to their producers and consumers.

## Features

-   **Static Analysis:** Uses `JavaParser` to safely parse and analyze source code without compiling.
-   **Definition Detection:** Finds static string constants that look like topic definitions.
-   **Usage Detection:** Identifies topic usages in:
    -   Spring `@KafkaListener` annotations.
    -   `KafkaTemplate.send()` or `Producer.send()` method calls.
-   **Symbol Resolution:** Attempts to link usages back to their original constant definitions.
-   **JSON Reporting:** Outputs a clear `topic_map.json` file containing found links and literals.

## Prerequisites

-   Docker

## Building and Running with Docker

You can build and run the application easily using Docker, without needing to install Java or Maven locally.

### 1. Build the Docker Image

Run the following command in the project root:

```bash
docker build -t kafka-cartographer .
```

### 2. Run the Scanner

To scan a directory on your local machine, you must mount it as a volume into the container. The tool will generate the report inside that directory.

**Syntax:**
```bash
docker run --rm -v <HOST_PATH_TO_SCAN>:/scan kafka-cartographer /scan
```

**Example:**
To scan your current directory:
```bash
docker run --rm -v $(pwd):/scan kafka-cartographer /scan
```

To scan a specific project path:
```bash
docker run --rm -v /home/user/my-projects:/scan kafka-cartographer /scan
```

### 3. Try the Sample Project

The repository includes a `sample` folder with a dummy Java project to test the scanner.

```bash
docker run --rm -v $(pwd)/sample:/scan kafka-cartographer /scan
```

## Running the Visualization

The project includes an `index.html` file to visualize the generated `topic_map.json`. You can serve this using a Docker container.

### 1. Build the Visualization Image

```bash
docker build -f Dockerfile.visualization -t kafka-cartographer-web .
```

### 2. Run the Visualization Server

```bash
docker run --rm -p 8080:80 kafka-cartographer-web
```

Once running, open [http://localhost:8080](http://localhost:8080) in your browser.
