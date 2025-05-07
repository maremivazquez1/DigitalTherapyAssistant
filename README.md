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
- AWS API 'Access' and 'Secret' keys (https://docs.aws.amazon.com/IAM/latest/UserGuide/id_credentials_access-keys.html)
- Github OAuth key, which is only needed for the deployment to pull the repo. Not needed for local runs.

Before running the backend locally, make sure to add your keys to `variables.sh` and run `source variables.sh` on the same terminal (or add them to your profile environment variables). Just be careful not to commit the filled out file with access keys!
See Deployments section for more information on how to deploy.

## ⚙️ Quick Setup Instructions

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
brew update
brew install openjdk maven mysql redis ffmpeg

# 2. Start services
brew services start mysql
brew services start redis

# 3. Configure MySQL schema
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

# 4. Build and run
cd backend
mvn clean install -Dnet.bytebuddy.experimental=true
mvn spring-boot:run
---

### 2️⃣ Manual Frontend Setup (if not using setup.sh)
#### **Install Dependencies**
cd frontend

# 1. Install Node.js if needed
brew install node

# 2. Install dependencies
npm install
npm install tailwindcss @tailwindcss/vite daisyui vite --save-dev

# 3. Run dev server
npm run dev
---

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

## 🚀 Deployment

Using Terraform to deploy on AWS. Terraform deploys backend on EC2 instance and React on Amplify with a load balancer in between to redirect HTTPs traffic from the frontend to the backend. The terraform configuration is located in the 'pipeline' branch and updates to it will trigger a pipeline to deploy.
Github contains the Terraform .yml that will need the following AWS and LLM keys:
'AWS_ACCESS_KEY_ID', 'AWS_SECRET_ACCESS_KEY', 'AWS_REGION', 'GEMINI_API_KEY' and 'HUME_API_KEY'.

Deploying requires the use of Amazon Certificate Manager (ACM).

There are no issues with initial deployment, but re-deploying requires removing the 'target' that the load balancer is targetting. This can be done by de-registering the target in the AWS load balancer page (in EC2 section) and removing the target.

---

## 🤝 Contributing

Want to contribute? Fork this repo and submit a pull request! 

---

## 📄 License

MIT License
