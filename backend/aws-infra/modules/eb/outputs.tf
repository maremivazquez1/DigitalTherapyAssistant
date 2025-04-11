output "eb_application_name" {
  description = "Name of the Elastic Beanstalk application"
  value       = aws_elastic_beanstalk_application.this.name
}

output "eb_environment_name" {
  description = "Name of the Elastic Beanstalk environment"
  value       = aws_elastic_beanstalk_environment.this.name
}

output "eb_application_version" {
  description = "The application version label deployed on Elastic Beanstalk"
  value       = aws_elastic_beanstalk_application_version.this.name
}

output "endpoint_url" {
  description = "The endpoint URL of the EB environment's load balancer"
  value       = aws_elastic_beanstalk_environment.this.endpoint_url
}
