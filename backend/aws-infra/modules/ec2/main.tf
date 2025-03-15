resource "aws_instance" "springboot_backend" {
  ami           = "ami-0abcdef1234567890"
  instance_type = "t3.medium"
  key_name      = var.key_name

  user_data = <<-EOF
    #!/bin/bash
    sudo yum update -y
    sudo yum install java-17-amazon-corretto -y
    aws s3 cp s3://your-s3-bucket/springboot-app.jar /home/ec2-user/app.jar
    java -jar /home/ec2-user/app.jar
  EOF

  tags = {
    Name = "SpringBootBackend"
  }
}
