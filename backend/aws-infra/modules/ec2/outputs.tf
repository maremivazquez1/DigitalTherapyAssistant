output "api_url" {
  value = aws_instance.springboot_backend.private_ip
}

output "backend_instance_id" {
  value = aws_instance.springboot_backend.id
}
