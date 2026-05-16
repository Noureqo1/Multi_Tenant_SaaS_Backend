output "namespace_name" {
  description = "The Kubernetes namespace created for WorkHub."
  value       = kubernetes_namespace.workhub.metadata[0].name
}

output "app_deployment_name" {
  description = "The Kubernetes deployment name for the WorkHub backend."
  value       = kubernetes_deployment.workhub_app.metadata[0].name
}

output "app_service_name" {
  description = "The Kubernetes service name for the WorkHub backend."
  value       = kubernetes_service.workhub_app.metadata[0].name
}

output "app_service_type" {
  description = "The Kubernetes service type."
  value       = kubernetes_service.workhub_app.spec[0].type
}

output "app_service_port" {
  description = "The service port exposed for the backend."
  value       = kubernetes_service.workhub_app.spec[0].port[0].port
}