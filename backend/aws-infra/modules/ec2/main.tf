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
    sudo dnf install redis6 -y

    # Start redis server
    sudo systemctl enable redis6
    sudo systemctl start redis6

    # Install ffmpeg (statically bc it's not available on dnf)
    wget https://github.com/BtbN/FFmpeg-Builds/releases/download/latest/ffmpeg-master-latest-linux64-gpl.tar.xz
    sudo mkdir -p /usr/local/ffmpeg
    sudo tar -xJf ffmpeg-master-latest-*.tar.xz -C /usr/local/ffmpeg --strip-components=1
    sudo ln -s /usr/local/ffmpeg/bin/ffmpeg /usr/bin/ffmpeg
    sudo ln -s /usr/local/ffmpeg/bin/ffprobe /usr/bin/ffprobe
    FFMPEG_PATH=/usr/bin/ffmpeg
    
    # Inject OAuth token into the repo URL
    REPO_URL=$(echo "${var.repo_url}" | sed "s|https://|https://${var.oauth_token}@|")
    TOKEN=$(curl -s -X PUT "http://169.254.169.254/latest/api/token" -H "X-aws-ec2-metadata-token-ttl-seconds: 21600")
    PUBLIC_IP=$(curl -s "http://169.254.169.254/latest/meta-data/public-ipv4" -H "X-aws-ec2-metadata-token: $TOKEN")

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
    export AWS_ACCESS_KEY_ID="${var.aws_key}"
    export AWS_SECRET_ACCESS_KEY="${var.aws_secret}"
    export AWS_DEFAULT_REGION=us-east-1
    export DB_HOST="${var.db_endpoint}"
    export DB_PORT="${var.db_port}"
    export DB_USER="${var.db_username}"
    export DB_PASSWORD="${var.db_password}"
    export HUME_API_KEY="${var.hume_key}"
    export GEMINI_API_KEY="${var.gemini_key}"
    export PUBLIC_IP=$${PUBLIC_IP}
    export JAVA_TOOL_OPTIONS="-Xmx1g" # prevents running out of heap (or use image with more RAM)
    export FFMPEG_PATH=$${FFMPEG_PATH}
    EOL

    # Source to include variables in current run
    sudo chmod +x /etc/profile.d/springboot_env.sh
    source /etc/profile.d/springboot_env.sh

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
    mvn spring-boot:run
  EOF

  tags = {
    Name = "SpringBootBackend"
  }
}

