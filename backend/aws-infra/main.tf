provider "aws" {
  region = "us-east-1"
}

module "amplify" {
  source      = "./modules/amplify"
  repo_url    = var.repo_url
  oauth_token = var.oauth_token # Use Terraform Cloud secrets for security
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

# check if key already exists and let count be 0 
# in the next sections to skip creating another
data "aws_key_pair" "existing" {
  key_name = "var.key_name"
}

# private key is used to ssh (will be output to console)
# Remove output when no longer need to ssh
resource "tls_private_key" "key" {
  count     = length(data.aws_key_pair.existing) > 0 ? 0 : 1
  algorithm = "RSA"
  rsa_bits  = 2048
}

# generate public key to authenticate to ec2
resource "aws_key_pair" "my_ssh_key" {
  count      = length(data.aws_key_pair.existing) > 0 ? 0 : 1
  key_name   = var.key_name
  public_key = tls_private_key.key[0].public_key_openssh
  depends_on = [tls_private_key.key]
}

module "ec2" {
  source        = "./modules/ec2"
  ami           = data.aws_ami.latest_amazon_linux.id # Dynamically pass the latest AMI ID
  instance_type = "t2.micro"
  key_name      = var.key_name
  repo_url      = var.repo_url
  oauth_token   = var.oauth_token
  depends_on    = [aws_key_pair.my_ssh_key]
}

module "rds" {
  source  = "./modules/rds"
  db_name = var.db_name
}
