#!/bin/bash
# WorkHub Phase 2 Demo — Async Messaging + Observability
BASE="http://localhost:8082"

echo "=== 1. Login ==="
TOKEN=$(curl -s -X POST $BASE/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@tenant-a.com","password":"password"}' \
  | jq -r '.token')
echo "Token: $TOKEN"

echo -e "\n=== 2. Create Project ==="
PROJECT_ID=$(curl -s -X POST $BASE/projects \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"Q3 Report Project"}' \
  | jq -r '.id')
echo "Project ID: $PROJECT_ID"

echo -e "\n=== 3. Enqueue Report Job (expect 202) ==="
JOB_ID=$(curl -s -o /tmp/job.json -w "%{http_code}" \
  -X POST $BASE/projects/$PROJECT_ID/generate-report \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Correlation-ID: demo-corr-001")
cat /tmp/job.json | jq .
JOB_ID=$(cat /tmp/job.json | jq -r '.jobId')

echo -e "\n=== 4. Poll Job Status (wait for COMPLETED) ==="
for i in {1..10}; do
  STATUS=$(curl -s $BASE/jobs/$JOB_ID \
    -H "Authorization: Bearer $TOKEN" | jq -r '.status')
  echo "Attempt $i: $STATUS"
  [ "$STATUS" = "COMPLETED" ] && break
  sleep 1
done

echo -e "\n=== 5. Actuator Health ==="
curl -s $BASE/actuator/health | jq .status

echo -e "\n=== 6. Liveness & Readiness ==="
curl -s $BASE/actuator/health/liveness | jq .status
curl -s $BASE/actuator/health/readiness | jq .status

echo -e "\n=== 7. Custom Metrics ==="
curl -s $BASE/actuator/metrics/workhub.jobs.published | jq .measurements
curl -s $BASE/actuator/metrics/workhub.jobs.completed | jq .measurements

echo -e "\n=== Done. Check logs for [demo-corr-001] correlation ID ==="
