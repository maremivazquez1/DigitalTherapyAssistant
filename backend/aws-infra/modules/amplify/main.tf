resource "aws_amplify_app" "frontend" {
  name        = "local-frontend"
  repository  = var.repo_url
  oauth_token = var.oauth_token  # Use Terraform Cloud secrets for security

  environment_variables = {
    VITE_API_BASE_URL = var.api_url
  }

  custom_rule {
    source = "/assets/<*>"
    target = "/assets/<*>"
    status = "200"
  }

  custom_rule {
    source = "/<*>"
    target = "/index.html"
    status = "200"
  }

}

resource "aws_amplify_branch" "pipeline" {
  app_id      = aws_amplify_app.frontend.id
  branch_name = "pipeline"
  stage       = "PRODUCTION"

  enable_auto_build = true  # deploy automatically
}
