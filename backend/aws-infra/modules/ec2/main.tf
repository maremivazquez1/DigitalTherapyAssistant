resource "aws_instance" "springboot_backend" {
  ami           = var.ami
  instance_type = var.instance_type
  key_name      = var.key_name

  user_data = <<-EOF
    #!/bin/bash
    sudo yum update -y
    sudo yum install java-17-amazon-corretto git maven -y
    
    # Inject OAuth token into the repo URL
    REPO_URL=$(echo "${var.repo_url}" | sed "s|https://|https://${var.oauth_token}@|")

    # Clone your GitHub repo
    git clone $REPO_URL /home/ec2-user/app

    # Build the Spring Boot application
    cd /home/ec2-user/app/backend
    mvn -N io.takari:maven:wrapper

    ./mvnw clean package

    # Run the application
    java -jar target/*.jar
  EOF

  tags = {
    Name = "SpringBootBackend"
  }
}
