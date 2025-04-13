variable "vpc_id" {
  description = "The VPC ID where resources are deployed."
  type        = string
}

variable "key_name" {
  description = "The EC2 key pair name for SSH access."
  type        = string
}

variable "repo_url" {
  description = "The Git repository URL for your application."
  type        = string
}

variable "oauth_token" {
  description = "OAuth token for accessing your Git repository."
  type        = string
}

variable "certificate_arn" {
  description = "The ARN of the ACM certificate to use for the ALB."
  type        = string
}

variable "db_name" {
  description = "Database name for RDS."
  type        = string
}

variable "rds_exists" {
  description = "Flag to indicate if an RDS instance already exists."
  type        = bool
}
