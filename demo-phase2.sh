#!/bin/bash
# WorkHub Phase 2 Demo — Async Messaging + Observability
BASE="http://localhost:8082"

echo "=== 1. Login ==="
cat << 'EOF' > login.json
{"email":"admin@tenant-a.com","password":"password"}
EOF
LOGIN_RESP=$(curl -s -X POST $BASE/auth/login \
  -H "Content-Type: application/json" \
  -d @login.json)
TOKEN=$(echo "$LOGIN_RESP" | grep -o '"token":"[^"]*' | cut -d'"' -f4)
echo "Token: $TOKEN"

echo -e "\n=== 2. Create Project ==="
cat << 'EOF' > proj.json
{"name":"Q3 Report Project"}
EOF
PROJ_RESP=$(curl -s -X POST $BASE/projects \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d @proj.json)
PROJECT_ID=$(echo "$PROJ_RESP" | grep -o '"id":[0-9]*' | cut -d':' -f2)
echo "Project ID: $PROJECT_ID"

echo -e "\n=== 3. Enqueue Report Job (expect 202) ==="
JOB_RESP=$(curl -s -X POST $BASE/projects/$PROJECT_ID/generate-report \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Correlation-ID: demo-corr-001")
echo "$JOB_RESP"
JOB_ID=$(echo "$JOB_RESP" | grep -o '"jobId":"[^"]*' | cut -d'"' -f4)

echo -e "\n=== 4. Poll Job Status (wait for COMPLETED) ==="
for i in {1..10}; do
  STATUS_RESP=$(curl -s $BASE/jobs/$JOB_ID -H "Authorization: Bearer $TOKEN")
  STATUS=$(echo "$STATUS_RESP" | grep -o '"status":"[^"]*' | cut -d'"' -f4)
  echo "Attempt $i: $STATUS"
  [ "$STATUS" = "COMPLETED" ] && break
  sleep 1
done

echo -e "\n=== 5. Actuator Health ==="
curl -s $BASE/actuator/health | grep -o '"status":"[^"]*'

echo -e "\n=== 6. Liveness & Readiness ==="
curl -s $BASE/actuator/health/liveness | grep -o '"status":"[^"]*'
curl -s $BASE/actuator/health/readiness | grep -o '"status":"[^"]*'

echo -e "\n=== 7. Custom Metrics ==="
curl -s $BASE/actuator/metrics/workhub.jobs.published | grep -o '"measurements":\[{[^}]*}\]'
curl -s $BASE/actuator/metrics/workhub.jobs.completed | grep -o '"measurements":\[{[^}]*}\]'

echo -e "\n=== Done. Check logs for [demo-corr-001] correlation ID ==="
