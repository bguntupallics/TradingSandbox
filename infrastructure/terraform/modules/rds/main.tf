resource "aws_db_subnet_group" "main" {
  name       = "tradingsandbox-${var.environment}"
  subnet_ids = var.subnet_ids

  tags = {
    Name        = "tradingsandbox-${var.environment}"
    Environment = var.environment
  }
}

resource "aws_security_group" "rds" {
  name   = "tradingsandbox-rds-${var.environment}"
  vpc_id = var.vpc_id

  ingress {
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"
    security_groups = var.allowed_security_groups
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name        = "tradingsandbox-rds-${var.environment}"
    Environment = var.environment
  }
}

resource "aws_db_parameter_group" "main" {
  family = "postgres15"
  name   = "tradingsandbox-${var.environment}"

  parameter {
    name  = "log_statement"
    value = var.environment == "prod" ? "ddl" : "all"
  }

  tags = {
    Environment = var.environment
  }
}

resource "random_password" "db_password" {
  length  = 32
  special = false
}

resource "aws_db_instance" "main" {
  identifier = "tradingsandbox-${var.environment}"

  engine         = "postgres"
  engine_version = "15"
  instance_class = var.instance_class

  allocated_storage     = var.allocated_storage
  max_allocated_storage = var.allocated_storage * 2
  storage_type          = "gp3"
  storage_encrypted     = true

  db_name  = var.db_name
  username = "tradingsandbox"
  password = random_password.db_password.result

  db_subnet_group_name   = aws_db_subnet_group.main.name
  vpc_security_group_ids = [aws_security_group.rds.id]
  parameter_group_name   = aws_db_parameter_group.main.name

  backup_retention_period = var.environment == "prod" ? 7 : 1
  backup_window           = "03:00-04:00"
  maintenance_window      = "Mon:04:00-Mon:05:00"

  skip_final_snapshot       = var.environment != "prod"
  final_snapshot_identifier = var.environment == "prod" ? "tradingsandbox-prod-final" : null
  deletion_protection       = var.environment == "prod"

  performance_insights_enabled = var.environment == "prod"

  tags = {
    Name        = "tradingsandbox-${var.environment}"
    Environment = var.environment
  }
}
