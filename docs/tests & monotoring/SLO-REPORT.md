# Service Level Objective (SLO) Performance Report

**Date Executed:** YYYY-MM-DD  
**Environment:** Canary / Staging / Production  
**Target URL:** `http://...`  

## 1. Executive Summary

Briefly describe the purpose of this load test (e.g., verifying database lock behavior under concurrent writes, or checking RabbitMQ scalability during async report generation spikes).

| Metric | Target SLO | Actual Result | Status |
|--------|------------|---------------|--------|
| **Sustained Read Latency p(95)** | < 200ms | `...ms` | 🟢 Pass / 🔴 Fail |
| **Global Error Rate (5xx)** | < 1.00% | `...%` | 🟢 Pass / 🔴 Fail |
| **Write Stress Latency p(95)** | < 500ms | `...ms` | 🟢 Pass / 🔴 Fail |

## 2. Workload Analysis

### Scenario 1: Authentication & Read (Sustained Load)
* **Goal:** Simulate peak tenant traffic authenticating and fetching projects.
* **Observations:** 
  * Did the JVM heap usage remain stable? (Check `/actuator/metrics/jvm.memory.used`)
  * Any HikariCP connection pool exhaustion? (Check `/actuator/metrics/hikaricp.connections.pending`)

### Scenario 2: Write Intensity (Stress Load)
* **Goal:** Verify that concurrent task creation for the same project does not result in deadlocks or data corruption.
* **Observations:** 
  * Lock acquisition times and transaction rollbacks.
  * DB CPU utilization.

### Scenario 3: Async Job Processing (Spike Load)
* **Goal:** Trigger a massive spike in report generation.
* **Observations:**
  * Did the API respond quickly with `202 Accepted`?
  * What was the maximum RabbitMQ queue depth? (Check `rabbitmq_queue_messages_ready`)
  * Did consumer lag recover within an acceptable timeframe?

## 3. Bottleneck Identification

*(Detail any system bottlenecks observed during the test)*

- **Database:** Were connections maxed out (`hikaricp.connections.active == maximumPoolSize`)?
- **Compute:** Did the Pods hit CPU throttling?
- **Memory:** Were there frequent Garbage Collection pauses? (`jvm.gc.pause`)

## 4. Remediation Steps & Tuning

If any SLOs were breached, outline the immediate remediation steps taken.
- *Example: Increased `spring.datasource.hikari.maximum-pool-size` from 20 to 50.*
- *Example: Scaled Async Worker replicas from 2 to 4 to reduce consumer lag.*
