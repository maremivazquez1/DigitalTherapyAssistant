resource "aws_instance" "springboot_backend" {
  ami                     = var.ami
  instance_type           = var.instance_type
  key_name                = var.key_name
  vpc_security_group_ids  = [var.ec2_security_group_id]
  
  associate_public_ip_address = true
  
  user_data = <<-EOF
    #!/bin/bash
    sudo su
    sudo dnf update -y
    sudo dnf install java-17-amazon-corretto git maven -y
    sudo dnf install nc -y
    sudo dnf install nodejs npm -y
    
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

    # writing to a file for validation and to run manually
    cat <<EOL | sudo tee /etc/profile.d/springboot_env.sh
    export DB_HOST="${var.db_endpoint}"
    export DB_PORT="${var.db_port}"
    export DB_USER="${var.db_username}"
    export DB_PASSWORD="${var.db_password}"
    export TOKEN=\$(curl -s -X PUT "http://169.254.169.254/latest/api/token" -H "X-aws-ec2-metadata-token-ttl-seconds: 21600")
    export PUBLIC_IP=\$(curl -s "http://169.254.169.254/latest/meta-data/public-ipv4" -H "X-aws-ec2-metadata-token: \$TOKEN")
    EOL


    # Source to include variables in current run
    sudo chmod +x /etc/profile.d/springboot_env.sh
    source /etc/profile.d/springboot_env.sh

    # SSL certificate sign (for HTTPS/WSS)
    keytool -genkeypair -alias sslkey -keyalg RSA -keysize 2048 -storetype PKCS12 -keystore keystore.p12 -validity 365 -storepass mysslpassword -dname "CN=${PUBLIC_IP}, OU=IT, O=DigitalTherapyAssistantCo, L=Orlando, S=Florida, C=US"
    mv keystore.p12 /home/ec2-user/keystore.p12


    host="$${DB_HOST%:*}"
    # Wait for the RDS instance to be available
    until nc -vz $host $DB_PORT; do
      echo "Waiting for database connection on $host:$DB_PORT..."
      sleep 5
    done

    # go to correct branch
    cd /home/ec2-user/app
    git config --global --add safe.directory /home/ec2-user/app
    git checkout pipeline

    # Build and run the Spring Boot application
    cd /home/ec2-user/app/backend
    sed -i "s/localhost/$(curl -s https://169.254.169.254/latest/meta-data/public-ipv4)/g" src/main/resources/static/websocket-cbt-audio.html
    mvn clean install
    mvn spring-boot:run  -Dspring-boot.run.arguments="--spring.datasource.username=$DB_USER --spring.datasource.url=jdbc:mysql://$DB_HOST/cbt --spring.datasource.password=$DB_PASSWORD"
  EOF

  tags = {
    Name = "SpringBootBackend"
  }
}

