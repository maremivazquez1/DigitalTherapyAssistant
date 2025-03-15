provider "aws" {
  region = "us-east-1"
}

module "amplify" {
  source = "./modules/amplify"
  repository  = var.repo_url
  oauth_token = var.oauth_token  # Use Terraform Cloud secrets for security
  api_url = var.api_url
}

module "ec2" {
  source = "./modules/ec2"
  key_name = var.key_name
}

module "rds" {
  source = "./modules/rds"
  db_name = var.db_name
}
