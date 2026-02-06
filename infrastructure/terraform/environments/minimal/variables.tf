variable "aws_region" {
  description = "AWS region"
  type        = string
  default     = "us-east-1"
}

variable "instance_type" {
  description = "EC2 instance type"
  type        = string
  default     = "t3.small"  # ~$15/month, can run all 3 services
  # Options:
  # - t3.micro:  ~$8/month  (free tier eligible, may struggle with Java)
  # - t3.small:  ~$15/month (recommended minimum)
  # - t3.medium: ~$30/month (comfortable headroom)
}
