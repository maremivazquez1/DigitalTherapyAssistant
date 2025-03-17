output "frontend_url" {
  value = module.amplify.frontend_url
}

output "api_url" {
  value = module.ec2.api_url
}

output "private_key" {
  value     = tls_private_key.key.private_key_pem
  sensitive = true
}

output "rds_endpoint" {
  value = module.rds.rds_endpoint
}
