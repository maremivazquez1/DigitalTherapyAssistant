resource "aws_db_instance" "rds_instance" {
  identifier          = "my-dta-db"
  engine              = "mysql"
  instance_class      = "db.t4g.micro"
  allocated_storage   = 20
  max_allocated_storage = 100  # Max storage to scale up to (in GB)
  db_name             = var.db_name
  username            = "admin"
  password            = "securepassword"
  skip_final_snapshot = true
  backup_retention_period = 1
}
