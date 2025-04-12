provider "aws" {
  region = "us-east-1"
}

resource "aws_security_group" "eb_sg" {
  name_prefix = "eb_sg-"
  description = "Security group for EC2 instance"
  vpc_id      = var.vpc_id

  # Allow SSH for management (adjust as needed)
  ingress {
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  # Allow HTTP traffic on port 8080
  ingress {
    from_port   = 8080
    to_port     = 8080
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  # Allow HTTPS traffic on port 8443
  ingress {
    from_port   = 8443
    to_port     = 8443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  # Allow all outbound traffic
  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

module "rds" {
  source                = "./modules/rds"
  db_name               = var.db_name
  vpc_id                = var.vpc_id
  ec2_security_group_id = aws_security_group.eb_sg.id
}

module "eb" {
  source              = "./modules/eb"
  application_name    = "springboot-app"
  environment_name    = "springboot-env"
  artifact_path       = "springboot-app.zip"
  artifact_key        = var.s3_key
  solution_stack_name = "64bit Amazon Linux 2 v3.4.10 running Corretto 17"
  instance_type       = "t3.micro"
  s3_bucket           = var.s3_bucket

  # from rds
  db_host     = module.rds.rds_endpoint
  db_port     = module.rds.rds_port
  db_user     = module.rds.rds_username
  db_password = module.rds.rds_password

  # security
  vpc_id             = var.vpc_id
  security_group_ids = [aws_security_group.eb_sg.id]
}

module "amplify" {
  source      = "./modules/amplify"
  repo_url    = var.repo_url
  oauth_token = var.oauth_token
  api_url     = "${module.eb.endpoint_url}:8443"
}

data "aws_ami" "latest_amazon_linux" {
  most_recent = true
  owners      = ["amazon"]
  filter {
    name   = "name"
    values = ["al2023-ami-2023.6.20250317.2-kernel-6.1-x86_64"]
  }
  filter {
    name   = "virtualization-type"
    values = ["hvm"]
  }
}
