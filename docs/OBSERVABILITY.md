# OBSERVABILITY.md

## Actuator Endpoints

| Endpoint                        | Purpose                        | Expected Response         |
|---------------------------------|--------------------------------|---------------------------|
| GET /actuator/health            | Overall health                 | 200 { status: "UP" }      |
| GET /actuator/health/liveness   | Liveness probe (k8s)          | 200 { status: "UP" }      |
| GET /actuator/health/readiness  | Readiness probe (k8s)         | 200 { status: "UP" }      |
| GET /actuator/metrics           | List available metric names    | 200 JSON array            |
| GET /actuator/prometheus        | Prometheus scrape endpoint     | 200 text/plain            |

## How to Verify Locally

### 1. Start the stack
```bash
docker-compose up -d
```

### 2. Health check
```bash
curl http://localhost:8082/actuator/health
```

### 3. Liveness / Readiness
```bash
curl http://localhost:8082/actuator/health/liveness
curl http://localhost:8082/actuator/health/readiness
```

### 4. Custom metrics (after generating a report)
```bash
curl http://localhost:8082/actuator/metrics/workhub.jobs.published
curl http://localhost:8082/actuator/metrics/workhub.jobs.completed
```

### 5. Prometheus scrape
```bash
curl http://localhost:8082/actuator/prometheus | grep workhub
```

## Correlation ID
All requests must include or will receive `X-Correlation-ID` header.
Verify:
```bash
curl -H "X-Correlation-ID: test-123" http://localhost:8082/projects
```
Check logs for: `[test-123]` appearing in every log line for that request.
