output "frontend_url" {
  value = module.amplify.frontend_url
}

output "api_url" {
  value = module.eb.endpoint_url
}

output "rds_endpoint" {
  value = module.rds.rds_endpoint
}
