# WorkHub Load Testing + SLO Report

## 1. Objective

This report documents the load testing performed on the WorkHub Spring Boot backend.

The goal was to verify that the main backend flow can handle concurrent users with acceptable response time and low failure rate.

The tested flow includes:

1. User login
2. Fetching projects
3. Creating a project
4. Generating a report job

The test also indirectly validates the async report generation flow because the `generate-report` endpoint creates a job and sends it through the messaging workflow.

---

## 2. Tool Used

The load test was performed using **k6** through Docker.

The test was executed with the following command:

```powershell
docker run --rm -i --add-host=host.docker.internal:host-gateway -v ${PWD}/load-testing:/scripts grafana/k6 run /scripts/workhub-load-test.js
```

Docker was used to avoid installing k6 directly on the local machine.

---

## 3. Test Environment

The application was tested locally using Docker Compose. Docker Compose was running the Spring Boot backend, PostgreSQL database, and RabbitMQ message broker together.

Because k6 was executed inside a Docker container, the script used:

```text
http://host.docker.internal:8082
```

This allowed the k6 container to access the backend running on the local machine.

---

## 4. Tested Endpoints

| Endpoint | Method | Purpose |
|---|---|---|
| `/auth/login` | POST | Authenticate tenant admin and get JWT token |
| `/projects` | GET | Retrieve tenant projects |
| `/projects` | POST | Create a new project |
| `/projects/{id}/generate-report` | POST | Generate an async report job |

---

## 5. Load Test Scenario

The test used a ramping virtual user scenario:

| Stage | Duration | Target Users |
|---|---:|---:|
| Ramp-up | 10 seconds | 5 users |
| Steady load | 20 seconds | 10 users |
| Ramp-down | 10 seconds | 0 users |

Maximum virtual users:

```text
10 VUs
```

Total test duration:

```text
40 seconds
```

---

## 6. Service Level Objectives

The following SLOs were defined for this test:

| SLO | Target |
|---|---|
| Request failure rate | Less than 5% failed HTTP requests |
| Latency | 95th percentile response time below 1000 ms |
| Functional correctness | 100% of checks should pass |

These thresholds were configured in the k6 script:

```javascript
thresholds: {
  http_req_failed: ["rate<0.05"],
  http_req_duration: ["p(95)<1000"]
}
```

---

## 7. Test Results

The load test passed all configured thresholds.

| Metric | Result |
|---|---:|
| HTTP request failure rate | 0.00% |
| 95th percentile response time | 119.65 ms |
| Average response time | 56.95 ms |
| Maximum response time | 304.57 ms |
| Total HTTP requests | 340 |
| Completed iterations | 85 |
| Maximum virtual users | 10 |
| Checks passed | 100% |
| Checks failed | 0% |

All endpoint checks passed successfully:

- Login returned HTTP 200
- Login returned a token
- Get projects returned HTTP 200
- Create project returned HTTP 201
- Create project returned an ID
- Generate report returned HTTP 202
- Generate report returned a job ID

---

## 8. Result Evaluation

The backend met the defined SLOs during the test.

The request failure rate was 0.00%, which is better than the target of less than 5%.

The 95th percentile response time was 119.65 ms, which is much lower than the target of 1000 ms.

All functional checks passed, which means the tested user flow worked correctly under the simulated load.

---

## 9. Evidence

Evidence collected:

1. k6 terminal output showing:
    - `http_req_failed = 0.00%`
    - `p(95) = 119.65ms`
    - `checks_succeeded = 100.00%`
    - `340 HTTP requests`
    - `85 iterations`
    - `10 max VUs`

2. Postman evidence from the outbox test showing:
    - Report job created as `PENDING`
    - Job later became `COMPLETED`

3. Docker logs showing:
    - Polling from `outbox_messages`
    - Job completed successfully

---

## 10. Conclusion

The WorkHub backend successfully handled the tested load scenario with 10 concurrent virtual users.

The system achieved:

- 0% failed requests
- 100% successful checks
- 95th percentile latency of 119.65 ms
- Successful execution of authentication, project creation, and report generation endpoints

Based on this test, the local deployment meets the defined SLOs for this workload.