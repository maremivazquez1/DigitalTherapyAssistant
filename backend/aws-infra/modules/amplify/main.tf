resource "aws_amplify_app" "frontend" {
  name        = "local-frontend"
  repository  = var.repo_url
  oauth_token = var.oauth_token  # Use Terraform Cloud secrets for security

  build_spec = <<EOF
version: 1
applications:
  - frontend:
      baseDirectory: frontend
      buildCommand: npm run build
      artifacts:
        baseDirectory: dist
        files:
          - '**/*'
      cache:
        paths:
          - frontend/node_modules/**/*
EOF

  environment_variables = {
    REACT_APP_API_URL = var.api_url
  }
}

resource "aws_amplify_branch" "pipeline" {
  app_id      = aws_amplify_app.frontend.id
  branch_name = "pipeline"
  stage       = "PRODUCTION"

  enable_auto_build = true  # Automatically triggers redeploy on code changes

  environment_variables = {
    REACT_APP_API_URL = var.api_url
  }
}
