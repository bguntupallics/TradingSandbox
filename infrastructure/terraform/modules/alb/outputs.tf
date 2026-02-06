output "alb_arn" {
  description = "ARN of the ALB"
  value       = aws_lb.main.arn
}

output "alb_dns_name" {
  description = "DNS name of the ALB"
  value       = aws_lb.main.dns_name
}

output "alb_zone_id" {
  description = "Zone ID of the ALB"
  value       = aws_lb.main.zone_id
}

output "security_group_id" {
  description = "Security group ID for ALB"
  value       = aws_security_group.alb.id
}

output "target_group_arns" {
  description = "Map of target group ARNs"
  value = {
    api      = aws_lb_target_group.api.arn
    frontend = aws_lb_target_group.frontend.arn
    data-acq = aws_lb_target_group.data_acq.arn
  }
}
