variable "kubeconfig_path" {
  description = "Path to the local kubeconfig file used by Terraform to connect to Kubernetes."
  type        = string
  default     = "~/.kube/config"
}

variable "kube_context" {
  description = "Kubernetes context name. For Docker Desktop, this is usually docker-desktop."
  type        = string
  default     = "docker-desktop"
}

variable "namespace" {
  description = "Kubernetes namespace for WorkHub resources."
  type        = string
  default     = "workhub"
}

variable "app_name" {
  description = "Name of the WorkHub backend application."
  type        = string
  default     = "workhub-app"
}

variable "app_image" {
  description = "Docker image used for the WorkHub backend."
  type        = string
  default     = "multi_tenant_saas_backend2-app:latest"
}

variable "app_replicas" {
  description = "Number of backend application replicas."
  type        = number
  default     = 1
}

variable "app_port" {
  description = "Container port used by the Spring Boot backend."
  type        = number
  default     = 8082
}

variable "service_port" {
  description = "Kubernetes service port for the backend."
  type        = number
  default     = 8082
}

variable "service_type" {
  description = "Kubernetes service type."
  type        = string
  default     = "NodePort"
}

variable "db_url" {
  description = "Database JDBC URL used by the backend."
  type        = string
  default     = "jdbc:postgresql://postgres:5432/workhubdb"
}

variable "db_username" {
  description = "Database username."
  type        = string
  default     = "postgres"
}

variable "db_password" {
  description = "Database password."
  type        = string
  default     = "postgres"
  sensitive   = true
}

variable "rabbitmq_host" {
  description = "RabbitMQ host used by the backend."
  type        = string
  default     = "rabbitmq"
}

variable "rabbitmq_port" {
  description = "RabbitMQ AMQP port."
  type        = number
  default     = 5672
}

variable "rabbitmq_username" {
  description = "RabbitMQ username."
  type        = string
  default     = "guest"
}

variable "rabbitmq_password" {
  description = "RabbitMQ password."
  type        = string
  default     = "guest"
  sensitive   = true
}

variable "jwt_secret" {
  description = "JWT secret used by the backend."
  type        = string
  default     = "U29tZVN1cGVyU2VjcmV0S2V5U29tZVN1cGVyU2VjcmV0S2V5MTIzNDU2"
  sensitive   = true
}