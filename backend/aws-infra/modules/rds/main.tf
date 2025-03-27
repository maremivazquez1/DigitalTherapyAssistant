# Create a security group for the RDS instance
resource "aws_security_group" "rds_sg" {
  name        = "rds_sg"
  description = "Allow access to RDS from EC2"
  vpc_id      = var.vpc_id

  ingress {
    from_port       = 3306
    to_port         = 3306
    protocol        = "tcp"
    security_groups = [var.ec2_security_group_id]  # Same EC2 network (vpc)
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

# Create the RDS instance and attach the security group
resource "aws_db_instance" "rds_instance" {
  count                  = var.rds_exists ? 0 : 1
  identifier             = "my-dta-db"
  allocated_storage      = 20
  storage_type           = "gp2"
  engine                 = "mysql"
  engine_version         = "8.0"
  instance_class         = "db.t3.micro"
  username               = "root"
  password               = "Newuser@123"
  db_name                = "my-dta-db"
  skip_final_snapshot    = true

  vpc_security_group_ids = [aws_security_group.rds_sg.id]

}