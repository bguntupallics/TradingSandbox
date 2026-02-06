variable "environment" {
  description = "Environment name (gamma, prod)"
  type        = string
}

variable "services" {
  description = "List of service names to create ECR repositories for"
  type        = list(string)
  default     = ["api", "frontend", "data-acq"]
}
