resource "aws_elastic_beanstalk_application" "this" {
  name        = var.application_name
  description = "Elastic Beanstalk application for ${var.application_name}"
}

resource "aws_s3_bucket" "artifact_bucket" {
  bucket = var.s3_bucket
}

resource "aws_s3_bucket_acl" "artifact_bucket_acl" {
  bucket = aws_s3_bucket.artifact_bucket.id
  acl    = "private"
}

resource "aws_s3_object" "artifact" {
  bucket = aws_s3_bucket.artifact_bucket.bucket
  key    = var.artifact_key
  source = var.artifact_path
  etag   = filemd5(var.artifact_path)
}

resource "aws_elastic_beanstalk_application_version" "this" {
  name        = "v1"
  application = aws_elastic_beanstalk_application.this.name
  bucket      = aws_s3_bucket.artifact_bucket.bucket
  key         = aws_s3_object.artifact.key
  description = "Application version v1"
}

resource "aws_elastic_beanstalk_environment" "this" {
  name                = "CBTbackend"
  application         = aws_elastic_beanstalk_application.this.name
  solution_stack_name = "64bit Amazon Linux 2023 v4.18.0 running Corretto 21"
  version_label       = aws_elastic_beanstalk_application_version.this.name

  # Pass in application environment variables, for example your DB settings.
  setting {
    namespace = "aws:elasticbeanstalk:application:environment"
    name      = "DB_HOST"
    value     = var.db_host
  }
  
  setting {
    namespace = "aws:elasticbeanstalk:application:environment"
    name      = "DB_PORT"
    value     = var.db_port
  }
  
  setting {
    namespace = "aws:elasticbeanstalk:application:environment"
    name      = "DB_USER"
    value     = var.db_user
  }
  
  setting {
    namespace = "aws:elasticbeanstalk:application:environment"
    name      = "DB_PASSWORD"
    value     = var.db_password
  }
  
  setting {
    namespace = "aws:autoscaling:launchconfiguration"
    name      = "InstanceType"
    value     = var.instance_type
  }
  
  # VPC Configuration
  setting {
    namespace = "aws:eb:vpc"
    name      = "VPCId"
    value     = var.vpc_id
  }

  setting {
    namespace = "aws:eb:vpc"
    name      = "SecurityGroups"
    value     = join(",", var.security_group_ids)
  }
}
