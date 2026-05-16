terraform {
  required_version = ">= 1.5.0"

  required_providers {
    kubernetes = {
      source  = "hashicorp/kubernetes"
      version = "~> 2.35"
    }
  }
}

provider "kubernetes" {
  config_path    = var.kubeconfig_path
  config_context = var.kube_context
}

resource "kubernetes_namespace" "workhub" {
  metadata {
    name = var.namespace
  }
}

# PostgreSQL Deployment
resource "kubernetes_deployment" "postgres" {
  metadata {
    name      = "postgres"
    namespace = kubernetes_namespace.workhub.metadata[0].name

    labels = {
      app = "postgres"
    }
  }

  spec {
    replicas = 1

    selector {
      match_labels = {
        app = "postgres"
      }
    }

    template {
      metadata {
        labels = {
          app = "postgres"
        }
      }

      spec {
        container {
          name  = "postgres"
          image = "postgres:15-alpine"

          port {
            container_port = 5432
          }

          env {
            name  = "POSTGRES_DB"
            value = "workhubdb"
          }

          env {
            name  = "POSTGRES_USER"
            value = var.db_username
          }

          env {
            name  = "POSTGRES_PASSWORD"
            value = var.db_password
          }

          readiness_probe {
            exec {
              command = ["pg_isready", "-U", var.db_username, "-d", "workhubdb"]
            }

            initial_delay_seconds = 10
            period_seconds        = 10
          }

          liveness_probe {
            exec {
              command = ["pg_isready", "-U", var.db_username, "-d", "workhubdb"]
            }

            initial_delay_seconds = 30
            period_seconds        = 15
          }
        }
      }
    }
  }
}

# PostgreSQL Service
resource "kubernetes_service" "postgres" {
  metadata {
    name      = "postgres"
    namespace = kubernetes_namespace.workhub.metadata[0].name
  }

  spec {
    type = "ClusterIP"

    selector = {
      app = "postgres"
    }

    port {
      port        = 5432
      target_port = 5432
    }
  }
}

# RabbitMQ Deployment
resource "kubernetes_deployment" "rabbitmq" {
  metadata {
    name      = "rabbitmq"
    namespace = kubernetes_namespace.workhub.metadata[0].name

    labels = {
      app = "rabbitmq"
    }
  }

  spec {
    replicas = 1

    selector {
      match_labels = {
        app = "rabbitmq"
      }
    }

    template {
      metadata {
        labels = {
          app = "rabbitmq"
        }
      }

      spec {
        container {
          name  = "rabbitmq"
          image = "rabbitmq:3-management"

          port {
            name           = "amqp"
            container_port = 5672
          }

          port {
            name           = "management"
            container_port = 15672
          }

          env {
            name  = "RABBITMQ_DEFAULT_USER"
            value = var.rabbitmq_username
          }

          env {
            name  = "RABBITMQ_DEFAULT_PASS"
            value = var.rabbitmq_password
          }

          readiness_probe {
            exec {
              command = ["rabbitmq-diagnostics", "check_port_connectivity"]
            }

            initial_delay_seconds = 30
            period_seconds        = 10
          }

          liveness_probe {
            exec {
              command = ["rabbitmq-diagnostics", "check_running"]
            }

            initial_delay_seconds = 60
            period_seconds        = 15
          }
        }
      }
    }
  }
}

# RabbitMQ Service
resource "kubernetes_service" "rabbitmq" {
  metadata {
    name      = "rabbitmq"
    namespace = kubernetes_namespace.workhub.metadata[0].name
  }

  spec {
    type = "ClusterIP"

    selector = {
      app = "rabbitmq"
    }

    port {
      name        = "amqp"
      port        = 5672
      target_port = 5672
    }

    port {
      name        = "management"
      port        = 15672
      target_port = 15672
    }
  }
}

# Spring Boot App Deployment
resource "kubernetes_deployment" "workhub_app" {
  metadata {
    name      = var.app_name
    namespace = kubernetes_namespace.workhub.metadata[0].name

    labels = {
      app = var.app_name
    }
  }

  spec {
    replicas = var.app_replicas

    selector {
      match_labels = {
        app = var.app_name
      }
    }

    template {
      metadata {
        labels = {
          app = var.app_name
        }
      }

      spec {
        container {
          name  = var.app_name
          image = var.app_image

          image_pull_policy = "IfNotPresent"

          port {
            container_port = var.app_port
          }

          env {
            name  = "DB_URL"
            value = var.db_url
          }

          env {
            name  = "DB_USERNAME"
            value = var.db_username
          }

          env {
            name  = "DB_PASSWORD"
            value = var.db_password
          }

          env {
            name  = "RABBITMQ_HOST"
            value = var.rabbitmq_host
          }

          env {
            name  = "RABBITMQ_PORT"
            value = tostring(var.rabbitmq_port)
          }

          env {
            name  = "RABBITMQ_USERNAME"
            value = var.rabbitmq_username
          }

          env {
            name  = "RABBITMQ_PASSWORD"
            value = var.rabbitmq_password
          }

          env {
            name  = "JWT_SECRET"
            value = var.jwt_secret
          }

          readiness_probe {
            http_get {
              path = "/actuator/health/readiness"
              port = var.app_port
            }

            initial_delay_seconds = 45
            period_seconds        = 10
          }

          liveness_probe {
            http_get {
              path = "/actuator/health/liveness"
              port = var.app_port
            }

            initial_delay_seconds = 90
            period_seconds        = 15
          }
        }
      }
    }
  }

  depends_on = [
    kubernetes_deployment.postgres,
    kubernetes_deployment.rabbitmq,
    kubernetes_service.postgres,
    kubernetes_service.rabbitmq
  ]
}

# Spring Boot App Service
resource "kubernetes_service" "workhub_app" {
  metadata {
    name      = "${var.app_name}-service"
    namespace = kubernetes_namespace.workhub.metadata[0].name
  }

  spec {
    type = var.service_type

    selector = {
      app = var.app_name
    }

    port {
      port        = var.service_port
      target_port = var.app_port
    }
  }
}