variable "aws_region" {
  description = "AWS region"
  type        = string
  default     = "us-east-1"
}

variable "app_name" {
  description = "Name of the data-acq app"
  type        = string
  default     = "data-acq"
}

variable "cpu" {
  description = "Fargate CPU units"
  type        = string
  default     = "256"
}

variable "memory" {
  description = "Fargate memory (MB)"
  type        = string
  default     = "512"
}

variable "container_port" {
  description = "Port the container listens on"
  type        = number
  default     = 80
}

variable "cluster_name" {
  description = "ECS cluster name"
  type        = string
  default     = "data-acq-cluster"
}

variable "desired_count" {
  description = "ECS desired count"
  type        = number
  default     = 1
}
