output "alb_dns_name" {
  description = "ALB DNS to reach the data-acq service"
  value       = aws_lb.alb.dns_name
}
