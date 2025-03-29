provider "aws" {
  region = "us-east-1"
}

resource "aws_security_group" "ec2_sg" {
  name_prefix = "ec2_sg-"
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

  # Allow all outbound traffic
  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}


module "ec2" {
  source                = "./modules/ec2"
  ami                   = data.aws_ami.latest_amazon_linux.id
  instance_type         = "t2.micro"
  key_name              = var.key_name
  repo_url              = var.repo_url
  oauth_token           = var.oauth_token
  vpc_id                = var.vpc_id
  ec2_security_group_id = aws_security_group.ec2_sg.id

  # from rds
  db_endpoint = module.rds.rds_endpoint
  db_port     = module.rds.rds_port
  db_username = module.rds.rds_username
  db_password = module.rds.rds_password
}

module "rds" {
  source                = "./modules/rds"
  db_name               = var.db_name
  rds_exists            = var.rds_exists
  vpc_id                = var.vpc_id
  ec2_security_group_id = aws_security_group.ec2_sg.id
}

module "amplify" {
  source      = "./modules/amplify"
  repo_url    = var.repo_url
  oauth_token = var.oauth_token
  api_url     = "https://${module.ec2.api_url}:8080"
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
