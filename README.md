# 🧠 Digital Therapy Assistant

A full-stack web application that provides Cognitive Behavioral Therapy (CBT) sessions using AI-driven interactions. The app includes multimodal analysis of voice, burnout assessments, AI-guided journaling, and daily check-ins.

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
```bash
brew install openjdk maven mysql
brew services start mysql
```
#### **Database Setup**
```bash
mysql -u root -p
CREATE DATABASE cbt;
USE cbt;
```
#### **Run Backend**
```bash
cd backend
mvn clean install
mvn spring-boot:run
```

---

### 2️⃣ Manual Frontend Setup (if not using setup.sh)
#### **Install Dependencies**
```bash
cd frontend
npm install
npm install tailwindcss @tailwindcss/vite daisyui
```
#### **Run Frontend**
```bash
npm run dev
```

---

## 🔥 Features

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

_Coming soon..._

---

## 🤝 Contributing

Want to contribute? Fork this repo and submit a pull request! 

---

## 📄 License

MIT License