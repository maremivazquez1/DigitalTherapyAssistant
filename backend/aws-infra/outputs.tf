output "frontend_url" {
  value = module.amplify.frontend_url
}

output "backend_url" {
  value = module.ec2.backend_url
}

output "rds_endpoint" {
  value = module.rds.endpoint
}
