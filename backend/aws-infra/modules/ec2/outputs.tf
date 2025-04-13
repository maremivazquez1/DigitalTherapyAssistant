output "instance_id" {
  value = aws_instance.springboot_backend.id
}

output "api_url" {
  value = aws_instance.springboot_backend.public_ip
}
