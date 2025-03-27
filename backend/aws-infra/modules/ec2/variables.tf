variable "ami" {}
variable "instance_type" {}
variable "repo_url" {}
variable "oauth_token" {}
variable "key_name" {}
variable "vpc_id" {description = "security group"}

# rds variables
variable "db_endpoint" {}
variable "db_port" {}
variable "db_username" {}
variable "db_password" {}
