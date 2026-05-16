# Deployment Guide — WorkHub Multi-Tenant SaaS Backend

> **Phase 3 — Kubernetes & Cloud Deployment**
>
> This guide covers every deployment target: local Docker Compose, standard Kubernetes,
> and Oracle Cloud Infrastructure (OCI) Oracle Kubernetes Engine (OKE).

---

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Build the Application](#build-the-application)
3. [Local Docker Compose Deployment](#local-docker-compose-deployment)
4. [Kubernetes Manifests Overview](#kubernetes-manifests-overview)
5. [Kubernetes Deployment — Local / Standard](#kubernetes-deployment--local--standard)
6. [Oracle Cloud (OCI) OKE Deployment](#oracle-cloud-oci-oke-deployment)
7. [Health & Observability](#health--observability)
8. [Rollback & Troubleshooting](#rollback--troubleshooting)

---

## Prerequisites

| Tool | Minimum Version | Purpose |
|------|----------------|---------|
| Java JDK | 17 | Compile & run the Spring Boot app |
| Gradle Wrapper | Included | Build automation (no global install needed) |
| Docker Desktop | 24+ | Container runtime |
| Docker Compose | v2+ | Local multi-container orchestration |
| kubectl | 1.27+ | Kubernetes CLI |
| OCI CLI | 3.x | Oracle Cloud authentication & cluster access |
| Minikube / kind | Latest | Local Kubernetes cluster (optional) |

---

## Build the Application

Generate the Spring Boot executable JAR before building a container image.

**Windows PowerShell:**

```powershell
.\gradlew.bat clean bootJar -x test
```

**Linux / macOS:**

```bash
./gradlew clean bootJar -x test
```

Output artifact:

```
build/libs/Project-0.0.1-SNAPSHOT.jar
```

### Build the Docker Image

```bash
docker build -t workhub-backend:latest .
```

> **Tip:** For OCI deployments, tag the image with the OCI Container Registry (OCIR) path:
>
> ```bash
> docker tag workhub-backend:latest <region>.ocir.io/<tenancy-namespace>/<repo>/workhub-backend:v1.0.0
> ```

---

## Local Docker Compose Deployment

The `docker-compose.yml` in the project root spins up the full stack:

| Service | Container Name | Host Port | Container Port |
|---------|---------------|----------:|---------------:|
| Spring Boot App | workhub-app | 8082 | 8082 |
| PostgreSQL 15 | workhub-postgres | 5433 | 5432 |
| RabbitMQ 3 (AMQP) | workhub-rabbitmq | 5672 | 5672 |
| RabbitMQ Management UI | workhub-rabbitmq | 15672 | 15672 |

### Start the Stack

```bash
docker compose up --build -d
```

### Verify Services

```bash
# Application health
curl http://localhost:8082/actuator/health

# Readiness probe
curl http://localhost:8082/actuator/health/readiness

# Liveness probe
curl http://localhost:8082/actuator/health/liveness
```

Expected response:

```json
{ "status": "UP" }
```

RabbitMQ Management UI: [http://localhost:15672](http://localhost:15672) — login: `guest` / `guest`

### Stop the Stack

```bash
# Stop containers (preserves data volumes)
docker compose down

# Stop containers AND delete PostgreSQL volume (data loss)
docker compose down -v
```

---

## Kubernetes Manifests Overview

All manifests live under `k8s/` and use **template placeholders** for reusability:

| Placeholder | Description | Example Value |
|-------------|-------------|---------------|
| `{{SERVICE_NAME}}` | Logical name of the microservice | `workhub-backend` |
| `{{IMAGE_TAG}}` | Full container image reference | `iad.ocir.io/ns/repo:v1.0.0` |
| `{{PORT}}` | Container port | `8080` |

### Manifest Inventory

| File | Kind | Purpose |
|------|------|---------|
| `k8s/configmap.yaml` | ConfigMap | Non-sensitive config (Spring profiles, DB URL, RabbitMQ host, etc.) |
| `k8s/secret.yaml` | Secret (Opaque) | Sensitive credentials (DB password, RabbitMQ password, JWT secret) |
| `k8s/deployment.yaml` | Deployment | 2-replica HA deployment with health probes & resource limits |
| `k8s/service.yaml` | Service | ClusterIP internal routing (promotable to LoadBalancer for OKE) |

### Architecture Diagram

```
┌────────────────────────────────────────────────────────────────┐
│                     Kubernetes Cluster                          │
│                                                                │
│  ┌──────────────────┐    ┌─────────────────────────────────┐  │
│  │  ConfigMap        │    │  Secret                         │  │
│  │  (env vars)       │    │  (DB_PASSWORD, JWT_SECRET, ...) │  │
│  └────────┬─────────┘    └────────────┬────────────────────┘  │
│           │ envFrom                   │ valueFrom              │
│           ▼                           ▼                        │
│  ┌─────────────────────────────────────────────────────────┐  │
│  │                    Deployment (replicas: 2)              │  │
│  │  ┌─────────────────┐    ┌─────────────────┐             │  │
│  │  │   Pod (app-1)   │    │   Pod (app-2)   │             │  │
│  │  │  :{{PORT}}      │    │  :{{PORT}}      │             │  │
│  │  │  readiness ✓    │    │  readiness ✓    │             │  │
│  │  │  liveness  ✓    │    │  liveness  ✓    │             │  │
│  │  └─────────────────┘    └─────────────────┘             │  │
│  └──────────────────────────┬──────────────────────────────┘  │
│                             │                                  │
│  ┌──────────────────────────▼──────────────────────────────┐  │
│  │                Service (ClusterIP / LoadBalancer)         │  │
│  │                :80 → :{{PORT}}                           │  │
│  └──────────────────────────────────────────────────────────┘  │
└────────────────────────────────────────────────────────────────┘
```

---

## Kubernetes Deployment — Local / Standard

This section covers deploying to any standard Kubernetes cluster (Minikube, kind, Docker Desktop, or bare-metal).

### Step 1 — Hydrate the Templates

Replace the placeholders with real values. Using `sed` (Linux/macOS) or PowerShell:

**Linux / macOS:**

```bash
export SERVICE_NAME=workhub-backend
export IMAGE_TAG=workhub-backend:latest
export PORT=8080

for f in k8s/*.yaml; do
  sed -i "s|{{SERVICE_NAME}}|${SERVICE_NAME}|g" "$f"
  sed -i "s|{{IMAGE_TAG}}|${IMAGE_TAG}|g" "$f"
  sed -i "s|{{PORT}}|${PORT}|g" "$f"
done
```

**Windows PowerShell:**

```powershell
$SERVICE_NAME = "workhub-backend"
$IMAGE_TAG    = "workhub-backend:latest"
$PORT         = "8080"

Get-ChildItem k8s\*.yaml | ForEach-Object {
    (Get-Content $_.FullName) `
        -replace '\{\{SERVICE_NAME\}\}', $SERVICE_NAME `
        -replace '\{\{IMAGE_TAG\}\}',    $IMAGE_TAG `
        -replace '\{\{PORT\}\}',         $PORT |
    Set-Content $_.FullName
}
```

### Step 2 — Update the Secret Values

Encode your production credentials:

```bash
echo -n 'my-real-db-password' | base64
# Output: bXktcmVhbC1kYi1wYXNzd29yZA==
```

Edit `k8s/secret.yaml` and replace the placeholder base64 values with the real ones.

> **⚠️ Security Warning:** Never commit real secrets to version control.
> Add `k8s/secret.yaml` to `.gitignore` or use [Sealed Secrets](https://github.com/bitnami-labs/sealed-secrets) / [External Secrets Operator](https://external-secrets.io/).

### Step 3 — Apply the Manifests (Correct Order)

Dependencies must exist before the Deployment references them:

```bash
# 1. Create the namespace (optional, but recommended)
kubectl create namespace workhub --dry-run=client -o yaml | kubectl apply -f -

# 2. Apply ConfigMap and Secret first
kubectl apply -f k8s/configmap.yaml -n workhub
kubectl apply -f k8s/secret.yaml    -n workhub

# 3. Apply the Deployment
kubectl apply -f k8s/deployment.yaml -n workhub

# 4. Apply the Service
kubectl apply -f k8s/service.yaml -n workhub
```

Or apply everything at once (Kubernetes resolves ordering):

```bash
kubectl apply -f k8s/ -n workhub
```

### Step 4 — Verify the Deployment

```bash
# Check rollout status
kubectl rollout status deployment/workhub-backend -n workhub

# Verify pods are running and ready
kubectl get pods -n workhub -l app=workhub-backend -o wide

# Check service endpoint
kubectl get svc -n workhub

# View pod logs
kubectl logs -l app=workhub-backend -n workhub --tail=100

# Port-forward to test locally
kubectl port-forward svc/workhub-backend-svc 8080:80 -n workhub
# Then: curl http://localhost:8080/actuator/health
```

### Step 5 — Scale (Optional)

```bash
# Scale to 3 replicas
kubectl scale deployment/workhub-backend --replicas=3 -n workhub

# Or use Horizontal Pod Autoscaler
kubectl autoscale deployment/workhub-backend \
  --cpu-percent=70 \
  --min=2 \
  --max=10 \
  -n workhub
```

---

## Oracle Cloud (OCI) OKE Deployment

### OKE-Specific Prerequisites

1. **OCI CLI** authenticated:
   ```bash
   oci session authenticate
   ```

2. **OKE cluster** created via OCI Console or Terraform.

3. **`kubeconfig`** configured for the cluster:
   ```bash
   oci ce cluster create-kubeconfig \
     --cluster-id <your-cluster-ocid> \
     --file $HOME/.kube/config \
     --region <region> \
     --token-version 2.0.0 \
     --kube-endpoint PUBLIC_ENDPOINT
   ```

4. **OCIR login** (to push/pull images):
   ```bash
   docker login <region>.ocir.io \
     -u '<tenancy-namespace>/<username>' \
     -p '<auth-token>'
   ```

### Step 1 — Verify Node Readiness

Before deploying any workload, confirm the OKE worker nodes are healthy:

```bash
# All nodes should show STATUS = Ready
kubectl get nodes -o wide

# Check node conditions for pressure warnings
kubectl describe nodes | grep -A5 "Conditions:"
```

Expected output:

```
NAME              STATUS   ROLES   AGE   VERSION
10.0.10.2         Ready    node    12d   v1.28.2
10.0.10.3         Ready    node    12d   v1.28.2
10.0.10.4         Ready    node    12d   v1.28.2
```

> **⚠️ Important:** If any node shows `NotReady`, investigate with `kubectl describe node <name>` before proceeding.

### Step 2 — Push the Image to OCIR

```bash
# Tag for OCIR
docker tag workhub-backend:latest \
  <region>.ocir.io/<tenancy-namespace>/workhub/workhub-backend:v1.0.0

# Push
docker push <region>.ocir.io/<tenancy-namespace>/workhub/workhub-backend:v1.0.0
```

### Step 3 — Create OCIR Image Pull Secret

If your OCIR repository is private:

```bash
kubectl create secret docker-registry ocir-secret \
  --docker-server=<region>.ocir.io \
  --docker-username='<tenancy-namespace>/<username>' \
  --docker-password='<auth-token>' \
  --docker-email='<email>' \
  -n workhub
```

Add the pull secret to `k8s/deployment.yaml`:

```yaml
spec:
  template:
    spec:
      imagePullSecrets:
        - name: ocir-secret
```

### Step 4 — Hydrate Templates for OKE

```bash
export SERVICE_NAME=workhub-backend
export IMAGE_TAG="<region>.ocir.io/<tenancy-namespace>/workhub/workhub-backend:v1.0.0"
export PORT=8080

for f in k8s/*.yaml; do
  sed -i "s|{{SERVICE_NAME}}|${SERVICE_NAME}|g" "$f"
  sed -i "s|{{IMAGE_TAG}}|${IMAGE_TAG}|g" "$f"
  sed -i "s|{{PORT}}|${PORT}|g" "$f"
done
```

### Step 5 — Expose via OCI Load Balancer

To expose the service externally on OKE, change the Service type and uncomment the OCI annotations in `k8s/service.yaml`:

```yaml
spec:
  type: LoadBalancer
  # ...

metadata:
  annotations:
    service.beta.kubernetes.io/oci-load-balancer-shape: "flexible"
    service.beta.kubernetes.io/oci-load-balancer-shape-flex-min: "10"
    service.beta.kubernetes.io/oci-load-balancer-shape-flex-max: "100"
```

### Step 6 — Apply All Manifests

```bash
kubectl create namespace workhub --dry-run=client -o yaml | kubectl apply -f -
kubectl apply -f k8s/ -n workhub
```

### Step 7 — Verify OKE Deployment

```bash
# Rollout status
kubectl rollout status deployment/workhub-backend -n workhub --timeout=300s

# Pod readiness
kubectl get pods -n workhub -o wide

# Get external Load Balancer IP / hostname
kubectl get svc workhub-backend-svc -n workhub -o wide
```

Wait for the `EXTERNAL-IP` to be assigned (OCI provisions a load balancer — this may take 1–3 minutes):

```bash
# Watch until EXTERNAL-IP changes from <pending>
kubectl get svc workhub-backend-svc -n workhub -w
```

Example output:

```
NAME                   TYPE           CLUSTER-IP    EXTERNAL-IP      PORT(S)        AGE
workhub-backend-svc    LoadBalancer   10.96.50.12   129.146.xxx.xx   80:31234/TCP   90s
```

### Step 8 — Validate via External IP

```bash
export LB_IP=$(kubectl get svc workhub-backend-svc -n workhub -o jsonpath='{.status.loadBalancer.ingress[0].ip}')

curl http://${LB_IP}/actuator/health
# → {"status":"UP"}

curl http://${LB_IP}/actuator/health/readiness
# → {"status":"UP"}
```

---

## Health & Observability

### Probe Endpoints

| Probe | Path | Purpose |
|-------|------|---------|
| Startup | `/actuator/health` | Gates liveness/readiness until app boots |
| Readiness | `/actuator/health/readiness` | Pod receives traffic only when ready |
| Liveness | `/actuator/health/liveness` | Restart pod if it becomes unresponsive |

### Prometheus Metrics

The application exposes a Prometheus scrape endpoint:

```
GET /actuator/prometheus
```

### Probe Timing Configuration

| Parameter | Startup | Readiness | Liveness |
|-----------|---------|-----------|----------|
| `initialDelaySeconds` | 10 | 30 | 60 |
| `periodSeconds` | 5 | 10 | 15 |
| `timeoutSeconds` | 3 | 5 | 5 |
| `failureThreshold` | 30 | 3 | 3 |

> The startup probe allows up to **160 seconds** (10 + 30×5) for Spring Boot to initialize before Kubernetes marks the pod as failed. This is sufficient for JPA schema migration and RabbitMQ connection setup.

---

## Rollback & Troubleshooting

### Rollback a Failed Deployment

```bash
# View revision history
kubectl rollout history deployment/workhub-backend -n workhub

# Roll back to previous revision
kubectl rollout undo deployment/workhub-backend -n workhub

# Roll back to a specific revision
kubectl rollout undo deployment/workhub-backend --to-revision=2 -n workhub
```

### Common Issues

| Symptom | Likely Cause | Resolution |
|---------|-------------|------------|
| `CrashLoopBackOff` | App fails to start; bad config | `kubectl logs <pod> -n workhub` — check for missing env vars or DB connectivity |
| `ImagePullBackOff` | Wrong image tag or missing OCIR secret | Verify `IMAGE_TAG`, check `imagePullSecrets` |
| Readiness probe failing | App not fully started yet | Increase `initialDelaySeconds` on readiness probe |
| `EXTERNAL-IP` stuck at `<pending>` | OCI LB not provisioned | Check OCI Console → Networking → Load Balancers; verify VCN security lists allow ingress on port 80 |
| `0/3 nodes available` | Insufficient resources | Check node capacity: `kubectl describe nodes` |

### Useful Debug Commands

```bash
# Full pod description (events, conditions)
kubectl describe pod <pod-name> -n workhub

# Interactive shell into running container
kubectl exec -it <pod-name> -n workhub -- /bin/sh

# Watch all events in the namespace
kubectl get events -n workhub --sort-by='.lastTimestamp'

# Resource usage per pod (requires metrics-server)
kubectl top pods -n workhub
```

---

## Notes

- The application was developed with Spring Boot 4.0.3 on Java 17.
- Actuator health probes (readiness + liveness) are enabled in `application.yml`.
- Prometheus metrics are exposed at `/actuator/prometheus` via Micrometer.
- The `k8s/secret.yaml` file ships with **placeholder** base64 values — **always** replace before production.
- For production secret management on OCI, use [OCI Vault](https://docs.oracle.com/en-us/iaas/Content/KeyManagement/home.htm) with the Secrets Store CSI Driver.