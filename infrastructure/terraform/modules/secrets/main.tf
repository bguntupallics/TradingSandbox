resource "aws_secretsmanager_secret" "app_secrets" {
  name        = "tradingsandbox/${var.environment}/app"
  description = "Application secrets for TradingSandbox ${var.environment}"

  tags = {
    Environment = var.environment
  }
}

# Note: Secret values should be set via AWS Console or CLI, not in Terraform
# Example structure for the secret JSON:
# {
#   "db_password": "...",
#   "jwt_secret": "...",
#   "fastapi_access_key": "...",
#   "admin_password": "..."
# }
