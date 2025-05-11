# 🧠 Digital Therapy Assistant

A full-stack web application that provides Cognitive Behavioral Therapy (CBT) sessions using AI-driven interactions. The app includes CBT sessions through multimodal analysis of voice, video and text, and burnout assessments.

## 🚀 Project Structure

```
DIGITALTHERAPYASSISTANT/
│── backend/          # Backend (Spring Boot)
│   ├── src/         # Java source code
│   ├── target/      # Compiled files (ignored in Git)
│   ├── pom.xml      # Maven configuration
│── frontend/         # Frontend (React + Vite)
│   ├── src/         # React source code
│   ├── public/      # Static assets
│   ├── node_modules/ # Dependencies (ignored in Git)
│   ├── package.json  # Project configuration
│── .gitignore        # Git ignore rules
│── README.md         # Project documentation
```

---

## 🛠️ Prerequisites

- **Operating System**: macOS
- **Java JDK** (latest version)
- **Node.js** (latest LTS version)
- **Maven**
- **MySQL**
- **Homebrew** (for macOS)

---

## ⚙️ API Keys

This project requires the use of **API keys**, specifically:
- Gemini API key (https://ai.google.dev/gemini-api/docs/api-key)
- Hume API key (https://dev.hume.ai/docs/introduction/api-key)
- AWS API 'Access ID' and 'Secret' keys (https://docs.aws.amazon.com/IAM/latest/UserGuide/id_credentials_access-keys.html)
- Github OAuth key, which is only needed for the deployment to pull the repo. Not needed for local runs.

Before running the backend locally, make sure to add your keys to `variables.sh` and run `source variables.sh` on the same terminal (or add them to your profile environment variables). Just be careful not to commit the filled out file with access keys!
See Deployments section for more information on how to deploy.

## ⚙️ Setup Instructions

To quickly set up both the backend and frontend, run the provided `setup.sh` script:

```bash
chmod +x setup.sh  # Grant execution permissions
./setup.sh         # Run the setup script
```

This script will:
- Install necessary dependencies (Java, Node.js, Maven, MySQL)
- Set up the backend and frontend environments
- Configure the database
- Install required frontend libraries (Tailwind CSS, DaisyUI, Vite)

If you prefer to install everything manually, follow the steps below.

### 1️⃣ Manual Backend Setup (if not using setup.sh)
#### **Install Dependencies**
# 1. Install via Homebrew
```
brew update
brew install openjdk maven mysql redis ffmpeg
```
# 2. Start services
```
brew services start mysql
brew services start redis
```
# 3. Configure MySQL schema
```
mysql -u root <<EOF
CREATE DATABASE IF NOT EXISTS cbt;
USE cbt;
DROP TABLE IF EXISTS users;
CREATE TABLE users (
  user_id       INT AUTO_INCREMENT PRIMARY KEY,
  email         VARCHAR(255) NOT NULL UNIQUE,
  password      VARCHAR(255) NOT NULL,
  first_name    VARCHAR(255),
  last_name     VARCHAR(255),
  phone         VARCHAR(20),
  date_of_birth DATE,
  created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
EOF
```
# 4. Build and run
```
cd backend 
mvn spring-boot:run -Dnet.bytebuddy.experimental=true
```

### 2️⃣ Manual Frontend Setup (if not using setup.sh)
#### **Install Dependencies**
`cd frontend`

# 1. Install Node.js if needed
`brew install node`

# 2. Install dependencies
```
npm install
npm install tailwindcss @tailwindcss/vite daisyui vite --save-dev
```
# 3. Run dev server
```npm run dev```


## 🔥 Planned Features

- 📊 **AI-powered CBT sessions**
- 🗣️ **Voice-based interaction**
- 📔 **Guided journaling**
- 📉 **Burnout assessment**
- 📅 **Daily check-ins**

---

## 🛠️ Tech Stack

### **Backend:**
- 🖥 **Spring Boot** (Java)
- 🛢 **MySQL** (Database)
- ⚙ **Maven** (Build tool)

### **Frontend:**
- ⚛ **React + Vite** (JS Framework)
- 🎨 **Tailwind CSS** (Styling)
- 🌼 **DaisyUI** (UI Components)

---

## 🚀 Deployment with Terraform on AWS

### Deployment Overview
- Terraform deploys the backend on an **EC2 instance** and the React frontend on **Amplify**.
- A **load balancer** is set up to redirect HTTPS traffic from the frontend to the backend.

### Terraform Configuration
- The Terraform configuration is located in the `pipeline` branch.
- Updates to this branch trigger a **pipeline deployment**.  
  Alternatively, you can manually trigger a deployment by rerunning the Terraform pipeline in **GitHub Actions**.

### Pipeline Branch Details
The `pipeline` branch contains:  
- Terraform `.yml` files.  
- Additional changes from the `main` branch related to:
  - HTTP requests.
  - Environment variables for `applications.properties`.

### Required AWS and LLM Keys
The following keys are required and are stored as **GitHub secrets**:
- `AWS_ACCESS_KEY_ID`
- `AWS_SECRET_ACCESS_KEY`
- `AWS_REGION`
- `GEMINI_API_KEY`
- `HUME_API_KEY`
- `OAUTH_TOKEN_GITHUB` (GitHub OAuth token for pulling the repository)

### Terraform Variables
Within `aws-infra/terraform.tfvars`, Terraform requires the following variables:
- `certificate_arn`: Provided by **Amazon Certificate Manager (ACM)**.
- `VPC`: AWS Virtual Private Cloud, configurable in the AWS console.
- `db_name`: RDS database name, defaulted to `cbt`.
- `key_name`: Key created in EC2, and used to create the EC2 instance.

> **Note**: These variables only need to be set once for the AWS account.

### Additional Deployment Requirements
To redirect traffic and complete the setup:
1. Use **Amazon Certificate Manager (ACM)** to set the `certificate_arn`.
2. Register a **VPC** in the AWS console.
3. Register a **domain** in **Route 53**.
4. Ensure ACM and the load balancer are registered together with the domain in Route 53 so that traffic can flow from the frontend to the backend.

---

There are no issues with clean deployment (no apps are currently running), but re-deploying requires the following steps:
1. Login to AWS console (https://aws.amazon.com/console/)
2. Go to EC2, followed by Load Balancers, then select the 'app-alb' load balancer and remove the listener target group rule that is redirecting traffic.
3. Within EC2 again, go to 'target groups' and delete the target group created there (should be something like tg-1).
4. Go to Amplify and remove the apps deployed. This is done by clicking on the apps, going to 'App Settings', then 'General Settings', and there should be a 'Delete app' button.
5. Now you can trigger the Github Terraform pipeline (either by rerunning one in Actions or pushing an update to the 'pipeline' branch).
6. You'll see that an EC2 instance is created, the load balancer has a new target group registered as listener and Amplify has a new App. In the Amplify section, click on the App, then select pipeline, and click 'Run job'.
7. Once the job is complieted just click on the URL provided (should look like: https ://pipeline.d38wotromdgihh.amplifyapp.com/). If you get a 'Network Error', make sure to wait 10 minutes for EC2 instance to start the backend server.

---

## 🤝 Contributing

Want to contribute? Fork this repo and submit a pull request! 

---

## 📄 License

MIT License
