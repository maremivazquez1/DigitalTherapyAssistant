output "rds_endpoint" {
  value = length(aws_db_instance.rds_instance) > 0 ? aws_db_instance.rds_instance[0].endpoint : null
}
