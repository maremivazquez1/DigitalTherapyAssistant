resource "aws_amplify_app" "frontend" {
  name        = "react-frontend"
  repository  = var.repo_url
  oauth_token = var.oauth_token  # Use Terraform Cloud secrets for security

  build_spec = <<EOF
version: 1
applications:
  - frontend:
      buildCommand: npm run build
      artifacts:
        baseDirectory: build
        files:
          - '**/*'
      cache:
        paths:
          - node_modules/**/*
EOF

  environment_variables = {
    REACT_APP_API_URL = var.api_url
  }
}
