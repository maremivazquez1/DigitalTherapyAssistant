output "rds_endpoint" {
  value = length(aws_db_instance.rds_instance) > 0 ? aws_db_instance.rds_instance[0].endpoint : null
}

output "rds_port" {
  value = length(aws_db_instance.rds_instance) > 0 ? aws_db_instance.rds_instance[0].port : null
}

output "rds_username" {
  value = length(aws_db_instance.rds_instance) > 0 ? aws_db_instance.rds_instance[0].username : null
}

output "rds_password" {
  value = length(aws_db_instance.rds_instance) > 0 ? aws_db_instance.rds_instance[0].password : null
}
