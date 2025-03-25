resource "aws_db_instance" "rds_instance" {
  count = var.rds_exists ? 0 : 1

  identifier            = "my-dta-db"
  allocated_storage     = 20
  storage_type          = "gp2"
  engine               = "mysql"
  engine_version       = "8.0"
  instance_class       = "db.t3.micro"
  username            = "admin"
  password            = "yourpassword"
  skip_final_snapshot = true
}
