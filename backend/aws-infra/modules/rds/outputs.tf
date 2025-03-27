output "rds_endpoint" {
  value = aws_db_instance.rds_instance.endpoint 
}

output "rds_port" {
  value = aws_db_instance.rds_instance.port 
}

output "rds_username" {
  value = aws_db_instance.rds_instance.username
}

output "rds_password" {
  value = aws_db_instance.rds_instance.password
}
