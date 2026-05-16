# Terraform Infrastructure as Code - WorkHub

This folder contains the Terraform configuration for deploying WorkHub resources to a Kubernetes cluster.

This project uses the Kubernetes Track for IaC. Terraform provisions Kubernetes resources instead of creating them manually.

---

## What Terraform Creates

Terraform creates the following Kubernetes resources:

- WorkHub namespace
- PostgreSQL Deployment and Service
- RabbitMQ Deployment and Service
- Spring Boot backend Deployment and Service
- Readiness probes for PostgreSQL, RabbitMQ, and the backend
- Liveness probes for PostgreSQL, RabbitMQ, and the backend
- Environment variables for database, RabbitMQ, and JWT configuration

---

## Prerequisites

Make sure the following tools are installed:

- Terraform
- Docker Desktop
- Docker Desktop Kubernetes enabled
- kubectl

Check Terraform:

```powershell
terraform -version
```

Check Kubernetes:

```powershell
kubectl config get-contexts
kubectl cluster-info
kubectl get nodes
```

Expected Kubernetes context for Docker Desktop:

```text
docker-desktop
```

---

## Files

| File | Purpose |
|---|---|
| `main.tf` | Defines the Kubernetes provider, namespace, PostgreSQL, RabbitMQ, backend deployments, and services |
| `variables.tf` | Defines reusable variables |
| `outputs.tf` | Prints created resource names and service information |
| `terraform.tfvars.example` | Example variable values with no production secrets |
| `README.md` | Instructions for running Terraform |

---

## Initialize Terraform

From the `terraform/` folder, run:

```powershell
terraform init
```

---

## Format and Validate Terraform Files

Format the Terraform files:

```powershell
terraform fmt
```

Validate the Terraform configuration:

```powershell
terraform validate
```

Expected result:

```text
Success! The configuration is valid.
```

---

## Preview the Infrastructure Plan

```powershell
terraform plan
```

Or using the example variables file:

```powershell
terraform plan -var-file="terraform.tfvars.example"
```

Expected plan summary:

```text
Plan: 7 to add, 0 to change, 0 to destroy.
```

---

## Apply the Infrastructure

```powershell
terraform apply
```

Or using the example variables file:

```powershell
terraform apply -var-file="terraform.tfvars.example"
```

Type `yes` when Terraform asks for confirmation.

Expected apply result:

```text
Apply complete! Resources: 7 added, 0 changed, 0 destroyed.
```

---

## Verify Resources

Check the namespace:

```powershell
kubectl get namespaces
```

Check all resources inside the namespace:

```powershell
kubectl get all -n workhub
```

Check the deployments:

```powershell
kubectl get deployment -n workhub
```

Check the services:

```powershell
kubectl get service -n workhub
```

Example expected result:

```text
pod/postgres      1/1 Running
pod/rabbitmq      1/1 Running
pod/workhub-app   1/1 Running
```

The expected services are:

```text
service/postgres
service/rabbitmq
service/workhub-app-service
```

---

## Verify the Backend Health Endpoint

The backend service is exposed using a Kubernetes NodePort service.

First, get the service information:

```powershell
kubectl get service workhub-app-service -n workhub
```

Then open the health endpoint using the displayed NodePort:

```text
http://localhost:<NODE_PORT>/actuator/health
```

Example:

```text
http://localhost:30486/actuator/health
```

Expected result:

```json
{
  "status": "UP"
}
```

The health output should confirm that PostgreSQL, RabbitMQ, readiness, and liveness are all UP.

---

## Destroy the Infrastructure

To remove the resources created by Terraform:

```powershell
terraform destroy
```

Or:

```powershell
terraform destroy -var-file="terraform.tfvars.example"
```

Type `yes` when Terraform asks for confirmation.

---

## Why Terraform?

Terraform allows infrastructure to be written as code instead of being created manually. This makes the infrastructure:

- Reproducible
- Reviewable through Git
- Easier to version
- Less likely to drift from the documented configuration

Manual infrastructure changes can be forgotten or applied inconsistently. Terraform helps prevent that by keeping the desired infrastructure state in code.

---

## Notes

This setup is designed for local Docker Desktop Kubernetes. For production or cloud deployment, variables such as image name, secrets, database host, and service type should be changed.

The current local setup creates PostgreSQL and RabbitMQ inside the Kubernetes namespace for demonstration and testing purposes.