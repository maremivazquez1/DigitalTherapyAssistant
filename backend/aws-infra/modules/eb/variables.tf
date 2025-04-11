variable "application_name" {
  description = "Name of the Elastic Beanstalk application"
  type        = string
}

variable "environment_name" {
  description = "Name of the Elastic Beanstalk environment"
  type        = string
}

variable "artifact_path" {
  description = "Local path to the ZIP artifact containing the Spring Boot JAR"
  type        = string
}

variable "artifact_key" {
  description = "S3 key where the artifact will be stored"
  type        = string
}

variable "solution_stack_name" {
  description = "The Elastic Beanstalk solution stack name (e.g., '64bit Amazon Linux 2 v3.4.10 running Corretto 17')"
  type        = string
}

variable "db_host" {
  description = "Database host (endpoint)"
  type        = string
}

variable "db_port" {
  description = "Database port"
  type        = string
}

variable "db_user" {
  description = "Database username"
  type        = string
}

variable "db_password" {
  description = "Database password"
  type        = string
}

variable "instance_type" {
  description = "The instance type for the EB environment"
  type        = string
  default     = "t3.micro"
}

variable "s3_bucket" {
  description = "Name of the S3 bucket to store the deployment artifact"
  type        = string
}

variable "vpc_id" {
  description = "The VPC ID where the environment will be launched"
  type        = string
}

variable "security_group_ids" {
  description = "List of security group IDs to assign to the EB environment"
  type        = list(string)
}
