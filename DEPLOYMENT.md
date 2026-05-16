# Deployment Guide - WorkHub SaaS Backend

## Local Docker Compose Deployment

This project can be deployed locally using Docker Compose. The compose stack runs:

- Spring Boot backend
- PostgreSQL database
- RabbitMQ message broker

---

## Prerequisites

Make sure the following tools are installed:

- Docker Desktop
- Docker Compose
- Java 17
- Gradle Wrapper, already included in the project

---

## Build the Spring Boot JAR

Before building the Docker image, generate the Spring Boot executable JAR.

For Windows PowerShell:

```powershell
.\gradlew.bat clean bootJar -x test
```

For Linux or macOS:

```bash
./gradlew clean bootJar -x test
```

The generated JAR file will be located at:

```text
build/libs/Project-0.0.1-SNAPSHOT.jar
```

---

## Dockerfile

The backend service is containerized using Java 17 runtime.

```dockerfile
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

COPY build/libs/Project-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8082

ENTRYPOINT ["java", "-jar", "app.jar"]
```

This Dockerfile copies the generated Spring Boot JAR into the container and runs it on port `8082`.

---

## Start the Full Local Stack

Run this command from the project root:

```bash
docker compose up --build
```

This starts the following containers:

- `workhub-app`
- `workhub-postgres`
- `workhub-rabbitmq`

---

## Services and Ports

| Service | Container Name | Host Port | Container Port |
|---|---|---:|---:|
| Spring Boot App | workhub-app | 8082 | 8082 |
| PostgreSQL | workhub-postgres | 5433 | 5432 |
| RabbitMQ AMQP | workhub-rabbitmq | 5672 | 5672 |
| RabbitMQ Management UI | workhub-rabbitmq | 15672 | 15672 |

---

## Environment Variables

The Docker Compose file passes these environment variables to the backend container:

```text
DB_URL=jdbc:postgresql://postgres:5432/workhubdb
DB_USERNAME=postgres
DB_PASSWORD=postgres
RABBITMQ_HOST=rabbitmq
RABBITMQ_PORT=5672
RABBITMQ_USERNAME=guest
RABBITMQ_PASSWORD=guest
JWT_SECRET=<base64-secret>
JWT_EXPIRATION=86400000
WORKHUB_QUEUE=report.generate
WORKHUB_EXCHANGE=report.exchange
WORKHUB_ROUTING_KEY=report.generate
```

Inside Docker Compose, the backend connects to PostgreSQL using the service name:

```text
postgres
```

and connects to RabbitMQ using the service name:

```text
rabbitmq
```

It does not use `localhost` inside the container network.

---

## Verify the Deployment

After the containers start successfully, open:

```text
http://localhost:8082/actuator/health
```

Expected result:

```json
{
  "status": "UP"
}
```

You can also verify readiness and liveness:

```text
http://localhost:8082/actuator/health/readiness
```

```text
http://localhost:8082/actuator/health/liveness
```

RabbitMQ Management UI:

```text
http://localhost:15672
```

Default login:

```text
Username: guest
Password: guest
```

---

## Stop the Stack

To stop the running containers:

```bash
docker compose down
```

To stop the containers and remove the PostgreSQL volume:

```bash
docker compose down -v
```

Note: `docker compose down -v` deletes the database volume and removes stored database data.

---

## Notes

The application was tested locally using Docker Compose. The health endpoint returned `UP`, and the health details confirmed that PostgreSQL, RabbitMQ, readiness, and liveness were all running successfully.