provider "aws" {
  region = "us-east-1"
}

###############################################################################
# Data Sources
###############################################################################

# Dynamically fetch public subnets in the VPC by filtering on the tag "Type" = "public"
data "aws_subnets" "public" {
  filter {
    name   = "vpc-id"
    values = [var.vpc_id]
  }
}

# Fetch the latest Amazon Linux AMI.
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

###############################################################################
# Security Groups
###############################################################################

# Security group for the EC2 instance (backend)
resource "aws_security_group" "ec2_sg" {
  name_prefix = "ec2_sg-"
  description = "Security group for EC2 instance"
  vpc_id      = var.vpc_id

  # Allow SSH for management.
  ingress {
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  # Allow inbound HTTP traffic on port 8080 only from the ALB.
  ingress {
    from_port       = 8080
    to_port         = 8080
    protocol        = "tcp"
    security_groups = [aws_security_group.alb_sg.id]
  }

  # Allow all outbound traffic.
  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

# Security group for the Application Load Balancer.
resource "aws_security_group" "alb_sg" {
  name_prefix = "alb_sg-"
  description = "Security group for Application Load Balancer"
  vpc_id      = var.vpc_id

  # Allow inbound HTTPS traffic on port 443 from anywhere.
  ingress {
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  # Allow all outbound traffic.
  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

###############################################################################
# ALB and Related Resources
###############################################################################

# Application Load Balancer deployed into the dynamic list of public subnets.
resource "aws_lb" "app_alb" {
  name               = "app-alb"
  internal           = false
  load_balancer_type = "application"
  security_groups    = [aws_security_group.alb_sg.id]
  subnets            = data.aws_subnets.public.ids

  tags = {
    Name = "AppALB"
  }
}

# Target group for the Spring Boot backend on port 8080.
resource "aws_lb_target_group" "springboot_tg" {
  # Use a short name prefix (no more than 6 characters) due to AWS restrictions.
  name_prefix = "tg-"
  port        = 8080
  protocol    = "HTTP"
  vpc_id      = var.vpc_id

  health_check {
    healthy_threshold   = 2
    unhealthy_threshold = 2
    timeout             = 5
    interval            = 30
    path                = "/actuator/health"
    matcher             = "200-399"
  }

  tags = {
    Name = "SpringBootTargetGroup"
  }
}

# HTTPS listener on the ALB that uses your ACM certificate.
resource "aws_lb_listener" "https_listener" {
  load_balancer_arn = aws_lb.app_alb.arn
  port              = "443"
  protocol          = "HTTPS"
  ssl_policy        = "ELBSecurityPolicy-2016-08"
  certificate_arn   = var.certificate_arn

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.springboot_tg.arn
  }
}

# Attach the EC2 instance to the target group.
resource "aws_lb_target_group_attachment" "springboot_attachment" {
  target_group_arn = aws_lb_target_group.springboot_tg.arn
  target_id        = module.ec2.instance_id
  port             = 8080
}

###############################################################################
# Modules
###############################################################################

module "ec2" {
  source                = "./modules/ec2"
  ami                   = data.aws_ami.latest_amazon_linux.id
  instance_type         = "t3.small"
  key_name              = var.key_name
  repo_url              = var.repo_url
  oauth_token           = var.oauth_token
  vpc_id                = var.vpc_id
  ec2_security_group_id = aws_security_group.ec2_sg.id
  aws_key               = var.aws_key
  aws_secret            = var.aws_secret
  hume_key              = var.hume_key
  gemini_key            = var.gemini_key

  # RDS parameters.
  db_endpoint = module.rds.rds_endpoint
  db_port     = module.rds.rds_port
  db_username = module.rds.rds_username
  db_password = module.rds.rds_password
}

module "rds" {
  source                = "./modules/rds"
  db_name               = var.db_name
  vpc_id                = var.vpc_id
  ec2_security_group_id = aws_security_group.ec2_sg.id
}

module "amplify" {
  source      = "./modules/amplify"
  repo_url    = var.repo_url
  oauth_token = var.oauth_token
  # Point your Amplify API configuration to the ALB DNS name.
  api_url = "digitaltherapyassistant.click"
}
