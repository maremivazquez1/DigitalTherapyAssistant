resource "aws_instance" "springboot_backend" {
  ami           = var.ami
  instance_type = var.instance_type
  key_name      = var.key_name

  user_data = <<-EOF
    #!/bin/bash
    sudo su
    sudo yum update -y
    sudo yum install java-17-amazon-corretto git maven -y
    
    # Inject OAuth token into the repo URL
    REPO_URL=$(echo "${var.repo_url}" | sed "s|https://|https://${var.oauth_token}@|")

    # Clone your GitHub repo
    git clone $REPO_URL /home/ec2-user/app

    # setup right mvn binary version
    cd /opt
    sudo curl -O https://downloads.apache.org/maven/maven-3/3.9.6/binaries/apache-maven-3.9.6-bin.tar.gz
    sudo tar -xvzf apache-maven-3.9.6-bin.tar.gz
    sudo ln -sfn /opt/apache-maven-3.9.6 /opt/maven
    echo 'export M2_HOME=/opt/maven' | sudo tee -a /etc/profile.d/maven.sh
    echo 'export PATH=$M2_HOME/bin:$PATH' | sudo tee -a /etc/profile.d/maven.sh
    source /etc/profile.d/maven.sh

    # Build the Spring Boot application
    cd /home/ec2-user/app/backend
    mvn clean install

    # Run the application
    mvn spring-boot:run
  EOF

  tags = {
    Name = "SpringBootBackend"
  }
}
