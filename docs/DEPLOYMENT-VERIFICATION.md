# Canary Deployment & Performance Verification Guide

This guide explains how to deploy the Kubernetes Canary manifests, route traffic, run the K6 load tests, and verify performance metrics (SLOs) before completing the rollout.

## 1. Kick Off the Canary Deployment

To initiate the canary rollout, apply the primary ingress (if not already applied) and the new canary manifests.
*Ensure your NGINX Ingress Controller is running on your Kubernetes cluster.*

```bash
# 1. Deploy the primary ingress (routes 100% to stable by default)
kubectl apply -f k8s/canary/ingress.yaml

# 2. Deploy the canary application (v2) and its dedicated service
kubectl apply -f k8s/canary/canary-deployment.yaml
kubectl apply -f k8s/canary/canary-service.yaml

# 3. Apply the Canary Ingress to split traffic (10% to Canary)
kubectl apply -f k8s/canary/ingress-canary.yaml
```

**Verification:**
Test the split by sending requests in a loop. You should see approximately 1 out of 10 requests hitting the new canary backend.
```bash
for i in {1..20}; do curl -s http://api.workhub.local/actuator/health | grep status; done
```

## 2. Run the K6 Load Test Against the Canary

Now that 10% of real-world traffic is hitting the canary, simulate peak multi-tenant load to verify the new version's performance.

```bash
# Install k6 (if not installed)
# Windows: winget install k6
# Mac: brew install k6

# Run the load test
k6 run k6/load-test.js -e TARGET_URL=http://api.workhub.local
```

### 3. Monitor Spring Boot Actuator Metrics

While the K6 test is running, actively monitor the Actuator endpoints on the Canary pods to identify bottlenecks:

1. **Check Connection Pool:**
   `GET /actuator/metrics/hikaricp.connections.pending`
2. **Check Latency (p95):**
   `GET /actuator/metrics/http.server.requests`
3. **Check Liveness / Readiness:**
   `GET /actuator/health`

## 4. Evaluate SLOs and Complete Rollout

Wait for the K6 script to complete. It will print a summary of the metrics.

- Did `http_req_duration{scenario:read}` stay under 200ms?
- Was `http_req_failed` under 1%?

**If the SLOs Pass:**
Increase the canary weight to 50%, then 100%, and finally update the primary deployment.
```bash
# Update weight to 50%
kubectl patch ingress workhub-ingress-canary -n workhub \
  -p '{"metadata":{"annotations":{"nginx.ingress.kubernetes.io/canary-weight":"50"}}}'

# Once fully validated at 100%, update the primary stable deployment to v2 and delete the canary.
```

**If the SLOs Fail:**
The Spring Boot startup probes or K6 metrics failed. Abort the canary instantly to protect tenants:
```bash
kubectl delete -f k8s/canary/ingress-canary.yaml
kubectl delete -f k8s/canary/canary-deployment.yaml
```
