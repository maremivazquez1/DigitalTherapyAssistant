provider "aws" {
  region = "us-east-1"
}

module "ec2" {
  source        = "./modules/ec2"
  ami           = data.aws_ami.latest_amazon_linux.id
  instance_type = "t2.micro"
  key_name      = var.key_name
  repo_url      = var.repo_url
  oauth_token   = var.oauth_token
  vpc_id        = var.vpc_id

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
  ec2_security_group_id = module.ec2.ec2_security_group_id
}

module "amplify" {
  source      = "./modules/amplify"
  repo_url    = var.repo_url
  oauth_token = var.oauth_token
  api_url     = "http://${module.ec2.api_url}:8080"
}

data "aws_ami" "latest_amazon_linux" {
  most_recent = true
  owners      = ["amazon"]
  filter {
    name   = "name"
    values = ["amzn2-ami-hvm-*-x86_64-gp2"]
  }
  filter {
    name   = "virtualization-type"
    values = ["hvm"]
  }
}
