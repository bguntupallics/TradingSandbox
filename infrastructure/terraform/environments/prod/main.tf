terraform {
  required_version = ">= 1.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
    random = {
      source  = "hashicorp/random"
      version = "~> 3.0"
    }
  }

  backend "s3" {
    bucket         = "tradingsandbox-tf-state-899607431870"
    key            = "prod/terraform.tfstate"
    region         = "us-east-1"
    encrypt        = true
    dynamodb_table = "terraform-locks"
  }
}

provider "aws" {
  region = var.aws_region

  default_tags {
    tags = {
      Project     = "TradingSandbox"
      Environment = "prod"
      ManagedBy   = "terraform"
    }
  }
}

locals {
  environment = "prod"
}

# VPC (no NAT Gateway - saves ~$33/month)
module "vpc" {
  source      = "../../modules/vpc"
  environment = local.environment
  vpc_cidr    = "10.2.0.0/16"
}

# ECR Repositories
module "ecr" {
  source      = "../../modules/ecr"
  environment = local.environment
  services    = ["api", "frontend", "data-acq"]
}

# Secrets
module "secrets" {
  source      = "../../modules/secrets"
  environment = local.environment
}

# ECS Cluster (disabled container insights to save costs)
resource "aws_ecs_cluster" "main" {
  name = "tradingsandbox-${local.environment}"

  setting {
    name  = "containerInsights"
    value = "disabled"
  }

  tags = {
    Environment = local.environment
  }
}

# ALB
module "alb" {
  source            = "../../modules/alb"
  environment       = local.environment
  vpc_id            = module.vpc.vpc_id
  public_subnet_ids = module.vpc.public_subnet_ids
}

# RDS - minimal size for personal use
module "rds" {
  source                  = "../../modules/rds"
  environment             = local.environment
  vpc_id                  = module.vpc.vpc_id
  subnet_ids              = module.vpc.public_subnet_ids
  allowed_security_groups = [module.alb.security_group_id]
  instance_class          = "db.t3.micro"
  allocated_storage       = 20
  db_name                 = "tradingsandbox_prod"
}

# API Service - minimal size
module "api_service" {
  source                = "../../modules/ecs-service"
  environment           = local.environment
  service_name          = "api"
  vpc_id                = module.vpc.vpc_id
  subnet_ids            = module.vpc.public_subnet_ids
  cluster_id            = aws_ecs_cluster.main.id
  ecr_repository_url    = module.ecr.repository_urls["api"]
  target_group_arn      = module.alb.target_group_arns["api"]
  alb_security_group_id = module.alb.security_group_id
  secrets_arn           = module.secrets.secrets_arn
  container_port        = 8080
  health_check_path     = "/actuator/health"
  cpu                   = "256"
  memory                = "512"
  desired_count         = 1

  environment_variables = {
    SPRING_PROFILES_ACTIVE = "prod"
    RDS_HOSTNAME           = module.rds.endpoint
    RDS_PORT               = "5432"
    RDS_DB_NAME            = "tradingsandbox_prod"
    RDS_USERNAME           = module.rds.username
    FASTAPI_BASE_URL       = "http://${module.alb.alb_dns_name}/data"
  }

  secrets_mapping = {
    RDS_PASSWORD       = "db_password"
    JWT_SECRET         = "jwt_secret"
    FASTAPI_ACCESS_KEY = "fastapi_access_key"
    ADMIN_USERNAME     = "admin_username"
    ADMIN_PASSWORD     = "admin_password"
    ADMIN_FIRST_NAME   = "admin_first_name"
    ADMIN_LAST_NAME    = "admin_last_name"
    ADMIN_EMAIL        = "admin_email"
  }
}

# Frontend Service - minimal size
module "frontend_service" {
  source                = "../../modules/ecs-service"
  environment           = local.environment
  service_name          = "frontend"
  vpc_id                = module.vpc.vpc_id
  subnet_ids            = module.vpc.public_subnet_ids
  cluster_id            = aws_ecs_cluster.main.id
  ecr_repository_url    = module.ecr.repository_urls["frontend"]
  target_group_arn      = module.alb.target_group_arns["frontend"]
  alb_security_group_id = module.alb.security_group_id
  secrets_arn           = module.secrets.secrets_arn
  container_port        = 80
  health_check_path     = "/health"
  cpu                   = "256"
  memory                = "512"
  desired_count         = 1

  environment_variables = {
    VITE_API_BASE_URL = "http://${module.alb.alb_dns_name}/api"
  }

  secrets_mapping = {}
}

# Data Acquisition Service - minimal size
module "data_acq_service" {
  source                = "../../modules/ecs-service"
  environment           = local.environment
  service_name          = "data-acq"
  vpc_id                = module.vpc.vpc_id
  subnet_ids            = module.vpc.public_subnet_ids
  cluster_id            = aws_ecs_cluster.main.id
  ecr_repository_url    = module.ecr.repository_urls["data-acq"]
  target_group_arn      = module.alb.target_group_arns["data-acq"]
  alb_security_group_id = module.alb.security_group_id
  secrets_arn           = module.secrets.secrets_arn
  container_port        = 8000
  health_check_path     = "/health"
  cpu                   = "256"
  memory                = "512"
  desired_count         = 1

  environment_variables = {
    POSTGRES_HOST = module.rds.endpoint
    POSTGRES_PORT = "5432"
    POSTGRES_DB   = "tradingsandbox_prod"
    POSTGRES_USER = module.rds.username
  }

  secrets_mapping = {
    POSTGRES_PASSWORD  = "db_password"
    ALPACA_API_KEY     = "alpaca_api_key"
    ALPACA_API_SECRET  = "alpaca_api_secret"
    FASTAPI_ACCESS_KEY = "fastapi_access_key"
  }
}
