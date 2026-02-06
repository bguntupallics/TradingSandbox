output "public_ip" {
  description = "Public IP of the EC2 instance"
  value       = aws_eip.app.public_ip
}

output "ssh_command" {
  description = "SSH command to connect"
  value       = "ssh ec2-user@${aws_eip.app.public_ip}"
}

output "frontend_url" {
  description = "Frontend URL"
  value       = "http://${aws_eip.app.public_ip}"
}

output "api_url" {
  description = "API URL"
  value       = "http://${aws_eip.app.public_ip}:8080"
}

output "instance_id" {
  description = "EC2 instance ID"
  value       = aws_instance.app.id
}
