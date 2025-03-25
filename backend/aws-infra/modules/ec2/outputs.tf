output "api_url" {
  value = aws_instance.springboot_backend.private_ip
}

output "backend_instance_id" {
  value = aws_instance.springboot_backend.id
}

output "ec2_security_group_id" {
  value = aws_security_group.ec2_sg.id
}
