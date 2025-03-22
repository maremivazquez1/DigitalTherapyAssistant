data "aws_db_instance" "existing" {
  db_instance_identifier = "my-dta-db"
}

resource "aws_db_instance" "rds_instance" {
  count = length(data.aws_db_instance.existing) > 0 ? 0 : 1

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
