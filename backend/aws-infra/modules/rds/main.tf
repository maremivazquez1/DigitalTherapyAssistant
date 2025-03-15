resource "aws_db_instance" "rds_instance" {
  identifier          = "my-rds-instance"
  engine              = "mysql"
  instance_class      = "db.t4.micro"
  allocated_storage   = 20
  db_name             = var.db_name
  username            = "admin"
  password            = "securepassword"
  skip_final_snapshot = true
}
