variable "ami" {
  description = "AMI to use for the instance"
  type        = string
}

variable "instance_type" {
  description = "EC2 instance type"
  type        = string
}

variable "key_name" {
  description = "Key pair name for SSH"
  type        = string
}

variable "repo_url" {
  description = "Repository URL for your application"
  type        = string
}

variable "oauth_token" {
  description = "OAuth token for repository access"
  type        = string
}

variable "vpc_id" {
  description = "VPC ID"
  type        = string
}

variable "ec2_security_group_id" {
  description = "The security group ID for this instance"
  type        = string
}

variable "db_endpoint" {
  description = "Database endpoint"
  type        = string
}

variable "db_port" {
  description = "Database port"
  type        = string
}

variable "db_username" {
  description = "Database username"
  type        = string
}

variable "db_password" {
  description = "Database password"
  type        = string
}

variable "aws_key" {
  description = "AWS key name"
  type        = string
}

variable "aws_secret" {
  description = "AWS key"
  type        = string
}

variable "hume_key" {
  description = "HUME key"
  type        = string
}

variable "gemini_key" {
  description = "GEMINI key"
  type        = string
}